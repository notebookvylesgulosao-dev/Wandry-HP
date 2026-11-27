package com.wandryhp.alignment;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;

/** Entrypoint do sistema de alinhamento (registrado em fabric.mod.json): attachment, livros e sincronização. */
public class AlignmentInit implements ModInitializer {
	@Override
	public void onInitialize() {
		// Força o registro do attachment (campo estático) antes de qualquer jogador entrar.
		AlignmentData.DATA.identifier();
		ModAlignmentItems.register();

		PayloadTypeRegistry.playS2C().register(BadgePayload.ID, BadgePayload.CODEC);
		ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> AlignmentSync.onJoin(handler.getPlayer(), server));
		ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> AlignmentSync.forget(handler.getPlayer()));
		ServerTickEvents.END_SERVER_TICK.register(AlignmentSync::tick);
		ServerLifecycleEvents.SERVER_STOPPED.register(server -> AlignmentSync.clear());

		// Renascer / voltar do End: o novo jogador herda o alinhamento (morrer não zera).
		ServerPlayerEvents.COPY_FROM.register((oldPlayer, newPlayer, alive) ->
				newPlayer.setAttached(AlignmentData.DATA, AlignmentData.of(oldPlayer)));
	}
}
