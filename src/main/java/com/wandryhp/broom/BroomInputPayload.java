package com.wandryhp.broom;

import com.wandryhp.WandryHP;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

/** Cliente -> servidor: teclas de voo pressionadas por quem esta montado na vassoura (bits de {@link BroomEntity}). */
public record BroomInputPayload(int flags) implements CustomPayload {
	public static final CustomPayload.Id<BroomInputPayload> ID =
			new CustomPayload.Id<>(Identifier.of(WandryHP.MOD_ID, "broom_input"));
	public static final PacketCodec<RegistryByteBuf, BroomInputPayload> CODEC = PacketCodec.tuple(
			PacketCodecs.VAR_INT, BroomInputPayload::flags,
			BroomInputPayload::new);

	@Override
	public CustomPayload.Id<? extends CustomPayload> getId() {
		return ID;
	}
}
