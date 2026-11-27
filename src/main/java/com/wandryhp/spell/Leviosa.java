package com.wandryhp.spell;

import net.minecraft.entity.Entity;
import net.minecraft.entity.boss.dragon.EnderDragonPart;
import net.minecraft.network.packet.s2c.play.EntityVelocityUpdateS2CPacket;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.RaycastContext;

import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Wingardium Leviosa: a entidade atingida levita seguindo a posição da mira do autor.
 *
 * <p>Controle: a cada tique o alvo é conduzido (por velocidade, então respeita colisão) até o ponto da mira. Se a
 * mira aponta para um bloco, o alvo vai até ali (mais perto ou mais longe, conforme a superfície); olhando para o
 * céu ele mantém a última distância. A altura vem da inclinação da cabeça. Ao terminar, a gravidade volta e o
 * alvo fica protegido contra dano de queda até pousar (ver {@link #protects}).
 */
public final class Leviosa {
	/** Alcance máximo (blocos) do ponto da mira. */
	public static final double MAX_REACH = 20.0;
	private static final double FOLLOW = 0.4;
	private static final double MAX_SPEED = 1.5;
	/** Limite (ticks) da proteção contra queda depois de soltar, caso o alvo nunca pouse. */
	private static final int LANDING_TIMEOUT_TICKS = 600;

	private static final class Entry {
		final Entity target;
		final Entity owner;
		final boolean hadNoGravity;
		double holdDistance;
		int ticksLeft;

		Entry(Entity target, Entity owner, boolean hadNoGravity, double holdDistance, int ticksLeft) {
			this.target = target;
			this.owner = owner;
			this.hadNoGravity = hadNoGravity;
			this.holdDistance = holdDistance;
			this.ticksLeft = ticksLeft;
		}
	}

	private static final class Landing {
		final Entity target;
		int ticksLeft = LANDING_TIMEOUT_TICKS;

		Landing(Entity target) {
			this.target = target;
		}
	}

	private static final Map<UUID, Entry> ACTIVE = new LinkedHashMap<>();
	private static final Map<UUID, Landing> LANDING = new LinkedHashMap<>();

	private Leviosa() {}

	public static void start(ServerWorld world, Entity owner, Entity target, int durationTicks) {
		if (owner == null || target == owner || target instanceof EnderDragonPart || target.isRemoved()) {
			return;
		}
		Entry previous = ACTIVE.remove(target.getUuid());
		boolean hadNoGravity = previous != null ? previous.hadNoGravity : target.hasNoGravity();
		LANDING.remove(target.getUuid());
		target.stopRiding();
		double distance = MathHelper.clamp(owner.getEyePos().distanceTo(target.getBoundingBox().getCenter()), 3.0, MAX_REACH);
		target.setNoGravity(true);
		ACTIVE.put(target.getUuid(), new Entry(target, owner, hadNoGravity, distance, durationTicks));
	}

	/** Verdadeiro enquanto a entidade levita ou até pousar depois de soltar: o dano de queda é cancelado. */
	public static boolean protects(Entity entity) {
		return ACTIVE.containsKey(entity.getUuid()) || LANDING.containsKey(entity.getUuid());
	}

	public static void tick() {
		if (!ACTIVE.isEmpty()) {
			Iterator<Entry> it = ACTIVE.values().iterator();
			while (it.hasNext()) {
				Entry entry = it.next();
				Entity target = entry.target;
				Entity owner = entry.owner;
				if (target.isRemoved() || !target.isAlive() || owner.isRemoved() || !owner.isAlive()
						|| target.getWorld() != owner.getWorld() || !(target.getWorld() instanceof ServerWorld world)
						|| --entry.ticksLeft < 0) {
					release(entry);
					it.remove();
					continue;
				}
				follow(world, entry);
			}
		}
		if (!LANDING.isEmpty()) {
			Iterator<Landing> it = LANDING.values().iterator();
			while (it.hasNext()) {
				Landing landing = it.next();
				Entity target = landing.target;
				target.fallDistance = 0;
				if (target.isRemoved() || !target.isAlive() || --landing.ticksLeft < 0 || target.isOnGround()
						|| target.isTouchingWater() || target.isInLava()) {
					it.remove();
				}
			}
		}
	}

	private static void follow(ServerWorld world, Entry entry) {
		Entity target = entry.target;
		Entity owner = entry.owner;
		Vec3d eye = owner.getEyePos();
		Vec3d look = owner.getRotationVec(1.0f);
		Vec3d end = eye.add(look.multiply(MAX_REACH));
		BlockHitResult hit = world.raycast(new RaycastContext(eye, end, RaycastContext.ShapeType.COLLIDER,
				RaycastContext.FluidHandling.NONE, owner));
		if (hit.getType() != HitResult.Type.MISS) {
			double margin = 0.4 + target.getWidth() / 2.0;
			entry.holdDistance = Math.max(1.5, eye.distanceTo(hit.getPos()) - margin);
		}
		Vec3d aim = eye.add(look.multiply(entry.holdDistance));
		Vec3d desiredFeet = aim.subtract(0.0, target.getHeight() / 2.0, 0.0);

		Vec3d velocity = desiredFeet.subtract(target.getPos()).multiply(FOLLOW);
		double speed = velocity.length();
		if (speed > MAX_SPEED) {
			velocity = velocity.multiply(MAX_SPEED / speed);
		}
		target.setVelocity(velocity);
		target.velocityModified = true;
		target.fallDistance = 0;
		if (target instanceof ServerPlayerEntity player) {
			player.networkHandler.sendPacket(new EntityVelocityUpdateS2CPacket(player));
		}
		SpellTrails.spawn(world, Spell.WINGARDIUM_LEVIOSA, target.getBoundingBox().getCenter(),
				Math.max(0.25, target.getWidth() / 2.0), 1, 2);
	}

	/** Devolve a gravidade e deixa o alvo cair (protegido) até pousar. */
	private static void release(Entry entry) {
		Entity target = entry.target;
		if (!entry.hadNoGravity) {
			target.setNoGravity(false);
		}
		if (target.isRemoved() || !target.isAlive()) {
			return;
		}
		target.setVelocity(target.getVelocity().multiply(0.3));
		target.velocityModified = true;
		if (target instanceof ServerPlayerEntity player) {
			player.networkHandler.sendPacket(new EntityVelocityUpdateS2CPacket(player));
		}
		LANDING.put(target.getUuid(), new Landing(target));
	}

	/** Jogador saiu: solta tudo em que ele é alvo ou autor, antes de o jogo salvar os dados dele. */
	public static void forget(ServerPlayerEntity player) {
		Iterator<Entry> it = ACTIVE.values().iterator();
		while (it.hasNext()) {
			Entry entry = it.next();
			if (entry.target == player || entry.owner == player) {
				release(entry);
				it.remove();
			}
		}
		LANDING.remove(player.getUuid());
	}

	/** Servidor parando: devolve a gravidade de todos (o NoGravity seria salvo no mundo). */
	public static void releaseAll() {
		for (Entry entry : ACTIVE.values()) {
			if (!entry.hadNoGravity) {
				entry.target.setNoGravity(false);
			}
		}
		ACTIVE.clear();
		LANDING.clear();
	}

	public static void clear() {
		ACTIVE.clear();
		LANDING.clear();
	}
}
