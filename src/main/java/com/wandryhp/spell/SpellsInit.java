package com.wandryhp.spell;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;

/**
 * Entrypoint dos feitiços (registrado em fabric.mod.json depois de WandryHP, que já carregou a config
 * e registrou o efeito de Confusão).
 */
public class SpellsInit implements ModInitializer {
	@Override
	public void onInitialize() {
		ModSpellEntities.register();

		PayloadTypeRegistry.playC2S().register(CastSpellPayload.ID, CastSpellPayload.CODEC);
		ServerPlayNetworking.registerGlobalReceiver(CastSpellPayload.ID,
				(payload, context) -> SpellCaster.tryCast(context.player(), payload.spell()));

		ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> SpellCaster.forget(handler.getPlayer()));
		ServerTickEvents.END_SERVER_TICK.register(server -> DamageOverTime.tick());
		ServerLifecycleEvents.SERVER_STOPPED.register(server -> {
			SpellCaster.clear();
			DamageOverTime.clear();
		});
	}
}
