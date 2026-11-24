package com.wandryhp.mana;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.entity.event.v1.ServerEntityWorldChangeEvents;
import net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;

/** Entrypoint da mana (registrado em fabric.mod.json): attachment, regeneracao e sincronizacao com a HUD. */
public class ManaInit implements ModInitializer {
	@Override
	public void onInitialize() {
		// Forca o registro do attachment antes de qualquer jogador entrar.
		Mana.DATA.identifier();

		PayloadTypeRegistry.playS2C().register(ManaPayload.ID, ManaPayload.CODEC);

		ServerTickEvents.END_SERVER_TICK.register(Mana::tick);
		ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> Mana.sync(handler.getPlayer()));
		ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> Mana.invalidate(handler.getPlayer()));
		ServerPlayerEvents.AFTER_RESPAWN.register((oldPlayer, newPlayer, alive) -> Mana.sync(newPlayer));
		ServerEntityWorldChangeEvents.AFTER_PLAYER_CHANGE_WORLD.register(
				(player, origin, destination) -> Mana.sync(player));
		ServerLifecycleEvents.SERVER_STOPPED.register(server -> Mana.clear());
	}
}
