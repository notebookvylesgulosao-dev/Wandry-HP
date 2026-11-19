package com.wandryhp.client;

import com.wandryhp.BroomEntity;
import com.wandryhp.MagicWandItem;
import com.wandryhp.SelectSpellPayload;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.item.ItemStack;
import net.minecraft.text.Text;
import org.lwjgl.glfw.GLFW;

/** Roda redonda com os sete feitiços. Clique (ou teclas 1-7) escolhe o feitiço da varinha na mão. */
public final class SpellWheelScreen extends Screen {
    private static final MagicWandItem.Spell[] SPELLS = MagicWandItem.Spell.values();
    private static final int N = SPELLS.length;
    private static final double STEP = Math.PI * 2 / N;
    private static final int INNER = 36, OUTER = 96;

    public SpellWheelScreen() { super(Text.translatable("screen.wandry_hp.wheel")); }

    /** Setor sob o ponto (dx, dy) relativo ao centro: 0 no topo, sentido horário; -1 fora do anel. */
    private static int sectorAt(double dx, double dy) {
        double r = Math.sqrt(dx * dx + dy * dy);
        if (r < INNER || r > OUTER) return -1;
        return (int) (angle(dx, dy) / STEP) % N;
    }

    private static double angle(double dx, double dy) {
        double a = Math.atan2(dx, -dy);
        return a < 0 ? a + Math.PI * 2 : a;
    }

    private ItemStack heldWand() {
        if (client == null || client.player == null) return ItemStack.EMPTY;
        ItemStack main = client.player.getMainHandStack();
        if (main.getItem() instanceof MagicWandItem) return main;
        ItemStack off = client.player.getOffHandStack();
        return off.getItem() instanceof MagicWandItem ? off : ItemStack.EMPTY;
    }

    private static int colorAt(double dx, double dy, int hover, int selected) {
        double r = Math.sqrt(dx * dx + dy * dy);
        if (r < INNER || r > OUTER) return 0;
        double a = angle(dx, dy);
        int idx = (int) (a / STEP) % N;
        double frac = a - idx * STEP;
        if (Math.min(frac, STEP - frac) * r < 1.5) return 0; // vão entre setores
        if (idx == selected && r > OUTER - 3) return 0xFFFFFFFF;
        int alpha = idx == selected ? 0xE6 : idx == hover ? 0xCC : 0x80;
        return (alpha << 24) | SPELLS[idx].color;
    }

    @Override public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        super.render(context, mouseX, mouseY, delta);
        int cx = width / 2, cy = height / 2;
        ItemStack wand = heldWand();
        int selected = wand.isEmpty() ? -1 : MagicWandItem.selectedSpell(wand).ordinal();
        int hover = sectorAt(mouseX - cx, mouseY - cy);

        for (int y = -OUTER; y <= OUTER; y += 2) {
            int runStart = 0, runColor = 0;
            for (int x = -OUTER; x <= OUTER + 2; x += 2) {
                int color = x <= OUTER ? colorAt(x + 1, y + 1, hover, selected) : 0;
                if (color != runColor) {
                    if (runColor != 0) context.fill(cx + runStart, cy + y, cx + x, cy + y + 2, runColor);
                    runStart = x;
                    runColor = color;
                }
            }
        }

        double ringMid = (INNER + OUTER) / 2.0;
        for (int i = 0; i < N; i++) {
            double mid = (i + 0.5) * STEP;
            double sx = Math.sin(mid), sy = -Math.cos(mid);
            context.drawCenteredTextWithShadow(textRenderer, String.valueOf(i + 1),
                cx + (int) (sx * ringMid), cy + (int) (sy * ringMid) - 4, 0xFFFFFF);
            String name = SPELLS[i].displayName;
            int lx = cx + (int) (sx * (OUTER + 12)), ly = cy + (int) (sy * (OUTER + 12)) - 4;
            int color = i == selected ? 0xFFFFFF : 0xC8C8C8;
            if (sx > 0.35) context.drawTextWithShadow(textRenderer, name, lx, ly, color);
            else if (sx < -0.35) context.drawTextWithShadow(textRenderer, name, lx - textRenderer.getWidth(name), ly, color);
            else context.drawCenteredTextWithShadow(textRenderer, name, lx, ly, color);
        }

        context.drawCenteredTextWithShadow(textRenderer, "Roda de Feitiços", cx, cy - OUTER - 30, 0xD9B8FF);
        String bottom = wand.isEmpty() ? "Segure uma varinha para escolher o feitiço"
            : hover >= 0 ? SPELLS[hover].displayName : "Feitiço atual: " + SPELLS[selected].displayName;
        context.drawCenteredTextWithShadow(textRenderer, bottom, cx, cy + OUTER + 30, 0xFFFFFF);
    }

    @Override public boolean mouseClicked(double mouseX, double mouseY, int button) {
        int sector = sectorAt(mouseX - width / 2.0, mouseY - height / 2.0);
        if (button == 0 && sector >= 0) { select(sector); return true; }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode >= GLFW.GLFW_KEY_1 && keyCode < GLFW.GLFW_KEY_1 + N) { select(keyCode - GLFW.GLFW_KEY_1); return true; }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    private void select(int index) {
        if (!heldWand().isEmpty()) ClientPlayNetworking.send(new SelectSpellPayload(index));
        close();
    }

    @Override public boolean shouldPause() { return false; }
}
