package com.wandryhp.entity;

import com.wandryhp.config.WandryConfig;
import com.wandryhp.item.WandType;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;

/** Efeitos aplicados ao alvo atingido, por varinha. (Confusao fica registrada em ModEffects para uso futuro.) */
public final class WandEffects {
	private WandEffects() {}

	public static void apply(WandType type, WandryConfig.WandSettings s, LivingEntity target, Entity source) {
		switch (type) {
			case AMETHYST -> add(target, new StatusEffectInstance(StatusEffects.SLOWNESS, s.effectDurationTicks, s.effectAmplifier), s, source);
			case BONE -> add(target, new StatusEffectInstance(StatusEffects.BLINDNESS, s.effectDurationTicks, s.effectAmplifier), s, source);
			case CELESTIAL -> add(target, new StatusEffectInstance(StatusEffects.GLOWING, s.effectDurationTicks, s.effectAmplifier), s, source);
			case WIDOW -> {
				if (s.fireSeconds > 0) {
					target.setOnFireFor(s.fireSeconds);
				}
				add(target, new StatusEffectInstance(StatusEffects.SLOWNESS, s.effectDurationTicks, s.effectAmplifier), s, source);
			}
		}
	}

	private static void add(LivingEntity target, StatusEffectInstance effect, WandryConfig.WandSettings s, Entity source) {
		if (s.effectDurationTicks > 0) { // 0 = nao definido/desativado
			// Capa Enchanter Azul: +20% de duracao dos efeitos aplicados pelo dono.
			int duration = com.wandryhp.accessory.AccessoryEffects.scaleEffectDuration(source, effect.getDuration());
			target.addStatusEffect(new StatusEffectInstance(effect.getEffectType(), duration, effect.getAmplifier()), source);
		}
	}
}
