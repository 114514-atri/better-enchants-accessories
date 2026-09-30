package com.betterenchants.mixin;

import com.betterenchants.accessory.AccessoryInventoryView;
import com.betterenchants.accessory.AccessoryPlayerScreenHandler;
import com.betterenchants.accessory.AccessorySlot;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.PlayerScreenHandler;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.slot.Slot;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * 学 OptionalPrivate：把本模组饰品槽注入原版 {@link PlayerScreenHandler}，固定在盔甲左侧。
 * 槽位数与 {@code data/.../accessory/slots.json} 对齐（含神格/帆位，默认 12）。
 */
@Mixin(PlayerScreenHandler.class)
public abstract class PlayerScreenHandlerAccessoryMixin extends ScreenHandler implements AccessoryPlayerScreenHandler {
	/** 与 slots.json 一致；datapack 未加载时也至少 12。布局：2 列 × 6 行。 */
	private static final int ACCESSORY_COUNT = 12;
	private static final int ACCESSORY_ROWS = 6;

	@Shadow
	@Final
	private PlayerEntity owner;

	@Unique
	private int betterEnchants$accessorySlotStart;
	@Unique
	private int betterEnchants$accessorySlotEnd;

	private PlayerScreenHandlerAccessoryMixin() {
		super(null, 0);
	}

	@Inject(method = "<init>", at = @At("RETURN"))
	private void betterEnchants$addAccessorySlots(PlayerInventory inventory, boolean onServer, PlayerEntity owner, CallbackInfo ci) {
		AccessoryInventoryView view = new AccessoryInventoryView(owner);
		this.betterEnchants$accessorySlotStart = this.slots.size();
		int count = Math.max(ACCESSORY_COUNT, com.betterenchants.accessory.AccessorySlotLoader.slotCount());
		for (int i = 0; i < count; i++) {
			int column = i / ACCESSORY_ROWS;
			int row = i % ACCESSORY_ROWS;
			int x = -22 - column * 18;
			int y = 8 + row * 18;
			this.addSlot(new AccessorySlot(view, i, x, y));
		}
		this.betterEnchants$accessorySlotEnd = this.slots.size();
	}

	@Inject(method = "quickMove", at = @At("HEAD"), cancellable = true)
	private void betterEnchants$quickMoveAccessories(PlayerEntity player, int index, CallbackInfoReturnable<ItemStack> cir) {
		if (index >= this.betterEnchants$accessorySlotStart && index < this.betterEnchants$accessorySlotEnd) {
			Slot slot = this.slots.get(index);
			if (!slot.hasStack()) {
				cir.setReturnValue(ItemStack.EMPTY);
				return;
			}
			ItemStack stack = slot.getStack();
			ItemStack copy = stack.copy();
			if (!this.insertItem(stack, 9, 45, false)) {
				cir.setReturnValue(ItemStack.EMPTY);
				return;
			}
			if (stack.isEmpty()) {
				slot.setStack(ItemStack.EMPTY);
			} else {
				slot.markDirty();
			}
			cir.setReturnValue(copy);
			return;
		}

		if (index >= 9 && index < 45) {
			Slot slot = this.slots.get(index);
			if (!slot.hasStack()) {
				return;
			}
			ItemStack stack = slot.getStack();
			for (int i = this.betterEnchants$accessorySlotStart; i < this.betterEnchants$accessorySlotEnd; i++) {
				if (stack.isEmpty()) {
					break;
				}
				Slot target = this.slots.get(i);
				if (target.hasStack() || !target.canInsert(stack)) {
					continue;
				}
				this.insertItem(stack, i, i + 1, false);
			}
		}
	}

	@Override
	public int betterEnchants$getAccessorySlotStart() {
		return this.betterEnchants$accessorySlotStart;
	}

	@Override
	public int betterEnchants$getAccessorySlotEnd() {
		return this.betterEnchants$accessorySlotEnd;
	}
}
