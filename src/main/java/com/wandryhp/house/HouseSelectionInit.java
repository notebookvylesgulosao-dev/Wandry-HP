package com.wandryhp.house;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.entity.event.v1.EntitySleepEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerEntityEvents;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.server.network.ServerPlayerEntity;

/** Entrypoint da selecao de casas (registrado em fabric.mod.json): attachment e todos os eventos que pontuam. */
public class HouseSelectionInit implements ModInitializer {
	@Override
	public void onInitialize() {
		// Forca o registro do attachment (campo estatico) antes de qualquer jogador entrar.
		HouseSelection.DATA.identifier();

		// Fogueira: registra o componente de marca e marca os itens que caem de uma fogueira acesa.
		CampfireCooking.init();
		ServerEntityEvents.ENTITY_LOAD.register(CampfireCooking::onEntityLoad);
		// Rede de seguranca: a marca de "fogueira acesa" nunca atravessa um tick.
		ServerTickEvents.END_SERVER_TICK.register(server -> CampfireCooking.end());

		ServerTickEvents.END_SERVER_TICK.register(HouseSelection::tick);
		ServerLivingEntityEvents.AFTER_DEATH.register(HouseSelection::onEntityDeath);
		ServerLivingEntityEvents.AFTER_DAMAGE.register(HouseSelection::onDamaged);
		ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> HouseSelection.onJoin(handler.getPlayer()));

		PlayerBlockBreakEvents.AFTER.register((world, player, pos, state, blockEntity) -> {
			if (player instanceof ServerPlayerEntity serverPlayer) {
				HouseSelection.onBlockBroken(serverPlayer, state);
			}
		});
		EntitySleepEvents.STOP_SLEEPING.register((entity, sleepingPos) -> {
			if (entity instanceof ServerPlayerEntity player) {
				HouseSelection.onWake(player);
			}
		});

		// Renascer / voltar do End: o novo jogador herda o progresso (senao morrer reiniciaria a analise).
		ServerPlayerEvents.COPY_FROM.register((oldPlayer, newPlayer, alive) ->
				newPlayer.setAttached(HouseSelection.DATA, HouseSelection.get(oldPlayer)));
	}
}
