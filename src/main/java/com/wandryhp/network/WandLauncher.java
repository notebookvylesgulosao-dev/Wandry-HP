package com.wandryhp.network;

import com.wandryhp.WandryHP;
import com.wandryhp.entity.ModEntities;
import com.wandryhp.entity.WandPropEntity;
import com.wandryhp.item.WandItem;
import com.wandryhp.item.WandType;
import net.minecraft.entity.Entity;
import net.minecraft.entity.projectile.ProjectileUtil;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Arm;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.RaycastContext;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/** Logica servidor do lancamento: valida varinha e cooldown, calcula ponta/direcao e spawna o prop. */
public final class WandLauncher {
	/** Tick do servidor do ultimo lancamento por jogador (cooldown global, vale entre varinhas). */
	private static final Map<UUID, Integer> LAST_LAUNCH = new HashMap<>();

	private WandLauncher() {}

	public static void forget(ServerPlayerEntity player) {
		LAST_LAUNCH.remove(player.getUuid());
	}

	public static void tryLaunch(ServerPlayerEntity player) {
		ItemStack stack = player.getMainHandStack();
		if (!(stack.getItem() instanceof WandItem wand) || player.isSpectator()) {
			return;
		}
		if (!(player.getWorld() instanceof ServerWorld world)) {
			return;
		}

		int now = world.getServer().getTicks();
		Integer last = LAST_LAUNCH.get(player.getUuid());
		if (last != null && now - last < WandryHP.CONFIG.cooldownTicks) {
			return;
		}
		LAST_LAUNCH.put(player.getUuid(), now);

		WandType type = wand.getWandType();
		Vec3d eye = player.getEyePos();
		Vec3d forward = player.getRotationVec(1.0f);

		// Base da camera do jogador: direita (a partir do yaw) e cima.
		double yaw = Math.toRadians(player.getYaw());
		Vec3d right = new Vec3d(-Math.cos(yaw), 0.0, -Math.sin(yaw));
		Vec3d up = right.crossProduct(forward);
		double side = player.getMainArm() == Arm.RIGHT ? 1.0 : -1.0;

		// Ponta da varinha; se houver parede entre o olho e a ponta, sai do olho.
		Vec3d tip = eye
				.add(right.multiply(side * type.tipRight))
				.add(up.multiply(type.tipUp))
				.add(forward.multiply(type.tipForward));
		if (world.raycast(new RaycastContext(eye, tip, RaycastContext.ShapeType.COLLIDER,
				RaycastContext.FluidHandling.NONE, player)).getType() != HitResult.Type.MISS) {
			tip = eye;
		}

		Vec3d direction = aimDirection(world, player, eye, forward, tip);

		WandPropEntity prop = new WandPropEntity(ModEntities.WAND_PROP, world);
		prop.setOwner(player);
		prop.setWandType(type);
		prop.setPosition(tip);
		prop.setVelocity(direction.multiply(WandryHP.CONFIG.projectileSpeed));
		world.spawnEntity(prop);

		player.swingHand(Hand.MAIN_HAND, true);
	}

	/**
	 * Direcao da ponta ate o ponto mirado (o que a mira esta apontando), para o prop acertar onde o
	 * jogador mira apesar de sair da ponta da varinha. Sem alvo, segue a direcao do olhar.
	 */
	private static Vec3d aimDirection(ServerWorld world, ServerPlayerEntity player, Vec3d eye, Vec3d forward, Vec3d tip) {
		double range = WandryHP.CONFIG.projectileSpeed * WandryHP.CONFIG.maxLifetimeTicks;
		Vec3d end = eye.add(forward.multiply(range));

		Vec3d aim = end;
		double bestSq = range * range;

		HitResult block = world.raycast(new RaycastContext(eye, end, RaycastContext.ShapeType.COLLIDER,
				RaycastContext.FluidHandling.NONE, player));
		if (block.getType() != HitResult.Type.MISS) {
			aim = block.getPos();
			bestSq = eye.squaredDistanceTo(aim);
		}

		Box box = player.getBoundingBox().stretch(forward.multiply(range)).expand(1.0);
		EntityHitResult entity = ProjectileUtil.raycast(player, eye, end, box,
				(Entity e) -> !e.isSpectator() && e.canHit(), bestSq);
		if (entity != null) {
			aim = entity.getPos();
		}

		Vec3d dir = aim.subtract(tip);
		if (dir.lengthSquared() < 1.0E-4) {
			return forward;
		}
		dir = dir.normalize();
		// Alvo muito proximo/atras da ponta geraria uma direcao estranha: nesse caso segue o olhar.
		return dir.dotProduct(forward) < 0.5 ? forward : dir;
	}
}
