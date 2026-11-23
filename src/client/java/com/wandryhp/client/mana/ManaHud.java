package com.wandryhp.client.mana;

import com.wandryhp.WandryHP;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.util.Identifier;

/**
 * Barra de mana compacta no canto inferior direito. Usa a textura enviada (textures/gui/mana_bar.png, 108x12,
 * arte original sem alteracao de visual): o preenchimento e a propria textura recortada na proporcao da mana,
 * sobre a mesma textura escurecida como trilho vazio.
 */
public final class ManaHud {
	public static final Identifier TEXTURE = Identifier.of(WandryHP.MOD_ID, "textures/gui/mana_bar.png");
	/** Tamanho da textura (e da barra na tela, em pixels de GUI). */
	private static final int W = 108;
	private static final int H = 12;
	/** Distancia da barra ate as bordas direita e inferior da tela. */
	private static final int MARGIN = 6;
	/** Tinta multiplicada na textura para formar o trilho vazio. */
	private static final int EMPTY_TINT = 0xFF3A3F55;

	private ManaHud() {}

	public static void render(DrawContext context, RenderTickCounter tickCounter) {
		MinecraftClient client = MinecraftClient.getInstance();
		if (client.player == null || client.player.isSpectator() || !ManaClientState.isKnown()) {
			return;
		}
		int current = ManaClientState.current();
		int max = ManaClientState.max();

		int x = context.getScaledWindowWidth() - W - MARGIN;
		int y = context.getScaledWindowHeight() - H - MARGIN;

		// Trilho vazio: a textura inteira, escurecida.
		context.drawTexture(RenderLayer::getGuiTextured, TEXTURE, x, y, 0f, 0f, W, H, W, H, W, H, EMPTY_TINT);

		// Preenchimento: a textura original recortada na proporcao da mana atual.
		int fill = Math.round(W * (current / (float) max));
		if (fill > 0) {
			context.drawTexture(RenderLayer::getGuiTextured, TEXTURE, x, y, 0f, 0f, fill, H, fill, H, W, H);
		}

		// Texto branco centralizado: "100 / 100 MP".
		TextRenderer font = client.textRenderer;
		String text = current + " / " + max + " MP";
		int textX = x + (W - font.getWidth(text)) / 2;
		int textY = y + (H - font.fontHeight) / 2 + 1;
		context.drawTextWithShadow(font, text, textX, textY, 0xFFFFFFFF);
	}
}
