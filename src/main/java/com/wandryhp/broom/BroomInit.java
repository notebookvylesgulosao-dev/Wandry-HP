package com.wandryhp.broom;

import com.wandryhp.WandryHP;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.SpawnGroup;
import net.minecraft.item.Item;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.util.Identifier;

/** Entrypoint da vassoura voadora (registrado em fabric.mod.json): entidade montavel, item e o envio das teclas de voo. */
public class BroomInit implements ModInitializer {
	public static EntityType<BroomEntity> ENTITY;
	public static Item ITEM;

	@Override
	public void onInitialize() {
		Identifier id = Identifier.of(WandryHP.MOD_ID, "witch_broom");

		RegistryKey<EntityType<?>> entityKey = RegistryKey.of(RegistryKeys.ENTITY_TYPE, id);
		ENTITY = Registry.register(Registries.ENTITY_TYPE, entityKey,
				EntityType.Builder.<BroomEntity>create(BroomEntity::new, SpawnGroup.MISC)
						// Caixa alta o bastante para cobrir o cavaleiro (evita sufocar a cabeca em tetos baixos);
						// o ponto de assento fica a 0.6 do chao da caixa, onde o jogador "senta" no cabo.
						.dimensions(0.9f, 1.9f)
						.passengerAttachments(0.6f)
						.maxTrackingRange(8)
						.trackingTickInterval(1)
						.makeFireImmune()
						.build(entityKey));

		RegistryKey<Item> itemKey = RegistryKey.of(RegistryKeys.ITEM, id);
		ITEM = Registry.register(Registries.ITEM, itemKey, new BroomItem(new Item.Settings().registryKey(itemKey).maxCount(1)));

		PayloadTypeRegistry.playC2S().register(BroomInputPayload.ID, BroomInputPayload.CODEC);
		ServerPlayNetworking.registerGlobalReceiver(BroomInputPayload.ID, (payload, context) -> {
			if (context.player().getVehicle() instanceof BroomEntity broom) {
				broom.setInput(payload.flags());
			}
		});
	}
}
