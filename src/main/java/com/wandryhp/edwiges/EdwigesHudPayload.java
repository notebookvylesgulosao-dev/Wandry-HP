package com.wandryhp.edwiges;

import com.wandryhp.WandryHP;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;

/**
 * Servidor -> cliente: estado da HUD de chegada da Edwiges. So serve para a HUD (nao altera o jogo).
 *
 * <p>{@code state}: {@link #PROGRESS} (a coruja esta a caminho: {@code distance} atual e {@code start} inicial, em
 * blocos), {@link #DELIVERED} (soltou a carta: a HUD fica 3 s e sai) ou {@link #STOP} (a coruja sumiu sem entregar:
 * a HUD sai).
 */
public record EdwigesHudPayload(int state, float distance, float start) implements CustomPayload {
	public static final int STOP = 0;
	public static final int PROGRESS = 1;
	public static final int DELIVERED = 2;

	public static final CustomPayload.Id<EdwigesHudPayload> ID =
			new CustomPayload.Id<>(Identifier.of(WandryHP.MOD_ID, "edwiges_hud"));
	public static final PacketCodec<RegistryByteBuf, EdwigesHudPayload> CODEC = PacketCodec.tuple(
			PacketCodecs.VAR_INT, EdwigesHudPayload::state,
			PacketCodecs.FLOAT, EdwigesHudPayload::distance,
			PacketCodecs.FLOAT, EdwigesHudPayload::start,
			EdwigesHudPayload::new);

	public static void sendProgress(ServerPlayerEntity player, double distance, double start) {
		ServerPlayNetworking.send(player, new EdwigesHudPayload(PROGRESS, (float) distance, (float) start));
	}

	public static void sendDelivered(ServerPlayerEntity player) {
		ServerPlayNetworking.send(player, new EdwigesHudPayload(DELIVERED, 0.0f, 0.0f));
	}

	public static void sendStop(ServerPlayerEntity player) {
		ServerPlayNetworking.send(player, new EdwigesHudPayload(STOP, 0.0f, 0.0f));
	}

	@Override
	public CustomPayload.Id<? extends CustomPayload> getId() {
		return ID;
	}
}
