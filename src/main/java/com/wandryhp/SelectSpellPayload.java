package com.wandryhp;

import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;

/** Cliente -> servidor: feitiço escolhido na roda para a varinha na mão. */
public record SelectSpellPayload(int spell) implements CustomPayload {
    public static final CustomPayload.Id<SelectSpellPayload> ID = new CustomPayload.Id<>(WandryHP.id("select_spell"));
    public static final PacketCodec<RegistryByteBuf, SelectSpellPayload> CODEC =
        PacketCodec.tuple(PacketCodecs.VAR_INT, SelectSpellPayload::spell, SelectSpellPayload::new);
    @Override public CustomPayload.Id<? extends CustomPayload> getId() { return ID; }
}
