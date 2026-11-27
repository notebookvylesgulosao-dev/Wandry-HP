package com.wandryhp.client.spell;

import com.wandryhp.WandryHP;
import com.wandryhp.spell.SpellCooldownPayload;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.rendering.v1.HudLayerRegistrationCallback;
import net.fabricmc.fabric.api.client.rendering.v1.IdentifiedLayer;
import net.minecraft.util.Identifier;

/** Entrypoint cliente da HUD de recarga dos feitiços: recebe o início de cada recarga e desenha a lista. */
public class SpellCooldownClientInit implements ClientModInitializer {
	private static final Identifier HUD_LAYER = Identifier.of(WandryHP.MOD_ID, "spell_cooldowns");

	@Override
	public void onInitializeClient() {
		ClientPlayNetworking.registerGlobalReceiver(SpellCooldownPayload.ID,
				(payload, context) -> context.client().execute(() -> SpellCooldownHud.start(payload.spell(), payload.ticks())));
		ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> SpellCooldownHud.clear());

		HudLayerRegistrationCallback.EVENT.register(
				layeredDrawer -> layeredDrawer.attachLayerAfter(IdentifiedLayer.HOTBAR_AND_BARS, HUD_LAYER, SpellCooldownHud::render));
	}
}
