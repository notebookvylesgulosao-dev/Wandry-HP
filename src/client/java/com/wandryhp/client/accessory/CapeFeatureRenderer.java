package com.wandryhp.client.accessory;

import com.wandryhp.accessory.CapeType;
import net.minecraft.client.model.ModelPart;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.feature.FeatureRenderer;
import net.minecraft.client.render.entity.feature.FeatureRendererContext;
import net.minecraft.client.render.entity.model.PlayerEntityModel;
import net.minecraft.client.render.entity.state.PlayerEntityRenderState;
import net.minecraft.client.util.math.MatrixStack;

/**
 * Desenha o chapeu equipado na cabeca do jogador. A geometria e presa a cabeca (ModelPart#rotate), entao segue
 * giro, inclinacao e agachar. O tamanho vem do .bbmodel, sem escala; a aba assenta no topo da cabeca.
 */
public class CapeFeatureRenderer extends FeatureRenderer<PlayerEntityRenderState, PlayerEntityModel> {
	/** Topo da cabeca (-8) mais 0,5 px da camada externa da skin (hat layer), em pixels; Y negativo sobe. */
	private static final float ANCHOR_Y = -8.5f;
	/** Escala do chapeu (1 = tamanho do .bbmodel), aplicada em torno do ponto de encaixe na cabeca. */
	private static final float HAT_SCALE = 1.8f;

	public CapeFeatureRenderer(FeatureRendererContext<PlayerEntityRenderState, PlayerEntityModel> context) {
		super(context);
	}

	@Override
	public void render(MatrixStack matrices, VertexConsumerProvider vertexConsumers, int light,
					   PlayerEntityRenderState state, float limbAngle, float limbDelta) {
		if (state.invisible) {
			return;
		}
		CapeType type = ((AccessoryRenderHolder) state).wandryhp$getCape();
		if (type == null) {
			return;
		}
		CapeGeometry geometry = CapeModels.get(type);
		if (geometry == null) {
			return;
		}
		ModelPart head = this.getContextModel().head;

		VertexConsumer consumer = vertexConsumers.getBuffer(RenderLayer.getEntityCutoutNoCull(CapeModels.texture(type)));
		matrices.push();
		head.rotate(matrices);
		// Escala em torno do ponto de encaixe (base da aba, no topo da cabeca): o encaixe nao muda.
		matrices.translate(0f, ANCHOR_Y / 16f, 0f);
		matrices.scale(HAT_SCALE, HAT_SCALE, HAT_SCALE);
		geometry.render(matrices, consumer, light, 0f, 0f, 0f);
		matrices.pop();
	}
}
