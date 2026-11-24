package com.wandryhp.accessory;

import net.minecraft.inventory.Inventory;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.slot.Slot;

import java.util.function.BooleanSupplier;

/** Slot de acessorio dentro do PlayerScreenHandler. So aceita o que {@link AccessorySlotType#accepts} permitir. */
public class AccessorySlot extends Slot {
	/**
	 * Define se os slots estao visiveis/clicaveis. No servidor e sempre verdadeiro; o cliente troca este
	 * fornecedor para refletir o botao de abrir/fechar (estado so de interface, sem efeito no servidor).
	 */
	public static BooleanSupplier PANEL_OPEN = () -> true;

	private final AccessorySlotType type;

	public AccessorySlot(Inventory inventory, AccessorySlotType type, int x, int y) {
		super(inventory, type.ordinal(), x, y);
		this.type = type;
	}

	public AccessorySlotType getType() {
		return type;
	}

	@Override
	public boolean canInsert(ItemStack stack) {
		return type.accepts(stack);
	}

	@Override
	public int getMaxItemCount() {
		return 1;
	}

	@Override
	public boolean isEnabled() {
		return PANEL_OPEN.getAsBoolean();
	}
}
