package com.wandryhp.mixin;

import com.wandryhp.house.HouseSelection;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.slot.FurnaceOutputSlot;
import net.minecraft.server.network.ServerPlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Selecao de casas: avisa os itens retirados prontos da fornalha/defumador (por quantidade retirada). */
@Mixin(FurnaceOutputSlot.class)
public abstract class FurnaceOutputSlotMixin {
	@Inject(method = "onTakeItem", at = @At("HEAD"))
	private void wandryhp$onTakeItem(PlayerEntity player, ItemStack stack, CallbackInfo ci) {
		if (player instanceof ServerPlayerEntity serverPlayer) {
			HouseSelection.onFurnaceOutput(serverPlayer, stack.getCount());
		}
	}
}
