package com.wandryhp.client.spell;

import com.wandryhp.client.render.PropGeometry;
import com.wandryhp.spell.SpellProjectileEntity;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.EntityRenderer;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.RotationAxis;
import net.minecraft.util.math.Vec3d;

/**
 * Renderiza o projétil com o modelo 3D original do .bbmodel (tamanho original, sem escala extra).
 * Os modelos são projéteis alongados com o nariz em +Z, então são girados para apontar na direção do voo.
 */
public class SpellProjectileRenderer extends EntityRenderer<SpellProjectileEntity, SpellProjectileRenderState> {
	public SpellProjectileRenderer(EntityRendererFactory.Context context) {
		super(context);
	}

	@Override
	public SpellProjectileRenderState createRenderState() {
		return new SpellProjectileRenderState();
	}

	@Override
	public void updateRenderState(SpellProjectileEntity entity, SpellProjectileRenderState state, float tickDelta) {
		super.updateRenderState(entity, state, tickDelta);
		state.modelId = entity.getSpell().modelId;
		Vec3d v = entity.getVelocity();
		if (v.lengthSquared() > 1.0E-6) {
			Vec3d n = v.normalize();
			state.dirYaw = (float) Math.atan2(n.x, n.z);
			state.dirPitch = (float) -Math.asin(n.y);
		}
	}

	@Override
	public void render(SpellProjectileRenderState state, MatrixStack matrices, VertexConsumerProvider vertexConsumers, int light) {
		PropGeometry geometry = SpellModels.get(state.modelId);
		if (geometry != null) {
			matrices.push();
			matrices.multiply(RotationAxis.POSITIVE_Y.rotation(state.dirYaw));
			matrices.multiply(RotationAxis.POSITIVE_X.rotation(state.dirPitch));
			geometry.render(matrices,
					vertexConsumers.getBuffer(RenderLayer.getEntityCutoutNoCull(SpellModels.texture(state.modelId))),
					light);
			matrices.pop();
		}
		super.render(state, matrices, vertexConsumers, light);
	}
}
