package com.wandryhp.alignment;

import com.wandryhp.WandryHP;
import net.fabricmc.fabric.api.attachment.v1.AttachmentRegistry;
import net.fabricmc.fabric.api.attachment.v1.AttachmentType;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;

/**
 * Alinhamento de cada jogador. No servidor é um data attachment persistente (gravado junto com o jogador, na pasta
 * do mundo: sobrevive a relogar, morrer e reiniciar o servidor). No cliente, o servidor envia o alinhamento de todos
 * os jogadores (ver {@link BadgePayload}); a leitura é sempre por {@link #of(PlayerEntity)}.
 */
public final class AlignmentData {
	public static final AttachmentType<Alignment> DATA = AttachmentRegistry.create(
			Identifier.of(WandryHP.MOD_ID, "alignment"),
			builder -> builder
					.initializer(() -> Alignment.NEUTRAL)
					.persistent(Alignment.CODEC));

	private AlignmentData() {}

	/** Alinhamento atual do jogador (Neutro enquanto nenhum livro foi usado, ou se o cliente ainda não foi informado). */
	public static Alignment of(PlayerEntity player) {
		if (player instanceof ServerPlayerEntity) {
			return player.getAttachedOrCreate(DATA);
		}
		PlayerBadge.Badge badge = PlayerBadge.cached(player.getUuid());
		return badge == null ? Alignment.NEUTRAL : badge.alignment();
	}

	/** Só no servidor: grava o novo alinhamento e avisa os clientes. */
	public static void set(ServerPlayerEntity player, Alignment alignment) {
		player.setAttached(DATA, alignment);
		AlignmentSync.refresh(player);
	}
}
