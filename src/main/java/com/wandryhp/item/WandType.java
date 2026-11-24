package com.wandryhp.item;

import com.wandryhp.WandryHP;
import net.minecraft.item.Item;
import net.minecraft.item.Items;

/**
 * Tipos de varinha. Cada um conhece seu prop, a posicao da ponta e a paleta do rastro.
 *
 * <p>tip*: posicao da ponta da varinha relativa aos olhos do jogador (direita / cima / frente, em blocos),
 * calculada a partir do modelo da varinha (vertice mais distante da haste) aplicando o display
 * "firstperson_righthand" e a transformacao padrao da mao no primeiro plano. Para canhotos o lado e espelhado.
 */
public enum WandType {
	AMETHYST("amethyst", "amethyst", 0.62f, -0.11f, 0.85f,
			new Item[] {Items.PURPLE_WOOL, Items.PURPLE_CONCRETE, Items.PURPLE_DYE},
			new int[] {0x9040E0}),

	BONE("bone", "bone", 0.62f, -0.08f, 0.86f,
			new Item[] {Items.WHITE_WOOL, Items.GRAY_WOOL, Items.WHITE_CONCRETE, Items.GRAY_CONCRETE,
					Items.WHITE_DYE, Items.GRAY_DYE},
			new int[] {0xFFFFFF, 0x9E9E9E}),

	CELESTIAL("celestial", "celestial", 0.62f, -0.08f, 0.86f,
			new Item[] {Items.YELLOW_WOOL, Items.CYAN_WOOL, Items.YELLOW_CONCRETE, Items.CYAN_CONCRETE,
					Items.YELLOW_DYE, Items.CYAN_DYE},
			new int[] {0xFFE81F, 0x2EE6D6}),

	/** Widow ainda nao tem prop proprio: usa o Prop Universal. */
	WIDOW("widow", null, 0.62f, -0.09f, 0.85f,
			new Item[] {Items.GRAY_WOOL, Items.WHITE_WOOL, Items.BLACK_WOOL, Items.RED_WOOL,
					Items.GRAY_CONCRETE, Items.WHITE_CONCRETE, Items.BLACK_CONCRETE, Items.RED_CONCRETE,
					Items.GRAY_DYE, Items.WHITE_DYE, Items.BLACK_DYE, Items.RED_DYE},
			new int[] {0x9E9E9E, 0xFFFFFF, 0x111111, 0xD21414});

	public final String id;
	private final String propId;
	public final float tipRight;
	public final float tipUp;
	public final float tipForward;
	private final Item[] trailItems;
	private final int[] trailColors;

	WandType(String id, String propId, float tipRight, float tipUp, float tipForward, Item[] trailItems, int[] trailColors) {
		this.id = id;
		this.propId = propId;
		this.tipRight = tipRight;
		this.tipUp = tipUp;
		this.tipForward = tipForward;
		this.trailItems = trailItems;
		this.trailColors = trailColors;
	}

	/** Prop correspondente; cai no Prop Universal se a varinha nao tiver um. */
	public String propId() {
		return propId != null ? propId : WandryHP.UNIVERSAL_PROP_ID;
	}

	/** Itens (la, concreto e corante) usados como particulas visuais do rastro. */
	public Item[] trailItems() {
		return trailItems;
	}

	/** Cores (RGB) das particulas de poeira do rastro. */
	public int[] trailColors() {
		return trailColors;
	}

	public static WandType byOrdinal(int ordinal) {
		WandType[] all = values();
		return all[Math.floorMod(ordinal, all.length)];
	}
}
