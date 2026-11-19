package com.wandryhp;

import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;

/** Cliente -> servidor: teclas pressionadas enquanto monta a vassoura (bits de BroomEntity). */
public record BroomInputPayload(int flags) implements CustomPayload {
    public static final CustomPayload.Id<BroomInputPayload> ID = new CustomPayload.Id<>(WandryHP.id("broom_input"));
    public static final PacketCodec<RegistryByteBuf, BroomInputPayload> CODEC =
        PacketCodec.tuple(PacketCodecs.VAR_INT, BroomInputPayload::flags, BroomInputPayload::new);
    @Override public CustomPayload.Id<? extends CustomPayload> getId() { return ID; }
}
