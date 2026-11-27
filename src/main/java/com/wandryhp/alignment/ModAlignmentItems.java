package com.wandryhp.alignment;

import com.wandryhp.WandryHP;
import net.minecraft.item.Item;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.util.Identifier;

/**
 * Os dois livros de alinhamento. Modelo e texturas vêm dos .bbmodel enviados (source_models/books/), convertidos por
 * tools/prepare_alignment_assets.py para assets/wandryhp/models/item/{good,bad}_book.json.
 */
public final class ModAlignmentItems {
	public static Item GOOD_BOOK;
	public static Item BAD_BOOK;

	private ModAlignmentItems() {}

	public static void register() {
		GOOD_BOOK = book("good_book", Alignment.GOOD);
		BAD_BOOK = book("bad_book", Alignment.EVIL);
	}

	private static Item book(String path, Alignment alignment) {
		RegistryKey<Item> key = RegistryKey.of(RegistryKeys.ITEM, Identifier.of(WandryHP.MOD_ID, path));
		return Registry.register(Registries.ITEM, key,
				new AlignmentBookItem(alignment, new Item.Settings().registryKey(key).maxCount(16)));
	}
}
