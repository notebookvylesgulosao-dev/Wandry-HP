package com.wandryhp.spell;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.entity.damage.DamageTypes;

/**
 * Entrypoint dos feitiços (registrado em fabric.mod.json depois de WandryHP, que já carregou a config
 * e registrou o efeito de Confusão).
 */
public class SpellsInit implements ModInitializer {
	@Override
	public void onInitialize() {
		ModSpellEntities.register();

		PayloadTypeRegistry.playC2S().register(CastSpellPayload.ID, CastSpellPayload.CODEC);
		// Servidor -> cliente: avisa o início da recarga de cada feitiço (só para a HUD de recarga).
		PayloadTypeRegistry.playS2C().register(SpellCooldownPayload.ID, SpellCooldownPayload.CODEC);
		ServerPlayNetworking.registerGlobalReceiver(CastSpellPayload.ID,
				(payload, context) -> SpellCaster.tryCast(context.player(), payload.spell()));

		ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> SpellCaster.forget(handler.getPlayer()));
		ServerTickEvents.END_SERVER_TICK.register(server -> DamageOverTime.tick());
		ServerLifecycleEvents.SERVER_STOPPED.register(server -> {
			SpellCaster.clear();
			DamageOverTime.clear();
			AccioPull.clear();
			Leviosa.clear();
			IncendioBurn.clear();
			PatronusEntity.clear();
		});

		// Accio, Wingardium Leviosa, Incendio, Lumos e Expecto Patronum.
		ServerTickEvents.END_SERVER_TICK.register(server -> {
			AccioPull.tick();
			Leviosa.tick();
			IncendioBurn.tick();
			LumosLight.tick(server);
		});
		ServerLivingEntityEvents.ALLOW_DAMAGE.register((entity, source, amount) -> {
			if (source.isOf(DamageTypes.FALL) && Leviosa.protects(entity)) {
				return false; // Leviosa: volta ao chão sem dano de queda
			}
			if (source.isOf(DamageTypes.ON_FIRE) && IncendioBurn.isBurning(entity)) {
				return false; // Incendio aplica o próprio dano de queimadura (limite de 3 corações)
			}
			return true;
		});
		ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> {
			Leviosa.forget(handler.getPlayer());
			LumosLight.turnOff(handler.getPlayer());
			PatronusEntity.forget(handler.getPlayer());
		});
		// Antes de salvar o mundo: devolve a gravidade (NoGravity seria salvo) e remove os blocos de luz.
		ServerLifecycleEvents.SERVER_STOPPING.register(server -> {
			Leviosa.releaseAll();
			LumosLight.clearAll(server);
		});
	}
}
