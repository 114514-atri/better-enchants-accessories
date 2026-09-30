package com.betterenchants.accessory;

import java.util.function.BooleanSupplier;

/** 客户端可覆盖：配方书打开时隐藏 GUI 左侧（x&lt;0）饰品槽。 */
public final class AccessorySlotVisibility {
	private static BooleanSupplier leftColumnVisible = () -> true;

	private AccessorySlotVisibility() {
	}

	public static void setLeftColumnVisible(BooleanSupplier supplier) {
		leftColumnVisible = supplier != null ? supplier : () -> true;
	}

	public static boolean isLeftColumnVisible() {
		return leftColumnVisible.getAsBoolean();
	}
}
