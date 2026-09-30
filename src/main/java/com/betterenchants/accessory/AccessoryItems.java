package com.betterenchants.accessory;

import com.betterenchants.BetterEnchantsMod;
import java.util.List;
import net.minecraft.item.Item;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.util.Rarity;

/** 本模组饰品物品（背饰用鞘翅，不在此注册）。 */
public final class AccessoryItems {
	public static final Item HEAD = item("accessory_head");
	public static final Item FACE = item("accessory_face");
	public static final Item NECKLACE = item("accessory_necklace");
	public static final Item RING = item("accessory_ring");
	public static final Item BRACELET = item("accessory_bracelet");
	public static final Item BELT = item("accessory_belt");
	public static final Item CHARM = item("accessory_charm");
	public static final Item AURA = item("accessory_aura");
	public static final Item SHIELD = item("accessory_shield");

	/** 可掉落/有效果的饰品（不含鞘翅）。 */
	private static final List<Item> EFFECT_ITEMS = List.of(
			HEAD, FACE, NECKLACE, RING, BRACELET, BELT, CHARM, AURA, SHIELD
	);

	private static final List<Item> ALL = List.of(
			HEAD, FACE, NECKLACE, RING, BRACELET, BELT, CHARM, AURA, SHIELD
	);

	private AccessoryItems() {
	}

	public static void register() {
		/* 静态字段注册即可 */
	}

	public static List<Item> all() {
		return ALL;
	}

	public static List<Item> effectItems() {
		return EFFECT_ITEMS;
	}

	private static Item item(String id) {
		return Registry.register(
				Registries.ITEM,
				BetterEnchantsMod.id(id),
				new AccessoryGearItem(new Item.Settings().maxCount(1).rarity(Rarity.EPIC))
		);
	}
}
