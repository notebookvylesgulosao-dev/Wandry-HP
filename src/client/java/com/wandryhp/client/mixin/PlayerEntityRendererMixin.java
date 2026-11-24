package com.wandryhp.client.mixin;

import com.wandryhp.client.anim.WandAnimClient;
import com.wandryhp.client.anim.WandAnimHolder;
import com.wandryhp.item.WandItem;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.render.entity.PlayerEntityRenderer;
import net.minecraft.client.render.entity.state.PlayerEntityRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Terceira pessoa (e Shoulder Surfing Reloaded, que usa este mesmo renderizador): copia a pose da conjuração
 * do jogador para o estado de renderização, de onde o modelo e o renderizador do item a leem.
 */
@Mixin(PlayerEntityRenderer.class)
public abstract class PlayerEntityRendererMixin {
	@Inject(method = "updateRenderState(Lnet/minecraft/client/network/AbstractClientPlayerEntity;Lnet/minecraft/client/render/entity/state/PlayerEntityRenderState;F)V",
			at = @At("TAIL"))
	private void wandryhp$fillWandPose(AbstractClientPlayerEntity player, PlayerEntityRenderState state, float tickDelta, CallbackInfo ci) {
		boolean holdsWand = player.getMainHandStack().getItem() instanceof WandItem;
		((WandAnimHolder) state).wandryhp$setPose(holdsWand ? WandAnimClient.poseFor(player.getId(), tickDelta) : null);
	}
}
