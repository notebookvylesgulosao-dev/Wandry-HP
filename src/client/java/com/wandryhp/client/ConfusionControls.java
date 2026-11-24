package com.wandryhp.client;

import com.wandryhp.WandryHP;
import com.wandryhp.effect.ModEffects;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.option.GameOptions;
import net.minecraft.client.option.KeyBinding;

/**
 * Mapa de inversao de controles da Confusao: W<->S, A<->D, botao esquerdo<->direito
 * (e, se configurado, pular<->agachar).
 */
public final class ConfusionControls {
	private ConfusionControls() {}

	/** Tecla que deve responder no lugar de {@code key}, ou null se nao ha inversao ativa. */
	public static KeyBinding partnerOf(KeyBinding key) {
		MinecraftClient client = MinecraftClient.getInstance();
		ClientPlayerEntity player = client.player;
		if (player == null || client.currentScreen != null || ModEffects.CONFUSION == null
				|| !player.hasStatusEffect(ModEffects.CONFUSION)) {
			return null;
		}
		GameOptions o = client.options;
		if (key == o.forwardKey) return o.backKey;
		if (key == o.backKey) return o.forwardKey;
		if (key == o.leftKey) return o.rightKey;
		if (key == o.rightKey) return o.leftKey;
		if (key == o.attackKey) return o.useKey;
		if (key == o.useKey) return o.attackKey;
		if (WandryHP.CONFIG.confusionSwapJumpSneak) {
			if (key == o.jumpKey) return o.sneakKey;
			if (key == o.sneakKey) return o.jumpKey;
		}
		return null;
	}
}
