package com.wandryhp.armor;

import com.wandryhp.WandryHP;
import net.minecraft.item.Item;
import net.minecraft.item.equipment.ArmorMaterial;
import net.minecraft.item.equipment.EquipmentAsset;
import net.minecraft.item.equipment.EquipmentAssetKeys;
import net.minecraft.item.equipment.EquipmentType;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.tag.TagKey;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.Identifier;

import java.util.Map;

/**
 * Material das armaduras das casas.
 *
 * <p>ATENCAO: os valores de defesa e durabilidade NAO foram especificados no pedido; estao equivalentes aos do
 * ferro e concentrados aqui para ajuste facil. Toughness e resistencia a repulsao do material ficam em 0 de
 * proposito: a unica reducao de knockback vem do bonus do conjunto (ver {@link HouseArmorSet}).
 */
public final class HouseArmorMaterial {
	/** Multiplicador de durabilidade (ferro = 15). */
	public static final int DURABILITY = 15;
	/** Defesa por peca, em pontos de armadura (ferro: peito 6, calca 5, botas 2). */
	public static final int DEFENSE_CHESTPLATE = 6;
	public static final int DEFENSE_LEGGINGS = 5;
	public static final int DEFENSE_BOOTS = 2;
	/** Encantabilidade (ferro = 9). */
	public static final int ENCHANTABILITY = 9;

	/** Id do asset de equipamento (assets/wandryhp/equipment/house.json); texturas transparentes, calca e botas sao desenhadas dos .bbmodel. */
	public static final RegistryKey<EquipmentAsset> ASSET_ID =
			RegistryKey.of(EquipmentAssetKeys.REGISTRY_KEY, Identifier.of(WandryHP.MOD_ID, "house"));

	/** Tag de reparo propria e vazia: nenhum item repara a armadura na bigorna ate que uma tag seja criada. */
	public static final TagKey<Item> REPAIR_TAG =
			TagKey.of(RegistryKeys.ITEM, Identifier.of(WandryHP.MOD_ID, "repairs_house_armor"));

	public static final ArmorMaterial MATERIAL = new ArmorMaterial(
			DURABILITY,
			Map.of(
					EquipmentType.CHESTPLATE, DEFENSE_CHESTPLATE,
					EquipmentType.LEGGINGS, DEFENSE_LEGGINGS,
					EquipmentType.BOOTS, DEFENSE_BOOTS),
			ENCHANTABILITY,
			SoundEvents.ITEM_ARMOR_EQUIP_GENERIC,
			0.0f,
			0.0f,
			REPAIR_TAG,
			ASSET_ID);

	private HouseArmorMaterial() {}
}
