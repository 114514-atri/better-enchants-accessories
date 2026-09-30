package com.betterenchants.accessory;

import com.betterenchants.BetterEnchantsMod;
import com.betterenchants.registry.ModItems;
import net.fabricmc.fabric.api.entity.event.v1.EntityElytraEvents;
import net.fabricmc.fabric.api.event.player.UseItemCallback;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ElytraItem;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.tag.TagKey;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.Identifier;
import net.minecraft.util.TypedActionResult;
import net.minecraft.world.event.GameEvent;

/** 饰品右键快装、背饰鞘翅滑翔。 */
public final class AccessoryGameplay {
	private static final Identifier BACK_ID = BetterEnchantsMod.id("back");

	private AccessoryGameplay() {
	}

	public static void register() {
		UseItemCallback.EVENT.register((player, world, hand) -> {
			ItemStack stack = player.getStackInHand(hand);
			if (!isAccessoryCandidate(stack)) {
				return TypedActionResult.pass(stack);
			}
			if (world.isClient()) {
				return canEquipSomewhere(player, stack)
						? TypedActionResult.success(stack)
						: TypedActionResult.pass(stack);
			}
			if (tryEquip(player, stack)) {
				return TypedActionResult.success(stack);
			}
			return TypedActionResult.pass(stack);
		});

		EntityElytraEvents.CUSTOM.register(AccessoryGameplay::useBackElytra);
	}

	public static int backSlotIndex() {
		var slots = AccessorySlotLoader.slots();
		for (int i = 0; i < slots.size(); i++) {
			if (slots.get(i).id().equals(BACK_ID)) {
				return i;
			}
		}
		return 3;
	}

	public static ItemStack getBackStack(PlayerEntity player) {
		return Accessories.get(player).getStack(backSlotIndex());
	}

	public static boolean isElytraStack(ItemStack stack) {
		return !stack.isEmpty()
				&& (stack.isOf(Items.ELYTRA) || stack.isOf(ModItems.AMETHYST_ELYTRA) || stack.getItem() instanceof ElytraItem);
	}

	/** 胸部没有可用鞘翅时，用背饰鞘翅参与渲染/飞行判定。 */
	public static ItemStack effectiveElytra(LivingEntity entity) {
		ItemStack chest = entity.getEquippedStack(EquipmentSlot.CHEST);
		if (isElytraStack(chest) && ElytraItem.isUsable(chest)) {
			return chest;
		}
		if (entity instanceof PlayerEntity player) {
			ItemStack back = getBackStack(player);
			if (isElytraStack(back) && ElytraItem.isUsable(back)) {
				return back;
			}
		}
		return ItemStack.EMPTY;
	}

	private static boolean isAccessoryCandidate(ItemStack stack) {
		if (stack.isEmpty()) {
			return false;
		}
		TagKey<net.minecraft.item.Item> any = TagKey.of(RegistryKeys.ITEM, AccessorySlotLoader.requireTag());
		return stack.isIn(any);
	}

	private static boolean canEquipSomewhere(PlayerEntity player, ItemStack stack) {
		PlayerAccessoryInventory inv = Accessories.get(player);
		for (int i = 0; i < inv.size(); i++) {
			if (inv.getStack(i).isEmpty() && PlayerAccessoryInventory.canAccept(i, stack)) {
				return true;
			}
		}
		return false;
	}

	public static boolean tryEquip(PlayerEntity player, ItemStack stack) {
		if (stack.isEmpty() || !isAccessoryCandidate(stack)) {
			return false;
		}
		PlayerAccessoryInventory inv = Accessories.get(player);
		for (int i = 0; i < inv.size(); i++) {
			if (!inv.getStack(i).isEmpty()) {
				continue;
			}
			if (!PlayerAccessoryInventory.canAccept(i, stack)) {
				continue;
			}
			inv.setStack(i, stack.copyWithCount(1));
			Accessories.set(player, inv);
			stack.decrement(1);
			player.playSound(SoundEvents.ITEM_ARMOR_EQUIP_GENERIC.value(), 1.0f, 1.0f);
			player.playerScreenHandler.sendContentUpdates();
			return true;
		}
		return false;
	}

	private static boolean useBackElytra(LivingEntity entity, boolean tickElytra) {
		if (!(entity instanceof PlayerEntity player)) {
			return false;
		}
		ItemStack chest = player.getEquippedStack(EquipmentSlot.CHEST);
		if (isElytraStack(chest) && ElytraItem.isUsable(chest)) {
			/* 胸部已有可用鞘翅：交给原版 / FabricElytraItem */
			return false;
		}
		int backIndex = backSlotIndex();
		PlayerAccessoryInventory inv = Accessories.get(player);
		ItemStack back = inv.getStack(backIndex);
		if (!isElytraStack(back) || !ElytraItem.isUsable(back)) {
			return false;
		}
		if (tickElytra) {
			tickBackElytra(player, inv, backIndex, back);
		}
		return true;
	}

	private static void tickBackElytra(PlayerEntity player, PlayerAccessoryInventory inv, int backIndex, ItemStack back) {
		if (!(player.getWorld() instanceof ServerWorld serverWorld)) {
			return;
		}
		int nextRoll = player.getFallFlyingTicks() + 1;
		if (nextRoll % 10 != 0) {
			return;
		}
		if ((nextRoll / 10) % 2 == 0) {
			ServerPlayerEntity serverPlayer = player instanceof ServerPlayerEntity sp ? sp : null;
			back.damage(1, serverWorld, serverPlayer, broken -> {
				inv.setStack(backIndex, ItemStack.EMPTY);
				Accessories.set(player, inv);
				player.playerScreenHandler.sendContentUpdates();
			});
			/* 耐久变化写回附件以触发同步 */
			if (!back.isEmpty()) {
				inv.setStack(backIndex, back.copy());
				Accessories.set(player, inv);
			}
		}
		player.emitGameEvent(GameEvent.ELYTRA_GLIDE);
	}
}
