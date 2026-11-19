package com.wandryhp;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.world.World;

/** Ao usar, invoca a vassoura como entidade e monta nela. O item só volta ao desmontar ou recuperar. */
public final class BroomItem extends Item {
    public BroomItem(Settings settings) { super(settings); }

    @Override public ActionResult use(World world, PlayerEntity user, Hand hand) {
        if (user.hasVehicle()) return ActionResult.PASS;
        if (!world.isClient) {
            BroomEntity broom = new BroomEntity(WandryHP.BROOM, world);
            broom.refreshPositionAndAngles(user.getX(), user.getY() + 0.2, user.getZ(), user.getYaw(), 0f);
            world.spawnEntity(broom);
            user.startRiding(broom);
            if (!user.getAbilities().creativeMode) user.getStackInHand(hand).decrement(1);
        }
        return ActionResult.SUCCESS;
    }
}
