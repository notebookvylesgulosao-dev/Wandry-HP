package com.wandryhp.spell;

import com.wandryhp.WandryHP;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

/** Cliente -> servidor: o jogador clicou com o botão direito segurando uma varinha, com este feitiço ativo. */
public record CastSpellPayload(int spell) implements CustomPayload {
	public static final CustomPayload.Id<CastSpellPayload> ID =
			new CustomPayload.Id<>(Identifier.of(WandryHP.MOD_ID, "cast_spell"));
	public static final PacketCodec<RegistryByteBuf, CastSpellPayload> CODEC =
			PacketCodec.tuple(PacketCodecs.VAR_INT, CastSpellPayload::spell, CastSpellPayload::new);

	@Override
	public CustomPayload.Id<? extends CustomPayload> getId() {
		return ID;
	}
}
