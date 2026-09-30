package com.betterenchants.accessory;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.inventory.Inventory;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.slot.Slot;

/** 注入到原版物品栏的本模组饰品槽。 */
public class AccessorySlot extends Slot {
	private final int accessoryIndex;

	public AccessorySlot(Inventory inventory, int index, int x, int y) {
		super(inventory, index, x, y);
		this.accessoryIndex = index;
	}

	public int getAccessoryIndex() {
		return accessoryIndex;
	}

	@Override
	public boolean canInsert(ItemStack stack) {
		return PlayerAccessoryInventory.canAccept(accessoryIndex, stack);
	}

	@Override
	public int getMaxItemCount() {
		return 1;
	}

	@Override
	public boolean isEnabled() {
		if (this.x >= 0) {
			return true;
		}
		return AccessorySlotVisibility.isLeftColumnVisible();
	}

	@Override
	public boolean canTakeItems(PlayerEntity playerEntity) {
		return true;
	}
}
