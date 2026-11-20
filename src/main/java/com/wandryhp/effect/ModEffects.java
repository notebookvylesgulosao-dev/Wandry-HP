package com.wandryhp.effect;

import com.wandryhp.WandryHP;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.util.Identifier;

public final class ModEffects {
	public static RegistryEntry<StatusEffect> CONFUSION;

	private ModEffects() {}

	public static void register() {
		CONFUSION = Registry.registerReference(Registries.STATUS_EFFECT,
				Identifier.of(WandryHP.MOD_ID, "confusion"), new ConfusionEffect());
	}
}
