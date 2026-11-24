package com.wandryhp.armor;

/**
 * Casas de Hogwarts. Cada casa tem um peitoral proprio (modelo 3D e textura originais do Blockbench);
 * a calca e as botas sao unicas e compartilhadas por todas as casas.
 *
 * <p>O id e usado no id do item ({id}_chestplate), no .bbmodel (bbmodels/armor/{id}.bbmodel)
 * e na textura (textures/entity/armor/{id}.png).
 */
public enum HouseType {
	GRYFFINDOR("gryffindor"),
	HUFFLEPUFF("hufflepuff"),
	RAVENCLAW("ravenclaw"),
	SLYTHERIN("slytherin");

	public final String id;

	HouseType(String id) {
		this.id = id;
	}
}
