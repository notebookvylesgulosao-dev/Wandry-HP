package com.wandryhp.client.mixin;

import com.wandryhp.client.anim.WandAnimHolder;
import com.wandryhp.client.anim.WandPose;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.feature.HeldItemFeatureRenderer;
import net.minecraft.client.render.entity.state.ArmedEntityRenderState;
import net.minecraft.client.render.item.ItemRenderState;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Arm;
import net.minecraft.util.math.RotationAxis;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Terceira pessoa: pulso e varinha. Roda logo antes de o item ser desenhado na mão, quando a matriz já está no
 * espaço da mão (eixo local Z = ao longo do antebraço, para o ombro; X = lateral; Y = direção da varinha).
 */
@Mixin(HeldItemFeatureRenderer.class)
public abstract class HeldItemFeatureRendererMixin {
	@Inject(method = "renderItem(Lnet/minecraft/client/render/entity/state/ArmedEntityRenderState;Lnet/minecraft/client/render/item/ItemRenderState;Lnet/minecraft/util/Arm;Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumerProvider;I)V",
			at = @At(value = "INVOKE",
					target = "Lnet/minecraft/client/render/item/ItemRenderState;render(Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumerProvider;II)V"))
	private void wandryhp$wristAndWand(ArmedEntityRenderState state, ItemRenderState itemState, Arm arm,
									   MatrixStack matrices, VertexConsumerProvider vertexConsumers, int light, CallbackInfo ci) {
		if (!(state instanceof WandAnimHolder holder) || arm != state.mainArm) {
			return;
		}
		WandPose pose = holder.wandryhp$getPose();
		if (pose == null) {
			return;
		}
		float side = arm == Arm.RIGHT ? 1f : -1f;
		// lateral (para o centro = +): giro em torno do eixo do antebraço; o chicote da varinha soma ao pulso.
		matrices.multiply(RotationAxis.POSITIVE_Z.rotation(side * (0.65f * pose.wristYaw() + pose.wandYaw())));
		matrices.multiply(RotationAxis.POSITIVE_X.rotation(pose.wristPitch()));
		matrices.multiply(RotationAxis.POSITIVE_Y.rotation(side * pose.wristRoll()));
	}
}
