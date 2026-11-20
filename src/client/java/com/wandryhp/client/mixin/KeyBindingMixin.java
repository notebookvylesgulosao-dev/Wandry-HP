package com.wandryhp.client.mixin;

import com.wandryhp.client.ConfusionControls;
import net.minecraft.client.option.KeyBinding;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Confusao: enquanto o jogador local tem o efeito, cada tecla responde com o estado da tecla oposta.
 * Como tudo (movimento, ataque/uso, lancamento da varinha) le o estado por KeyBinding, a inversao
 * fica consistente em todos os controles.
 */
@Mixin(KeyBinding.class)
public abstract class KeyBindingMixin {
	@Unique
	private static boolean wandryhp$resolving = false;

	@Inject(method = "wasPressed", at = @At("HEAD"), cancellable = true)
	private void wandryhp$swapWasPressed(CallbackInfoReturnable<Boolean> cir) {
		if (wandryhp$resolving) return;
		KeyBinding partner = ConfusionControls.partnerOf((KeyBinding) (Object) this);
		if (partner == null) return;
		wandryhp$resolving = true;
		try {
			cir.setReturnValue(partner.wasPressed());
		} finally {
			wandryhp$resolving = false;
		}
	}

	@Inject(method = "isPressed", at = @At("HEAD"), cancellable = true)
	private void wandryhp$swapIsPressed(CallbackInfoReturnable<Boolean> cir) {
		if (wandryhp$resolving) return;
		KeyBinding partner = ConfusionControls.partnerOf((KeyBinding) (Object) this);
		if (partner == null) return;
		wandryhp$resolving = true;
		try {
			cir.setReturnValue(partner.isPressed());
		} finally {
			wandryhp$resolving = false;
		}
	}
}
