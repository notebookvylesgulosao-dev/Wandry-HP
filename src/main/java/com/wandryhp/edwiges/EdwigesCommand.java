package com.wandryhp.edwiges;

import com.mojang.brigadier.CommandDispatcher;
import com.wandryhp.armor.HouseType;
import com.wandryhp.house.HouseSelection;
import net.minecraft.command.CommandSource;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;

import java.util.Arrays;

/** /edwiges [casa]: invoca a Edwiges para quem executa (usa a casa do jogador; sem casa, a informada ou Grifinoria). */
public final class EdwigesCommand {
	private EdwigesCommand() {}

	public static void register(CommandDispatcher<ServerCommandSource> dispatcher) {
		dispatcher.register(CommandManager.literal("edwiges")
				.requires(src -> src.hasPermissionLevel(2))
				.executes(ctx -> run(ctx.getSource(), null))
				.then(CommandManager.argument("casa", com.mojang.brigadier.arguments.StringArgumentType.word())
						.suggests((ctx, b) -> CommandSource.suggestMatching(
								Arrays.stream(HouseType.values()).map(h -> h.id), b))
						.executes(ctx -> run(ctx.getSource(),
								com.mojang.brigadier.arguments.StringArgumentType.getString(ctx, "casa")))));
	}

	private static int run(ServerCommandSource source, String houseArg) {
		ServerPlayerEntity player = source.getPlayer();
		if (player == null) {
			source.sendError(Text.literal("Somente jogadores podem usar este comando."));
			return 0;
		}
		HouseType house = null;
		if (houseArg != null) {
			for (HouseType h : HouseType.values()) {
				if (h.id.equalsIgnoreCase(houseArg)) house = h;
			}
			if (house == null) {
				source.sendError(Text.literal("Casa invalida: " + houseArg));
				return 0;
			}
		} else {
			house = HouseSelection.getResult(player).orElse(HouseType.GRYFFINDOR);
		}
		boolean ok = EdwigesDelivery.spawn(player.getServerWorld(), player, house);
		if (ok) {
			source.sendFeedback(() -> Text.literal("Edwiges a caminho!"), false);
		}
		return ok ? 1 : 0;
	}
}
