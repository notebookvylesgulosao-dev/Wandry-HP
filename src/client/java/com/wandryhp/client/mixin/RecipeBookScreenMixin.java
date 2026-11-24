package com.wandryhp.client.mixin;

import com.wandryhp.client.accessory.AccessoryClientState;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.client.gui.screen.ingame.InventoryScreen;
import net.minecraft.client.gui.screen.ingame.RecipeBookScreen;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.screen.PlayerScreenHandler;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Clicar no painel de acessorios (fora dos slots) nao deve ser tratado como clique fora da janela (soltar o item). */
@Mixin(RecipeBookScreen.class)
public abstract class RecipeBookScreenMixin extends HandledScreen<PlayerScreenHandler> {
	protected RecipeBookScreenMixin(PlayerScreenHandler handler, PlayerInventory inventory, Text title) {
		super(handler, inventory, title);
	}

	@Inject(method = "isClickOutsideBounds", at = @At("HEAD"), cancellable = true, require = 0)
	private void wandryhp$panelIsInside(double mouseX, double mouseY, int left, int top, int button,
										CallbackInfoReturnable<Boolean> cir) {
		if ((Object) this instanceof InventoryScreen && AccessoryClientState.open
				&& mouseX >= this.x - 25 && mouseX < this.x
				&& mouseY >= this.y + 4 && mouseY < this.y + 100) {
			cir.setReturnValue(false);
		}
	}
}
