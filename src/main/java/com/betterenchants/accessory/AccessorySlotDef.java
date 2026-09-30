package com.betterenchants.accessory;

import com.betterenchants.BetterEnchantsMod;
import net.minecraft.util.Identifier;

/**
 * 单条饰品槽定义（由数据包 {@code data/better_enchants/accessory/slots.json} 加载）。
 */
public record AccessorySlotDef(Identifier id, int order, Identifier icon, Identifier allowedTag) {
	public Identifier translationKeyId() {
		return id;
	}

	public String translationKey() {
		return "accessory.better_enchants.slot." + id.getPath();
	}

	public static Identifier slotId(String path) {
		return BetterEnchantsMod.id(path);
	}
}
