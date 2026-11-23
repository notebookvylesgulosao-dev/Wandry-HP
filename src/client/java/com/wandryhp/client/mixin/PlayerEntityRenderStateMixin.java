package com.wandryhp.client.mixin;

import com.wandryhp.client.anim.WandAnimHolder;
import com.wandryhp.client.anim.WandPose;
import net.minecraft.client.render.entity.state.PlayerEntityRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

/** Adiciona ao estado de renderização do jogador a pose atual da varinha (preenchida por PlayerEntityRendererMixin). */
@Mixin(PlayerEntityRenderState.class)
public abstract class PlayerEntityRenderStateMixin implements WandAnimHolder {
	@Unique
	private WandPose wandryhp$pose;

	@Override
	public WandPose wandryhp$getPose() {
		return wandryhp$pose;
	}

	@Override
	public void wandryhp$setPose(WandPose pose) {
		this.wandryhp$pose = pose;
	}
}
