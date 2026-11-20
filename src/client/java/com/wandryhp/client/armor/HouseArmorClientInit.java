package com.wandryhp.client.armor;

import com.wandryhp.WandryHP;
import com.wandryhp.armor.HouseType;
import com.wandryhp.armor.ModArmorItems;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.rendering.v1.ArmorRenderer;
import net.fabricmc.fabric.api.resource.ResourceManagerHelper;
import net.fabricmc.fabric.api.resource.SimpleSynchronousResourceReloadListener;
import net.minecraft.resource.ResourceManager;
import net.minecraft.resource.ResourceType;
import net.minecraft.util.Identifier;

/**
 * Entrypoint cliente das armaduras das casas (registrado em fabric.mod.json depois de SpellsClientInit):
 * renderizador dos quatro peitorais, da calca e das botas e carregamento dos .bbmodel originais. A calca e as botas
 * tambem sao desenhadas dos .bbmodel (house_leggings / house_boots); o asset de equipamento vanilla
 * (equipment/house.json) existe so por exigencia do ArmorMaterial e usa texturas transparentes.
 */
public class HouseArmorClientInit implements ClientModInitializer {
	@Override
	public void onInitializeClient() {
		for (HouseType house : HouseType.values()) {
			ArmorRenderer.register(new HouseChestplateRenderer(house), ModArmorItems.chestplate(house));
		}

		ArmorRenderer.register(new HouseLegwearRenderer(false), ModArmorItems.LEGGINGS);
		ArmorRenderer.register(new HouseLegwearRenderer(true), ModArmorItems.BOOTS);

		ResourceManagerHelper.get(ResourceType.CLIENT_RESOURCES).registerReloadListener(
				new SimpleSynchronousResourceReloadListener() {
					@Override
					public Identifier getFabricId() {
						return Identifier.of(WandryHP.MOD_ID, "house_armor_models");
					}

					@Override
					public void reload(ResourceManager manager) {
						HouseArmorModels.reload(manager);
					}
				});
	}
}
