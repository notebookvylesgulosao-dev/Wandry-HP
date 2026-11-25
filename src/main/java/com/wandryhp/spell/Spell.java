package com.wandryhp.spell;

import net.minecraft.item.Item;
import net.minecraft.item.Items;

/**
 * Os feitiços (os sete originais + Accio, Wingardium Leviosa, Lumos, Expecto Patronum e Incendio). Cada um conhece o modelo 3D do projétil, a cor, a recarga e a paleta do rastro.
 *
 * <p>Corinficus e Crucio usam o mesmo modelo vermelho (só existe um .bbmodel vermelho).
 * Os itens do rastro seguem a ordem lã, concreto, corante (alternados), nas cores do feitiço.
 */
public enum Spell {
	AVADA_KEDAVRA("avada_kedavra", "Avada Kedavra", "green", 0x2ECC40, 60, 80,
			new Item[] {Items.LIME_WOOL, Items.GREEN_CONCRETE, Items.LIME_DYE,
					Items.GREEN_WOOL, Items.LIME_CONCRETE, Items.GREEN_DYE},
			new int[] {0x32E632, 0x1E9E1E}),

	CORINFICUS("corinficus", "Corinficus", "red", 0xE02020, 45, 55,
			new Item[] {Items.RED_WOOL, Items.RED_CONCRETE, Items.RED_DYE},
			new int[] {0xE02020, 0xB01010}),

	CRUCIO("crucio", "Crucio", "red", 0xE02020, 35, 45,
			new Item[] {Items.RED_WOOL, Items.RED_CONCRETE, Items.RED_DYE},
			new int[] {0xE02020, 0xB01010}),

	EXPELLIARMUS("expelliarmus", "Expelliarmus", "turquoise", 0x2EE6D6, 25, 35,
			new Item[] {Items.CYAN_WOOL, Items.CYAN_CONCRETE, Items.CYAN_DYE},
			new int[] {0x2EE6D6, 0x1FB5A8}),

	IMPERIUS("imperius", "Imperius", "yellow", 0xFFE81F, 50, 40,
			new Item[] {Items.YELLOW_WOOL, Items.YELLOW_CONCRETE, Items.YELLOW_DYE},
			new int[] {0xFFE81F, 0xE0B800}),

	PETRIFICUS_TOTALIS("petrificus_totalis", "Petrificus Totalis", "gray", 0x9E9E9E, 40, 35,
			new Item[] {Items.GRAY_WOOL, Items.LIGHT_GRAY_CONCRETE, Items.GRAY_DYE,
					Items.LIGHT_GRAY_WOOL, Items.GRAY_CONCRETE, Items.LIGHT_GRAY_DYE},
			new int[] {0x808080, 0xB8B8B8}),

	SECTUMSEMPRA("sectumsempra", "Sectumsempra", "purple", 0x9040E0, 30, 30,
			new Item[] {Items.PURPLE_WOOL, Items.PURPLE_CONCRETE, Items.PURPLE_DYE},
			new int[] {0x9040E0, 0x7020B0}),

	// ---- Feitiços adicionados depois (no fim da lista: a ordem dos antigos, enviada pela rede, não muda).
	// Reaproveitam os modelos de projétil já existentes: roxo, turquesa, amarelo e vermelho.
	// Recarga e mana: assumidos (não definidos no pedido), ajuste aqui.

	ACCIO("accio", "Accio", "purple", 0x9040E0, 20, 30,
			new Item[] {Items.PURPLE_WOOL, Items.PURPLE_CONCRETE, Items.PURPLE_DYE},
			new int[] {0x9040E0, 0x7020B0}),

	WINGARDIUM_LEVIOSA("wingardium_leviosa", "Wingardium Leviosa", "turquoise", 0x2EE6D6, 30, 40,
			new Item[] {Items.CYAN_WOOL, Items.CYAN_CONCRETE, Items.CYAN_DYE},
			new int[] {0x2EE6D6, 0x1FB5A8}),

	/** Sem projétil: acende a ponta da varinha (liga/desliga). */
	LUMOS("lumos", "Lumos", "yellow", 0xFFE81F, 3, 10,
			new Item[] {Items.YELLOW_WOOL, Items.YELLOW_CONCRETE, Items.YELLOW_DYE},
			new int[] {0xFFE81F, 0xE0B800}),

	/** Sem projétil: invoca o Patrono ({@link PatronusEntity}). */
	EXPECTO_PATRONUM("expecto_patronum", "Expecto Patronum", "turquoise", 0x2EE6D6, 60, 80,
			new Item[] {Items.CYAN_WOOL, Items.CYAN_CONCRETE, Items.CYAN_DYE},
			new int[] {0x2EE6D6, 0x1FB5A8}),

	INCENDIO("incendio", "Incendio", "red", 0xE02020, 20, 35,
			new Item[] {Items.RED_WOOL, Items.RED_CONCRETE, Items.RED_DYE},
			new int[] {0xE02020, 0xB01010});

	public final String id;
	public final String displayName;
	/** Id do modelo 3D em bbmodels/spells/ e da textura em textures/entity/spell/. */
	public final String modelId;
	/** Cor do feitiço (RGB), usada na roda de seleção. */
	public final int color;
	public final int cooldownTicks;
	/** Custo de mana (MP) de cada lançamento. */
	public final int manaCost;
	private final Item[] trailItems;
	private final int[] trailColors;

	Spell(String id, String displayName, String modelId, int color, int cooldownSeconds, int manaCost, Item[] trailItems, int[] trailColors) {
		this.id = id;
		this.displayName = displayName;
		this.modelId = modelId;
		this.color = color;
		this.cooldownTicks = cooldownSeconds * 20;
		this.manaCost = manaCost;
		this.trailItems = trailItems;
		this.trailColors = trailColors;
	}

	/** Itens (lã, concreto e corante) usados como partículas visuais do rastro, em ordem de alternância. */
	public Item[] trailItems() {
		return trailItems;
	}

	/** Cores (RGB) das partículas de poeira do rastro. */
	public int[] trailColors() {
		return trailColors;
	}

	/** Feitiço pelo índice, ou null se o índice for inválido (o valor vem do cliente). */
	public static Spell byOrdinal(int ordinal) {
		Spell[] all = values();
		return ordinal >= 0 && ordinal < all.length ? all[ordinal] : null;
	}
}
