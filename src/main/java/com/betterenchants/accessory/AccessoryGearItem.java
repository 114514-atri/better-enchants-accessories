package com.betterenchants.accessory;

import java.util.List;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.tooltip.TooltipType;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

/** 带机制说明的饰品物品。 */
public class AccessoryGearItem extends Item {
	public AccessoryGearItem(Settings settings) {
		super(settings);
	}

	@Override
	public void appendTooltip(ItemStack stack, TooltipContext context, List<Text> tooltip, TooltipType type) {
		tooltip.add(Text.translatable(this.getTranslationKey() + ".effect").formatted(Formatting.GOLD));
		tooltip.add(Text.translatable("item.better_enchants.accessory.rule").formatted(Formatting.DARK_GRAY));
	}
}
