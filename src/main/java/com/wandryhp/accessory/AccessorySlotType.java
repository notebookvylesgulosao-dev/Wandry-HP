package com.wandryhp.accessory;

/** Os quatro slots de acessorio, independentes dos slots de armadura vanilla. */
public enum AccessorySlotType {
	HELMET("helmet"),
	CHEST("chest"),
	LEGS("legs"),
	BOOTS("boots");

	public final String id;

	AccessorySlotType(String id) {
		this.id = id;
	}

	public String translationKey() {
		return "wandryhp.accessory.slot." + id;
	}

	/** Regra de encaixe: os chapeus Enchanter so podem ir no slot Chest. Nenhum outro acessorio existe ainda. */
	public boolean accepts(net.minecraft.item.ItemStack stack) {
		return this == CHEST && stack.getItem() instanceof EnchanterCapeItem;
	}
}
