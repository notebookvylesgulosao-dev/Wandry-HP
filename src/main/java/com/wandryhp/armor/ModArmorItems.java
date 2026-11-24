package com.wandryhp.armor;

import com.wandryhp.WandryHP;
import net.minecraft.item.Item;
import net.minecraft.item.equipment.EquipmentType;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.util.Identifier;

import java.util.EnumMap;
import java.util.Map;

/**
 * Registro dos 6 itens de armadura: 4 peitorais (um por casa), 1 calca e 1 bota compartilhadas.
 * Mesmo padrao de {@code ModItems}: Identifier + RegistryKey + Registry.register.
 */
public final class ModArmorItems {
	private static final Map<HouseType, Item> CHESTPLATES = new EnumMap<>(HouseType.class);

	/** Calca unica, usada pelas quatro casas. */
	public static Item LEGGINGS;
	/** Bota unica, usada pelas quatro casas. */
	public static Item BOOTS;

	private ModArmorItems() {}

	public static void register() {
		for (HouseType house : HouseType.values()) {
			RegistryKey<Item> key = key(house.id + "_chestplate");
			Item item = new HouseChestplateItem(house, settings(key, EquipmentType.CHESTPLATE));
			CHESTPLATES.put(house, Registry.register(Registries.ITEM, key, item));
		}

		RegistryKey<Item> leggingsKey = key("house_leggings");
		LEGGINGS = Registry.register(Registries.ITEM, leggingsKey,
				new Item(settings(leggingsKey, EquipmentType.LEGGINGS)));

		RegistryKey<Item> bootsKey = key("house_boots");
		BOOTS = Registry.register(Registries.ITEM, bootsKey,
				new Item(settings(bootsKey, EquipmentType.BOOTS)));
	}

	public static Item chestplate(HouseType house) {
		return CHESTPLATES.get(house);
	}

	private static RegistryKey<Item> key(String path) {
		return RegistryKey.of(RegistryKeys.ITEM, Identifier.of(WandryHP.MOD_ID, path));
	}

	/** Durabilidade, atributos de armadura, slot equipavel e reparo vem do ArmorMaterial (sem maxCount: armadura tem durabilidade). */
	private static Item.Settings settings(RegistryKey<Item> key, EquipmentType type) {
		return HouseArmorMaterial.MATERIAL.applySettings(new Item.Settings().registryKey(key), type);
	}
}
