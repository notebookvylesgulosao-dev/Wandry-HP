package com.wandryhp.client.spell;

import com.wandryhp.spell.Spell;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.RenderTickCounter;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * HUD compacta de recarga dos feitiços, no lado direito perto do centro da tela. Cada feitiço em recarga ganha
 * uma linha com o ícone (bolinha na cor do feitiço), o nome, o tempo restante em segundos e uma barra fina que
 * esvazia. A linha some sozinha quando a recarga termina e, sem nenhuma recarga, nada é desenhado.
 *
 * <p>O servidor avisa só o início ({@code SpellCooldownPayload}); a contagem é feita aqui no relógio do mundo
 * do cliente (pausa junto com o jogo) mais a fração do tick, para a barra e os décimos andarem suaves.
 * Desenhada só com {@link DrawContext#fill}, como a roda de feitiços.
 */
public final class SpellCooldownHud {
	private static final int MARGIN_RIGHT = 6;
	private static final int PAD = 4;
	private static final int ROW_H = 14;
	private static final int ROW_GAP = 2;
	private static final int DOT = 7;
	private static final int DOT_GAP = 5;
	private static final int BAR_H = 2;

	private static final int PANEL_BG = 0xB0121218;
	private static final int PANEL_EDGE = 0x30FFFFFF;
	private static final int TRACK = 0x44FFFFFF;
	private static final int NAME_COLOR = 0xFFE6E6E6;
	private static final int TIME_COLOR = 0xFFFFFFFF;

	/** Largura em pixels de cada linha da bolinha de 7x7 (centrada). */
	private static final int[] DOT_ROWS = {3, 5, 7, 7, 7, 5, 3};

	/** Instante (tempo do mundo do cliente, em ticks) em que cada recarga termina, e sua duração total. */
	private static final Map<Spell, Cooldown> ACTIVE = new EnumMap<>(Spell.class);

	private record Cooldown(long endTick, int totalTicks) {}

	private record Entry(Spell spell, float remaining, int total) {}

	private SpellCooldownHud() {}

	public static void start(int spellOrdinal, int ticks) {
		MinecraftClient client = MinecraftClient.getInstance();
		Spell spell = Spell.byOrdinal(spellOrdinal);
		if (spell == null || client.world == null || ticks <= 0) {
			return;
		}
		ACTIVE.put(spell, new Cooldown(client.world.getTime() + ticks, ticks));
	}

	public static void clear() {
		ACTIVE.clear();
	}

	public static void render(DrawContext context, RenderTickCounter tickCounter) {
		if (ACTIVE.isEmpty()) {
			return; // vazio: não desenha nem ocupa espaço
		}
		MinecraftClient client = MinecraftClient.getInstance();
		if (client.world == null || client.player == null) {
			return;
		}
		float now = client.world.getTime() + tickCounter.getTickDelta(false);

		// Remove o que terminou e monta a lista, do maior tempo restante para o menor.
		List<Entry> entries = new ArrayList<>(ACTIVE.size());
		ACTIVE.entrySet().removeIf(e -> e.getValue().endTick() - now <= 0f);
		for (Map.Entry<Spell, Cooldown> e : ACTIVE.entrySet()) {
			entries.add(new Entry(e.getKey(), e.getValue().endTick() - now, e.getValue().totalTicks()));
		}
		if (entries.isEmpty()) {
			return;
		}
		entries.sort((a, b) -> Float.compare(b.remaining(), a.remaining()));

		TextRenderer font = client.textRenderer;
		int timeW = font.getWidth("00.0s"); // dígitos têm largura fixa: o tempo não "treme"
		int nameW = 0;
		for (Entry e : entries) {
			nameW = Math.max(nameW, font.getWidth(e.spell().displayName));
		}
		int textX0 = PAD + DOT + DOT_GAP;
		int panelW = textX0 + nameW + 8 + timeW + PAD;
		int rows = entries.size();
		int panelH = PAD * 2 - ROW_GAP + rows * (ROW_H + ROW_GAP) - 1;

		int x = context.getScaledWindowWidth() - panelW - MARGIN_RIGHT;
		int y = context.getScaledWindowHeight() / 2 - 22; // topo fixo, perto do centro vertical

		// Painel escuro translúcido de cantos cortados, com contorno sutil.
		panel(context, x, y, panelW, panelH, PANEL_EDGE);
		panel(context, x + 1, y + 1, panelW - 2, panelH - 2, PANEL_BG);

		int rowY = y + PAD;
		for (Entry e : entries) {
			Spell spell = e.spell();
			int color = 0xFF000000 | spell.color;

			dot(context, x + PAD, rowY + 1, color);

			context.drawTextWithShadow(font, spell.displayName, x + textX0, rowY, NAME_COLOR);
			String time = String.format(Locale.ROOT, "%.1fs", Math.ceil(e.remaining() / 2.0f) / 10.0);
			context.drawTextWithShadow(font, time, x + panelW - PAD - font.getWidth(time), rowY, TIME_COLOR);

			int barX = x + textX0;
			int barW = panelW - PAD - textX0;
			int barY = rowY + ROW_H - BAR_H;
			context.fill(barX, barY, barX + barW, barY + BAR_H, TRACK);
			int fill = Math.round(barW * Math.min(1f, e.remaining() / e.total()));
			if (fill > 0) {
				context.fill(barX, barY, barX + fill, barY + BAR_H, color);
			}
			rowY += ROW_H + ROW_GAP;
		}
	}

	/** Retângulo com os quatro cantos cortados em 1 pixel. */
	private static void panel(DrawContext context, int x, int y, int w, int h, int color) {
		context.fill(x + 1, y, x + w - 1, y + h, color);
		context.fill(x, y + 1, x + w, y + h - 1, color);
	}

	/** Bolinha 7x7 na cor do feitiço, com um brilho claro no canto superior esquerdo. */
	private static void dot(DrawContext context, int x, int y, int color) {
		for (int i = 0; i < DOT_ROWS.length; i++) {
			int w = DOT_ROWS[i];
			int left = x + (DOT - w) / 2;
			context.fill(left, y + i, left + w, y + i + 1, color);
		}
		context.fill(x + 2, y + 1, x + 3, y + 2, 0x80FFFFFF);
	}
}
