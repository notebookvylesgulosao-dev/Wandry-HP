package com.wandryhp.house;

import com.mojang.serialization.Codec;
import com.wandryhp.WandryHP;
import net.minecraft.component.ComponentType;
import net.minecraft.entity.Entity;
import net.minecraft.entity.ItemEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Identifier;

/**
 * Cozinhar na fogueira (Lufa-Lufa). Quando a fogueira termina de cozinhar, o item cai no chao como entidade;
 * ela e marcada com um componente invisivel ({@link #MARK}). Ao ser coletada por um jogador, a marca e retirada
 * e cada unidade realmente coletada pontua uma vez (ver ItemEntityMixin). Como a marca e do item, empilhar
 * entidades proximas soma as unidades corretamente, e sem marca (item solto de novo, vindo de bau etc.) nao
 * pontua: nenhuma unidade conta duas vezes.
 */
public final class CampfireCooking {
	/** Marca "saiu cozido de uma fogueira e ainda nao foi coletado". */
	public static final ComponentType<Boolean> MARK = Registry.register(Registries.DATA_COMPONENT_TYPE,
			Identifier.of(WandryHP.MOD_ID, "campfire_cooked"),
			ComponentType.<Boolean>builder().codec(Codec.BOOL).packetCodec(PacketCodecs.BOOLEAN).build());

	/** true so durante o tick de uma fogueira acesa (o servidor roda num thread so). */
	private static boolean cooking;

	private CampfireCooking() {}

	/** Forca o registro do componente antes de qualquer item ser criado. */
	public static void init() {
		MARK.hashCode();
	}

	public static void begin() {
		cooking = true;
	}

	public static void end() {
		cooking = false;
	}

	/** ENTITY_LOAD: item que acabou de cair da fogueira recebe a marca. */
	public static void onEntityLoad(Entity entity, ServerWorld world) {
		if (cooking && entity instanceof ItemEntity item) {
			item.getStack().set(MARK, true);
		}
	}

	/** Retira a marca dos itens de um jogador (itens que passaram por funil/bau, para voltarem a empilhar). */
	public static void clear(ItemStack stack) {
		if (!stack.isEmpty() && stack.contains(MARK)) {
			stack.remove(MARK);
		}
	}

	public static void collected(ServerPlayerEntity player, int count) {
		HouseSelection.onCampfireItemsCollected(player, count);
	}
}
