package com.wandryhp.client.broom;

import com.wandryhp.broom.BroomEntity;
import com.wandryhp.broom.BroomInit;
import com.wandryhp.broom.BroomInputPayload;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.GameOptions;

/**
 * Entrypoint cliente da vassoura (registrado em fabric.mod.json): renderizador e envio das teclas de voo.
 * As teclas sao lidas por isPressed(), entao o efeito de Confusao continua invertendo os controles.
 */
public class BroomClientInit implements ClientModInitializer {
	/** Ultimo estado enviado (-1 = nada enviado desde que montou). */
	private static int lastSent = -1;

	@Override
	public void onInitializeClient() {
		EntityRendererRegistry.register(BroomInit.ENTITY, BroomRenderer::new);

		ClientTickEvents.END_CLIENT_TICK.register(BroomClientInit::tick);
		ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> lastSent = -1);
	}

	private static void tick(MinecraftClient client) {
		if (client.player == null || !(client.player.getVehicle() instanceof BroomEntity broom)) {
			lastSent = -1;
			return;
		}
		int flags = 0;
		if (client.currentScreen == null) {
			GameOptions o = client.options;
			if (o.forwardKey.isPressed()) flags |= BroomEntity.FORWARD;
			if (o.backKey.isPressed()) flags |= BroomEntity.BACK;
			if (o.leftKey.isPressed()) flags |= BroomEntity.LEFT;
			if (o.rightKey.isPressed()) flags |= BroomEntity.RIGHT;
			if (o.jumpKey.isPressed()) flags |= BroomEntity.UP;
			if (o.sprintKey.isPressed()) flags |= BroomEntity.DOWN;
		}
		broom.setInput(flags); // o cliente do cavaleiro calcula o voo localmente (sem atraso)
		if (flags != lastSent) {
			ClientPlayNetworking.send(new BroomInputPayload(flags));
			lastSent = flags;
		}
	}
}
