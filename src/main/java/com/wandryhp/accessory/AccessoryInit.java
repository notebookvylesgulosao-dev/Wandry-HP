package com.wandryhp.accessory;

import com.wandryhp.WandryHP;
import com.wandryhp.armor.HouseType;
import com.wandryhp.armor.ModArmorItems;
import com.wandryhp.broom.BroomInit;
import com.wandryhp.item.ModItems;
import com.wandryhp.item.WandType;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.itemgroup.v1.FabricItemGroup;
import net.minecraft.item.ItemGroup;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

/**
 * Entrypoint dos acessorios (ultimo entrypoint main em fabric.mod.json, para que todos os itens ja existam):
 * capas, atributos e a aba do criativo com todos os itens do mod.
 */
public class AccessoryInit implements ModInitializer {
	@Override
	public void onInitialize() {
		ModAccessoryItems.register();
		// Forca o registro do attachment (campo estatico) antes de qualquer jogador entrar.
		Accessories.DATA.identifier();
		AccessoryEffects.register();

		RegistryKey<ItemGroup> key = RegistryKey.of(RegistryKeys.ITEM_GROUP, Identifier.of(WandryHP.MOD_ID, "main"));
		Registry.register(Registries.ITEM_GROUP, key, FabricItemGroup.builder()
				.icon(() -> new ItemStack(ModItems.get(WandType.values()[0])))
				.displayName(Text.translatable("itemGroup.wandryhp.main"))
				.entries((context, entries) -> {
					for (WandType type : WandType.values()) {
						entries.add(ModItems.get(type));
					}
					for (HouseType house : HouseType.values()) {
						entries.add(ModArmorItems.chestplate(house));
					}
					entries.add(ModArmorItems.LEGGINGS);
					entries.add(ModArmorItems.BOOTS);
					for (CapeType cape : CapeType.values()) {
						entries.add(ModAccessoryItems.cape(cape));
					}
					entries.add(BroomInit.ITEM);
					entries.add(com.wandryhp.edwiges.ModEdwigesItems.ENVELOPE);
					for (HouseType house : HouseType.values()) {
						entries.add(com.wandryhp.edwiges.ModEdwigesItems.letter(house));
					}
				})
				.build());
	}
}
