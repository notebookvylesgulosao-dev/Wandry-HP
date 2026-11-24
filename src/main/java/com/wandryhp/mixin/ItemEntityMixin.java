package com.wandryhp.mixin;

import com.wandryhp.house.CampfireCooking;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Selecao de casas: coleta de itens cozidos na fogueira. Antes da coleta vanilla a marca e retirada (o item entra
 * no inventario comum, empilhando normalmente); depois, o que realmente entrou pontua e o resto que ficou no chao
 * recupera a marca. Se nada foi coletado (inventario cheio, atraso de coleta), tudo volta como estava.
 */
@Mixin(ItemEntity.class)
public abstract class ItemEntityMixin {
	@Unique
	private int wandryhp$markedCount = -1;

	@Inject(method = "onPlayerCollision", at = @At("HEAD"))
	private void wandryhp$beforePickup(PlayerEntity player, CallbackInfo ci) {
		this.wandryhp$markedCount = -1;
		ItemEntity self = (ItemEntity) (Object) this;
		if (!(player instanceof ServerPlayerEntity)) {
			return;
		}
		ItemStack stack = self.getStack();
		if (stack.contains(CampfireCooking.MARK)) {
			this.wandryhp$markedCount = stack.getCount();
			stack.remove(CampfireCooking.MARK);
		}
	}

	@Inject(method = "onPlayerCollision", at = @At("RETURN"))
	private void wandryhp$afterPickup(PlayerEntity player, CallbackInfo ci) {
		int before = this.wandryhp$markedCount;
		if (before < 0) {
			return;
		}
		this.wandryhp$markedCount = -1;
		ItemEntity self = (ItemEntity) (Object) this;
		int remaining = 0;
		if (!self.isRemoved() && !self.getStack().isEmpty()) {
			remaining = self.getStack().getCount();
			self.getStack().set(CampfireCooking.MARK, true);
		}
		int picked = before - remaining;
		if (picked > 0 && player instanceof ServerPlayerEntity serverPlayer) {
			CampfireCooking.collected(serverPlayer, picked);
		}
	}
}
