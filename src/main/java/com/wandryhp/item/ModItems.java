package com.wandryhp.item;

import com.wandryhp.WandryHP;
import net.minecraft.item.Item;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.util.Identifier;

import java.util.EnumMap;
import java.util.Map;

public final class ModItems {
	private static final Map<WandType, Item> WANDS = new EnumMap<>(WandType.class);

	private ModItems() {}

	public static void register() {
		for (WandType type : WandType.values()) {
			Identifier id = Identifier.of(WandryHP.MOD_ID, type.id + "_wand");
			RegistryKey<Item> key = RegistryKey.of(RegistryKeys.ITEM, id);
			Item item = new WandItem(type, new Item.Settings().registryKey(key).maxCount(1));
			WANDS.put(type, Registry.register(Registries.ITEM, key, item));
		}
	}

	public static Item get(WandType type) {
		return WANDS.get(type);
	}
}
