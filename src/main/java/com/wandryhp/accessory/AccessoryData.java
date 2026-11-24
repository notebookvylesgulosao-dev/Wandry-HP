package com.wandryhp.accessory;

import com.mojang.serialization.Codec;
import net.minecraft.item.ItemStack;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;

import java.util.ArrayList;
import java.util.List;

/**
 * Conteudo imutavel dos 4 slots de acessorio de um jogador (data attachment). Sempre tem exatamente 4 entradas,
 * na ordem de {@link AccessorySlotType}. Imutavel de proposito: toda alteracao cria um novo valor e e gravada com
 * setAttached, o que dispara a sincronizacao com os clientes.
 */
public record AccessoryData(List<ItemStack> stacks) {
	public static final int SIZE = AccessorySlotType.values().length;

	public static final AccessoryData EMPTY = new AccessoryData(emptyList());

	public static final Codec<AccessoryData> CODEC = ItemStack.OPTIONAL_CODEC.listOf()
			.xmap(AccessoryData::new, AccessoryData::stacks);

	public static final PacketCodec<RegistryByteBuf, AccessoryData> PACKET_CODEC =
			ItemStack.OPTIONAL_PACKET_CODEC.collect(PacketCodecs.toList()).xmap(AccessoryData::new, AccessoryData::stacks);

	public AccessoryData {
		List<ItemStack> copy = new ArrayList<>(SIZE);
		for (int i = 0; i < SIZE; i++) {
			copy.add(i < stacks.size() ? stacks.get(i).copy() : ItemStack.EMPTY);
		}
		stacks = List.copyOf(copy);
	}

	private static List<ItemStack> emptyList() {
		List<ItemStack> list = new ArrayList<>(SIZE);
		for (int i = 0; i < SIZE; i++) {
			list.add(ItemStack.EMPTY);
		}
		return list;
	}

	public ItemStack get(int slot) {
		return stacks.get(slot).copy();
	}

	public AccessoryData with(int slot, ItemStack stack) {
		List<ItemStack> list = new ArrayList<>(stacks);
		list.set(slot, stack.copy());
		return new AccessoryData(list);
	}

	public boolean isEmpty() {
		return stacks.stream().allMatch(ItemStack::isEmpty);
	}
}
