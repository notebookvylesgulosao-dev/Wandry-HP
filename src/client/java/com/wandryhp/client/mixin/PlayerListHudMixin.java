package com.wandryhp.client.mixin;

import com.wandryhp.alignment.PlayerBadge;
import net.minecraft.client.gui.hud.PlayerListHud;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Lista TAB: coloca os ícones da casa e do alinhamento na frente de cada nick. Lê o que o servidor informou (vale
 * também para jogadores longe, que nem existem como entidade no cliente).
 */
@Mixin(PlayerListHud.class)
public abstract class PlayerListHudMixin {
	@Inject(method = "getPlayerName(Lnet/minecraft/client/network/PlayerListEntry;)Lnet/minecraft/text/Text;",
			at = @At("RETURN"), cancellable = true)
	private void wandryhp$addBadge(PlayerListEntry entry, CallbackInfoReturnable<Text> cir) {
		PlayerBadge.Badge badge = PlayerBadge.cached(entry.getProfile().getId());
		if (badge != null) {
			cir.setReturnValue(PlayerBadge.decorate(badge, cir.getReturnValue()));
		}
	}
}
