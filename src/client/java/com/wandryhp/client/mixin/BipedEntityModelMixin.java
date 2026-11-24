package com.wandryhp.client.mixin;

import com.wandryhp.client.anim.WandAnimHolder;
import com.wandryhp.client.anim.WandPose;
import net.minecraft.client.model.ModelPart;
import net.minecraft.client.render.entity.model.BipedEntityModel;
import net.minecraft.client.render.entity.state.BipedEntityRenderState;
import net.minecraft.util.Arm;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Terceira pessoa: soma a pose da conjuração ao braço que segura a varinha, DEPOIS da pose normal do modelo
 * (por isso é aditiva: no repouso não há efeito e início/fim coincidem com a pose vanilla). O PlayerEntityModel
 * chama este método antes de copiar os braços para as mangas, então as mangas acompanham.
 *
 * <p>O modelo do jogador tem um osso só por braço (sem cotovelo): a flexão do antebraço entra, em parte, no
 * ângulo do ombro; o pulso e a varinha são animados no renderizador do item (HeldItemFeatureRendererMixin).
 */
@Mixin(BipedEntityModel.class)
public abstract class BipedEntityModelMixin {
	@Inject(method = "setAngles(Lnet/minecraft/client/render/entity/state/BipedEntityRenderState;)V", at = @At("TAIL"))
	private void wandryhp$applyCastPose(BipedEntityRenderState state, CallbackInfo ci) {
		if (!(state instanceof WandAnimHolder holder)) {
			return;
		}
		WandPose pose = holder.wandryhp$getPose();
		if (pose == null) {
			return;
		}
		BipedEntityModel<?> self = (BipedEntityModel<?>) (Object) this;
		boolean right = state.mainArm == Arm.RIGHT;
		ModelPart arm = right ? self.rightArm : self.leftArm;
		float side = right ? 1f : -1f;

		// pitch negativo levanta o braço à frente; o antebraço dobrado "empurra" a mão para cima/à frente.
		arm.pitch -= pose.shoulderPitch() + 0.55f * pose.elbow();
		// yaw positivo no braço direito abre para fora; "para o centro" = sentido oposto (espelhado na mão esquerda).
		arm.yaw -= side * (pose.shoulderYaw() + 0.35f * pose.wristYaw());
		arm.roll += side * pose.shoulderRoll();
	}
}
