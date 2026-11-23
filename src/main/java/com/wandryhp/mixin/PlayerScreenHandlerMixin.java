package com.wandryhp.mixin;

import com.wandryhp.accessory.AccessoryInventory;
import com.wandryhp.accessory.AccessorySlot;
import com.wandryhp.accessory.AccessorySlotType;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.PlayerScreenHandler;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.ScreenHandlerType;
import net.minecraft.screen.slot.Slot;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Acrescenta os 4 slots de acessorio ao inventario do jogador (ao final da lista, sem tocar nos slots vanilla de
 * armadura). Como os slots fazem parte do ScreenHandler, os cliques usam o pacote vanilla e a validacao e feita
 * no servidor, em single-player e multiplayer.
 */
@Mixin(PlayerScreenHandler.class)
public abstract class PlayerScreenHandlerMixin extends ScreenHandler {
	/** Posicao no painel (relativa ao canto do inventario; o painel fica a esquerda dele). */
	private static final int PANEL_X = -22;
	private static final int PANEL_Y = 8;

	protected PlayerScreenHandlerMixin(ScreenHandlerType<?> type, int syncId) {
		super(type, syncId);
	}

	@Inject(method = "<init>", at = @At("TAIL"))
	private void wandryhp$addAccessorySlots(PlayerInventory inventory, boolean onServer, PlayerEntity owner, CallbackInfo ci) {
		AccessoryInventory accessories = new AccessoryInventory(owner);
		for (AccessorySlotType type : AccessorySlotType.values()) {
			this.addSlot(new AccessorySlot(accessories, type, PANEL_X, PANEL_Y + type.ordinal() * 18));
		}
	}

	/** Shift-click: capa do inventario vai para o slot Chest; item de acessorio volta para o inventario. */
	@Inject(method = "quickMove", at = @At("HEAD"), cancellable = true)
	private void wandryhp$quickMoveAccessories(PlayerEntity player, int index, CallbackInfoReturnable<ItemStack> cir) {
		if (index < 0 || index >= this.slots.size()) {
			return;
		}
		Slot slot = this.slots.get(index);
		if (!slot.hasStack()) {
			return;
		}
		if (slot instanceof AccessorySlot) {
			ItemStack stack = slot.getStack();
			ItemStack original = stack.copy();
			if (!this.insertItem(stack, 9, 45, false)) {
				cir.setReturnValue(ItemStack.EMPTY);
				return;
			}
			slot.setStack(stack.isEmpty() ? ItemStack.EMPTY : stack);
			cir.setReturnValue(original);
			return;
		}
		if (AccessorySlotType.CHEST.accepts(slot.getStack())) {
			for (Slot candidate : this.slots) {
				if (candidate instanceof AccessorySlot accessory && accessory.getType() == AccessorySlotType.CHEST
						&& !accessory.hasStack()) {
					ItemStack stack = slot.getStack();
					ItemStack original = stack.copy();
					accessory.setStack(stack.split(1));
					slot.setStack(stack.isEmpty() ? ItemStack.EMPTY : stack);
					cir.setReturnValue(original);
					return;
				}
			}
		}
	}
}
