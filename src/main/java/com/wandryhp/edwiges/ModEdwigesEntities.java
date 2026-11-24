package com.wandryhp.edwiges;

import com.wandryhp.WandryHP;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.SpawnGroup;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.util.Identifier;

public final class ModEdwigesEntities {
	public static EntityType<EdwigesEntity> EDWIGES;
	public static EntityType<LetterEntity> LETTER;

	private ModEdwigesEntities() {}

	public static void register() {
		RegistryKey<EntityType<?>> owlKey = RegistryKey.of(RegistryKeys.ENTITY_TYPE, Identifier.of(WandryHP.MOD_ID, "edwiges"));
		EDWIGES = Registry.register(Registries.ENTITY_TYPE, owlKey,
				EntityType.Builder.<EdwigesEntity>create(EdwigesEntity::new, SpawnGroup.MISC)
						.dimensions(1.0f, 1.2f)
						.maxTrackingRange(10) // 160 blocos: ela aparece a ate 100 do jogador
						.trackingTickInterval(1)
						.makeFireImmune()
						.disableSaving()
						.disableSummon()
						.build(owlKey));

		RegistryKey<EntityType<?>> letterKey = RegistryKey.of(RegistryKeys.ENTITY_TYPE, Identifier.of(WandryHP.MOD_ID, "letter"));
		LETTER = Registry.register(Registries.ENTITY_TYPE, letterKey,
				EntityType.Builder.<LetterEntity>create(LetterEntity::new, SpawnGroup.MISC)
						.dimensions(0.3f, 0.1f)
						.maxTrackingRange(8)
						.trackingTickInterval(2)
						.makeFireImmune()
						.disableSummon()
						.build(letterKey));
	}
}
