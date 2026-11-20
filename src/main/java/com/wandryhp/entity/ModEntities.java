package com.wandryhp.entity;

import com.wandryhp.WandryHP;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.SpawnGroup;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.util.Identifier;

public final class ModEntities {
	public static EntityType<WandPropEntity> WAND_PROP;

	private ModEntities() {}

	public static void register() {
		RegistryKey<EntityType<?>> key = RegistryKey.of(RegistryKeys.ENTITY_TYPE, Identifier.of(WandryHP.MOD_ID, "wand_prop"));
		WAND_PROP = Registry.register(Registries.ENTITY_TYPE, key,
				EntityType.Builder.<WandPropEntity>create(WandPropEntity::new, SpawnGroup.MISC)
						.dimensions(0.3f, 0.3f)
						.maxTrackingRange(8)
						.trackingTickInterval(1)
						.disableSaving()
						.disableSummon()
						.build(key));
	}
}
