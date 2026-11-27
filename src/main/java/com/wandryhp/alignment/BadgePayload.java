package com.wandryhp.alignment;

import com.wandryhp.WandryHP;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

/**
 * Servidor -> cliente: casa e alinhamento de UM jogador (identificado pelo UUID em texto). Enviado a todos quando
 * algo muda e, ao entrar, o de cada jogador já online. Alimenta o ícone ao lado do nick (cima da cabeça e TAB) e a
 * checagem de feitiços do próprio cliente.
 *
 * @param houseCode 0 = sem casa ainda; senão ordinal da HouseType + 1
 */
public record BadgePayload(String uuid, int houseCode, int alignment) implements CustomPayload {
	public static final CustomPayload.Id<BadgePayload> ID =
			new CustomPayload.Id<>(Identifier.of(WandryHP.MOD_ID, "badge"));
	public static final PacketCodec<RegistryByteBuf, BadgePayload> CODEC = PacketCodec.tuple(
			PacketCodecs.STRING, BadgePayload::uuid,
			PacketCodecs.VAR_INT, BadgePayload::houseCode,
			PacketCodecs.VAR_INT, BadgePayload::alignment,
			BadgePayload::new);

	@Override
	public CustomPayload.Id<? extends CustomPayload> getId() {
		return ID;
	}
}
