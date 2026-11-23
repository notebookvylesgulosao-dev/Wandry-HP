package com.wandryhp.client.accessory;

import com.wandryhp.WandryHP;
import com.wandryhp.accessory.AccessorySlot;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.rendering.v1.LivingEntityFeatureRendererRegistrationCallback;
import net.fabricmc.fabric.api.resource.ResourceManagerHelper;
import net.fabricmc.fabric.api.resource.SimpleSynchronousResourceReloadListener;
import net.minecraft.client.render.entity.PlayerEntityRenderer;
import net.minecraft.entity.EntityType;
import net.minecraft.resource.ResourceManager;
import net.minecraft.resource.ResourceType;
import net.minecraft.util.Identifier;

/** Entrypoint cliente dos acessorios: modelos das capas, renderizador nas costas e estado do painel. */
public class AccessoryClientInit implements ClientModInitializer {
	@Override
	public void onInitializeClient() {
		AccessorySlot.PANEL_OPEN = () -> AccessoryClientState.open;

		LivingEntityFeatureRendererRegistrationCallback.EVENT.register((entityType, entityRenderer, registrationHelper, context) -> {
			if (entityType == EntityType.PLAYER && entityRenderer instanceof PlayerEntityRenderer playerRenderer) {
				registrationHelper.register(new CapeFeatureRenderer(playerRenderer));
			}
		});

		ResourceManagerHelper.get(ResourceType.CLIENT_RESOURCES).registerReloadListener(
				new SimpleSynchronousResourceReloadListener() {
					@Override
					public Identifier getFabricId() {
						return Identifier.of(WandryHP.MOD_ID, "cape_models");
					}

					@Override
					public void reload(ResourceManager manager) {
						CapeModels.reload(manager);
					}
				});
	}
}
