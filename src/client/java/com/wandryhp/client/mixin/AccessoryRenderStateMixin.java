package com.wandryhp.client.mixin;

import com.wandryhp.accessory.CapeType;
import com.wandryhp.client.accessory.AccessoryRenderHolder;
import net.minecraft.client.render.entity.state.PlayerEntityRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

/** Carrega a capa equipada no estado de renderizacao do jogador (preenchida por AccessoryRendererMixin). */
@Mixin(PlayerEntityRenderState.class)
public abstract class AccessoryRenderStateMixin implements AccessoryRenderHolder {
	@Unique
	private CapeType wandryhp$cape;

	@Override
	public CapeType wandryhp$getCape() {
		return wandryhp$cape;
	}

	@Override
	public void wandryhp$setCape(CapeType cape) {
		this.wandryhp$cape = cape;
	}
}
