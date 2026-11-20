package com.wandryhp.network;

import com.wandryhp.WandryHP;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

/** Cliente -> servidor: o jogador clicou com o botao esquerdo segurando uma varinha. */
public record LaunchPayload() implements CustomPayload {
	public static final CustomPayload.Id<LaunchPayload> ID =
			new CustomPayload.Id<>(Identifier.of(WandryHP.MOD_ID, "launch"));
	public static final PacketCodec<RegistryByteBuf, LaunchPayload> CODEC = PacketCodec.unit(new LaunchPayload());

	@Override
	public CustomPayload.Id<? extends CustomPayload> getId() {
		return ID;
	}
}
