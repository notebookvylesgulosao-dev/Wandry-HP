package com.wandryhp.broom;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.HitResult;
import net.minecraft.world.World;

/** Botao direito: coloca a vassoura sob o jogador e ja monta nela (o item e consumido; pegar de volta = agachar + clique na vassoura). */
public class BroomItem extends Item {
	/** Alcance (blocos) do raycast que procura o chao. */
	private static final double PLACE_REACH = 5.0;

	public BroomItem(Settings settings) {
		super(settings);
	}

	@Override
	public ActionResult use(World world, PlayerEntity user, Hand hand) {
		if (user.hasVehicle()) {
			return ActionResult.PASS;
		}
		// So pode ser colocada olhando para um bloco (raycast): nunca para o ceu ou o vazio.
		HitResult hit = user.raycast(PLACE_REACH, 0.0f, false);
		if (hit.getType() != HitResult.Type.BLOCK) {
			if (!world.isClient) {
				user.sendMessage(Text.translatable("message.wandryhp.broom.look_down"), true);
			}
			return ActionResult.FAIL;
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
