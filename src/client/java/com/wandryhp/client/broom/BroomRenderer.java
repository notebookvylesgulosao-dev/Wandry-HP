package com.wandryhp.client.broom;

import com.wandryhp.WandryHP;
import com.wandryhp.broom.BroomEntity;
import com.wandryhp.client.edwiges.BbModelDrawer;
import com.wandryhp.edwiges.model.Affine;
import com.wandryhp.edwiges.model.BbModel;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.EntityRenderer;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RotationAxis;

/**
 * Desenha o modelo original da vassoura. No arquivo o cabo aponta para +X e os gravetos ficam em -X, entao
 * a frente da vassoura e o lado do cabo. O centro do modelo fica na altura do assento (o jogador senta no cabo).
 */
public class BroomRenderer extends EntityRenderer<BroomEntity, BroomRenderState> {
	private static final Identifier TEXTURE = Identifier.of(WandryHP.MOD_ID, "textures/entity/broom/witch_broom.png");

	/** Tamanho do modelo: 1.0 = original (1 px = 1/16 de bloco); 1.5 deixa a vassoura com ~1,6 bloco, proporcional ao jogador. */
	private static final double MODEL_SCALE = 1.5;
	/** Altura (blocos, acima dos pes do jogador montado) do centro do modelo = cabo logo abaixo do assento. */
	private static final float CENTER_HEIGHT = 0.52f;
	/** Alongamento so no eixo do cabo (X do modelo); espessura e largura ficam iguais. */
	private static final float LENGTH_STRETCH = 1.4f;

	public BroomRenderer(EntityRendererFactory.Context context) {
		super(context);
	}

	@Override
	public BroomRenderState createRenderState() {
		return new BroomRenderState();
	}

	@Override
	public void updateRenderState(BroomEntity entity, BroomRenderState state, float tickDelta) {
		super.updateRenderState(entity, state, tickDelta);
		state.yaw = MathHelper.lerpAngleDegrees(tickDelta, entity.prevYaw, entity.getYaw());
		state.pitch = MathHelper.lerp(tickDelta, entity.prevPitch, entity.getPitch());
	}

	@Override
	public void render(BroomRenderState state, MatrixStack matrices, VertexConsumerProvider vertexConsumers, int light) {
		BbModel model = BroomModel.get();
		if (model != null) {
			matrices.push();
			matrices.translate(0.0f, CENTER_HEIGHT, 0.0f);
			// Frente do modelo = +X: leva +X para a direcao do olhar (yaw 0 = +Z) e depois inclina o bico (pitch > 0 = olhando para baixo).
			matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(-state.yaw - 90.0f));
			matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(-state.pitch));

			matrices.scale(LENGTH_STRETCH, 1.0f, 1.0f);

			double[] b = model.bounds(-1);
			Affine centered = Affine.translation(-(b[0] + b[3]) / 2.0, -(b[1] + b[4]) / 2.0, -(b[2] + b[5]) / 2.0);
			Affine[] rest = model.pose(null, 0, false);
			BbModelDrawer.draw(model, rest, centered, 0, matrices.peek(),
					vertexConsumers.getBuffer(RenderLayer.getEntityCutoutNoCull(TEXTURE)), light, MODEL_SCALE / 16.0);
			matrices.pop();
		}
		super.render(state, matrices, vertexConsumers, light);
	}
}
