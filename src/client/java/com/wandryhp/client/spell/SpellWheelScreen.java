package com.wandryhp.client.spell;

import com.wandryhp.spell.Spell;
import com.wandryhp.spell.SpellUnlocks;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;

import java.util.List;

/**
 * Roda circular de feitiços. Mostra todos os feitiços desbloqueados em fatias; a fatia sob o ponteiro
 * do mouse fica destacada, e o clique esquerdo nela fecha a roda e ativa o feitiço.
 * G (ou Esc) fecha a roda sem trocar o feitiço.
 *
 * <p>A roda é desenhada só com {@link DrawContext#fill}: a máscara de fatias é calculada uma vez em
 * {@link #init()} e a cada quadro é percorrida por linhas, preenchendo trechos de mesma cor.
 */
public class SpellWheelScreen extends Screen {
	private static final double TWO_PI = Math.PI * 2.0;
	/** Folga angular (rad) entre fatias. */
	private static final double GAP = 0.03;
	/** Distância extra além do anel em que o ponteiro ainda conta como apontando para a fatia. */
	private static final int REACH = 40;

	private final List<Spell> spells;
	private int outer;
	private int inner;
	/** Índice da fatia de cada pixel do anel (ou -1), com origem no canto superior esquerdo do quadrado do anel. */
	private int[][] cells = new int[0][0];

	public SpellWheelScreen() {
		super(Text.translatable("key.wandryhp.spell_wheel"));
		this.spells = SpellUnlocks.unlocked(MinecraftClient.getInstance().player);
	}

	@Override
	protected void init() {
		outer = Math.max(50, Math.min(100, height / 2 - 30));
		inner = (int) (outer * 0.36);
		buildCells();
	}

	private void buildCells() {
		int size = outer * 2 + 1;
		cells = new int[size][size];
		int n = spells.size();
		double w = n > 0 ? TWO_PI / n : TWO_PI;
		for (int y = 0; y < size; y++) {
			for (int x = 0; x < size; x++) {
				int dx = x - outer;
				int dy = y - outer;
				int d2 = dx * dx + dy * dy;
				if (n == 0 || d2 < inner * inner || d2 > outer * outer) {
					cells[y][x] = -1;
					continue;
				}
				double shifted = angleOf(dx, dy) + w / 2.0;
				int index = ((int) (shifted / w)) % n;
				double local = shifted - index * w;
				boolean inGap = n > 1 && (local < GAP / 2.0 || local > w - GAP / 2.0);
				cells[y][x] = inGap ? -1 : index;
			}
		}
	}

	/** Ângulo em radianos, no sentido horário a partir do topo (0 = cima), em [0, 2π). */
	private static double angleOf(double dx, double dy) {
		double angle = Math.atan2(dx, -dy);
		return angle < 0 ? angle + TWO_PI : angle;
	}

	/** Índice do feitiço apontado pelo mouse, ou -1 (centro morto, ou longe demais). */
	private int hoveredIndex(double mouseX, double mouseY) {
		int n = spells.size();
		if (n == 0) {
			return -1;
		}
		double dx = mouseX - width / 2.0;
		double dy = mouseY - height / 2.0;
		double d2 = dx * dx + dy * dy;
		double reach = outer + REACH;
		if (d2 < (double) inner * inner || d2 > reach * reach) {
			return -1;
		}
		double w = TWO_PI / n;
		return ((int) ((angleOf(dx, dy) + w / 2.0) / w)) % n;
	}

	@Override
	public void renderBackground(DrawContext context, int mouseX, int mouseY, float delta) {
		// Sem o desfoque padrão: só escurece levemente para destacar a roda.
		context.fill(0, 0, width, height, 0x66000000);
	}

	@Override
	public void render(DrawContext context, int mouseX, int mouseY, float delta) {
		super.render(context, mouseX, mouseY, delta);
		int n = spells.size();
		if (n == 0) {
			return;
		}
		int hovered = hoveredIndex(mouseX, mouseY);
		Spell active = SpellClientState.selected();
		int left = width / 2 - outer;
		int top = height / 2 - outer;
		int size = outer * 2 + 1;

		// Anel: linha a linha, preenchendo trechos contíguos de mesma cor.
		for (int y = 0; y < size; y++) {
			int runStart = 0;
			int runColor = 0;
			for (int x = 0; x <= size; x++) {
				int color = x < size ? colorAt(cells[y][x], x - outer, y - outer, hovered, active) : 0;
				if (color != runColor) {
					if (runColor != 0) {
						context.fill(left + runStart, top + y, left + x, top + y + 1, runColor);
					}
					runStart = x;
					runColor = color;
				}
			}
		}

		// Nomes, fora do anel, alinhados conforme o lado da roda.
		double w = TWO_PI / n;
		for (int i = 0; i < n; i++) {
			Spell spell = spells.get(i);
			double a = i * w;
			double sx = Math.sin(a);
			double sy = -Math.cos(a);
			int lx = width / 2 + (int) (sx * (outer + 8));
			int ly = height / 2 + (int) (sy * (outer + 8));
			Text name = Text.literal(spell.displayName);
			int tw = textRenderer.getWidth(name);
			int tx = sx > 0.25 ? lx : sx < -0.25 ? lx - tw : lx - tw / 2;
			int ty = ly - 4 + (Math.abs(sx) <= 0.25 ? (int) (sy * 6) : 0);
			int textColor = i == hovered ? (0xFF000000 | spell.color) : 0xFFDDDDDD;
			context.drawTextWithShadow(textRenderer, name, tx, ty, textColor);
		}
	}

	/** Cor ARGB do pixel (0 = transparente). Fatia sob o mouse fica opaca; o feitiço ativo ganha borda branca. */
	private int colorAt(int index, int dx, int dy, int hovered, Spell active) {
		if (index < 0) {
			return 0;
		}
		Spell spell = spells.get(index);
		if (spell == active) {
			int d2 = dx * dx + dy * dy;
			int edge = outer - 3;
			if (d2 > edge * edge) {
				return 0xFFFFFFFF;
			}
		}
		int alpha = index == hovered ? 0xFF : 0x99;
		return (alpha << 24) | spell.color;
	}

	@Override
	public boolean mouseClicked(double mouseX, double mouseY, int button) {
		if (button == 0) {
			int index = hoveredIndex(mouseX, mouseY);
			if (index >= 0) {
				SpellClientState.select(spells.get(index));
				close();
				return true;
			}
		}
		return super.mouseClicked(mouseX, mouseY, button);
	}

	@Override
	public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
		if (SpellsClientInit.WHEEL_KEY != null && SpellsClientInit.WHEEL_KEY.matchesKey(keyCode, scanCode)) {
			close();
			return true;
		}
		return super.keyPressed(keyCode, scanCode, modifiers);
	}

	@Override
	public boolean shouldPause() {
		return false;
	}
}
