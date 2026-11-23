package com.wandryhp.client.edwiges;

import com.wandryhp.WandryHP;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.util.Identifier;

import java.util.Map;

/**
 * HUD de chegada da Edwiges: faixa de pergaminho no topo central, com a coruja a esquerda, o titulo e a barra de
 * progresso com a distancia em blocos. Reproduz a arte enviada em escala 1:1 (148x26 pixels de GUI): o fundo
 * (textures/gui/edwiges_hud.png) traz pergaminho, coruja, titulo e o trilho vazio; o preenchimento
 * (edwiges_hud_fill.png) e recortado conforme a distancia e o numero e desenhado pixel a pixel na mesma grade.
 * As coordenadas abaixo foram medidas na arte (ver tools/prepare_edwiges_hud.py).
 */
public final class EdwigesHud {
	private static final Identifier BACKGROUND = Identifier.of(WandryHP.MOD_ID, "textures/gui/edwiges_hud.png");
	private static final Identifier FILL = Identifier.of(WandryHP.MOD_ID, "textures/gui/edwiges_hud_fill.png");

	private static final int W = 148;
	private static final int H = 26;
	/** Distancia da faixa ao topo da tela quando ja esta no lugar. */
	private static final int MARGIN_TOP = 4;

	/** Preenchimento: posicao dentro da faixa e tamanho da textura. */
	private static final int FILL_X = 32;
	private static final int FILL_Y = 16;
	private static final int FILL_W = 79;
	private static final int FILL_H = 5;

	/** Texto da distancia: cor da arte, linha de base (5 linhas de altura) e limites horizontais. */
	private static final int TEXT_COLOR = 0xFF501216;
	private static final int TEXT_Y = 16;
	private static final int TEXT_X = 114;
	private static final int TEXT_RIGHT = 147;

	/** Fonte de pixels 3x5 (b e l sobem; o c s ocupam as 3 linhas de baixo), como o texto pequeno da arte. */
	private static final Map<Character, String[]> GLYPHS = Map.ofEntries(
			Map.entry('0', new String[]{"###", "#.#", "#.#", "#.#", "###"}),
			Map.entry('1', new String[]{".#.", "##.", ".#.", ".#.", "###"}),
			Map.entry('2', new String[]{"###", "..#", "###", "#..", "###"}),
			Map.entry('3', new String[]{"###", "..#", "###", "..#", "###"}),
			Map.entry('4', new String[]{"#.#", "#.#", "###", "..#", "..#"}),
			Map.entry('5', new String[]{"###", "#..", "###", "..#", "###"}),
			Map.entry('6', new String[]{"###", "#..", "###", "#.#", "###"}),
			Map.entry('7', new String[]{"###", "..#", "..#", ".#.", ".#."}),
			Map.entry('8', new String[]{"###", "#.#", "###", "#.#", "###"}),
			Map.entry('9', new String[]{"###", "#.#", "###", "..#", "###"}),
			Map.entry('b', new String[]{"#..", "#..", "###", "#.#", "###"}),
			Map.entry('l', new String[]{"#", "#", "#", "#", "#"}),
			Map.entry('o', new String[]{"...", "...", "###", "#.#", "###"}),
			Map.entry('c', new String[]{"...", "...", "###", "#..", "###"}),
			Map.entry('s', new String[]{"...", "...", "###", "##.", ".##"}),
			Map.entry(' ', new String[]{".", ".", ".", ".", "."}));

	private EdwigesHud() {}

	public static void render(DrawContext context, RenderTickCounter tickCounter) {
		EdwigesHudState.update();
		if (!EdwigesHudState.isVisible()) return;
		MinecraftClient client = MinecraftClient.getInstance();
		if (client.player == null) return;

		int x = (context.getScaledWindowWidth() - W) / 2;
		// Desliza de cima (totalmente acima da tela) ate a margem do topo.
		int y = Math.round(-H + (H + MARGIN_TOP) * EdwigesHudState.eased());

		context.drawTexture(RenderLayer::getGuiTextured, BACKGROUND, x, y, 0f, 0f, W, H, W, H, W, H);

		int fill = Math.round(FILL_W * EdwigesHudState.progress());
		if (fill > 0) {
			context.drawTexture(RenderLayer::getGuiTextured, FILL, x + FILL_X, y + FILL_Y, 0f, 0f, fill, FILL_H, fill, FILL_H, FILL_W, FILL_H);
		}

		int blocks = EdwigesHudState.distanceBlocks();
		drawText(context, blocks + (blocks == 1 ? " bloco" : " blocos"), x, y);
	}

	/** Desenha o texto pixel a pixel; alinhado a esquerda como na arte, recuando so se nao couber (3 digitos, ate colar na ponta da barra). */
	private static void drawText(DrawContext context, String text, int panelX, int panelY) {
		int width = 0;
		for (char c : text.toCharArray()) width += glyph(c)[0].length() + 1;
		width -= 1;
		int cx = panelX + Math.min(TEXT_X, TEXT_RIGHT - width + 1);
		for (char c : text.toCharArray()) {
			String[] g = glyph(c);
			for (int row = 0; row < g.length; row++) {
				for (int col = 0; col < g[row].length(); col++) {
					if (g[row].charAt(col) == '#') {
						int px = cx + col, py = panelY + TEXT_Y + row;
						context.fill(px, py, px + 1, py + 1, TEXT_COLOR);
					}
				}
			}
			cx += g[0].length() + 1;
		}
	}

	private static String[] glyph(char c) {
		String[] g = GLYPHS.get(c);
		return g != null ? g : GLYPHS.get(' ');
	}
}
