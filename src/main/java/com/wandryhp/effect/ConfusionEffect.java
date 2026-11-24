package com.wandryhp.effect;

import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.entity.effect.StatusEffectCategory;

/**
 * Confusao: inverte os controles de movimento e ataque do jogador afetado.
 * A inversao em si e aplicada no cliente (ver KeyBindingMixin / ConfusionControls).
 */
public class ConfusionEffect extends StatusEffect {
	public ConfusionEffect() {
		super(StatusEffectCategory.HARMFUL, 0x2EE6D6);
	}
}
