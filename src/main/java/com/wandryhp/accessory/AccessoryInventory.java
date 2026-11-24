package com.wandryhp.accessory;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.inventory.Inventory;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;

/** Visao de {@link Inventory} sobre o attachment do jogador, para os slots funcionarem com o ScreenHandler vanilla. */
public class AccessoryInventory implements Inventory {
	private final PlayerEntity player;

	public AccessoryInventory(PlayerEntity player) {
		this.player = player;
	}

	@Override
	public int size() {
		return AccessoryData.SIZE;
	}

	@Override
	public boolean isEmpty() {
		return Accessories.get(player).isEmpty();
	}

	@Override
	public ItemStack getStack(int slot) {
		return Accessories.get(player).get(slot);
	}

	@Override
	public ItemStack removeStack(int slot, int amount) {
		ItemStack current = getStack(slot);
		if (current.isEmpty() || amount <= 0) {
			return ItemStack.EMPTY;
		}
		ItemStack taken = current.split(amount);
		setStack(slot, current);
		return taken;
	}

	@Override
	public ItemStack removeStack(int slot) {
		ItemStack current = getStack(slot);
		setStack(slot, ItemStack.EMPTY);
		return current;
	}

	@Override
	public void setStack(int slot, ItemStack stack) {
		if (stack.getCount() > 1) {
			stack = stack.copyWithCount(1);
		}
		Accessories.set(player, Accessories.get(player).with(slot, stack));
		// Aplica/remove o atributo imediatamente, sem esperar o proximo tick.
		if (player instanceof ServerPlayerEntity serverPlayer) {
			AccessoryEffects.update(serverPlayer);
		}
	}

	@Override
	public void markDirty() {
	}

	@Override
	public boolean canPlayerUse(PlayerEntity player) {
		return true;
	}

	@Override
	public void clear() {
		Accessories.set(player, AccessoryData.EMPTY);
		if (player instanceof ServerPlayerEntity serverPlayer) {
			AccessoryEffects.update(serverPlayer);
		}
	}
}
