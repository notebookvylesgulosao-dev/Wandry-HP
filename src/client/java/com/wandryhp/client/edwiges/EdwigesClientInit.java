package com.wandryhp.client.edwiges;

import com.wandryhp.WandryHP;
import com.wandryhp.edwiges.EdwigesHudPayload;
import com.wandryhp.edwiges.ModEdwigesEntities;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.HudLayerRegistrationCallback;
import net.fabricmc.fabric.api.client.rendering.v1.IdentifiedLayer;
import net.minecraft.util.Identifier;

/** Entrypoint cliente da Edwiges (registrado em fabric.mod.json): renderizadores da coruja/envelope e HUD de chegada. */
public class EdwigesClientInit implements ClientModInitializer {
	private static final Identifier HUD_LAYER = Identifier.of(WandryHP.MOD_ID, "edwiges_arrival");

	@Override
	public void onInitializeClient() {
		EntityRendererRegistry.register(ModEdwigesEntities.EDWIGES, EdwigesRenderer::new);
		EntityRendererRegistry.register(ModEdwigesEntities.LETTER, LetterRenderer::new);

		// HUD de chegada: o servidor informa a distancia; o cliente anima a faixa e a barra.
		ClientPlayNetworking.registerGlobalReceiver(EdwigesHudPayload.ID,
				(payload, context) -> context.client().execute(() -> EdwigesHudState.onPacket(payload)));
		ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> EdwigesHudState.clear());
		HudLayerRegistrationCallback.EVENT.register(
				layeredDrawer -> layeredDrawer.attachLayerAfter(IdentifiedLayer.HOTBAR_AND_BARS, HUD_LAYER, EdwigesHud::render));
	}
}
