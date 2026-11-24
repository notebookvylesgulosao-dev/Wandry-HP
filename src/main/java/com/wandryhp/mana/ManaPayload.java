package com.wandryhp.mana;

import com.wandryhp.WandryHP;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

/** Servidor -> cliente: mana atual e maxima do proprio jogador, em MP inteiros. So serve para a HUD. */
public record ManaPayload(int current, int max) implements CustomPayload {
	public static final CustomPayload.Id<ManaPayload> ID =
			new CustomPayload.Id<>(Identifier.of(WandryHP.MOD_ID, "mana"));
	public static final PacketCodec<RegistryByteBuf, ManaPayload> CODEC = PacketCodec.tuple(
			PacketCodecs.VAR_INT, ManaPayload::current,
			PacketCodecs.VAR_INT, ManaPayload::max,
			ManaPayload::new);

	@Override
	public CustomPayload.Id<? extends CustomPayload> getId() {
		return ID;
	}
}
