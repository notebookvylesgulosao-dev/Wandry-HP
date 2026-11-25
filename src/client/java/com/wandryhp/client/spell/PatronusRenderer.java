package com.wandryhp.client.spell;

import com.wandryhp.WandryHP;
import com.wandryhp.client.edwiges.BbModelDrawer;
import com.wandryhp.edwiges.model.Affine;
import com.wandryhp.edwiges.model.BbModel;
import com.wandryhp.spell.PatronusEntity;
import net.minecraft.client.render.LightmapTextureManager;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.EntityRenderer;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RotationAxis;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;

/**
 * Renderiza o Patrono com o modelo e a animação originais de ghost.bbmodel (animação "Idle" em loop), no tamanho
 * original (1 px = 1/16 de bloco), translúcido e sempre iluminado (brilha no escuro). Usa o mesmo leitor/desenhista
 * de .bbmodel da Edwiges.
 */
public class PatronusRenderer extends EntityRenderer<PatronusEntity, PatronusRenderState> {
	private static final Identifier TEXTURE = Identifier.of(WandryHP.MOD_ID, "textures/entity/patronus/ghost.png");
	private static final String MODEL = "/assets/" + WandryHP.MOD_ID + "/bbmodels/patronus/ghost.bbmodel";
	private static final String ANIMATION = "Idle";
	/** 1 px do modelo = 1/16 de bloco. */
	private static final double PX = 1.0 / 16.0;

	private static BbModel model;
	private static boolean loaded;

	public PatronusRenderer(EntityRendererFactory.Context context) {
		super(context);
	}

	private static synchronized BbModel model() {
		if (!loaded) {
			loaded = true;
			try (InputStream in = PatronusRenderer.class.getResourceAsStream(MODEL)) {
				if (in != null) {
					try (Reader reader = new InputStreamReader(in, StandardCharsets.UTF_8)) {
						model = BbModel.parse(reader);
					}
				} else {
					System.err.println("[wandryhp] bbmodel do Patrono não encontrado: " + MODEL);
				}
			} catch (Exception e) {
				System.err.println("[wandryhp] Falha ao ler " + MODEL + ": " + e);
			}
		}
		return model;
	}

	@Override
	public PatronusRenderState createRenderState() {
		return new PatronusRenderState();
	}

	@Override
	public void updateRenderState(PatronusEntity entity, PatronusRenderState state, float tickDelta) {
		super.updateRenderState(entity, state, tickDelta);
		state.attacking = entity.isAttacking();
		state.animTime = (entity.getAnimTicks() + tickDelta) / 20.0f;
		state.yaw = MathHelper.lerpAngleDegrees(tickDelta, entity.prevYaw, entity.getYaw());
	}

	@Override
	public void render(PatronusRenderState state, MatrixStack matrices, VertexConsumerProvider vertexConsumers, int light) {
		BbModel ghost = model();
		if (ghost != null) {
			matrices.push();
			// Frente do modelo = -Z (igual à Edwiges): yaw 0 olha para +Z, então gira 180 - yaw.
			matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(180.0f - state.yaw));
			Affine[] pose = ghost.pose(ghost.animation(ANIMATION), state.animTime, true);
			BbModelDrawer.draw(ghost, pose, null, 0, matrices.peek(),
					vertexConsumers.getBuffer(RenderLayer.getEntityTranslucent(TEXTURE)),
					LightmapTextureManager.MAX_LIGHT_COORDINATE, PX);
			matrices.pop();
		}
		super.render(state, matrices, vertexConsumers, light);
	}
}
