package com.wandryhp.edwiges;

import com.wandryhp.armor.HouseType;
import com.wandryhp.house.HouseSelection;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.world.World;

import java.util.Optional;

/** Envelope coletavel: so com o clique direito e consumido e vira a carta da casa escolhida pelo jogador. */
public class EnvelopeItem extends Item {
	public EnvelopeItem(Settings settings) {
		super(settings);
	}

	@Override
	public ActionResult use(World world, PlayerEntity user, Hand hand) {
		Optional<HouseType> house = HouseSelection.getResult(user);
		if (house.isEmpty()) {
			return ActionResult.PASS;
		}
		if (!world.isClient) {
			user.getStackInHand(hand).decrementUnlessCreative(1, user);
			user.getInventory().offerOrDrop(new ItemStack(ModEdwigesItems.letter(house.get())));
		}
		return ActionResult.SUCCESS;
	}
}
