package com.wandryhp.edwiges;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;

/** Entrypoint da Edwiges (registrado em fabric.mod.json): entidades, attachment de entrega e tick. */
public class EdwigesInit implements ModInitializer {
	@Override
	public void onInitialize() {
		ModEdwigesEntities.register();

		// Forca o registro do attachment antes de qualquer jogador entrar.
		EdwigesDelivery.DELIVERED.identifier();

		PayloadTypeRegistry.playS2C().register(EdwigesHudPayload.ID, EdwigesHudPayload.CODEC);

		ServerTickEvents.END_SERVER_TICK.register(EdwigesDelivery::tick);
		// Renascer / voltar do End: o novo jogador herda o estado da entrega.
		ServerPlayerEvents.COPY_FROM.register((oldPlayer, newPlayer, alive) ->
				newPlayer.setAttached(EdwigesDelivery.DELIVERED, EdwigesDelivery.isDelivered(oldPlayer)));
		// A HUD de chegada nao sobrevive a morte (a Edwiges some e uma nova aparece depois do renascimento).
		ServerPlayerEvents.AFTER_RESPAWN.register((oldPlayer, newPlayer, alive) -> EdwigesHudPayload.sendStop(newPlayer));
		ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> EdwigesDelivery.forget(handler.getPlayer()));
		ServerLifecycleEvents.SERVER_STOPPED.register(server -> EdwigesDelivery.clear());
	}
}
