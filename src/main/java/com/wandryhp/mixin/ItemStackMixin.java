package com.wandryhp.mixin;

import com.wandryhp.house.HouseSelection;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Selecao de casas: avisa quando um jogador termina de consumir um alimento (cada alimento consumido). */
@Mixin(ItemStack.class)
public abstract class ItemStackMixin {
	@Inject(method = "finishUsing", at = @At("HEAD"))
	private void wandryhp$onFinishUsing(World world, LivingEntity user, CallbackInfoReturnable<ItemStack> cir) {
		ItemStack self = (ItemStack) (Object) this;
		if (user instanceof ServerPlayerEntity player && self.get(DataComponentTypes.FOOD) != null) {
			HouseSelection.onFoodEaten(player);
		}
	}
}
