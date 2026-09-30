package com.betterenchants.loot;

import com.betterenchants.BetterEnchantsMod;
import com.betterenchants.accessory.AccessoryItems;
import java.util.Set;
import net.fabricmc.fabric.api.loot.v3.LootTableEvents;
import net.minecraft.item.Item;
import net.minecraft.loot.LootPool;
import net.minecraft.loot.LootTables;
import net.minecraft.loot.condition.RandomChanceLootCondition;
import net.minecraft.loot.entry.ItemEntry;
import net.minecraft.loot.provider.number.ConstantLootNumberProvider;
import net.minecraft.registry.RegistryKey;
import net.minecraft.loot.LootTable;

/**
 * 饰品掉落与传说附魔同档：仅远古城市 / 末地城，概率对齐 Silence trim 公式。
 */
public final class AccessoryLoot {
	private static final float SILENCE_TRIM = 0.0125f;
	private static final float CHANCE_ANCIENT = SILENCE_TRIM * 0.5f;
	private static final float CHANCE_END = SILENCE_TRIM * (2.0f / 3.0f);

	private static final Set<RegistryKey<LootTable>> ANCIENT = Set.of(LootTables.ANCIENT_CITY_CHEST);
	private static final Set<RegistryKey<LootTable>> END = Set.of(LootTables.END_CITY_TREASURE_CHEST);

	private AccessoryLoot() {
	}

	public static void register() {
		LootTableEvents.MODIFY.register((key, tableBuilder, source, registries) -> {
			if (!source.isBuiltin()) {
				return;
			}
			if (ANCIENT.contains(key)) {
				addPool(tableBuilder, CHANCE_ANCIENT);
			}
			if (END.contains(key)) {
				addPool(tableBuilder, CHANCE_END);
			}
		});
		BetterEnchantsMod.LOGGER.info(
				"Accessory chest loot: ancient ~{}, end ~{} (legend-tier).",
				CHANCE_ANCIENT, CHANCE_END
		);
	}

	private static void addPool(LootTable.Builder tableBuilder, float chance) {
		LootPool.Builder pool = LootPool.builder()
				.rolls(ConstantLootNumberProvider.create(1))
				.conditionally(RandomChanceLootCondition.builder(chance));
		for (Item item : AccessoryItems.effectItems()) {
			pool.with(ItemEntry.builder(item)
					.weight(1)
					.conditionally(SealedAllowLootCondition.builder()));
		}
		tableBuilder.pool(pool);
	}
}
