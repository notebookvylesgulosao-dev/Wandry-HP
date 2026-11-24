package com.wandryhp.client.mixin;

import com.wandryhp.accessory.AccessorySlot;
import com.wandryhp.client.accessory.AccessoryClientState;
import com.wandryhp.client.accessory.AccessoryToggleButton;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.client.gui.screen.ingame.InventoryScreen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.screen.PlayerScreenHandler;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Botao do chapeu de bruxo ao lado do inventario e painel dos 4 slots de acessorio. O botao alterna o painel
 * (abrir/fechar); os slots em si ficam no PlayerScreenHandler (ver PlayerScreenHandlerMixin).
 */
@Mixin(InventoryScreen.class)
public abstract class InventoryScreenMixin extends HandledScreen<PlayerScreenHandler> {
	// x, y, focusedSlot (HandledScreen) e textRenderer (Screen) sao campos herdados: acessados direto via heranca
	// (um @Shadow de campo herdado nao e encontrado pelo Mixin na classe alvo InventoryScreen).
	protected InventoryScreenMixin(PlayerScreenHandler handler, PlayerInventory inventory, Text title) {
		super(handler, inventory, title);
	}

	@Unique
	private static final int PANEL_LEFT = -25;
	@Unique
	private static final int PANEL_TOP = 4;
	@Unique
	private static final int PANEL_BOTTOM = 100;

	@Unique
	private ButtonWidget wandryhp$button;

	@Inject(method = "init", at = @At("TAIL"))
	private void wandryhp$addButton(CallbackInfo ci) {
		wandryhp$button = new AccessoryToggleButton(this.x - 22, this.y + 8,
				b -> AccessoryClientState.open = !AccessoryClientState.open);
		((ScreenInvoker) this).wandryhp$addDrawableChild(wandryhp$button);
	}

	/** O botao acompanha a janela (ela se desloca quando o livro de receitas abre). */
	@Inject(method = "render", at = @At("HEAD"))
	private void wandryhp$moveButton(DrawContext context, int mouseX, int mouseY, float delta, CallbackInfo ci) {
		if (wandryhp$button != null) {
			wandryhp$button.setX(this.x - 22);
			wandryhp$button.setY(this.y + 8);
		}
	}

	@Inject(method = "drawBackground", at = @At("TAIL"))
	private void wandryhp$drawPanel(DrawContext context, float delta, int mouseX, int mouseY, CallbackInfo ci) {
		if (!AccessoryClientState.open) {
			return;
		}
		int left = this.x + PANEL_LEFT, right = this.x, top = this.y + PANEL_TOP, bottom = this.y + PANEL_BOTTOM;
		context.fill(left, top, right, bottom, 0xFF000000);
		context.fill(left + 1, top + 1, right - 1, bottom - 1, 0xFFC6C6C6);
		context.fill(left + 1, top + 1, right - 1, top + 2, 0xFFFFFFFF);
		context.fill(left + 1, top + 1, left + 2, bottom - 1, 0xFFFFFFFF);
		context.fill(left + 1, bottom - 2, right - 1, bottom - 1, 0xFF555555);
		for (int i = 0; i < 4; i++) {
			int sx = this.x - 21 - 1, sy = this.y + 26 + i * 18 - 1;
			context.fill(sx, sy, sx + 18, sy + 18, 0xFF373737);
			context.fill(sx + 1, sy + 1, sx + 17, sy + 17, 0xFF8B8B8B);
		}
	}

	/** Dica com o nome do slot quando o cursor esta sobre um slot de acessorio vazio. */
	@Inject(method = "render", at = @At("TAIL"))
	private void wandryhp$slotTooltip(DrawContext context, int mouseX, int mouseY, float delta, CallbackInfo ci) {
		if (AccessoryClientState.open && this.focusedSlot instanceof AccessorySlot slot && !slot.hasStack()) {
			context.drawTooltip(this.textRenderer, Text.translatable(slot.getType().translationKey()), mouseX, mouseY);
		}
	}
}
