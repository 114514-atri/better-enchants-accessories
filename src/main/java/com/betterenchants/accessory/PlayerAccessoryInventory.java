package com.betterenchants.accessory;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.registry.Registries;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.tag.TagKey;
import net.minecraft.util.Identifier;

/**
 * 玩家本模组饰品栏。槽位顺序 = {@link AccessorySlotLoader#slots()}。
 * 仅接受本 mod 物品（命名空间 + 标签）；不接入 OptionalPrivate。
 */
public final class PlayerAccessoryInventory {
	public static final Codec<PlayerAccessoryInventory> CODEC = RecordCodecBuilder.create(instance -> instance.group(
			Codec.list(ItemStack.OPTIONAL_CODEC).fieldOf("items").forGetter(inv -> List.copyOf(inv.items))
	).apply(instance, PlayerAccessoryInventory::new));

	public static final PacketCodec<RegistryByteBuf, PlayerAccessoryInventory> PACKET_CODEC =
			ItemStack.OPTIONAL_LIST_PACKET_CODEC.xmap(PlayerAccessoryInventory::new, inv -> List.copyOf(inv.items));

	private final List<ItemStack> items;

	public PlayerAccessoryInventory(List<ItemStack> items) {
		int size = Math.max(12, AccessorySlotLoader.slotCount());
		this.items = new ArrayList<>(size);
		for (int i = 0; i < size; i++) {
			ItemStack stack = i < items.size() && items.get(i) != null ? items.get(i) : ItemStack.EMPTY;
			this.items.add(stack.isEmpty() ? ItemStack.EMPTY : stack.copy());
		}
	}

	public static PlayerAccessoryInventory createDefault() {
		return new PlayerAccessoryInventory(List.of());
	}

	public int size() {
		return items.size();
	}

	public ItemStack getStack(int index) {
		if (index < 0 || index >= items.size()) {
			return ItemStack.EMPTY;
		}
		return items.get(index);
	}

	public void setStack(int index, ItemStack stack) {
		if (index < 0 || index >= items.size()) {
			return;
		}
		items.set(index, stack == null || stack.isEmpty() ? ItemStack.EMPTY : stack.copy());
	}

	/** 标签允许列表为主；reject_other_mods 时仍允许本 mod 与 minecraft（原版鞘翅）。 */
	public static boolean canAccept(int slotIndex, ItemStack stack) {
		if (stack == null || stack.isEmpty()) {
			return false;
		}
		AccessorySlotDef slot = AccessorySlotLoader.byIndex(slotIndex).orElse(null);
		if (slot == null) {
			return false;
		}
		Identifier itemId = Registries.ITEM.getId(stack.getItem());
		if (AccessorySlotLoader.rejectOtherMods()) {
			String ns = itemId.getNamespace();
			if (!ns.equals(AccessorySlotLoader.namespaceOnly()) && !ns.equals("minecraft")) {
				return false;
			}
		}
		TagKey<Item> anyTag = TagKey.of(RegistryKeys.ITEM, AccessorySlotLoader.requireTag());
		if (!stack.isIn(anyTag)) {
			return false;
		}
		TagKey<Item> slotTag = TagKey.of(RegistryKeys.ITEM, slot.allowedTag());
		return stack.isIn(slotTag);
	}

	public PlayerAccessoryInventory copy() {
		List<ItemStack> copy = new ArrayList<>(items.size());
		for (ItemStack stack : items) {
			copy.add(stack.isEmpty() ? ItemStack.EMPTY : stack.copy());
		}
		return new PlayerAccessoryInventory(copy);
	}
}
