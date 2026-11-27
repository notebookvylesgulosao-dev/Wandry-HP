package com.wandryhp.mixin;

import com.wandryhp.alignment.PlayerBadge;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Coloca os ícones da casa e do alinhamento na frente do nome de exibição do jogador. É esse nome que o servidor usa
 * como remetente das mensagens do chat e que o cliente desenha acima da cabeça. (A lista TAB usa outro caminho: ver
 * PlayerListHudMixin.) Roda nos dois lados: o servidor lê o attachment, o cliente lê o que o servidor informou.
 */
@Mixin(PlayerEntity.class)
public abstract class PlayerEntityDisplayNameMixin {
	@Inject(method = "getDisplayName()Lnet/minecraft/text/Text;", at = @At("RETURN"), cancellable = true)
	private void wandryhp$addBadge(CallbackInfoReturnable<Text> cir) {
		cir.setReturnValue(PlayerBadge.decorate((PlayerEntity) (Object) this, cir.getReturnValue()));
	}
}
