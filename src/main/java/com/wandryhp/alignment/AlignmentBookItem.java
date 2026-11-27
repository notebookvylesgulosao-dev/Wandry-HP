package com.wandryhp.alignment;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.world.World;

/**
 * Livro do Bem / do Mal. Clique direito: o jogador passa a ter o alinhamento do livro (troca o anterior, inclusive o
 * Neutro inicial) e o livro é consumido, também no criativo, como o envelope da Edwiges. Se o jogador já tem esse
 * alinhamento nada acontece e o livro é mantido (consumi-lo não mudaria nada).
 */
public class AlignmentBookItem extends Item {
	private final Alignment alignment;

	public AlignmentBookItem(Alignment alignment, Settings settings) {
		super(settings);
		this.alignment = alignment;
	}

	public Alignment alignment() {
		return alignment;
	}

	@Override
	public ActionResult use(World world, PlayerEntity user, Hand hand) {
		if (world.isClient) {
			return ActionResult.SUCCESS; // o servidor decide e sincroniza
		}
		if (!(user instanceof ServerPlayerEntity player)) {
			return ActionResult.PASS;
		}
		if (AlignmentData.of(player) == alignment) {
			player.sendMessage(Text.translatable("message.wandryhp.alignment.already", Text.translatable(alignment.translationKey())), true);
			return ActionResult.FAIL;
		}
		AlignmentData.set(player, alignment);
		player.sendMessage(Text.translatable("message.wandryhp.alignment.changed", Text.translatable(alignment.translationKey())), true);

		ItemStack remaining = player.getStackInHand(hand).copy();
		remaining.decrement(1);
		return ActionResult.SUCCESS.withNewHandStack(remaining);
	}
}
