package com.wandryhp.accessory;

import com.wandryhp.WandryHP;
import net.minecraft.item.Item;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.util.Identifier;

import java.util.EnumMap;
import java.util.Map;

public final class ModAccessoryItems {
	private static final Map<CapeType, Item> CAPES = new EnumMap<>(CapeType.class);

	private ModAccessoryItems() {}

	public static void register() {
		for (CapeType type : CapeType.values()) {
			RegistryKey<Item> key = RegistryKey.of(RegistryKeys.ITEM, Identifier.of(WandryHP.MOD_ID, type.itemId()));
			Item item = new EnchanterCapeItem(type, new Item.Settings().registryKey(key).maxCount(1));
			CAPES.put(type, Registry.register(Registries.ITEM, key, item));
		}
	}

	public static Item cape(CapeType type) {
		return CAPES.get(type);
	}
}
