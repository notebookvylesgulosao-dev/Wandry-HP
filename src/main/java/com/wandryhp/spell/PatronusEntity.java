package com.wandryhp.spell;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.data.DataTracker;
import net.minecraft.entity.data.TrackedData;
import net.minecraft.entity.data.TrackedDataHandlerRegistry;
import net.minecraft.entity.mob.Monster;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Patrono (Expecto Patronum). Fica atrás do jogador, acompanha os movimentos e ataca sozinho os mobs hostis
 * ({@link Monster}) num raio de {@link SpellEffects#PATRONUS_RADIUS} blocos em volta do jogador: até
 * {@link SpellEffects#PATRONUS_MAX_TARGETS} alvos por ataque, {@link SpellEffects#PATRONUS_DAMAGE} de dano cada.
 *
 * <p>Toda a lógica é do servidor (sem IA nem colisão, como a Edwiges). Só existe um Patrono por jogador: invocar de
 * novo troca o anterior. Some quando o tempo acaba ou o dono morre, sai do mundo ou muda de dimensão.
 *
 * <p>O cliente desenha o modelo ghost.bbmodel em {@code PatronusRenderer} (animação Idle em loop);
 * {@link #isAttacking()} fica verdadeiro por alguns ticks depois de cada ataque (o modelo não tem animação de ataque).
 */
public class PatronusEntity extends Entity {
	/**
	 * Distância (blocos) atrás do jogador e altura do pé do Patrono sobre o chão. O modelo já flutua sozinho
	 * (osso Controller da animação Idle sobe de 4 a 6 px), então aqui o pé fica no nível do jogador.
	 */
	private static final double BEHIND = 1.8;
	private static final double HOVER_HEIGHT = 0.0;
	/** Altura (blocos) do peito do Patrono: de onde saem as partículas e os feixes de ataque. */
	private static final double CHEST = 1.3;
	private static final double FOLLOW_FACTOR = 0.25;
	private static final double MAX_STEP = 1.5;
	private static final double TELEPORT_DISTANCE = 32.0;
	private static final int ATTACKING_TICKS = 10;

	private static final TrackedData<Boolean> ATTACKING =
			DataTracker.registerData(PatronusEntity.class, TrackedDataHandlerRegistry.BOOLEAN);

	private static final Map<UUID, PatronusEntity> ACTIVE = new HashMap<>();

	private UUID ownerId;
	private int life;
	private int attackCooldown;
	private int attackingTicks;
	/** Ticks desde que a entidade existe (nos dois lados): relógio da animação Idle do modelo. */
	private int animTicks;

	public PatronusEntity(EntityType<? extends PatronusEntity> type, World world) {
		super(type, world);
		this.noClip = true;
		this.setNoGravity(true);
	}

	@Override
	protected void initDataTracker(DataTracker.Builder builder) {
		builder.add(ATTACKING, false);
	}

	public boolean isAttacking() {
		return this.dataTracker.get(ATTACKING);
	}

	private void setAttacking(boolean attacking) {
		this.dataTracker.set(ATTACKING, attacking);
	}

	/** Relógio da animação (ticks, nos dois lados). */
	public int getAnimTicks() {
		return animTicks;
	}

	public UUID getOwnerId() {
		return ownerId;
	}

	// ---------------------------------------------------------------- invocação

	/** Invoca o Patrono atrás do jogador (e dispensa o anterior dele, se houver). */
	public static void summon(ServerPlayerEntity owner) {
		if (!(owner.getWorld() instanceof ServerWorld world)) {
			return;
		}
		PatronusEntity previous = ACTIVE.remove(owner.getUuid());
		if (previous != null && !previous.isRemoved()) {
			previous.discard();
		}
		PatronusEntity patronus = new PatronusEntity(ModSpellEntities.PATRONUS, world);
		patronus.ownerId = owner.getUuid();
		patronus.setPosition(anchor(owner, 0.0));
		patronus.setYaw(owner.getYaw());
		world.spawnEntity(patronus);
		ACTIVE.put(owner.getUuid(), patronus);
		SpellTrails.spawn(world, Spell.EXPECTO_PATRONUM, patronus.getPos().add(0.0, CHEST, 0.0), 0.4, 6, 10);
	}

	public static void forget(ServerPlayerEntity owner) {
		PatronusEntity patronus = ACTIVE.remove(owner.getUuid());
		if (patronus != null && !patronus.isRemoved()) {
			patronus.discard();
		}
	}

	public static void clear() {
		ACTIVE.clear();
	}

	/** Ponto de repouso do Patrono: atrás do jogador, pairando. */
	private static Vec3d anchor(PlayerEntity owner, double bob) {
		double yaw = Math.toRadians(owner.getYaw());
		Vec3d forward = new Vec3d(-Math.sin(yaw), 0.0, Math.cos(yaw));
		return owner.getPos().add(forward.multiply(-BEHIND)).add(0.0, HOVER_HEIGHT + bob, 0.0);
	}

	// ---------------------------------------------------------------- tique

	@Override
	public void tick() {
		super.tick();
		this.animTicks++;
		if (!(this.getWorld() instanceof ServerWorld world)) {
			return;
		}
		PlayerEntity owner = this.ownerId == null ? null : world.getPlayerByUuid(this.ownerId);
		if (owner == null || !owner.isAlive() || owner.isSpectator() || ++this.life > SpellEffects.PATRONUS_LIFETIME_TICKS) {
			SpellTrails.spawn(world, Spell.EXPECTO_PATRONUM, this.getPos().add(0.0, CHEST, 0.0), 0.4, 6, 10);
			this.discard();
			return;
		}
		follow(world, owner);
		attack(world, owner);
	}

	private void follow(ServerWorld world, PlayerEntity owner) {
		Vec3d anchor = anchor(owner, 0.0);
		Vec3d delta = anchor.subtract(this.getPos());
		Vec3d next;
		if (delta.length() > TELEPORT_DISTANCE) {
			next = anchor;
		} else {
			Vec3d step = delta.multiply(FOLLOW_FACTOR);
			double length = step.length();
			if (length > MAX_STEP) {
				step = step.multiply(MAX_STEP / length);
			}
			next = this.getPos().add(step);
		}
		boolean moved = next.squaredDistanceTo(this.getPos()) > 1.0E-4;
		this.setPosition(next);
		if (this.attackingTicks == 0) {
			this.setYaw(owner.getYaw());
		}
		this.setPitch(0.0f);
		if (moved) {
			SpellTrails.spawn(world, Spell.EXPECTO_PATRONUM, this.getPos().add(0.0, CHEST, 0.0), 0.2, 1, 1);
		}
	}

	private void attack(ServerWorld world, PlayerEntity owner) {
		if (this.attackingTicks > 0 && --this.attackingTicks == 0) {
			setAttacking(false);
		}
		if (this.attackCooldown > 0) {
			this.attackCooldown--;
			return;
		}
		double radius = SpellEffects.PATRONUS_RADIUS;
		List<LivingEntity> targets = world.getEntitiesByClass(LivingEntity.class, owner.getBoundingBox().expand(radius),
				e -> e instanceof Monster && e.isAlive() && !e.isSpectator() && e != owner
						&& e.squaredDistanceTo(owner) <= radius * radius);
		if (targets.isEmpty()) {
			return;
		}
		targets.sort(Comparator.comparingDouble(e -> e.squaredDistanceTo(owner)));

		Vec3d from = this.getPos().add(0.0, CHEST, 0.0);
		int count = Math.min(SpellEffects.PATRONUS_MAX_TARGETS, targets.size());
		for (int i = 0; i < count; i++) {
			LivingEntity target = targets.get(i);
			SpellTrails.line(world, Spell.EXPECTO_PATRONUM, from, target.getBoundingBox().getCenter());
			target.damage(world, world.getDamageSources().indirectMagic(this, owner), SpellEffects.PATRONUS_DAMAGE);
		}
		Vec3d toFirst = targets.get(0).getPos().subtract(this.getPos());
		this.setYaw((float) Math.toDegrees(Math.atan2(-toFirst.x, toFirst.z)));
		this.attackCooldown = SpellEffects.PATRONUS_ATTACK_INTERVAL_TICKS;
		this.attackingTicks = ATTACKING_TICKS;
		setAttacking(true);
	}

	// ---------------------------------------------------------------- Entity

	@Override
	public boolean damage(ServerWorld world, DamageSource source, float amount) {
		return false;
	}

	@Override
	public boolean isAttackable() {
		return false;
	}

	@Override
	public boolean isPushable() {
		return false;
	}

	@Override
	protected void readCustomDataFromNbt(NbtCompound nbt) {}

	@Override
	protected void writeCustomDataToNbt(NbtCompound nbt) {}
}
