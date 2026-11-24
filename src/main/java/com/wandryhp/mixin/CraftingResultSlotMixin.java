package com.wandryhp.mixin;

import com.wandryhp.house.HouseSelection;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.slot.CraftingResultSlot;
import net.minecraft.server.network.ServerPlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Selecao de casas: avisa cada operacao de fabricacao (resultado retirado da bancada ou da grade 2x2). */
@Mixin(CraftingResultSlot.class)
public abstract class CraftingResultSlotMixin {
	@Inject(method = "onTakeItem", at = @At("HEAD"))
	private void wandryhp$onTakeItem(PlayerEntity player, ItemStack stack, CallbackInfo ci) {
		if (player instanceof ServerPlayerEntity serverPlayer) {
			HouseSelection.onItemCrafted(serverPlayer, stack);
		}
	}
}
