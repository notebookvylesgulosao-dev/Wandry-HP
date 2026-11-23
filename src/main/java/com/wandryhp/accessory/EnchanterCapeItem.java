package com.wandryhp.accessory;

import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.tooltip.TooltipType;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import java.util.List;

/** Chapeu Enchanter (acessorio). Nao e armadura vanilla (sem componente equippable): so entra no slot Chest dos acessorios. */
public class EnchanterCapeItem extends Item {
	private final CapeType type;

	public EnchanterCapeItem(CapeType type, Settings settings) {
		super(settings);
		this.type = type;
	}

	public CapeType getCapeType() {
		return type;
	}

	@Override
	public void appendTooltip(ItemStack stack, TooltipContext context, List<Text> tooltip, TooltipType tooltipType) {
		tooltip.add(Text.translatable("wandryhp.accessory.tooltip." + type.id).formatted(Formatting.BLUE));
	}
}
