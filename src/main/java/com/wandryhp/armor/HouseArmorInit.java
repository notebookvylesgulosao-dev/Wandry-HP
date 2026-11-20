package com.wandryhp.armor;

import net.fabricmc.api.ModInitializer;

/**
 * Entrypoint das armaduras das casas (registrado em fabric.mod.json depois de WandryHP e SpellsInit):
 * registra os 6 itens e os bonus do conjunto.
 */
public class HouseArmorInit implements ModInitializer {
	@Override
	public void onInitialize() {
		ModArmorItems.register();
		HouseArmorSet.register();
	}
}
