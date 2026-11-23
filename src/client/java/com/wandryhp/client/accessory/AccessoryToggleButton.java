package com.wandryhp.client.accessory;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;

/** Botao pequeno 2D com um chapeu de bruxo branco, desenhado em pixels (sem textura e sem aparencia 3D). */
public class AccessoryToggleButton extends ButtonWidget {
	private static final String[] HAT = {
			"....##....",
			"...###....",
			"...###....",
			"..####....",
			"..####....",
			".######...",
			".######...",
			"##########",
			"##########",
	};

	public AccessoryToggleButton(int x, int y, PressAction onPress) {
		super(x, y, 14, 14, Text.translatable("wandryhp.accessory.button"), onPress, DEFAULT_NARRATION_SUPPLIER);
	}

	@Override
	protected void renderWidget(DrawContext context, int mouseX, int mouseY, float delta) {
		int x = getX(), y = getY();
		int border = isHovered() ? 0xFFFFFFFF : 0xFF000000;
		int fill = AccessoryClientState.open ? 0xFF6B6B6B : (isHovered() ? 0xFF5A5A5A : 0xFF373737);
		context.fill(x, y, x + 14, y + 14, border);
		context.fill(x + 1, y + 1, x + 13, y + 13, fill);
		for (int row = 0; row < HAT.length; row++) {
			String line = HAT[row];
			for (int col = 0; col < line.length(); col++) {
				if (line.charAt(col) == '#') {
					context.fill(x + 2 + col - 0, y + 3 + row - 1, x + 3 + col, y + 4 + row - 1, 0xFFFFFFFF);
				}
			}
		}
	}
}
