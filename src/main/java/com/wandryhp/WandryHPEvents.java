package com.wandryhp;

import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;

/** Pacotes cliente -> servidor (roda de feitiços, ataque base, controles da vassoura). */
public final class WandryHPEvents {
    private WandryHPEvents() {}

    public static void initialize() {
        PayloadTypeRegistry.playC2S().register(SelectSpellPayload.ID, SelectSpellPayload.CODEC);
        PayloadTypeRegistry.playC2S().register(BaseAttackPayload.ID, BaseAttackPayload.CODEC);
        PayloadTypeRegistry.playC2S().register(BroomInputPayload.ID, BroomInputPayload.CODEC);

        ServerPlayNetworking.registerGlobalReceiver(SelectSpellPayload.ID, (payload, context) -> {
            ServerPlayerEntity player = context.player();
            int index = payload.spell();
            if (index < 0 || index >= MagicWandItem.Spell.values().length) return;
            ItemStack stack = player.getMainHandStack();
            if (!(stack.getItem() instanceof MagicWandItem)) stack = player.getOffHandStack();
            if (!(stack.getItem() instanceof MagicWandItem)) return;
            MagicWandItem.Spell spell = MagicWandItem.Spell.values()[index];
            MagicWandItem.setSelectedSpell(stack, spell);
            player.sendMessage(Text.literal("Feitiço: " + spell.displayName), true);
        });

        ServerPlayNetworking.registerGlobalReceiver(BaseAttackPayload.ID,
            (payload, context) -> MagicWandItem.baseAttack(context.player()));

        ServerPlayNetworking.registerGlobalReceiver(BroomInputPayload.ID, (payload, context) -> {
            if (context.player().getVehicle() instanceof BroomEntity broom) broom.setInput(payload.flags());
        });
    }
}
