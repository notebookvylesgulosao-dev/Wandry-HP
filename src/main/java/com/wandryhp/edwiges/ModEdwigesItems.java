package com.wandryhp.edwiges;

import com.wandryhp.WandryHP;
import com.wandryhp.armor.HouseType;
import net.minecraft.item.Item;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.util.Identifier;

import java.util.EnumMap;
import java.util.Map;

/** Itens da entrega: o envelope (solto pela Edwiges) e as 4 cartas das casas (resultado do clique direito no envelope). */
public final class ModEdwigesItems {
	public static Item ENVELOPE;
	private static final Map<HouseType, Item> LETTERS = new EnumMap<>(HouseType.class);

	private ModEdwigesItems() {}

	public static void register() {
		RegistryKey<Item> envKey = RegistryKey.of(RegistryKeys.ITEM, Identifier.of(WandryHP.MOD_ID, "envelope"));
		ENVELOPE = Registry.register(Registries.ITEM, envKey, new EnvelopeItem(new Item.Settings().registryKey(envKey).maxCount(1)));
		for (HouseType house : HouseType.values()) {
			RegistryKey<Item> key = RegistryKey.of(RegistryKeys.ITEM, Identifier.of(WandryHP.MOD_ID, "letter_" + house.id));
			LETTERS.put(house, Registry.register(Registries.ITEM, key, new Item(new Item.Settings().registryKey(key).maxCount(1))));
		}
	}

	public static Item letter(HouseType house) {
		return LETTERS.get(house);
	}
}
