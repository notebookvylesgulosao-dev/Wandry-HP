package com.wandryhp.client.mana;

import com.wandryhp.WandryHP;
import com.wandryhp.mana.ManaPayload;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.rendering.v1.HudLayerRegistrationCallback;
import net.fabricmc.fabric.api.client.rendering.v1.IdentifiedLayer;
import net.minecraft.util.Identifier;

/** Entrypoint cliente da mana: recebe os valores do servidor e desenha a barra na HUD. */
public class ManaClientInit implements ClientModInitializer {
	private static final Identifier HUD_LAYER = Identifier.of(WandryHP.MOD_ID, "mana_bar");

	@Override
	public void onInitializeClient() {
		ClientPlayNetworking.registerGlobalReceiver(ManaPayload.ID,
				(payload, context) -> context.client().execute(() -> ManaClientState.set(payload.current(), payload.max())));
		ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> ManaClientState.clear());

		HudLayerRegistrationCallback.EVENT.register(
				layeredDrawer -> layeredDrawer.attachLayerAfter(IdentifiedLayer.HOTBAR_AND_BARS, HUD_LAYER, ManaHud::render));
	}
}
