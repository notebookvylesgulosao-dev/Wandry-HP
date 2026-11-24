package com.wandryhp.network;

import com.wandryhp.WandryHP;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

/**
 * Servidor -> clientes (o proprio jogador e quem o ve): fase da animacao da varinha daquele jogador.
 * O cliente so anima; quem decide quando o projetil nasce e sempre o servidor ({@link WandCasting}).
 */
public record WandAnimPayload(int entityId, int phase) implements CustomPayload {
	/** Animacao de conjuracao (~1,2 s). O projetil NAO existe nesta fase. */
	public static final int CAST = 0;
	/** Animacao de lancamento, tocada no instante em que o servidor cria o projetil. */
	public static final int LAUNCH = 1;
	/** Conjuracao interrompida (trocou de item, morreu...): o cliente suaviza de volta ao repouso. */
	public static final int CANCEL = 2;

	public static final CustomPayload.Id<WandAnimPayload> ID =
			new CustomPayload.Id<>(Identifier.of(WandryHP.MOD_ID, "wand_anim"));
	public static final PacketCodec<RegistryByteBuf, WandAnimPayload> CODEC = PacketCodec.tuple(
			PacketCodecs.VAR_INT, WandAnimPayload::entityId,
			PacketCodecs.VAR_INT, WandAnimPayload::phase,
			WandAnimPayload::new);

	@Override
	public CustomPayload.Id<? extends CustomPayload> getId() {
		return ID;
	}
}
