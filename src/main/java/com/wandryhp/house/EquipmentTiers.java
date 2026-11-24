package com.wandryhp.house;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.stat.StatHandler;
import net.minecraft.stat.Stats;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Categorias de equipamento da Sonserina (ferramentas, armas e armaduras), da mais fraca a mais forte:
 * 0 madeira, 1 pedra, 2 ferro, 3 ouro, 4 diamante, 5 netherite.
 *
 * <p>Assumido (armadura nao tem madeira nem pedra): couro conta como madeira e cota de malha como pedra.
 * Ficam de fora arco, besta, tridente, maca, escudo, elmo de tartaruga e as armaduras das casas do mod, que nao
 * pertencem a progressao pedida.
 */
public final class EquipmentTiers {
	public static final int WOOD = 0;
	public static final int NETHERITE = 5;

	private static final class Table {
		static final List<List<Item>> TIERS = List.of(
				List.of(Items.WOODEN_SWORD, Items.WOODEN_PICKAXE, Items.WOODEN_AXE, Items.WOODEN_SHOVEL, Items.WOODEN_HOE,
						Items.LEATHER_HELMET, Items.LEATHER_CHESTPLATE, Items.LEATHER_LEGGINGS, Items.LEATHER_BOOTS),
				List.of(Items.STONE_SWORD, Items.STONE_PICKAXE, Items.STONE_AXE, Items.STONE_SHOVEL, Items.STONE_HOE,
						Items.CHAINMAIL_HELMET, Items.CHAINMAIL_CHESTPLATE, Items.CHAINMAIL_LEGGINGS, Items.CHAINMAIL_BOOTS),
				List.of(Items.IRON_SWORD, Items.IRON_PICKAXE, Items.IRON_AXE, Items.IRON_SHOVEL, Items.IRON_HOE,
						Items.IRON_HELMET, Items.IRON_CHESTPLATE, Items.IRON_LEGGINGS, Items.IRON_BOOTS),
				List.of(Items.GOLDEN_SWORD, Items.GOLDEN_PICKAXE, Items.GOLDEN_AXE, Items.GOLDEN_SHOVEL, Items.GOLDEN_HOE,
						Items.GOLDEN_HELMET, Items.GOLDEN_CHESTPLATE, Items.GOLDEN_LEGGINGS, Items.GOLDEN_BOOTS),
				List.of(Items.DIAMOND_SWORD, Items.DIAMOND_PICKAXE, Items.DIAMOND_AXE, Items.DIAMOND_SHOVEL, Items.DIAMOND_HOE,
						Items.DIAMOND_HELMET, Items.DIAMOND_CHESTPLATE, Items.DIAMOND_LEGGINGS, Items.DIAMOND_BOOTS),
				List.of(Items.NETHERITE_SWORD, Items.NETHERITE_PICKAXE, Items.NETHERITE_AXE, Items.NETHERITE_SHOVEL, Items.NETHERITE_HOE,
						Items.NETHERITE_HELMET, Items.NETHERITE_CHESTPLATE, Items.NETHERITE_LEGGINGS, Items.NETHERITE_BOOTS));

		static final Map<Item, Integer> TIER_OF = new HashMap<>();

		static {
			for (int tier = 0; tier < TIERS.size(); tier++) {
				for (Item item : TIERS.get(tier)) {
					TIER_OF.put(item, tier);
				}
			}
		}
	}

	private EquipmentTiers() {}

	/** Categoria do item, ou -1 se nao for um equipamento da progressao. */
	public static int tierOf(ItemStack stack) {
		return Table.TIER_OF.getOrDefault(stack.getItem(), HouseProgress.TIER_NONE);
	}

	/**
	 * Maior categoria de equipamento que o jogador ja obteve: o que ele pegou ou fabricou (estatisticas vanilla,
	 * que continuam contando mesmo se o item for perdido) ou que esta com ele agora (cobre bau, troca, pesca e
	 * qualquer outra origem). -1 se nenhum.
	 */
	public static int highestObtained(PlayerEntity player) {
		int best = HouseProgress.TIER_NONE;
		StatHandler stats = player instanceof net.minecraft.server.network.ServerPlayerEntity sp ? sp.getStatHandler() : null;
		if (stats != null) {
			for (int tier = Table.TIERS.size() - 1; tier > best; tier--) {
				for (Item item : Table.TIERS.get(tier)) {
					if (stats.getStat(Stats.PICKED_UP, item) + stats.getStat(Stats.CRAFTED, item) > 0) {
						best = tier;
						break;
					}
				}
			}
		}
		PlayerInventory inventory = player.getInventory();
		for (int slot = 0; slot < inventory.size(); slot++) {
			best = Math.max(best, tierOf(inventory.getStack(slot)));
		}
		return best;
	}
}
