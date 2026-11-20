package com.wandryhp.client;

import com.wandryhp.WandryHP;
import com.wandryhp.client.render.PropModels;
import com.wandryhp.client.render.WandPropRenderer;
import com.wandryhp.entity.ModEntities;
import com.wandryhp.item.WandItem;
import com.wandryhp.network.LaunchPayload;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.player.ClientPreAttackCallback;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;
import net.fabricmc.fabric.api.resource.ResourceManagerHelper;
import net.fabricmc.fabric.api.resource.SimpleSynchronousResourceReloadListener;
import net.minecraft.resource.ResourceManager;
import net.minecraft.resource.ResourceType;
import net.minecraft.util.Identifier;

public class WandryHPClient implements ClientModInitializer {
	@Override
	public void onInitializeClient() {
		EntityRendererRegistry.register(ModEntities.WAND_PROP, WandPropRenderer::new);

		// Carrega a geometria dos props direto dos .bbmodel originais.
		ResourceManagerHelper.get(ResourceType.CLIENT_RESOURCES).registerReloadListener(
				new SimpleSynchronousResourceReloadListener() {
					@Override
					public Identifier getFabricId() {
						return Identifier.of(WandryHP.MOD_ID, "prop_models");
					}

					@Override
					public void reload(ResourceManager manager) {
						PropModels.reload(manager);
					}
				});

		// Botao esquerdo com uma varinha na mao principal: lanca o prop (o servidor valida o cooldown)
		// e cancela o ataque/quebra de bloco vanilla.
		ClientPreAttackCallback.EVENT.register((client, player, clickCount) -> {
			if (!(player.getMainHandStack().getItem() instanceof WandItem)) {
				return false;
			}
			if (clickCount > 0) {
				ClientPlayNetworking.send(new LaunchPayload());
			}
			return true;
		});
	}
}
