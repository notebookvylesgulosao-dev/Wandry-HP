package com.wandryhp.spell;

import com.wandryhp.item.WandItem;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.state.property.Properties;
import net.minecraft.registry.RegistryKey;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;

/**
 * Lumos: enquanto ativo, a ponta da varinha solta partículas amarelas e um bloco de luz invisível (Blocks.LIGHT)
 * acompanha a ponta, iluminando o ambiente ao redor do jogador para todos. Liga/desliga ao lançar de novo; desliga
 * sozinho se o jogador morre, sai, vira espectador ou guarda a varinha.
 *
 * <p>O bloco de luz só é colocado em ar e é removido ao mover, desligar, sair ou parar o servidor.
 */
public final class LumosLight {
	private static final class State {
		RegistryKey<World> world;
		BlockPos pos;
	}

	private static final Map<UUID, State> ACTIVE = new HashMap<>();

	private LumosLight() {}

	public static boolean isActive(ServerPlayerEntity player) {
		return ACTIVE.containsKey(player.getUuid());
	}

	public static void turnOn(ServerPlayerEntity player) {
		ACTIVE.putIfAbsent(player.getUuid(), new State());
	}

	public static void turnOff(ServerPlayerEntity player) {
		State state = ACTIVE.remove(player.getUuid());
		if (state != null && player.getServer() != null) {
			clearLight(player.getServer(), state);
		}
	}

	public static void tick(MinecraftServer server) {
		if (ACTIVE.isEmpty()) {
			return;
		}
		Iterator<Map.Entry<UUID, State>> it = ACTIVE.entrySet().iterator();
		while (it.hasNext()) {
			Map.Entry<UUID, State> entry = it.next();
			State state = entry.getValue();
			ServerPlayerEntity player = server.getPlayerManager().getPlayer(entry.getKey());
			if (player == null || !player.isAlive() || player.isSpectator()
					|| !(player.getMainHandStack().getItem() instanceof WandItem wand)
					|| !(player.getWorld() instanceof ServerWorld world)) {
				clearLight(server, state);
				it.remove();
				continue;
			}
			Vec3d tip = SpellCaster.wandTip(player, wand.getWandType());

			BlockPos want = choose(world, player, tip, state);
			RegistryKey<World> key = world.getRegistryKey();
			boolean same = want != null && want.equals(state.pos) && key.equals(state.world);
			if (!same) {
				clearLight(server, state);
				if (want != null) {
					world.setBlockState(want, lightState(), Block.NOTIFY_LISTENERS);
					state.world = key;
					state.pos = want;
				}
			} else if (!world.getBlockState(want).isOf(Blocks.LIGHT)) {
				world.setBlockState(want, lightState(), Block.NOTIFY_LISTENERS);
			}

			// Ponta acesa: poeira amarela todo tique, itens (lã/concreto/corante) de vez em quando.
			SpellTrails.spawn(world, Spell.LUMOS, tip, 0.03, player.age % 3 == 0 ? 1 : 0, 1);
		}
	}

	/** Primeiro ponto livre (ar) perto da ponta; mantém o atual se ainda for o nosso bloco de luz. */
	private static BlockPos choose(ServerWorld world, ServerPlayerEntity player, Vec3d tip, State state) {
		BlockPos[] candidates = {
				BlockPos.ofFloored(tip),
				BlockPos.ofFloored(player.getEyePos()),
				player.getBlockPos().up(),
				player.getBlockPos()
		};
		for (BlockPos candidate : candidates) {
			BlockState current = world.getBlockState(candidate);
			boolean ours = candidate.equals(state.pos) && world.getRegistryKey().equals(state.world)
					&& current.isOf(Blocks.LIGHT);
			if (current.isAir() || ours) {
				return candidate;
			}
		}
		return null;
	}

	private static BlockState lightState() {
		return Blocks.LIGHT.getDefaultState().with(Properties.LEVEL_15, SpellEffects.LUMOS_LIGHT_LEVEL);
	}

	private static void clearLight(MinecraftServer server, State state) {
		if (state.pos == null || state.world == null) {
			return;
		}
		ServerWorld world = server.getWorld(state.world);
		if (world != null && world.getChunkManager().isChunkLoaded(state.pos.getX() >> 4, state.pos.getZ() >> 4)
				&& world.getBlockState(state.pos).isOf(Blocks.LIGHT)) {
			world.setBlockState(state.pos, Blocks.AIR.getDefaultState(), Block.NOTIFY_LISTENERS);
		}
		state.pos = null;
	}

	/** Servidor parando: remove todos os blocos de luz. */
	public static void clearAll(MinecraftServer server) {
		for (State state : ACTIVE.values()) {
			clearLight(server, state);
		}
		ACTIVE.clear();
	}
}
