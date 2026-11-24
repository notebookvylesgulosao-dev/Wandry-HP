package com.wandryhp.mana;

import com.mojang.serialization.Codec;
import com.wandryhp.WandryHP;
import com.wandryhp.spell.Spell;
import net.fabricmc.fabric.api.attachment.v1.AttachmentRegistry;
import net.fabricmc.fabric.api.attachment.v1.AttachmentType;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Sistema de mana (somente servidor decide). A mana fica num data attachment do jogador, salvo junto com ele.
 *
 * <p>Para a regeneracao de 3 MP/s ser exata (0,15 MP por tick), a mana e guardada em "unidades" de 1/20 MP:
 * 1 MP = {@link #UNITS_PER_MP} unidades, e a regeneracao e de {@link #REGEN_UNITS_PER_TICK} = 3 unidades por tick
 * (3 MP/s). Assim nao ha erro de ponto flutuante. O cliente so recebe valores inteiros de MP, e somente quando
 * o valor exibido muda (ver {@link ManaPayload}).
 */
public final class Mana {
	public static final int MAX_MP = 100;
	/** Regeneracao em MP por segundo. */
	public static final int REGEN_MP_PER_SECOND = 3;
	/** Custo, em MP, de cada lancamento de prop da varinha. */
	public static final int PROP_COST = 1;

	public static final int UNITS_PER_MP = 20;
	public static final int MAX_UNITS = MAX_MP * UNITS_PER_MP;
	/** 3 MP/s = 3 * 20 unidades / 20 ticks = 3 unidades por tick. */
	public static final int REGEN_UNITS_PER_TICK = REGEN_MP_PER_SECOND * UNITS_PER_MP / 20;

	/** Mana em unidades. Persistente (salva com o jogador); a sincronizacao e feita por {@link ManaPayload}. */
	public static final AttachmentType<Integer> DATA = AttachmentRegistry.create(
			Identifier.of(WandryHP.MOD_ID, "mana"),
			builder -> builder
					.initializer(() -> MAX_UNITS)
					.persistent(Codec.INT));

	/** Ultimo valor (MP inteiros) enviado a cada jogador, para so reenviar quando o valor exibido mudar. */
	private static final Map<UUID, Integer> LAST_SENT = new HashMap<>();

	private Mana() {}

	/** Custo em MP de um feitiço. */
	public static int cost(Spell spell) {
		return spell.manaCost;
	}

	private static int units(PlayerEntity player) {
		// clamp defensivo: um valor corrompido/editado no NBT nunca passa do maximo nem fica negativo
		return Math.max(0, Math.min(MAX_UNITS, player.getAttachedOrCreate(DATA)));
	}

	private static void setUnits(PlayerEntity player, int units) {
		player.setAttached(DATA, Math.max(0, Math.min(MAX_UNITS, units)));
	}

	/** Mana atual em MP inteiros (arredondada para baixo). */
	public static int get(PlayerEntity player) {
		return units(player) / UNITS_PER_MP;
	}

	public static boolean has(PlayerEntity player, int mp) {
		return units(player) >= mp * UNITS_PER_MP;
	}

	/** Gasta {@code mp} se houver o bastante; devolve false (sem alterar nada) se nao houver. */
	public static boolean tryConsume(PlayerEntity player, int mp) {
		int cost = mp * UNITS_PER_MP;
		int current = units(player);
		if (current < cost) {
			return false;
		}
		setUnits(player, current - cost);
		return true;
	}

	/** Devolve mana de um lancamento aceito que acabou cancelado antes de sair (nunca passa do maximo). */
	public static void refund(PlayerEntity player, int mp) {
		setUnits(player, units(player) + mp * UNITS_PER_MP);
	}

	/** Chamado a cada tick do servidor: regenera e sincroniza quando o valor exibido mudou. */
	public static void tick(MinecraftServer server) {
		for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
			int current = units(player);
			if (player.isAlive() && current < MAX_UNITS) {
				current = Math.min(MAX_UNITS, current + REGEN_UNITS_PER_TICK);
				setUnits(player, current);
			}
			int mp = current / UNITS_PER_MP;
			Integer sent = LAST_SENT.get(player.getUuid());
			if (sent == null || sent != mp) {
				send(player, mp);
			}
		}
	}

	/** Envia o valor atual ao proprio jogador (entrada no mundo / respawn / troca de dimensao). */
	public static void sync(ServerPlayerEntity player) {
		send(player, get(player));
	}

	private static void send(ServerPlayerEntity player, int mp) {
		LAST_SENT.put(player.getUuid(), mp);
		ServerPlayNetworking.send(player, new ManaPayload(mp, MAX_MP));
	}

	/** Esquece o ultimo valor enviado: o proximo tick reenvia (usado apos respawn/troca de mundo). */
	public static void invalidate(ServerPlayerEntity player) {
		LAST_SENT.remove(player.getUuid());
	}

	public static void clear() {
		LAST_SENT.clear();
	}
}
