package com.wandryhp;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.itemgroup.v1.FabricItemGroup;
import net.minecraft.component.type.AttributeModifierSlot;
import net.minecraft.component.type.AttributeModifiersComponent;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.SpawnGroup;
import net.minecraft.entity.attribute.EntityAttributeModifier;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.item.*;
import net.minecraft.registry.*;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

import java.util.function.Function;

public final class WandryHP implements ModInitializer {
    public static final String MOD_ID = "wandry_hp";
    public static final Item AMETHYST_WAND = wand("amethyst_wand", MagicWandItem.Spell.EXPELLIARMUS, MagicWandItem.Style.AMETHYST);
    public static final Item BONE_WAND = wand("bone_wand", MagicWandItem.Spell.CRUCIO, MagicWandItem.Style.BONE);
    public static final Item CELESTIAL_WAND = wand("celestial_wand", MagicWandItem.Spell.IMPERIUS, MagicWandItem.Style.CELESTIAL);
    public static final Item DRAGON_WAND = wand("dragon_wand", MagicWandItem.Spell.CORINFICUS, MagicWandItem.Style.DRAGON);
    public static final Item EMERALD_WAND = wand("emerald_wand", MagicWandItem.Spell.PETRIFICUS_TOTALIS, MagicWandItem.Style.EMERALD);
    public static final Item MAGMA_WAND = wand("magma_wand", MagicWandItem.Spell.SECTUMSEMPRA, MagicWandItem.Style.MAGMA);
    public static final Item WIDOW_WAND = wand("widow_wand", MagicWandItem.Spell.AVADA_KEDAVRA, MagicWandItem.Style.WIDOW);

    /** Chapéu: slot da cabeça, +5 corações (10 de vida), 15% menos repulsão e 30% mais velocidade. */
    public static final Item WITCH_HAT = register("witch_hat", Item::new, new Item.Settings().maxCount(1)
        .equippable(EquipmentSlot.HEAD)
        .attributeModifiers(AttributeModifiersComponent.builder()
            .add(EntityAttributes.MAX_HEALTH,
                new EntityAttributeModifier(id("witch_hat_health"), 10.0, EntityAttributeModifier.Operation.ADD_VALUE),
                AttributeModifierSlot.HEAD)
            .add(EntityAttributes.KNOCKBACK_RESISTANCE,
                new EntityAttributeModifier(id("witch_hat_knockback"), 0.15, EntityAttributeModifier.Operation.ADD_VALUE),
                AttributeModifierSlot.HEAD)
            .add(EntityAttributes.MOVEMENT_SPEED,
                new EntityAttributeModifier(id("witch_hat_speed"), 0.30, EntityAttributeModifier.Operation.ADD_MULTIPLIED_BASE),
                AttributeModifierSlot.HEAD)
            .build()));
    public static final Item WITCH_BROOM = register("witch_broom", BroomItem::new, new Item.Settings().maxCount(1));
    public static final Item SPELL_SCROLL = register("spell_scroll", ScrollItem::new, new Item.Settings().maxCount(16));

    public static final EntityType<BroomEntity> BROOM = entity("witch_broom",
        EntityType.Builder.create(BroomEntity::new, SpawnGroup.MISC)
            .dimensions(0.9f, 0.4f).maxTrackingRange(10).trackingTickInterval(1).passengerAttachments(0.45f));
    public static final EntityType<SpellProjectileEntity> SPELL_PROJECTILE = entity("spell_projectile",
        EntityType.Builder.<SpellProjectileEntity>create(SpellProjectileEntity::new, SpawnGroup.MISC)
            .dimensions(0.3f, 0.3f).maxTrackingRange(4).trackingTickInterval(5));

    public static final RegistryKey<ItemGroup> GROUP_KEY = RegistryKey.of(RegistryKeys.ITEM_GROUP, id("main"));
    public static final ItemGroup GROUP = FabricItemGroup.builder().icon(() -> new ItemStack(AMETHYST_WAND))
        .displayName(Text.translatable("itemGroup.wandry_hp.main"))
        .entries((ctx, entries) -> { entries.add(AMETHYST_WAND); entries.add(BONE_WAND); entries.add(CELESTIAL_WAND); entries.add(DRAGON_WAND); entries.add(EMERALD_WAND); entries.add(MAGMA_WAND); entries.add(WIDOW_WAND); entries.add(WITCH_HAT); entries.add(WITCH_BROOM); entries.add(SPELL_SCROLL); }).build();

    private static Item wand(String name, MagicWandItem.Spell spell, MagicWandItem.Style style) {
        return register(name, s -> new MagicWandItem(spell, style, s), new Item.Settings().maxCount(1));
    }

    private static <T extends Item> T register(String name, Function<Item.Settings, T> factory, Item.Settings settings) {
        RegistryKey<Item> key = RegistryKey.of(RegistryKeys.ITEM, id(name));
        return Registry.register(Registries.ITEM, key, factory.apply(settings.registryKey(key)));
    }

    private static <T extends Entity> EntityType<T> entity(String name, EntityType.Builder<T> builder) {
        RegistryKey<EntityType<?>> key = RegistryKey.of(RegistryKeys.ENTITY_TYPE, id(name));
        return Registry.register(Registries.ENTITY_TYPE, key, builder.build(key));
    }

    public static Identifier id(String path) { return Identifier.of(MOD_ID, path); }

    @Override public void onInitialize() {
        Registry.register(Registries.ITEM_GROUP, GROUP_KEY, GROUP);
        WandryHPEvents.initialize();
    }
}
