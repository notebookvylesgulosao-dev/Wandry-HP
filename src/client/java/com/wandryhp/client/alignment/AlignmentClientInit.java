package com.wandryhp.client.alignment;

import com.wandryhp.alignment.BadgePayload;
import com.wandryhp.alignment.PlayerBadge;
import com.wandryhp.client.spell.SpellClientState;
import com.wandryhp.spell.Spell;
import com.wandryhp.spell.SpellUnlocks;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.network.ClientPlayerEntity;

import java.util.UUID;

/** Entrypoint cliente do alinhamento: guarda casa/alinhamento de cada jogador (ícones do nick e feitiços liberados). */
public class AlignmentClientInit implements ClientModInitializer {
	@Override
	public void onInitializeClient() {
		ClientPlayNetworking.registerGlobalReceiver(BadgePayload.ID, (payload, context) -> context.client().execute(() -> {
			UUID uuid;
			try {
				uuid = UUID.fromString(payload.uuid());
			} catch (IllegalArgumentException e) {
				return;
			}
			PlayerBadge.putClient(uuid, payload.houseCode(), payload.alignment());

			// Trocou de alinhamento com um feitiço ativo na roda que agora está bloqueado: volta a "nenhum ativo".
			ClientPlayerEntity self = context.client().player;
			Spell selected = SpellClientState.selected();
			if (self != null && self.getUuid().equals(uuid) && selected != null && !SpellUnlocks.isUnlocked(self, selected)) {
				SpellClientState.select(null);
			}
		}));
		ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> PlayerBadge.clearClient());
	}
}
