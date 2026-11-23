package com.wandryhp.mixin;

import com.wandryhp.house.CampfireCooking;
import net.minecraft.block.entity.CampfireBlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Selecao de casas: marca os itens que a fogueira acesa solta ao terminar de cozinhar (ver CampfireCooking). */
@Mixin(CampfireBlockEntity.class)
public abstract class CampfireBlockEntityMixin {
	@Inject(method = "litServerTick", at = @At("HEAD"))
	private static void wandryhp$beginCooking(CallbackInfo ci) {
		CampfireCooking.begin();
	}

	@Inject(method = "litServerTick", at = @At("RETURN"))
	private static void wandryhp$endCooking(CallbackInfo ci) {
		CampfireCooking.end();
	}
}
