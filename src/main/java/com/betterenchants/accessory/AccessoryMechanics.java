package com.betterenchants.accessory;

import com.betterenchants.BetterEnchantsMod;
import com.betterenchants.challenge.MobAffixHolder;
import com.betterenchants.compat.OptionalPrivateHooks;
import com.betterenchants.registry.ModDamageTypes;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.block.BlockState;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.attribute.EntityAttribute;
import net.minecraft.entity.attribute.EntityAttributeInstance;
import net.minecraft.entity.attribute.EntityAttributeModifier;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.entity.effect.StatusEffectCategory;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.registry.tag.BlockTags;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Identifier;

/**
 * 饰品强化机制（按设计表）：属性 / 护盾 / 强制伤害 / 免疫 / 飞行等。
 */
public final class AccessoryMechanics {
	private static final Identifier ARMOR = BetterEnchantsMod.id("accessory_armor");
	private static final Identifier TOUGHNESS = BetterEnchantsMod.id("accessory_toughness");
	private static final Identifier ATTACK = BetterEnchantsMod.id("accessory_attack");
	private static final Identifier ATTACK_TOTAL = BetterEnchantsMod.id("accessory_attack_total");
	private static final Identifier ATTACK_SPEED = BetterEnchantsMod.id("accessory_attack_speed");
	private static final Identifier SPEED = BetterEnchantsMod.id("accessory_speed");
	private static final Identifier HEALTH = BetterEnchantsMod.id("accessory_health");
	private static final Identifier HEALTH_TOTAL = BetterEnchantsMod.id("accessory_health_total");
	private static final Identifier LUCK = BetterEnchantsMod.id("accessory_luck");
	private static final Identifier KNOCKBACK = BetterEnchantsMod.id("accessory_knockback");

	private static final int MINUTE = 20 * 60;
	private static final int REWIND_WINDOW = 20 * 10;
	private static final int INVULN_PERIOD = 20 * 60;
	private static final int INVULN_DURATION = 20 * 5;
	private static final float NEAR_DEATH = 0.25f;

	private static final Map<UUID, Integer> ATTR_FINGERPRINT = new HashMap<>();
	private static final Map<UUID, Boolean> BELT_FLIGHT = new HashMap<>();
	private static final Map<UUID, Deque<Float>> HP_HISTORY = new HashMap<>();
	private static final Map<UUID, Integer> DAMAGE_CAP_TICK = new HashMap<>();
	private static final Map<UUID, Float> DAMAGE_CAP_USED = new HashMap<>();
	private static final ThreadLocal<Boolean> RING_FORCE = ThreadLocal.withInitial(() -> false);
	/** 防止 afterOutgoingHit 内再 damage 触发自身递归（蓝宝斩杀）。 */
	private static final ThreadLocal<Boolean> POST_HIT = ThreadLocal.withInitial(() -> false);

	private AccessoryMechanics() {
	}

	public static void register() {
		ServerTickEvents.END_WORLD_TICK.register(world -> {
			if (world.isClient()) {
				return;
			}
			for (ServerPlayerEntity player : world.getPlayers()) {
				if (player.isSpectator()) {
					clearBeltFlight(player);
					continue;
				}
				Set<String> ids = equippedIds(player);
				syncAttributes(player, ids);
				tickBelt(player, ids);
				tickPotions(player, ids);
				tickShieldGrants(player, ids);
				tickAuraHeal(player, ids);
				tickCharm(player, ids);
				tickNecklaceScarlet(player, ids);
			}
			pruneMaps(world);
		});
	}

	public static Set<String> equippedIds(PlayerEntity player) {
		Set<String> ids = new HashSet<>();
		PlayerAccessoryInventory inv = Accessories.get(player);
		for (int i = 0; i < inv.size(); i++) {
			ItemStack stack = inv.getStack(i);
			if (stack.isEmpty() || !(stack.getItem() instanceof AccessoryGearItem)) {
				continue;
			}
			String path = Registries.ITEM.getId(stack.getItem()).getPath();
			if (path.startsWith("accessory_")) {
				ids.add(path.substring("accessory_".length()));
			}
		}
		return ids;
	}

	public static boolean has(PlayerEntity player, String id) {
		return equippedIds(player).contains(id);
	}

	public static boolean isRingForce(DamageSource source) {
		return source != null && source.isOf(ModDamageTypes.ACCESSORY_RING_FORCE);
	}

	public static boolean isRingForceContext() {
		return Boolean.TRUE.equals(RING_FORCE.get());
	}

	/** 冠环：免疫真伤判定（不含强制真伤 / 终焉）。 */
	public static boolean isTrueDamageSource(DamageSource source, LivingEntity attacker) {
		if (source == null || com.betterenchants.combat.DamageKinds.isForceOrEnding(source)) {
			return false;
		}
		return com.betterenchants.combat.DamageKinds.isTrueFamily(source)
				|| (attacker != null && MobAffixHolder.has(attacker, com.betterenchants.challenge.MobAffix.TRUE_DAMAGE));
	}

	/** 伤害入口取消：冠环微伤/真伤、护符周期无敌（不挡强制/终焉）。 */
	public static boolean tryCancelIncoming(PlayerEntity player, DamageSource source, float amount) {
		if (!com.betterenchants.combat.DamageKinds.canImmune(source)) {
			return false;
		}
		Set<String> ids = equippedIds(player);
		if (ids.contains("head")) {
			if (isTrueDamageSource(source, source.getAttacker() instanceof LivingEntity la ? la : null)) {
				return true;
			}
			if (amount > 0.0f && amount <= player.getMaxHealth() * 0.05f) {
				return true;
			}
		}
		if (ids.contains("charm") && isCharmInvulnerable(player) && !isRingForce(source)) {
			return true;
		}
		return false;
	}

	public static void syncAttributes(PlayerEntity player) {
		syncAttributes(player, equippedIds(player));
	}

	private static void syncAttributes(PlayerEntity player, Set<String> ids) {
		int fingerprint = ids.hashCode();
		UUID uuid = player.getUuid();
		Integer prev = ATTR_FINGERPRINT.get(uuid);
		if (prev != null && prev == fingerprint) {
			return;
		}
		ATTR_FINGERPRINT.put(uuid, fingerprint);

		double armor = 0, toughness = 0, luck = 0, knockback = 0;
		double attackBase = 0, attackSpeed = 0, speed = 0;
		double healthBase = 0, healthTotal = 0, attackTotal = 0;

		if (ids.contains("face")) {
			luck = Math.max(luck, 5.0);
		}
		if (ids.contains("necklace") || ids.contains("bracelet")) {
			healthBase = Math.max(healthBase, 5.0); // +500%
		}
		if (ids.contains("ring")) {
			attackBase = Math.max(attackBase, 1.80);
			attackSpeed = Math.max(attackSpeed, 0.12);
		}
		if (ids.contains("charm")) {
			healthTotal = Math.max(healthTotal, 1.0); // ×2
			attackTotal = Math.max(attackTotal, 1.0); // ×2
		}

		applyAdd(player, EntityAttributes.GENERIC_ARMOR, ARMOR, armor);
		applyAdd(player, EntityAttributes.GENERIC_ARMOR_TOUGHNESS, TOUGHNESS, toughness);
		applyMult(player, EntityAttributes.GENERIC_ATTACK_DAMAGE, ATTACK, attackBase);
		applyTotal(player, EntityAttributes.GENERIC_ATTACK_DAMAGE, ATTACK_TOTAL, attackTotal);
		applyMult(player, EntityAttributes.GENERIC_ATTACK_SPEED, ATTACK_SPEED, attackSpeed);
		applyMult(player, EntityAttributes.GENERIC_MOVEMENT_SPEED, SPEED, speed);
		applyMult(player, EntityAttributes.GENERIC_MAX_HEALTH, HEALTH, healthBase);
		applyTotal(player, EntityAttributes.GENERIC_MAX_HEALTH, HEALTH_TOTAL, healthTotal);
		applyAdd(player, EntityAttributes.GENERIC_LUCK, LUCK, luck);
		applyAdd(player, EntityAttributes.GENERIC_KNOCKBACK_RESISTANCE, KNOCKBACK, knockback);

		if (player.getHealth() > player.getMaxHealth()) {
			player.setHealth(player.getMaxHealth());
		}
	}

	private static void tickPotions(ServerPlayerEntity player, Set<String> ids) {
		if (player.age % 40 != 0) {
			return;
		}
		if (ids.contains("face")) {
			player.addStatusEffect(new StatusEffectInstance(StatusEffects.NIGHT_VISION, 320, 0, true, false, true));
		}
	}

	private static void tickBelt(ServerPlayerEntity player, Set<String> ids) {
		boolean hasBelt = ids.contains("belt");
		Boolean was = BELT_FLIGHT.get(player.getUuid());
		if (hasBelt) {
			player.getHungerManager().setFoodLevel(20);
			player.getHungerManager().setSaturationLevel(20.0f);
		}
		if (flightOk) {
			player.fallDistance = 0.0f;
			// 只开「可飞」权限，等同创造：双击空格切换飞行/行走，绝不每 tick 强制 flying
			if (!player.getAbilities().allowFlying) {
				player.getAbilities().allowFlying = true;
				player.sendAbilitiesUpdate();
			}
			BELT_FLIGHT.put(player.getUuid(), true);
		} else if (Boolean.TRUE.equals(was)) {
			clearBeltFlight(player);
		}
	}

	private static void clearBeltFlight(ServerPlayerEntity player) {
		BELT_FLIGHT.remove(player.getUuid());
		if (player.isCreative() || player.isSpectator()) {
			return;
		}
		if (com.betterenchants.combat.SwordFlightHandler.isActive(player.getUuid())) {
			return;
		}
		player.getAbilities().allowFlying = false;
		player.getAbilities().flying = false;
		player.sendAbilitiesUpdate();
	}

	private static void tickShieldGrants(ServerPlayerEntity player, Set<String> ids) {
		if (player.age % MINUTE != 0) {
			return;
		}
		boolean necklace = ids.contains("necklace");
		boolean bracelet = ids.contains("bracelet");
		boolean shield = ids.contains("shield");
		if (!necklace && !bracelet && !shield) {
			return;
		}
		float charm = ids.contains("charm") ? 2.0f : 1.0f;
		float maxHp = Math.max(1.0f, player.getMaxHealth());
		float abs = player.getAbsorptionAmount();
		if (necklace) {
			abs = 2.0f * maxHp * charm; // 强制覆盖
		}
		if (bracelet) {
			abs += 1.0f * maxHp * charm;
		}
		if (shield) {
			abs += 5.0f * maxHp * charm;
		}
		player.setAbsorptionAmount(Math.max(0.0f, abs));
	}

	private static void tickAuraHeal(ServerPlayerEntity player, Set<String> ids) {
		if (!ids.contains("aura")) {
			return;
		}
		float heal = player.getMaxHealth() * 0.025f;
		if (heal > 0.0f) {
			player.heal(heal);
		}
	}

	private static void tickCharm(ServerPlayerEntity player, Set<String> ids) {
		UUID id = player.getUuid();
		Deque<Float> hist = HP_HISTORY.computeIfAbsent(id, u -> new ArrayDeque<>(REWIND_WINDOW + 2));
		hist.addLast(player.getHealth());
		while (hist.size() > REWIND_WINDOW) {
			hist.removeFirst();
		}
		if (!ids.contains("charm")) {
			return;
		}
		if (player.age % REWIND_WINDOW != 0 || hist.size() < REWIND_WINDOW) {
			return;
		}
		float maxHp = Math.max(1.0f, player.getMaxHealth());
		if (player.getHealth() > maxHp * NEAR_DEATH) {
			return;
		}
		float past = hist.peekFirst();
		float next = Math.max(player.getHealth(), past);
		if (next > player.getHealth()) {
			player.setHealth(Math.min(maxHp, next));
		}
	}

	private static boolean isCharmInvulnerable(PlayerEntity player) {
		int cycle = Math.floorMod(player.age, INVULN_PERIOD);
		return cycle < INVULN_DURATION;
	}

	private static void tickNecklaceScarlet(ServerPlayerEntity player, Set<String> ids) {
		if (!ids.contains("necklace") || !OptionalPrivateHooks.isInScarletRealm(player.getWorld())) {
			return;
		}
		if (player.age % 10 != 0) {
			return;
		}
		List<RegistryEntry<StatusEffect>> remove = new ArrayList<>();
		for (StatusEffectInstance inst : player.getStatusEffects()) {
			if (inst.getEffectType().value().getCategory() == StatusEffectCategory.HARMFUL) {
				remove.add(inst.getEffectType());
			}
		}
		for (RegistryEntry<StatusEffect> e : remove) {
			player.removeStatusEffect(e);
		}
		player.setAttached(com.betterenchants.registry.ModAttachments.BLESSING_MARKS,
				com.betterenchants.blessing.BlessingMarkData.EMPTY);
	}

	/** 绯境紫晶：免疫祝福焰/霜叠层与虚空引爆。 */
	public static boolean blocksBlessingMarks(LivingEntity target) {
		return target instanceof PlayerEntity player
				&& has(player, "necklace")
				&& OptionalPrivateHooks.isInScarletRealm(player.getWorld());
	}

	/** 蓝宝：免疫词条反伤。 */
	public static boolean blocksAffixReflect(LivingEntity attacker) {
		return attacker instanceof PlayerEntity player && has(player, "bracelet");
	}

	/** 蓝宝：免疫词条施加的负面。 */
	public static boolean blocksAffixDebuff(LivingEntity target) {
		return target instanceof PlayerEntity player && has(player, "bracelet");
	}

	public static float modifyOutgoing(PlayerEntity attacker, LivingEntity target, float amount) {
		Set<String> ids = equippedIds(attacker);
		float mult = 1.0f;
		if (ids.contains("head")) {
			mult *= 6.0f; // +500%
		}
		if (ids.contains("ring") && !MobAffixHolder.get(target).isEmpty()) {
			mult *= 1.0f + MobAffixHolder.get(target).size() * 0.25f;
		}
		if (ids.contains("aura") && !MobAffixHolder.get(target).isEmpty()) {
			mult *= 1.8f;
		}
		return amount * mult;
	}

	public static float affixPierce(PlayerEntity attacker) {
		return 0.0f;
	}

	public static float modifyIncoming(PlayerEntity player, DamageSource source, float amount) {
		if (amount <= 0.0f || isRingForce(source)) {
			return amount;
		}
		Set<String> ids = equippedIds(player);
		float out = amount;

		if (ids.contains("aura") && source.getAttacker() instanceof LivingEntity attacker
				&& !MobAffixHolder.get(attacker).isEmpty()) {
			out *= 0.10f;
		}

		if (ids.contains("shield")) {
			out = applyPerTickDamageCap(player, out);
		}
		return out;
	}

	private static float applyPerTickDamageCap(PlayerEntity player, float amount) {
		if (!(player.getWorld() instanceof ServerWorld world)) {
			return amount;
		}
		UUID id = player.getUuid();
		int tick = world.getServer().getTicks();
		Integer last = DAMAGE_CAP_TICK.get(id);
		float used = 0.0f;
		if (last != null && last == tick) {
			used = DAMAGE_CAP_USED.getOrDefault(id, 0.0f);
		} else {
			DAMAGE_CAP_TICK.put(id, tick);
		}
		float cap = Math.max(1.0f, player.getMaxHealth()) * 0.10f;
		float remain = Math.max(0.0f, cap - used);
		float deal = Math.min(amount, remain);
		DAMAGE_CAP_USED.put(id, used + deal);
		return deal;
	}

	/** 金戒强制 % 最大生命伤 + 蓝宝斩杀。 */
	public static void afterOutgoingHit(ServerPlayerEntity attacker, LivingEntity target, DamageSource source, float amount) {
		if (amount <= 0.0f || !target.isAlive()
				|| isRingForce(source)
				|| source.isOf(ModDamageTypes.EXECUTION)
				|| Boolean.TRUE.equals(RING_FORCE.get())
				|| Boolean.TRUE.equals(POST_HIT.get())) {
			return;
		}
		POST_HIT.set(true);
		try {
			Set<String> ids = equippedIds(attacker);
			if (ids.contains("ring") && target.getWorld() instanceof ServerWorld world) {
				float force = Math.max(0.01f, target.getMaxHealth() * 0.01f);
				RING_FORCE.set(true);
				try {
					target.timeUntilRegen = 0;
					target.hurtTime = 0;
					DamageSource forced = world.getDamageSources().create(ModDamageTypes.ACCESSORY_RING_FORCE, attacker);
					target.damage(forced, force);
				} finally {
					RING_FORCE.set(false);
				}
			}
			if (ids.contains("bracelet") && target.isAlive()) {
				float max = Math.max(1.0f, target.getMaxHealth());
				if (target.getHealth() / max < 0.10f) {
					target.timeUntilRegen = 0;
					target.hurtTime = 0;
					target.damage(attacker.getDamageSources().create(ModDamageTypes.EXECUTION, attacker),
							Math.max(target.getHealth() + target.getAbsorptionAmount() + 1.0f, max * 2.0f));
				}
			}
		} finally {
			POST_HIT.set(false);
		}
	}

	public static float xpMultiplier(PlayerEntity player) {
		return has(player, "face") ? 4.0f : 1.0f;
	}

	public static float exhaustionMultiplier(PlayerEntity player) {
		return has(player, "belt") ? 0.0f : 1.0f;
	}

	/** 金边眼镜：矿物额外掉落 ×2（总量约 ×3）。 */
	public static List<ItemStack> applyOreExtraDrops(List<ItemStack> drops, PlayerEntity player, BlockState state) {
		if (drops == null || drops.isEmpty() || player == null || !has(player, "face") || !isOre(state)) {
			return drops;
		}
		List<ItemStack> out = new ArrayList<>(drops.size() * 2);
		out.addAll(drops);
		for (ItemStack stack : drops) {
			if (stack.isEmpty()) {
				continue;
			}
			ItemStack extra = stack.copy();
			extra.setCount(stack.getCount() * 2);
			out.add(extra);
		}
		return out;
	}

	private static boolean isOre(BlockState state) {
		return state.isIn(BlockTags.COAL_ORES)
				|| state.isIn(BlockTags.COPPER_ORES)
				|| state.isIn(BlockTags.IRON_ORES)
				|| state.isIn(BlockTags.GOLD_ORES)
				|| state.isIn(BlockTags.DIAMOND_ORES)
				|| state.isIn(BlockTags.EMERALD_ORES)
				|| state.isIn(BlockTags.LAPIS_ORES)
				|| state.isIn(BlockTags.REDSTONE_ORES);
	}

	private static void pruneMaps(ServerWorld world) {
		if (world.getServer().getTicks() % 200 != 0) {
			return;
		}
		Set<UUID> online = new HashSet<>();
		for (ServerPlayerEntity p : world.getServer().getPlayerManager().getPlayerList()) {
			online.add(p.getUuid());
		}
		ATTR_FINGERPRINT.keySet().removeIf(u -> !online.contains(u));
		BELT_FLIGHT.keySet().removeIf(u -> !online.contains(u));
		HP_HISTORY.keySet().removeIf(u -> !online.contains(u));
		DAMAGE_CAP_TICK.keySet().removeIf(u -> !online.contains(u));
		DAMAGE_CAP_USED.keySet().removeIf(u -> !online.contains(u));
	}

	private static void applyAdd(PlayerEntity player, RegistryEntry<EntityAttribute> attr, Identifier id, double value) {
		apply(player, attr, id, value, EntityAttributeModifier.Operation.ADD_VALUE);
	}

	private static void applyMult(PlayerEntity player, RegistryEntry<EntityAttribute> attr, Identifier id, double value) {
		apply(player, attr, id, value, EntityAttributeModifier.Operation.ADD_MULTIPLIED_BASE);
	}

	private static void applyTotal(PlayerEntity player, RegistryEntry<EntityAttribute> attr, Identifier id, double value) {
		apply(player, attr, id, value, EntityAttributeModifier.Operation.ADD_MULTIPLIED_TOTAL);
	}

	private static void apply(
			PlayerEntity player,
			RegistryEntry<EntityAttribute> attrKey,
			Identifier id,
			double value,
			EntityAttributeModifier.Operation op
	) {
		EntityAttributeInstance attr = player.getAttributeInstance(attrKey);
		if (attr == null) {
			return;
		}
		boolean active = value > 1.0E-6;
		EntityAttributeModifier existing = attr.getModifier(id);
		if (!active) {
			if (existing != null) {
				attr.removeModifier(id);
			}
			return;
		}
		if (existing == null || existing.operation() != op || Math.abs(existing.value() - value) > 1.0E-6) {
			attr.removeModifier(id);
			attr.addTemporaryModifier(new EntityAttributeModifier(id, value, op));
		}
	}
}
