package com.wandryhp.item;

import net.minecraft.item.Item;

public class WandItem extends Item {
	private final WandType type;

	public WandItem(WandType type, Settings settings) {
		super(settings);
		this.type = type;
	}

	public WandType getWandType() {
		return type;
	}
}
