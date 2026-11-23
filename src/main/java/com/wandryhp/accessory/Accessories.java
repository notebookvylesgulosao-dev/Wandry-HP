package com.wandryhp.accessory;

import com.wandryhp.WandryHP;
import net.fabricmc.fabric.api.attachment.v1.AttachmentRegistry;
import net.fabricmc.fabric.api.attachment.v1.AttachmentSyncPredicate;
import net.fabricmc.fabric.api.attachment.v1.AttachmentType;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Identifier;

/**
 * Armazenamento dos acessorios: um data attachment no jogador. E salvo junto com o jogador (persistente) e
 * sincronizado com todos os clientes (para que outros jogadores vejam a capa e o dono veja os proprios slots).
 */
public final class Accessories {
	public static final AttachmentType<AccessoryData> DATA = AttachmentRegistry.create(
			Identifier.of(WandryHP.MOD_ID, "accessories"),
			builder -> builder
					.initializer(() -> AccessoryData.EMPTY)
					.persistent(AccessoryData.CODEC)
					.syncWith(AccessoryData.PACKET_CODEC, AttachmentSyncPredicate.all()));

	private Accessories() {}

	public static AccessoryData get(PlayerEntity player) {
		return player.getAttachedOrCreate(DATA);
	}

	public static void set(PlayerEntity player, AccessoryData data) {
		if (data.isEmpty()) {
			player.removeAttached(DATA);
		} else {
			player.setAttached(DATA, data);
		}
	}

	public static ItemStack getStack(PlayerEntity player, AccessorySlotType slot) {
		return get(player).get(slot.ordinal());
	}

	/** A capa equipada no slot Chest, ou null. */
	public static CapeType equippedCape(PlayerEntity player) {
		if (getStack(player, AccessorySlotType.CHEST).getItem() instanceof EnchanterCapeItem cape) {
			return cape.getCapeType();
		}
		return null;
	}
}
