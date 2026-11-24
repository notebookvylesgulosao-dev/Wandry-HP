package com.wandryhp.edwiges;

import com.mojang.serialization.Codec;
import com.wandryhp.WandryHP;
import com.wandryhp.armor.HouseType;
import com.wandryhp.house.HouseSelection;
import net.fabricmc.fabric.api.attachment.v1.AttachmentRegistry;
import net.fabricmc.fabric.api.attachment.v1.AttachmentType;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.Heightmap;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * Entrega da carta (somente servidor). Quando a selecao de casas de um jogador termina e a carta ainda nao foi
 * entregue, faz a Edwiges aparecer. Apenas LE o resultado de {@link HouseSelection}; nao altera esse sistema.
 *
 * <p>"Entregue" e um attachment persistente do jogador, marcado quando a Edwiges solta o envelope. Se ela
 * sumir antes (jogador saiu, morreu, trocou de dimensao, ficou para tras), uma nova aparece depois de uma
 * espera, ate a carta ser entregue. Jogadores que ja tinham terminado a selecao antes deste recurso tambem
 * recebem a carta.
 */
public final class EdwigesDelivery {
	public static final AttachmentType<Boolean> DELIVERED = AttachmentRegistry.create(
			Identifier.of(WandryHP.MOD_ID, "edwiges_delivered"),
			builder -> builder
					.initializer(() -> Boolean.FALSE)
					.persistent(Codec.BOOL));

	/** A cada quantos ticks verifica quem precisa de uma Edwiges. */
	private static final int POLL_INTERVAL_TICKS = 20;

	/** jogador -> Edwiges ativa dele. */
	private static final Map<UUID, UUID> ACTIVE = new HashMap<>();
	/** jogador -> tempo do mundo a partir do qual pode aparecer outra. */
	private static final Map<UUID, Long> RETRY_AT = new HashMap<>();

	private EdwigesDelivery() {}

	public static boolean isDelivered(PlayerEntity player) {
		return Boolean.TRUE.equals(player.getAttachedOrCreate(DELIVERED));
	}

	/** Chamado pela Edwiges no instante em que solta o envelope. */
	public static void markDelivered(ServerPlayerEntity player) {
		player.setAttached(DELIVERED, Boolean.TRUE);
		ACTIVE.remove(player.getUuid());
		RETRY_AT.remove(player.getUuid());
	}

	public static void tick(MinecraftServer server) {
		if (server.getTicks() % POLL_INTERVAL_TICKS != 0) return;
		for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
			UUID id = player.getUuid();
			Optional<HouseType> house = HouseSelection.isFinished(player) ? HouseSelection.getResult(player) : Optional.empty();
			if (house.isEmpty() || isDelivered(player)) {
				ACTIVE.remove(id);
				continue;
			}
			if (!player.isAlive()) continue;

			ServerWorld world = player.getServerWorld();
			UUID owlId = ACTIVE.get(id);
			if (owlId != null) {
				Entity owl = world.getEntity(owlId);
				if (owl instanceof EdwigesEntity && owl.isAlive()) continue; // ja esta a caminho
				ACTIVE.remove(id);
				EdwigesHudPayload.sendStop(player); // sumiu sem entregar: a HUD de chegada sai
				RETRY_AT.put(id, world.getTime() + EdwigesSettings.RETRY_DELAY_TICKS);
			}
			if (world.getTime() < RETRY_AT.getOrDefault(id, 0L)) continue;
			spawn(world, player, house.get());
		}
	}

	private static void spawn(ServerWorld world, ServerPlayerEntity player, HouseType house) {
		Vec3d pos = pickSpawnPoint(world, player);
		Vec3d toPlayer = player.getEyePos().subtract(pos);

		EdwigesEntity owl = new EdwigesEntity(ModEdwigesEntities.EDWIGES, world);
		float yaw = (float) Math.toDegrees(Math.atan2(-toPlayer.x, toPlayer.z));
		owl.refreshPositionAndAngles(pos.x, pos.y, pos.z, yaw, 0.0f);
		owl.setTarget(player.getUuid());
		owl.setHouse(house);
		owl.setHasLetter(true);
		owl.setVelocity(toPlayer.normalize().multiply(EdwigesSettings.SPEED_PER_TICK));
		if (world.spawnEntity(owl)) {
			ACTIVE.put(player.getUuid(), owl.getUuid());
		}
	}

	/**
	 * Ponto de aparicao: direcao (azimute) aleatoria, a no maximo {@link EdwigesSettings#SPAWN_RADIUS} blocos do
	 * jogador (distancia 3D) e pelo menos {@link EdwigesSettings#MIN_ALTITUDE_ABOVE_GROUND} blocos acima do chao
	 * daquela coluna. Se nenhuma tentativa servir (ex.: jogador numa caverna sob uma montanha), fica logo acima
	 * dele, mantendo a altura minima.
	 */
	private static Vec3d pickSpawnPoint(ServerWorld world, ServerPlayerEntity player) {
		Vec3d p = player.getPos();
		Random random = world.getRandom();
		for (int attempt = 0; attempt < 64; attempt++) {
			double azimuth = random.nextDouble() * Math.PI * 2.0;
			double elevation = Math.toRadians(45.0 + random.nextDouble() * 45.0);
			double radius = EdwigesSettings.SPAWN_RADIUS * (0.8 + random.nextDouble() * 0.2);
			double x = p.x + Math.cos(elevation) * Math.cos(azimuth) * radius;
			double z = p.z + Math.cos(elevation) * Math.sin(azimuth) * radius;
			double y = p.y + Math.sin(elevation) * radius;
			if (y >= groundAt(world, x, z, p.y) + EdwigesSettings.MIN_ALTITUDE_ABOVE_GROUND) {
				return new Vec3d(x, y, z);
			}
		}
		double ground = Math.max(p.y, groundAt(world, p.x, p.z, p.y));
		return new Vec3d(p.x, ground + EdwigesSettings.MIN_ALTITUDE_ABOVE_GROUND + 5.0, p.z);
	}

	/** Altura do chao na coluna (ignora folhas); sem forcar carregar chunk: se nao carregado, usa o nivel do jogador. */
	private static double groundAt(ServerWorld world, double x, double z, double fallback) {
		int bx = MathHelper.floor(x), bz = MathHelper.floor(z);
		if (!world.isChunkLoaded(bx >> 4, bz >> 4)) return fallback;
		return world.getTopY(Heightmap.Type.MOTION_BLOCKING_NO_LEAVES, bx, bz);
	}

	public static void forget(ServerPlayerEntity player) {
		ACTIVE.remove(player.getUuid());
		RETRY_AT.remove(player.getUuid());
	}

	public static void clear() {
		ACTIVE.clear();
		RETRY_AT.clear();
	}
}
