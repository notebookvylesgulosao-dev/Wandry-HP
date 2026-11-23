package com.wandryhp.client.edwiges;

import com.wandryhp.WandryHP;
import com.wandryhp.edwiges.EdwigesRig;
import com.wandryhp.edwiges.LetterEntity;
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

/** Envelope caido: modelo original centrado; ao pousar deita no chao com o brasao para cima. */
public class LetterRenderer extends EntityRenderer<LetterEntity, LetterRenderState> {
	/** Altura do centro do envelope deitado (blocos). */
	private static final float FLAT_PIVOT = 0.02f;
	private static final float SETTLE_TICKS = 6.0f;

	public LetterRenderer(EntityRendererFactory.Context context) {
		super(context);
	}

	static Identifier texture(String key, int index) {
		return Identifier.of(WandryHP.MOD_ID, "textures/entity/edwiges/letter/" + key + "_" + index + ".png");
	}

	@Override
	public LetterRenderState createRenderState() {
		return new LetterRenderState();
	}

	@Override
	public void updateRenderState(LetterEntity entity, LetterRenderState state, float tickDelta) {
		super.updateRenderState(entity, state, tickDelta);
		state.houseId = entity.getHouseId();
		state.yaw = MathHelper.lerpAngleDegrees(tickDelta, entity.prevYaw, entity.getYaw());
		state.pitch = MathHelper.lerp(tickDelta, entity.prevPitch, entity.getPitch());
		float t = MathHelper.clamp((entity.getLandedTicks() + tickDelta) / SETTLE_TICKS, 0.0f, 1.0f);
		state.flat = entity.hasLanded() ? t * t * (3.0f - 2.0f * t) : 0.0f;
	}

	@Override
	public void render(LetterRenderState state, MatrixStack matrices, VertexConsumerProvider vertexConsumers, int light) {
		BbModel letter = EdwigesRig.letter(state.houseId);
		if (letter != null) {
			matrices.push();
			matrices.translate(0.0f, MathHelper.lerp(state.flat, LetterEntity.AIRBORNE_PIVOT, FLAT_PIVOT), 0.0f);
			matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(180.0f - state.yaw));
			// Em pe: mesma inclinacao de quando estava preso; deitado: +90 graus em X leva o brasao (-Z) para cima.
			matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(MathHelper.lerp(state.flat, -state.pitch, 90.0f)));

			double[] lb = letter.bounds(-1);
			Affine centered = Affine.translation(-(lb[0] + lb[3]) / 2.0, -(lb[1] + lb[4]) / 2.0, -(lb[2] + lb[5]) / 2.0);
			Affine[] rest = letter.pose(null, 0, false);
			String key = EdwigesRig.letterKey(state.houseId);
			for (int tex = 0; tex < letter.textureCount; tex++) {
				BbModelDrawer.draw(letter, rest, centered, tex, matrices.peek(),
						vertexConsumers.getBuffer(RenderLayer.getEntityCutoutNoCull(texture(key, tex))),
						light, EdwigesRig.PX);
			}
			matrices.pop();
		}
		super.render(state, matrices, vertexConsumers, light);
	}
}
