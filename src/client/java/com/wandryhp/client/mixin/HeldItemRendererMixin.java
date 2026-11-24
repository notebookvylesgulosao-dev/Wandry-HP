package com.wandryhp.client.mixin;

import com.wandryhp.client.anim.WandAnimClient;
import com.wandryhp.client.anim.WandPose;
import com.wandryhp.item.WandItem;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.item.HeldItemRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Arm;
import net.minecraft.util.Hand;
import net.minecraft.util.math.RotationAxis;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Primeira pessoa: o modelo do jogador não aparece, só o item na mão. A pose da conjuração é aplicada como uma cadeia
 * ombro -> antebraço -> pulso -> varinha, cada junta girando em torno do seu pivô, ANTES das transformações vanilla
 * do item (por isso o item acompanha como se estivesse preso à mão).
 *
 * <p>Espaço da câmera: x = direita, y = cima, z = para trás (a frente é -z). Os pivôs abaixo são ajustáveis.
 */
@Mixin(HeldItemRenderer.class)
public abstract class HeldItemRendererMixin {
	/** Posição aproximada da mão (onde o vanilla desenha o item). */
	private static final double HAND_X = 0.56, HAND_Y = -0.52, HAND_Z = -0.72;
	/** Ombro (fora da tela, embaixo/à direita) e cotovelo (meio do caminho). */
	private static final double SHOULDER_X = 0.45, SHOULDER_Y = -0.95, SHOULDER_Z = 0.30;
	/** A câmera está muito mais perto do braço que a terceira pessoa: reduz a amplitude de subida. */
	private static final float RAISE_SCALE = 0.50f;

	@Inject(method = "renderFirstPersonItem(Lnet/minecraft/client/network/AbstractClientPlayerEntity;FFLnet/minecraft/util/Hand;FLnet/minecraft/item/ItemStack;FLnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumerProvider;I)V",
			at = @At("HEAD"))
	private void wandryhp$castChain(AbstractClientPlayerEntity player, float tickDelta, float pitch, Hand hand,
									float swingProgress, ItemStack stack, float equipProgress, MatrixStack matrices,
									VertexConsumerProvider vertexConsumers, int light, CallbackInfo ci) {
		if (hand != Hand.MAIN_HAND || !(stack.getItem() instanceof WandItem)) {
			return;
		}
		WandPose pose = WandAnimClient.poseFor(player.getId(), tickDelta);
		if (pose == null) {
			return;
		}
		double side = player.getMainArm() == Arm.RIGHT ? 1.0 : -1.0;
		double sx = SHOULDER_X * side, hx = HAND_X * side;
		double ex = (sx + hx) * 0.5, ey = (SHOULDER_Y + HAND_Y) * 0.5, ez = (SHOULDER_Z + HAND_Z) * 0.5;

		// ombro: levanta (X) e varre para o centro (Y); roll leve (Z)
		matrices.translate(sx, SHOULDER_Y, SHOULDER_Z);
		matrices.multiply(RotationAxis.POSITIVE_X.rotation(pose.shoulderPitch() * RAISE_SCALE));
		matrices.multiply(RotationAxis.POSITIVE_Y.rotation((float) side * pose.shoulderYaw()));
		matrices.multiply(RotationAxis.POSITIVE_Z.rotation((float) side * pose.shoulderRoll()));
		matrices.translate(-sx, -SHOULDER_Y, -SHOULDER_Z);

		// antebraço: flexão em torno do cotovelo
		matrices.translate(ex, ey, ez);
		matrices.multiply(RotationAxis.POSITIVE_X.rotation(pose.elbow() * RAISE_SCALE));
		matrices.translate(-ex, -ey, -ez);

		// pulso + varinha: lateral (Y), inclinação (X) e giro (Z) em torno da mão; o chicote soma ao pulso
		matrices.translate(hx, HAND_Y, HAND_Z);
		matrices.multiply(RotationAxis.POSITIVE_Y.rotation((float) side * (pose.wristYaw() + pose.wandYaw())));
		matrices.multiply(RotationAxis.POSITIVE_X.rotation(pose.wristPitch()));
		matrices.multiply(RotationAxis.POSITIVE_Z.rotation((float) side * pose.wristRoll()));
		matrices.translate(-hx, -HAND_Y, -HAND_Z);
	}
}
