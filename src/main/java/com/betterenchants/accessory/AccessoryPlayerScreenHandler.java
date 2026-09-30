package com.betterenchants.accessory;

/** 暴露注入到 {@link net.minecraft.screen.PlayerScreenHandler} 的饰品槽区间。 */
public interface AccessoryPlayerScreenHandler {
	int betterEnchants$getAccessorySlotStart();

	int betterEnchants$getAccessorySlotEnd();
}
