package com.wandryhp;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Hand;
import net.minecraft.util.ActionResult;
import net.minecraft.world.World;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

/** Test-phase scroll: unlocks one of the seven currently specified spells as a wand item. */
public final class ScrollItem extends Item {
    private static final Item[] REWARDS = { WandryHP.AMETHYST_WAND, WandryHP.BONE_WAND, WandryHP.CELESTIAL_WAND,
            WandryHP.DRAGON_WAND, WandryHP.EMERALD_WAND, WandryHP.MAGMA_WAND, WandryHP.WIDOW_WAND };
    public ScrollItem(Settings settings) { super(settings); }
    @Override public ActionResult use(World world, PlayerEntity user, Hand hand) {
        ItemStack stack = user.getStackInHand(hand);
        if (!world.isClient) {
            if (world.random.nextFloat() < .20f) {
                user.sendMessage(Text.literal("O pergaminho não continha nenhum feitiço.").formatted(Formatting.GRAY), true);
            } else {
                java.util.List<Item> available = new java.util.ArrayList<>();
                for (Item reward : REWARDS) if (!user.getInventory().contains(new ItemStack(reward))) available.add(reward);
                if (available.isEmpty()) {
                    user.sendMessage(Text.literal("Você já desbloqueou todas as varinhas disponíveis nesta versão.").formatted(Formatting.GRAY), true);
                    return ActionResult.SUCCESS;
                }
                Item reward = available.get(world.random.nextInt(available.size()));
                ItemStack wand = new ItemStack(reward);
                if (!user.getInventory().insertStack(wand)) user.dropItem(wand, false);
                user.sendMessage(Text.literal("Feitiço desbloqueado: ").append(reward.getName()).formatted(Formatting.LIGHT_PURPLE), true);
            }
            if (!user.getAbilities().creativeMode) stack.decrement(1);
        }
        return ActionResult.SUCCESS;
    }
}
