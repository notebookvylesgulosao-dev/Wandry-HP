package com.wandryhp.client.armor;

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
 * Desenha a calca (slot das pernas) ou as botas (slot dos pes) com o modelo 3D original do .bbmodel
 * (cubos leg0 / leg1), acompanhando a pose das pernas do jogador e com brilho de encantamento.
 */
public class HouseLegwearRenderer implements ArmorRenderer {
	private final boolean boots;

	public HouseLegwearRenderer(boolean boots) {
		this.boots = boots;
	}

	@Override
	public void render(MatrixStack matrices, VertexConsumerProvider vertexConsumers, ItemStack stack,
					   BipedEntityRenderState state, EquipmentSlot slot, int light,
					   BipedEntityModel<BipedEntityRenderState> contextModel) {
		if (slot != (boots ? EquipmentSlot.FEET : EquipmentSlot.LEGS)) {
			return;
		}
		HouseArmorGeometry geometry = boots ? HouseArmorModels.boots() : HouseArmorModels.leggings();
		if (geometry == null) {
			return;
		}

		String id = boots ? HouseArmorModels.BOOTS_ID : HouseArmorModels.LEGGINGS_ID;
		VertexConsumer consumer = ItemRenderer.getArmorGlintConsumer(vertexConsumers,
				RenderLayer.getArmorCutoutNoCull(HouseArmorModels.texture(id)), stack.hasGlint());

		draw(geometry, HouseArmorGeometry.Part.RIGHT_LEG, contextModel.rightLeg, matrices, consumer, light);
		draw(geometry, HouseArmorGeometry.Part.LEFT_LEG, contextModel.leftLeg, matrices, consumer, light);
	}

	private static void draw(HouseArmorGeometry geometry, HouseArmorGeometry.Part part, ModelPart modelPart,
							 MatrixStack matrices, VertexConsumer consumer, int light) {
		if (!modelPart.visible) {
			return;
		}
		ModelTransform pivot = modelPart.getDefaultTransform();
		matrices.push();
		modelPart.rotate(matrices);
		geometry.render(part, matrices, consumer, light, pivot.pivotX(), pivot.pivotY(), pivot.pivotZ());
		matrices.pop();
	}
}
