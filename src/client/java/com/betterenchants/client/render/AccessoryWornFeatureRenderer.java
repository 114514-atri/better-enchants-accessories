package com.betterenchants.client.render;

import com.betterenchants.BetterEnchantsMod;
import com.betterenchants.accessory.Accessories;
import com.betterenchants.accessory.AccessoryGameplay;
import com.betterenchants.accessory.AccessoryItems;
import com.betterenchants.accessory.PlayerAccessoryInventory;
import com.betterenchants.spirit.SunMoonRadianceItem;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.option.Perspective;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.feature.FeatureRenderer;
import net.minecraft.client.render.entity.feature.FeatureRendererContext;
import net.minecraft.client.render.entity.model.PlayerEntityModel;
import net.minecraft.client.render.item.ItemRenderer;
import net.minecraft.client.render.model.json.ModelTransformationMode;
import net.minecraft.client.util.SkinTextures;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.tag.ItemTags;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RotationAxis;
import org.joml.Matrix4f;

/** 第三人称 / 背包纸娃娃：把已装备饰品画在对应身体部位。 */
@Environment(EnvType.CLIENT)
public final class AccessoryWornFeatureRenderer
		extends FeatureRenderer<AbstractClientPlayerEntity, PlayerEntityModel<AbstractClientPlayerEntity>> {
	private static final Identifier WORN_TEX = BetterEnchantsMod.id("textures/entity/accessory/worn.png");
	private static final Identifier HEAD_TEX = BetterEnchantsMod.id("textures/item/accessory_head.png");
	private static final Identifier FACE_TEX = BetterEnchantsMod.id("textures/item/accessory_face.png");
	private static final Identifier NECKLACE_TEX = BetterEnchantsMod.id("textures/item/accessory_necklace.png");
	private static final Identifier RING_TEX = BetterEnchantsMod.id("textures/item/accessory_ring.png");
	private static final Identifier BRACELET_TEX = BetterEnchantsMod.id("textures/item/accessory_bracelet.png");
	private static final Identifier BELT_TEX = BetterEnchantsMod.id("textures/item/accessory_belt.png");
	private static final Identifier CHARM_TEX = BetterEnchantsMod.id("textures/item/accessory_charm.png");
	private static final Identifier AURA_TEX = BetterEnchantsMod.id("textures/item/accessory_aura.png");
	private static final Identifier SHIELD_TEX = BetterEnchantsMod.id("textures/item/accessory_shield.png");

	private final AccessoryWornModels models = new AccessoryWornModels();

	public AccessoryWornFeatureRenderer(
			FeatureRendererContext<AbstractClientPlayerEntity, PlayerEntityModel<AbstractClientPlayerEntity>> context
	) {
		super(context);
	}

	@Override
	public void render(
			MatrixStack matrices,
			VertexConsumerProvider vertexConsumers,
			int light,
			AbstractClientPlayerEntity entity,
			float limbAngle,
			float limbDistance,
			float tickDelta,
			float animationProgress,
			float headYaw,
			float headPitch
	) {
		if (entity.isInvisible()) {
			return;
		}
		MinecraftClient client = MinecraftClient.getInstance();
		if (entity == client.player && client.options.getPerspective() == Perspective.FIRST_PERSON) {
			return;
		}

		PlayerAccessoryInventory inv = Accessories.get(entity);
		boolean slim = entity.getSkinTextures().model() == SkinTextures.Model.SLIM;
		PlayerEntityModel<AbstractClientPlayerEntity> model = this.getContextModel();
		int overlay = OverlayTexture.DEFAULT_UV;

		for (int i = 0; i < inv.size(); i++) {
			ItemStack stack = inv.getStack(i);
			if (stack.isEmpty() || AccessoryGameplay.isElytraStack(stack)) {
				continue;
			}
			Item item = stack.getItem();
			VertexConsumer metal = vertexConsumers.getBuffer(RenderLayer.getEntityCutoutNoCull(WORN_TEX));
			if (item == AccessoryItems.HEAD) {
				matrices.push();
				model.head.rotate(matrices);
				this.models.renderCirclet(matrices, metal, light, overlay);
				drawEmblem(matrices, vertexConsumers, HEAD_TEX, light, 0.0f, -0.48f, -0.34f, 0.11f, 0.13f);
				matrices.pop();
			} else if (item == AccessoryItems.FACE) {
				matrices.push();
				model.head.rotate(matrices);
				this.models.renderGlasses(matrices, metal, light, overlay);
				drawEmblem(matrices, vertexConsumers, FACE_TEX, light, 0.0f, -0.28f, -0.325f, 0.16f, 0.08f);
				matrices.pop();
			} else if (item == AccessoryItems.NECKLACE) {
				matrices.push();
				model.body.rotate(matrices);
				this.models.renderNecklace(matrices, metal, light, overlay);
				drawEmblem(matrices, vertexConsumers, NECKLACE_TEX, light, 0.0f, 0.18f, -0.17f, 0.09f, 0.12f);
				matrices.pop();
			} else if (item == AccessoryItems.RING) {
				matrices.push();
				model.rightArm.rotate(matrices);
				this.models.renderRing(matrices, metal, light, overlay);
				drawEmblem(matrices, vertexConsumers, RING_TEX, light, -0.04f, 0.56f, -0.16f, 0.055f, 0.055f);
				matrices.pop();
			} else if (item == AccessoryItems.BRACELET) {
				matrices.push();
				model.leftArm.rotate(matrices);
				this.models.renderBracelet(matrices, metal, light, overlay, slim);
				drawEmblem(matrices, vertexConsumers, BRACELET_TEX, light, slim ? 0.03f : 0.06f, 0.47f, -0.16f, 0.07f, 0.07f);
				matrices.pop();
			} else if (item == AccessoryItems.BELT) {
				matrices.push();
				model.body.rotate(matrices);
				this.models.renderBelt(matrices, metal, light, overlay);
				drawEmblem(matrices, vertexConsumers, BELT_TEX, light, 0.0f, 0.64f, -0.18f, 0.09f, 0.07f);
				matrices.pop();
			} else if (item == AccessoryItems.CHARM) {
				matrices.push();
				model.body.rotate(matrices);
				drawEmblem(matrices, vertexConsumers, CHARM_TEX, light, 0.18f, 0.72f, -0.17f, 0.08f, 0.08f);
				matrices.pop();
			} else if (item == AccessoryItems.SHIELD) {
				matrices.push();
				model.body.rotate(matrices);
				drawEmblem(matrices, vertexConsumers, SHIELD_TEX, light, 0.0f, 0.28f, 0.18f, 0.16f, 0.20f);
				matrices.pop();
			} else if (item == AccessoryItems.AURA) {
				renderAura(matrices, vertexConsumers, model, entity, tickDelta, light);
			} else if (item instanceof SunMoonRadianceItem) {
				matrices.push();
				model.body.rotate(matrices);
				drawEmblem(matrices, vertexConsumers, BetterEnchantsMod.id("textures/item/sun_moon_radiance.png"),
						light, 0.0f, 0.22f, -0.18f, 0.10f, 0.10f);
				matrices.pop();
			} else if (stack.isIn(ItemTags.BANNERS)) {
				renderBannerBack(matrices, vertexConsumers, model, entity, stack, light, tickDelta);
			}
		}
	}

	private static void renderBannerBack(
			MatrixStack matrices,
			VertexConsumerProvider consumers,
			PlayerEntityModel<AbstractClientPlayerEntity> model,
			AbstractClientPlayerEntity entity,
			ItemStack banner,
			int light,
			float tickDelta
	) {
		matrices.push();
		if (entity.isInSneakingPose()) {
			matrices.translate(0.0f, 0.25f, 0.0f);
		}
		model.body.rotate(matrices);
		/* 比背剑 phantom (z=0.34) 更贴背，避免与武器重叠 */
		float floatY = 0.006f * MathHelper.sin((entity.age + tickDelta) * 0.12f);
		matrices.translate(-0.02f, 0.05f + floatY, 0.20f);
		matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(180.0f));
		matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(-8.0f));
		matrices.scale(1.05f, 1.05f, 1.05f);
		ItemRenderer items = MinecraftClient.getInstance().getItemRenderer();
		items.renderItem(
				entity,
				banner,
				ModelTransformationMode.FIXED,
				false,
				matrices,
				consumers,
				entity.getWorld(),
				light,
				OverlayTexture.DEFAULT_UV,
				entity.getId() + 701
		);
		matrices.pop();
	}

	private static void renderAura(
			MatrixStack matrices,
			VertexConsumerProvider consumers,
			PlayerEntityModel<AbstractClientPlayerEntity> model,
			AbstractClientPlayerEntity entity,
			float tickDelta,
			int light
	) {
		matrices.push();
		model.head.rotate(matrices);
		/* 头顶水平光环：头模顶约 y=-0.5，再上移；环面在 XZ（水平） */
		matrices.translate(0.0f, -0.72f, 0.0f);
		float spin = (entity.age + tickDelta) * 1.4f;
		float pulse = 0.92f + 0.08f * MathHelper.sin((entity.age + tickDelta) * 0.18f);
		matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(spin));
		matrices.scale(pulse, pulse, pulse);
		VertexConsumer buf = consumers.getBuffer(RenderLayer.getEntityTranslucent(AURA_TEX));
		drawRing(matrices, buf, light, 0.34f, 0.045f, 0.82f);
		matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(-spin * 1.6f));
		drawRing(matrices, buf, light, 0.24f, 0.032f, 0.55f);
		matrices.pop();
	}

	private static void drawRing(
			MatrixStack matrices, VertexConsumer buf, int light, float radius, float halfH, float alpha
	) {
		int segs = 12;
		int a = MathHelper.clamp((int) (alpha * 255.0f), 0, 255);
		Matrix4f m = matrices.peek().getPositionMatrix();
		for (int i = 0; i < segs; i++) {
			float t0 = (float) (i * Math.PI * 2.0 / segs);
			float t1 = (float) ((i + 1) * Math.PI * 2.0 / segs);
			float x0 = MathHelper.cos(t0) * radius;
			float z0 = MathHelper.sin(t0) * radius;
			float x1 = MathHelper.cos(t1) * radius;
			float z1 = MathHelper.sin(t1) * radius;
			float u0 = i / (float) segs;
			float u1 = (i + 1) / (float) segs;
			quad(buf, m, light, a,
					x0, -halfH, z0, u0, 1.0f,
					x1, -halfH, z1, u1, 1.0f,
					x1, halfH, z1, u1, 0.0f,
					x0, halfH, z0, u0, 0.0f);
		}
	}

	private static void drawEmblem(
			MatrixStack matrices,
			VertexConsumerProvider consumers,
			Identifier texture,
			int light,
			float x,
			float y,
			float z,
			float halfW,
			float halfH
	) {
		matrices.push();
		matrices.translate(x, y, z);
		VertexConsumer buf = consumers.getBuffer(RenderLayer.getEntityTranslucent(texture));
		Matrix4f m = matrices.peek().getPositionMatrix();
		quad(buf, m, light, 255,
				-halfW, -halfH, 0.0f, 0.0f, 0.0f,
				halfW, -halfH, 0.0f, 1.0f, 0.0f,
				halfW, halfH, 0.0f, 1.0f, 1.0f,
				-halfW, halfH, 0.0f, 0.0f, 1.0f);
		quad(buf, m, light, 255,
				-halfW, halfH, 0.0f, 0.0f, 1.0f,
				halfW, halfH, 0.0f, 1.0f, 1.0f,
				halfW, -halfH, 0.0f, 1.0f, 0.0f,
				-halfW, -halfH, 0.0f, 0.0f, 0.0f);
		matrices.pop();
	}

	private static void quad(
			VertexConsumer buf,
			Matrix4f m,
			int light,
			int alpha,
			float x0, float y0, float z0, float u0, float v0,
			float x1, float y1, float z1, float u1, float v1,
			float x2, float y2, float z2, float u2, float v2,
			float x3, float y3, float z3, float u3, float v3
	) {
		vert(buf, m, light, alpha, x0, y0, z0, u0, v0);
		vert(buf, m, light, alpha, x1, y1, z1, u1, v1);
		vert(buf, m, light, alpha, x2, y2, z2, u2, v2);
		vert(buf, m, light, alpha, x3, y3, z3, u3, v3);
	}

	private static void vert(
			VertexConsumer buf, Matrix4f m, int light, int alpha,
			float x, float y, float z, float u, float v
	) {
		buf.vertex(m, x, y, z)
				.color(255, 255, 255, alpha)
				.texture(u, v)
				.overlay(OverlayTexture.DEFAULT_UV)
				.light(light)
				.normal(0.0f, 0.0f, -1.0f);
	}
}
