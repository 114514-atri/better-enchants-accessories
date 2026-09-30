package com.betterenchants.accessory;

import com.betterenchants.BetterEnchantsMod;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import net.fabricmc.fabric.api.resource.IdentifiableResourceReloadListener;
import net.fabricmc.fabric.api.resource.ResourceManagerHelper;
import net.minecraft.resource.Resource;
import net.minecraft.resource.ResourceManager;
import net.minecraft.resource.ResourceType;
import net.minecraft.resource.SinglePreparationResourceReloader;
import net.minecraft.util.Identifier;
import net.minecraft.util.profiler.Profiler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** 加载本模组饰品栏槽位数据包；与 OptionalPrivate 无关。 */
public final class AccessorySlotLoader extends SinglePreparationResourceReloader<List<AccessorySlotDef>>
		implements IdentifiableResourceReloadListener {
	private static final Logger LOGGER = LoggerFactory.getLogger("better_enchants/accessory");
	private static final Identifier PATH = BetterEnchantsMod.id("accessory/slots.json");

	private static volatile List<AccessorySlotDef> SLOTS = List.of();
	private static volatile String namespaceOnly = BetterEnchantsMod.MOD_ID;
	private static volatile Identifier requireTag = BetterEnchantsMod.id("accessory/any");
	private static volatile boolean rejectOtherMods = true;

	private AccessorySlotLoader() {
	}

	public static void register() {
		ResourceManagerHelper.get(ResourceType.SERVER_DATA).registerReloadListener(new AccessorySlotLoader());
	}

	public static List<AccessorySlotDef> slots() {
		return SLOTS;
	}

	public static int slotCount() {
		return SLOTS.size();
	}

	public static Optional<AccessorySlotDef> byIndex(int index) {
		List<AccessorySlotDef> list = SLOTS;
		if (index < 0 || index >= list.size()) {
			return Optional.empty();
		}
		return Optional.of(list.get(index));
	}

	public static Optional<AccessorySlotDef> byId(Identifier id) {
		for (AccessorySlotDef slot : SLOTS) {
			if (slot.id().equals(id)) {
				return Optional.of(slot);
			}
		}
		return Optional.empty();
	}

	public static String namespaceOnly() {
		return namespaceOnly;
	}

	public static Identifier requireTag() {
		return requireTag;
	}

	public static boolean rejectOtherMods() {
		return rejectOtherMods;
	}

	@Override
	public Identifier getFabricId() {
		return BetterEnchantsMod.id("accessory_slot_loader");
	}

	@Override
	protected List<AccessorySlotDef> prepare(ResourceManager manager, Profiler profiler) {
		Optional<Resource> resource = manager.getResource(PATH);
		if (resource.isEmpty()) {
			LOGGER.warn("Missing accessory slots datapack: {}", PATH);
			return List.of();
		}
		try (InputStreamReader reader = new InputStreamReader(resource.get().getInputStream(), StandardCharsets.UTF_8)) {
			JsonObject root = JsonParser.parseReader(reader).getAsJsonObject();
			JsonObject rules = root.has("rules") && root.get("rules").isJsonObject()
					? root.getAsJsonObject("rules")
					: new JsonObject();
			String ns = rules.has("namespace_only")
					? rules.get("namespace_only").getAsString()
					: BetterEnchantsMod.MOD_ID;
			Identifier reqTag = rules.has("require_tag")
					? Identifier.of(rules.get("require_tag").getAsString())
					: BetterEnchantsMod.id("accessory/any");
			boolean reject = !rules.has("reject_other_mods") || rules.get("reject_other_mods").getAsBoolean();

			JsonArray arr = root.getAsJsonArray("slots");
			List<AccessorySlotDef> parsed = new ArrayList<>();
			for (JsonElement el : arr) {
				JsonObject obj = el.getAsJsonObject();
				String idPath = obj.get("id").getAsString();
				int order = obj.has("order") ? obj.get("order").getAsInt() : parsed.size();
				Identifier icon = obj.has("icon")
						? Identifier.of(obj.get("icon").getAsString())
						: BetterEnchantsMod.id("gui/accessory/" + idPath);
				Identifier allowed = obj.has("allowed_tag")
						? Identifier.of(obj.get("allowed_tag").getAsString())
						: BetterEnchantsMod.id("accessory/" + idPath);
				parsed.add(new AccessorySlotDef(AccessorySlotDef.slotId(idPath), order, icon, allowed));
			}
			parsed.sort(Comparator.comparingInt(AccessorySlotDef::order));

			/* 暂存在 prepare 结果外的静态缓冲，apply 时一并提交 */
			PreparedBundle.NAMESPACE = ns;
			PreparedBundle.REQUIRE_TAG = reqTag;
			PreparedBundle.REJECT = reject;
			return Collections.unmodifiableList(parsed);
		} catch (Exception e) {
			LOGGER.error("Failed to load accessory slots from {}", PATH, e);
			return List.of();
		}
	}

	@Override
	protected void apply(List<AccessorySlotDef> prepared, ResourceManager manager, Profiler profiler) {
		SLOTS = prepared;
		namespaceOnly = PreparedBundle.NAMESPACE;
		requireTag = PreparedBundle.REQUIRE_TAG;
		rejectOtherMods = PreparedBundle.REJECT;
		LOGGER.info("Loaded {} accessory slots (namespace_only={})", SLOTS.size(), namespaceOnly);
	}

	/** prepare→apply 之间传递 rules。 */
	private static final class PreparedBundle {
		private static String NAMESPACE = BetterEnchantsMod.MOD_ID;
		private static Identifier REQUIRE_TAG = BetterEnchantsMod.id("accessory/any");
		private static boolean REJECT = true;
	}
}
