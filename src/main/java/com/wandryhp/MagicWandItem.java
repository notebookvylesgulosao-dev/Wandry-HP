package com.wandryhp;

import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.NbtComponent;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.world.World;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Botão direito: lança o feitiço selecionado na roda (projétil com rastro do feitiço + rastro da varinha).
 * Botão esquerdo: ataque base (prop da varinha com rastro da varinha).
 */
public final class MagicWandItem extends Item {
    public enum Spell {
        AVADA_KEDAVRA("Avada Kedavra", 0x22C55E),
        CORINFICUS("Corinficus", 0xE53935),
        CRUCIO("Crucio", 0xFF1111),
        EXPELLIARMUS("Expelliarmus", 0xF2F2F2),
        IMPERIUS("Imperius", 0xFFC93C),
        PETRIFICUS_TOTALIS("Petrificus Totalis", 0x9E9E9E),
        SECTUMSEMPRA("Sectumsempra", 0x8B2BE2);
        public final String displayName;
        public final int color;
        Spell(String displayName, int color) { this.displayName = displayName; this.color = color; }
    }

    /** Estilo de rastro da varinha e índice do prop de ataque base (ordem de QuadModel.NAMES). */
    public enum Style {
        AMETHYST(7), BONE(8), CELESTIAL(9), DRAGON(10),
        EMERALD(7),  // sem prop próprio: usa o do Amethyst (teste)
        MAGMA(10),   // sem prop próprio: usa o do Dragon (teste)
        WIDOW(11);
        public final int propModel;
        Style(int propModel) { this.propModel = propModel; }
    }

    private static final String SPELL_KEY = "wandry_spell";
    private static final int BASE_ATTACK_COOLDOWN_TICKS = 10;
    private static final Map<UUID, Long> LAST_BASE_ATTACK = new HashMap<>();

    private final Spell defaultSpell;
    private final Style style;

    public MagicWandItem(Spell defaultSpell, Style style, Settings settings) {
        super(settings);
        this.defaultSpell = defaultSpell;
        this.style = style;
    }

    public Spell spell() { return defaultSpell; }
    public Style style() { return style; }

    /** Feitiço escolhido na roda para esta varinha (ou o padrão dela). */
    public static Spell selectedSpell(ItemStack stack) {
        Spell fallback = stack.getItem() instanceof MagicWandItem wand ? wand.defaultSpell : Spell.EXPELLIARMUS;
        NbtComponent data = stack.get(DataComponentTypes.CUSTOM_DATA);
        if (data != null) {
            NbtCompound nbt = data.copyNbt();
            if (nbt.contains(SPELL_KEY)) {
                int i = nbt.getInt(SPELL_KEY);
                if (i >= 0 && i < Spell.values().length) return Spell.values()[i];
            }
        }
        return fallback;
    }

    public static void setSelectedSpell(ItemStack stack, Spell spell) {
        NbtComponent.set(DataComponentTypes.CUSTOM_DATA, stack, nbt -> nbt.putInt(SPELL_KEY, spell.ordinal()));
    }

    @Override public ActionResult use(World world, PlayerEntity user, Hand hand) {
        ItemStack stack = user.getStackInHand(hand);
        Spell spell = selectedSpell(stack);
        if (!world.isClient && world instanceof ServerWorld serverWorld) {
            SpellProjectileEntity.launch(serverWorld, user, spell.ordinal(), spell.ordinal(), style.ordinal());
            serverWorld.playSound(null, user.getBlockPos(), SoundEvents.ENTITY_EVOKER_CAST_SPELL, SoundCategory.PLAYERS, .7f, 1.15f);
        }
        int cooldown = switch (spell) {
            case AVADA_KEDAVRA, SECTUMSEMPRA -> 600;
            case CORINFICUS, IMPERIUS -> 400;
            case CRUCIO -> 300;
            case EXPELLIARMUS, PETRIFICUS_TOTALIS -> 200;
        };
        user.getItemCooldownManager().set(stack, cooldown);
        return ActionResult.SUCCESS;
    }

    /** Chamado pelo servidor quando o cliente clica com o botão esquerdo segurando uma varinha. */
    public static void baseAttack(ServerPlayerEntity player) {
        ItemStack stack = player.getMainHandStack();
        if (!(stack.getItem() instanceof MagicWandItem wand)) return;
        ServerWorld world = player.getServerWorld();
        long now = world.getTime();
        Long last = LAST_BASE_ATTACK.get(player.getUuid());
        if (last != null && now - last < BASE_ATTACK_COOLDOWN_TICKS) return;
        LAST_BASE_ATTACK.put(player.getUuid(), now);
        player.swingHand(Hand.MAIN_HAND, true);
        SpellProjectileEntity.launch(world, player, wand.style.propModel, -1, wand.style.ordinal());
        world.playSound(null, player.getBlockPos(), SoundEvents.ENTITY_EVOKER_CAST_SPELL, SoundCategory.PLAYERS, .4f, 1.7f);
    }
}
