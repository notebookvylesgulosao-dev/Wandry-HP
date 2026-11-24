package com.wandryhp.broom;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.world.World;

/** Botao direito: coloca a vassoura sob o jogador e ja monta nela (o item e consumido; pegar de volta = agachar + clique na vassoura). */
public class BroomItem extends Item {
	public BroomItem(Settings settings) {
		super(settings);
	}

	@Override
	public ActionResult use(World world, PlayerEntity user, Hand hand) {
		if (user.hasVehicle()) {
			return ActionResult.PASS;
		}
		if (!world.isClient) {
			BroomEntity broom = new BroomEntity(BroomInit.ENTITY, world);
			broom.refreshPositionAndAngles(user.getX(), user.getY(), user.getZ(), user.getYaw(), 0.0f);
			world.spawnEntity(broom);
			user.startRiding(broom);
			user.getStackInHand(hand).decrementUnlessCreative(1, user);
		}
		return ActionResult.SUCCESS;
	}
}
