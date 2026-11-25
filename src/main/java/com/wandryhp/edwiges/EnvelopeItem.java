package com.wandryhp.edwiges;

import com.wandryhp.armor.HouseType;
import com.wandryhp.house.HouseSelection;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemUsage;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.world.World;

import java.util.Optional;

/**
 * Envelope coletavel: so com o clique direito e consumido e vira a carta da casa do jogador. A troca usa o
 * mecanismo vanilla de "item que vira outro item" (como balde/garrafa): com o envelope sozinho na mao, a carta
 * assume a mao (nao precisa de espaco livre); se houver mais de um, a carta vai ao inventario ou cai aos pes.
 */
public class EnvelopeItem extends Item {
	public EnvelopeItem(Settings settings) {
		super(settings);
	}

	@Override
	public ActionResult use(World world, PlayerEntity user, Hand hand) {
		ItemStack envelope = user.getStackInHand(hand);
		if (world.isClient) {
			return ActionResult.SUCCESS; // o servidor decide e sincroniza
		}
		Optional<HouseType> house = HouseSelection.getResult(user);
		if (house.isEmpty()) {
			return ActionResult.FAIL; // sem casa definida: nada acontece e o envelope e mantido
		}
		ItemStack letter = new ItemStack(ModEdwigesItems.letter(house.get()));
		return ActionResult.SUCCESS.withNewHandStack(ItemUsage.exchangeStack(envelope, user, letter, false));
	}
}
