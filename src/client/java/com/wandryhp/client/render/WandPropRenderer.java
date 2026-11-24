package com.wandryhp.client.render;

import com.wandryhp.WandryHP;
import com.wandryhp.entity.WandPropEntity;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.EntityRenderer;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.util.math.MatrixStack;

/**
 * Renderiza o prop com o modelo 3D original do .bbmodel. Os modelos sao "sprites com espessura" (finos em Z),
 * entao ficam voltados para a camera, como um item arremessado vanilla.
 */
public class WandPropRenderer extends EntityRenderer<WandPropEntity, WandPropRenderState> {
	public WandPropRenderer(EntityRendererFactory.Context context) {
		super(context);
	}

	@Override
	public WandPropRenderState createRenderState() {
		return new WandPropRenderState();
	}

	@Override
	public void updateRenderState(WandPropEntity entity, WandPropRenderState state, float tickDelta) {
		super.updateRenderState(entity, state, tickDelta);
		state.propId = entity.getPropId();
	}

	@Override
	public void render(WandPropRenderState state, MatrixStack matrices, VertexConsumerProvider vertexConsumers, int light) {
		PropGeometry geometry = PropModels.get(state.propId);
		if (geometry != null) {
			matrices.push();
			matrices.multiply(MinecraftClient.getInstance().gameRenderer.getCamera().getRotation());
			float scale = WandryHP.CONFIG.propScale;
			matrices.scale(scale, scale, scale);
			geometry.render(matrices,
					vertexConsumers.getBuffer(RenderLayer.getEntityCutoutNoCull(PropModels.texture(state.propId))),
					light);
			matrices.pop();
		}
		super.render(state, matrices, vertexConsumers, light);
	}
}
