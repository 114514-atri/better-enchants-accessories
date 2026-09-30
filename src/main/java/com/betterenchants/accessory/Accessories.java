package com.betterenchants.accessory;

import com.betterenchants.registry.ModAttachments;
import net.minecraft.entity.player.PlayerEntity;

/** 本模组饰品栏对外入口（不接 OptionalPrivate）。 */
public final class Accessories {
	private Accessories() {
	}

	public static PlayerAccessoryInventory get(PlayerEntity player) {
		return player.getAttachedOrCreate(ModAttachments.PLAYER_ACCESSORIES);
	}

	public static void set(PlayerEntity player, PlayerAccessoryInventory inventory) {
		player.setAttached(ModAttachments.PLAYER_ACCESSORIES, inventory);
	}
}
