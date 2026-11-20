package com.wandryhp.armor;

import net.minecraft.item.Item;

/** Peitoral de uma casa. Qualquer um dos quatro conta como a peca de peito do conjunto. */
public class HouseChestplateItem extends Item {
	private final HouseType house;

	public HouseChestplateItem(HouseType house, Settings settings) {
		super(settings);
		this.house = house;
	}

	public HouseType getHouse() {
		return house;
	}
}
