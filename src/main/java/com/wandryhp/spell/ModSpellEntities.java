package com.wandryhp.spell;

import com.wandryhp.WandryHP;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.SpawnGroup;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.util.Identifier;

public final class ModSpellEntities {
	public static EntityType<SpellProjectileEntity> SPELL_PROJECTILE;
	public static EntityType<PatronusEntity> PATRONUS;

	private ModSpellEntities() {}

	public static void register() {
		RegistryKey<EntityType<?>> key = RegistryKey.of(RegistryKeys.ENTITY_TYPE, Identifier.of(WandryHP.MOD_ID, "spell_projectile"));
		SPELL_PROJECTILE = Registry.register(Registries.ENTITY_TYPE, key,
				EntityType.Builder.<SpellProjectileEntity>create(SpellProjectileEntity::new, SpawnGroup.MISC)
						.dimensions(0.3f, 0.3f)
						.maxTrackingRange(8)
						.trackingTickInterval(1)
						.disableSaving()
						.disableSummon()
						.build(key));

		RegistryKey<EntityType<?>> patronusKey = RegistryKey.of(RegistryKeys.ENTITY_TYPE, Identifier.of(WandryHP.MOD_ID, "patronus"));
		PATRONUS = Registry.register(Registries.ENTITY_TYPE, patronusKey,
				EntityType.Builder.<PatronusEntity>create(PatronusEntity::new, SpawnGroup.MISC)
						.dimensions(0.6f, 2.0f)
						.maxTrackingRange(8)
						.trackingTickInterval(1)
						.makeFireImmune()
						.disableSaving()
						.disableSummon()
						.build(patronusKey));
	}
}
