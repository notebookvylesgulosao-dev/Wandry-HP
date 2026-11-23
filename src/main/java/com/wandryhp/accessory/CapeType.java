package com.wandryhp.accessory;

/**
 * Os cinco chapeus Enchanter (os itens mantem o nome enchanter_*_cape por compatibilidade com mundos salvos).
 * O id e usado no registro do item, no icone, no .bbmodel e na textura.
 */
public enum CapeType {
	RED("red"),
	BLUE("blue"),
	GREEN("green"),
	YELLOW("yellow"),
	PURPLE("purple");

	public final String id;

	CapeType(String id) {
		this.id = id;
	}

	public String itemId() {
		return "enchanter_" + id + "_cape";
	}
}
