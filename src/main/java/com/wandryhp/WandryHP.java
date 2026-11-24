package com.wandryhp;

import com.wandryhp.config.WandryConfig;
import com.wandryhp.effect.ModEffects;
import com.wandryhp.entity.ModEntities;
import com.wandryhp.item.ModItems;
import com.wandryhp.network.LaunchPayload;
import com.wandryhp.network.WandAnimPayload;
import com.wandryhp.network.WandCasting;
import com.wandryhp.network.WandLauncher;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;

public class WandryHP implements ModInitializer {
	public static final String MOD_ID = "wandryhp";

	/** Id do prop usado quando a varinha nao possui um prop proprio. */
	public static final String UNIVERSAL_PROP_ID = "universal";

	public static WandryConfig CONFIG = new WandryConfig();

	@Override
	public void onInitialize() {
		CONFIG = WandryConfig.loadOrCreate();

		ModItems.register();
		ModEntities.register();
		ModEffects.register();

		PayloadTypeRegistry.playC2S().register(LaunchPayload.ID, LaunchPayload.CODEC);
		ServerPlayNetworking.registerGlobalReceiver(LaunchPayload.ID,
				(payload, context) -> WandLauncher.tryLaunch(context.player()));
		ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> WandLauncher.forget(handler.getPlayer()));

		// Sequencia conjuracao -> lancamento (compartilhada por props e feitiços): o servidor avisa os clientes
		// da fase da animacao e so cria o projetil depois que a conjuracao terminou.
		PayloadTypeRegistry.playS2C().register(WandAnimPayload.ID, WandAnimPayload.CODEC);
		ServerTickEvents.END_SERVER_TICK.register(WandCasting::tick);
		ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> WandCasting.forget(handler.getPlayer()));
		ServerLifecycleEvents.SERVER_STOPPED.register(server -> WandCasting.clear());
	}
}
