package com.wandryhp;

import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;

/** Cliente -> servidor: botão esquerdo com uma varinha na mão (ataque base). */
public record BaseAttackPayload() implements CustomPayload {
    public static final CustomPayload.Id<BaseAttackPayload> ID = new CustomPayload.Id<>(WandryHP.id("base_attack"));
    public static final PacketCodec<RegistryByteBuf, BaseAttackPayload> CODEC = PacketCodec.unit(new BaseAttackPayload());
    @Override public CustomPayload.Id<? extends CustomPayload> getId() { return ID; }
}
