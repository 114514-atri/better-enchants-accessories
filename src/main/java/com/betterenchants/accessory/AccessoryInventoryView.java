package com.betterenchants.accessory;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.inventory.Inventory;
import net.minecraft.item.ItemStack;

/** 把玩家饰品附件暴露为 {@link Inventory}，供 ScreenHandler 使用。每次读写都走当前附件，避免持有过期引用。 */
public final class AccessoryInventoryView implements Inventory {
	private final PlayerEntity player;

	public AccessoryInventoryView(PlayerEntity player) {
		this.player = player;
	}

	private PlayerAccessoryInventory data() {
		return Accessories.get(player);
	}

	private void persist(PlayerAccessoryInventory data) {
		Accessories.set(player, data);
	}

	@Override
	public int size() {
		return Math.max(12, Math.max(data().size(), AccessorySlotLoader.slotCount()));
	}

	@Override
	public boolean isEmpty() {
		PlayerAccessoryInventory data = data();
		for (int i = 0; i < data.size(); i++) {
			if (!data.getStack(i).isEmpty()) {
				return false;
			}
		}
		return true;
	}

	@Override
	public ItemStack getStack(int slot) {
		return data().getStack(slot);
	}

	@Override
	public ItemStack removeStack(int slot, int amount) {
		PlayerAccessoryInventory data = data();
		ItemStack stack = data.getStack(slot);
		if (stack.isEmpty() || amount <= 0) {
			return ItemStack.EMPTY;
		}
		ItemStack split = stack.split(amount);
		data.setStack(slot, stack);
		persist(data);
		return split;
	}

	@Override
	public ItemStack removeStack(int slot) {
		PlayerAccessoryInventory data = data();
		ItemStack stack = data.getStack(slot);
		data.setStack(slot, ItemStack.EMPTY);
		persist(data);
		return stack;
	}

	@Override
	public void setStack(int slot, ItemStack stack) {
		PlayerAccessoryInventory data = data();
		data.setStack(slot, stack);
		persist(data);
	}

	@Override
	public void markDirty() {
		persist(data());
	}

	@Override
	public boolean canPlayerUse(PlayerEntity player) {
		return player == this.player && !player.isRemoved();
	}

	@Override
	public void clear() {
		PlayerAccessoryInventory data = data();
		for (int i = 0; i < data.size(); i++) {
			data.setStack(i, ItemStack.EMPTY);
		}
		persist(data);
	}

	@Override
	public boolean isValid(int slot, ItemStack stack) {
		return PlayerAccessoryInventory.canAccept(slot, stack);
	}
}
