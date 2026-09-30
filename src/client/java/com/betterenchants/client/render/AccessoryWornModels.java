package com.betterenchants.client.render;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.model.ModelData;
import net.minecraft.client.model.ModelPart;
import net.minecraft.client.model.ModelPartBuilder;
import net.minecraft.client.model.ModelPartData;
import net.minecraft.client.model.ModelTransform;
import net.minecraft.client.model.TexturedModelData;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.util.math.MatrixStack;

/**
 * 穿戴饰品的实体立方体（坐标与玩家头/身/臂 ModelPart 一致，单位像素）。
 * UV 落在 {@code textures/entity/accessory/worn.png} 色板分区。
 */
@Environment(EnvType.CLIENT)
public final class AccessoryWornModels {
	private final ModelPart circlet;
	private final ModelPart glasses;
	private final ModelPart necklace;
	private final ModelPart belt;
	private final ModelPart bracelet;
	private final ModelPart braceletSlim;
	private final ModelPart ring;

	public AccessoryWornModels() {
		ModelPart root = mesh().createModel();
		this.circlet = root.getChild("circlet");
		this.glasses = root.getChild("glasses");
		this.necklace = root.getChild("necklace");
		this.belt = root.getChild("belt");
		this.bracelet = root.getChild("bracelet");
		this.braceletSlim = root.getChild("bracelet_slim");
		this.ring = root.getChild("ring");
	}

	public void renderCirclet(MatrixStack matrices, VertexConsumer vertices, int light, int overlay) {
		this.circlet.render(matrices, vertices, light, overlay);
	}

	public void renderGlasses(MatrixStack matrices, VertexConsumer vertices, int light, int overlay) {
		this.glasses.render(matrices, vertices, light, overlay);
	}

	public void renderNecklace(MatrixStack matrices, VertexConsumer vertices, int light, int overlay) {
		this.necklace.render(matrices, vertices, light, overlay);
	}

	public void renderBelt(MatrixStack matrices, VertexConsumer vertices, int light, int overlay) {
		this.belt.render(matrices, vertices, light, overlay);
	}

	public void renderBracelet(MatrixStack matrices, VertexConsumer vertices, int light, int overlay, boolean slim) {
		(slim ? this.braceletSlim : this.bracelet).render(matrices, vertices, light, overlay);
	}

	public void renderRing(MatrixStack matrices, VertexConsumer vertices, int light, int overlay) {
		this.ring.render(matrices, vertices, light, overlay);
	}

	private static TexturedModelData mesh() {
		ModelData data = new ModelData();
		ModelPartData root = data.getRoot();

		ModelPartData circlet = root.addChild("circlet", ModelPartBuilder.create(), ModelTransform.NONE);
		// gold band around skull
		circlet.addChild("front", ModelPartBuilder.create().uv(0, 0)
				.cuboid(-4.7f, -7.7f, -4.85f, 9.4f, 1.6f, 0.85f), ModelTransform.NONE);
		circlet.addChild("back", ModelPartBuilder.create().uv(0, 0)
				.cuboid(-4.7f, -7.7f, 4.0f, 9.4f, 1.6f, 0.85f), ModelTransform.NONE);
		circlet.addChild("left", ModelPartBuilder.create().uv(0, 0)
				.cuboid(-4.85f, -7.7f, -4.0f, 0.85f, 1.6f, 8.0f), ModelTransform.NONE);
		circlet.addChild("right", ModelPartBuilder.create().uv(16, 0)
				.cuboid(4.0f, -7.7f, -4.0f, 0.85f, 1.6f, 8.0f), ModelTransform.NONE);
		circlet.addChild("spike_c", ModelPartBuilder.create().uv(32, 0)
				.cuboid(-0.7f, -9.4f, -0.7f, 1.4f, 2.0f, 1.4f), ModelTransform.NONE);
		circlet.addChild("spike_l", ModelPartBuilder.create().uv(32, 0)
				.cuboid(-3.2f, -8.8f, -0.5f, 1.0f, 1.3f, 1.0f), ModelTransform.NONE);
		circlet.addChild("spike_r", ModelPartBuilder.create().uv(32, 0)
				.cuboid(2.2f, -8.8f, -0.5f, 1.0f, 1.3f, 1.0f), ModelTransform.NONE);
		circlet.addChild("gem", ModelPartBuilder.create().uv(32, 0)
				.cuboid(-1.1f, -8.35f, -5.15f, 2.2f, 2.4f, 0.7f), ModelTransform.NONE);

		ModelPartData glasses = root.addChild("glasses", ModelPartBuilder.create(), ModelTransform.NONE);
		glasses.addChild("lens_l", ModelPartBuilder.create().uv(48, 0)
				.cuboid(-3.7f, -5.7f, -4.62f, 3.0f, 2.5f, 0.45f), ModelTransform.NONE);
		glasses.addChild("lens_r", ModelPartBuilder.create().uv(48, 0)
				.cuboid(0.7f, -5.7f, -4.62f, 3.0f, 2.5f, 0.45f), ModelTransform.NONE);
		glasses.addChild("bridge", ModelPartBuilder.create().uv(0, 0)
				.cuboid(-0.75f, -5.15f, -4.55f, 1.5f, 0.7f, 0.35f), ModelTransform.NONE);
		glasses.addChild("arm_l", ModelPartBuilder.create().uv(16, 0)
				.cuboid(-4.05f, -5.05f, -4.4f, 0.4f, 0.5f, 4.2f), ModelTransform.NONE);
		glasses.addChild("arm_r", ModelPartBuilder.create().uv(16, 0)
				.cuboid(3.65f, -5.05f, -4.4f, 0.4f, 0.5f, 4.2f), ModelTransform.NONE);

		ModelPartData necklace = root.addChild("necklace", ModelPartBuilder.create(), ModelTransform.NONE);
		necklace.addChild("chain_l", ModelPartBuilder.create().uv(0, 0)
				.cuboid(-3.4f, 0.15f, -2.35f, 2.6f, 0.45f, 0.35f), ModelTransform.NONE);
		necklace.addChild("chain_r", ModelPartBuilder.create().uv(0, 0)
				.cuboid(0.8f, 0.15f, -2.35f, 2.6f, 0.45f, 0.35f), ModelTransform.NONE);
		necklace.addChild("chain_drop", ModelPartBuilder.create().uv(0, 0)
				.cuboid(-0.35f, 0.35f, -2.4f, 0.7f, 1.6f, 0.35f), ModelTransform.NONE);
		necklace.addChild("bail", ModelPartBuilder.create().uv(16, 0)
				.cuboid(-0.7f, 1.7f, -2.55f, 1.4f, 0.7f, 0.5f), ModelTransform.NONE);

		ModelPartData belt = root.addChild("belt", ModelPartBuilder.create(), ModelTransform.NONE);
		belt.addChild("front", ModelPartBuilder.create().uv(16, 16)
				.cuboid(-4.35f, 9.55f, -2.45f, 8.7f, 1.7f, 0.5f), ModelTransform.NONE);
		belt.addChild("back", ModelPartBuilder.create().uv(16, 16)
				.cuboid(-4.35f, 9.55f, 1.95f, 8.7f, 1.7f, 0.5f), ModelTransform.NONE);
		belt.addChild("left", ModelPartBuilder.create().uv(16, 16)
				.cuboid(-4.45f, 9.55f, -2.0f, 0.5f, 1.7f, 4.0f), ModelTransform.NONE);
		belt.addChild("right", ModelPartBuilder.create().uv(16, 16)
				.cuboid(3.95f, 9.55f, -2.0f, 0.5f, 1.7f, 4.0f), ModelTransform.NONE);
		belt.addChild("buckle", ModelPartBuilder.create().uv(0, 0)
				.cuboid(-1.5f, 9.35f, -2.7f, 3.0f, 2.1f, 0.55f), ModelTransform.NONE);

		addBracelet(root, "bracelet", 4.0f);
		addBracelet(root, "bracelet_slim", 3.0f);

		ModelPartData ring = root.addChild("ring", ModelPartBuilder.create(), ModelTransform.NONE);
		ring.addChild("band", ModelPartBuilder.create().uv(0, 0)
				.cuboid(-1.15f, 8.85f, -2.35f, 1.5f, 0.7f, 1.15f), ModelTransform.NONE);
		ring.addChild("gem", ModelPartBuilder.create().uv(0, 16)
				.cuboid(-0.85f, 8.55f, -2.55f, 0.9f, 0.7f, 0.45f), ModelTransform.NONE);

		return TexturedModelData.of(data, 64, 64);
	}

	private static void addBracelet(ModelPartData root, String name, float armW) {
		float x0 = -1.0f;
		float x1 = x0 + armW;
		ModelPartData bracelet = root.addChild(name, ModelPartBuilder.create(), ModelTransform.NONE);
		bracelet.addChild("front", ModelPartBuilder.create().uv(0, 0)
				.cuboid(x0 - 0.28f, 6.85f, -2.28f, armW + 0.56f, 1.45f, 0.38f), ModelTransform.NONE);
		bracelet.addChild("back", ModelPartBuilder.create().uv(0, 0)
				.cuboid(x0 - 0.28f, 6.85f, 1.9f, armW + 0.56f, 1.45f, 0.38f), ModelTransform.NONE);
		bracelet.addChild("left", ModelPartBuilder.create().uv(16, 0)
				.cuboid(x0 - 0.28f, 6.85f, -2.0f, 0.38f, 1.45f, 4.0f), ModelTransform.NONE);
		bracelet.addChild("right", ModelPartBuilder.create().uv(48, 0)
				.cuboid(x1 - 0.1f, 6.85f, -2.0f, 0.38f, 1.45f, 4.0f), ModelTransform.NONE);
		bracelet.addChild("gem", ModelPartBuilder.create().uv(48, 0)
				.cuboid(x0 + armW * 0.5f - 0.7f, 6.7f, -2.55f, 1.4f, 1.7f, 0.4f), ModelTransform.NONE);
	}
}
