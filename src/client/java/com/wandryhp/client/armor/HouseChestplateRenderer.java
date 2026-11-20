package com.wandryhp.client.armor;

import com.wandryhp.armor.HouseType;
import net.fabricmc.fabric.api.client.rendering.v1.ArmorRenderer;
import net.minecraft.client.model.ModelPart;
import net.minecraft.client.model.ModelTransform;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.model.BipedEntityModel;
import net.minecraft.client.render.entity.state.BipedEntityRenderState;
import net.minecraft.client.render.item.ItemRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.item.ItemStack;

/**
 * Desenha o peitoral de uma casa com o modelo 3D original do .bbmodel, acompanhando a pose do corpo e dos bracos
 * do jogador (agachar, balancar os bracos, etc.) e com brilho de encantamento.
 */
public class HouseChestplateRenderer implements ArmorRenderer {
	private final HouseType house;

	public HouseChestplateRenderer(HouseType house) {
		this.house = house;
	}

	@Override
	public void render(MatrixStack matrices, VertexConsumerProvider vertexConsumers, ItemStack stack,
					   BipedEntityRenderState state, EquipmentSlot slot, int light,
					   BipedEntityModel<BipedEntityRenderState> contextModel) {
		if (slot != EquipmentSlot.CHEST) {
			return;
		}
		HouseArmorGeometry geometry = HouseArmorModels.get(house);
		if (geometry == null) {
			return;
		}

		VertexConsumer consumer = ItemRenderer.getArmorGlintConsumer(vertexConsumers,
				RenderLayer.getArmorCutoutNoCull(HouseArmorModels.texture(house)), stack.hasGlint());

		draw(geometry, HouseArmorGeometry.Part.BODY, contextModel.body, matrices, consumer, light);
		draw(geometry, HouseArmorGeometry.Part.RIGHT_ARM, contextModel.rightArm, matrices, consumer, light);
		draw(geometry, HouseArmorGeometry.Part.LEFT_ARM, contextModel.leftArm, matrices, consumer, light);
	}

	private static void draw(HouseArmorGeometry geometry, HouseArmorGeometry.Part part, ModelPart modelPart,
							 MatrixStack matrices, VertexConsumer consumer, int light) {
		if (!modelPart.visible) {
			return;
		}
		// O pivo padrao da parte muda entre bracos largos (Steve) e finos (Alex); os cubos sao relativos a ele.
		ModelTransform pivot = modelPart.getDefaultTransform();
		matrices.push();
		modelPart.rotate(matrices);
		geometry.render(part, matrices, consumer, light, pivot.pivotX(), pivot.pivotY(), pivot.pivotZ());
		matrices.pop();
	}
}
