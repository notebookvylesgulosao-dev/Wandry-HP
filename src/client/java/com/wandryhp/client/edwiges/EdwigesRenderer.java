package com.wandryhp.client.edwiges;

import com.wandryhp.WandryHP;
import com.wandryhp.edwiges.EdwigesEntity;
import com.wandryhp.edwiges.EdwigesRig;
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
 * Renderiza a Edwiges com o modelo e a animacao originais do .bbmodel ("fly" em loop, "pouso" uma vez, "idle"
 * = pose de repouso). O envelope fica preso ao fundo do corpo, seguindo o osso do corpo, ate ser solto.
 */
public class EdwigesRenderer extends EntityRenderer<EdwigesEntity, EdwigesRenderState> {
	private static final Identifier OWL_TEXTURE = Identifier.of(WandryHP.MOD_ID, "textures/entity/edwiges/owl.png");

	public EdwigesRenderer(EntityRendererFactory.Context context) {
		super(context);
	}

	@Override
	public EdwigesRenderState createRenderState() {
		return new EdwigesRenderState();
	}

	@Override
	public void updateRenderState(EdwigesEntity entity, EdwigesRenderState state, float tickDelta) {
		super.updateRenderState(entity, state, tickDelta);
		state.animation = entity.getAnimation();
		state.animTime = (entity.getStateTicks() + tickDelta) / 20.0f;
		state.hasLetter = entity.hasLetter();
		state.houseId = entity.getHouseId();
		state.yaw = MathHelper.lerpAngleDegrees(tickDelta, entity.prevYaw, entity.getYaw());
		state.pitch = MathHelper.lerp(tickDelta, entity.prevPitch, entity.getPitch());
	}

	@Override
	public void render(EdwigesRenderState state, MatrixStack matrices, VertexConsumerProvider vertexConsumers, int light) {
		BbModel owl = EdwigesRig.owl();
		if (owl != null) {
			matrices.push();
			// Frente do modelo = -Z; mesma ordem de rotacoes usada pelo servidor para achar o ponto de soltura.
			matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(180.0f - state.yaw));
			matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(-state.pitch));

			BbModel.Animation anim = owl.animation(EdwigesRig.animationName(state.animation));
			boolean loop = state.animation == EdwigesEntity.ANIM_FLY;
			Affine[] pose = owl.pose(anim, state.animTime, loop);

			MatrixStack.Entry entry = matrices.peek();
			BbModelDrawer.draw(owl, pose, null, 0, entry,
					vertexConsumers.getBuffer(RenderLayer.getEntityCutoutNoCull(OWL_TEXTURE)), light, EdwigesRig.PX);

			if (state.hasLetter) {
				BbModel letter = EdwigesRig.letter(state.houseId);
				if (letter != null) {
					Affine base = EdwigesRig.letterBase(owl, letter, pose[owl.boneIndex(EdwigesRig.BODY_BONE)]);
					Affine[] rest = letter.pose(null, 0, false);
					String key = EdwigesRig.letterKey(state.houseId);
					// Um buffer por vez: termina de escrever uma textura antes de pegar o da proxima.
					for (int tex = 0; tex < letter.textureCount; tex++) {
						BbModelDrawer.draw(letter, rest, base, tex, entry,
								vertexConsumers.getBuffer(RenderLayer.getEntityCutoutNoCull(LetterRenderer.texture(key, tex))),
								light, EdwigesRig.PX);
					}
				}
			}
			matrices.pop();
		}
		super.render(state, matrices, vertexConsumers, light);
	}
}
