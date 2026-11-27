package com.wandryhp.alignment;

import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Mantém os clientes informados da casa e do alinhamento de cada jogador. A casa muda sozinha (quando a análise
 * termina) e o alinhamento muda por livro; em vez de ligar cada origem a um aviso, o servidor confere a cada segundo
 * e só envia quando algo mudou. Mudanças de alinhamento por livro chamam {@link #refresh} na hora.
 */
public final class AlignmentSync {
	private static final int CHECK_INTERVAL_TICKS = 20;

	/** Último valor enviado por jogador (houseCode * 4 + alinhamento). */
	private static final Map<UUID, Integer> LAST = new HashMap<>();

	private AlignmentSync() {}

	public static void tick(MinecraftServer server) {
		if (server.getTicks() % CHECK_INTERVAL_TICKS != 0) {
			return;
		}
		for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
			refresh(player);
		}
	}

	/** Envia o estado do jogador a todos, se mudou desde o último envio. */
	public static void refresh(ServerPlayerEntity player) {
		MinecraftServer server = player.getServer();
		if (server == null) {
			return;
		}
		BadgePayload payload = payloadOf(player);
		int code = payload.houseCode() * 4 + payload.alignment();
		Integer last = LAST.get(player.getUuid());
		if (last != null && last == code) {
			return;
		}
		LAST.put(player.getUuid(), code);
		for (ServerPlayerEntity other : server.getPlayerManager().getPlayerList()) {
			ServerPlayNetworking.send(other, payload);
		}
	}

	/** Entrou no mundo: recebe o estado de quem já está online e todos recebem o dele. */
	public static void onJoin(ServerPlayerEntity joining, MinecraftServer server) {
		for (ServerPlayerEntity other : server.getPlayerManager().getPlayerList()) {
			if (other != joining) {
				ServerPlayNetworking.send(joining, payloadOf(other));
			}
		}
		LAST.remove(joining.getUuid());
		refresh(joining);
	}

	public static void forget(ServerPlayerEntity player) {
		LAST.remove(player.getUuid());
	}

	public static void clear() {
		LAST.clear();
	}

	private static BadgePayload payloadOf(ServerPlayerEntity player) {
		PlayerBadge.Badge badge = PlayerBadge.of(player);
		return new BadgePayload(player.getUuid().toString(), badge.houseCode(), badge.alignment().ordinal());
	}
}
