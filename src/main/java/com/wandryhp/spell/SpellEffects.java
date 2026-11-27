package com.wandryhp.spell;

import com.wandryhp.effect.ModEffects;
import net.minecraft.block.AbstractFireBlock;
import net.minecraft.block.BlockState;
import net.minecraft.entity.Entity;
import net.minecraft.entity.FallingBlockEntity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.network.packet.s2c.play.EntityVelocityUpdateS2CPacket;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

import java.util.ArrayList;
import java.util.List;

/**
 * O que cada feitiço faz ao atingir um alvo (entidade) ou um bloco. Roda somente no servidor.
 *
 * <p>Valores marcados como "definido" vieram do pedido; os marcados como "assumido" não foram
 * especificados e estão aqui, juntos, para ajuste fácil.
 */
public final class SpellEffects {
	private static final float HEART = 2.0f;

	/** Definido: 20 corações. */
	public static final float AVADA_KEDAVRA_DAMAGE = 20 * HEART;
	/** Definido: 4 corações. */
	public static final float SECTUMSEMPRA_DAMAGE = 4 * HEART;
	/** Definido: explosão de poder 3. */
	public static final float CORINFICUS_EXPLOSION_POWER = 3.0f;

	/** Definido: dano contínuo por 3 s. */
	public static final int CRUCIO_DAMAGE_TICKS = 3 * 20;
	/** Definido: Lentidão por 2 s. */
	public static final int CRUCIO_SLOWNESS_TICKS = 2 * 20;
	/** Definido: Náusea por 3 s. */
	public static final int CRUCIO_NAUSEA_TICKS = 3 * 20;
	/** Assumido: o pedido não define quanto dano por tique; meio coração a cada 0,5 s (3 corações em 3 s). */
	public static final int CRUCIO_DAMAGE_INTERVAL_TICKS = 10;
	public static final float CRUCIO_DAMAGE_PER_HIT = 1.0f;

	/** Definido: Lentidão 255 por 3 s (nível 255 = amplificador 254; o movimento zera de qualquer forma). */
	public static final int PETRIFICUS_SLOWNESS_AMPLIFIER = 254;
	public static final int PETRIFICUS_TICKS = 3 * 20;

	/** Assumido: o pedido não define a duração da Confusão do Imperius (5 s). */
	public static final int IMPERIUS_CONFUSION_TICKS = 5 * 20;

	/** Definido: alcance de 8 blocos, contado a partir do ponto de impacto. */
	public static final double EXPELLIARMUS_RADIUS = 8.0;

	/** Definido: Wingardium Leviosa levita a entidade por 4 s. */
	public static final int LEVIOSA_TICKS = 4 * 20;

	/** Definido: Incendio põe fogo por 8 s; o dano total da queimadura não passa de 3 corações. */
	public static final int INCENDIO_FIRE_TICKS = 8 * 20;
	public static final float INCENDIO_MAX_DAMAGE = 3 * HEART;

	/** Definido: Patrono, 2 corações por ataque, até 3 alvos ao mesmo tempo, raio de 5 blocos. */
	public static final float PATRONUS_DAMAGE = 2 * HEART;
	public static final int PATRONUS_MAX_TARGETS = 3;
	public static final double PATRONUS_RADIUS = 5.0;
	/** Assumido: o pedido não define quanto o Patrono dura (45 s) nem a frequência dos ataques (1 por segundo). */
	public static final int PATRONUS_LIFETIME_TICKS = 45 * 20;
	public static final int PATRONUS_ATTACK_INTERVAL_TICKS = 20;

	/** Assumido: o pedido não define o alcance do Accio: 5 blocos em volta do impacto e 2 blocos em volta do projétil em voo. */
	public static final double ACCIO_IMPACT_RADIUS = 5.0;
	public static final double ACCIO_SWEEP_RADIUS = 2.0;

	/** Assumido: nível de luz do Lumos (14, o mesmo da tocha). */
	public static final int LUMOS_LIGHT_LEVEL = 14;

	private SpellEffects() {}

	/** Duracao do efeito com o bonus da capa Enchanter Azul do autor do feitico (+20%). */
	private static int dur(Entity owner, int ticks) {
		return com.wandryhp.accessory.AccessoryEffects.scaleEffectDuration(owner, ticks);
	}

	/**
	 * @param target entidade atingida, ou null se o projétil bateu em um bloco
	 * @param pos    ponto de impacto
	 */
	public static void impact(ServerWorld world, SpellProjectileEntity projectile, Spell spell, Entity owner, Entity target, Vec3d pos) {
		switch (spell) {
			case AVADA_KEDAVRA -> hurt(world, projectile, owner, target, AVADA_KEDAVRA_DAMAGE);
			case SECTUMSEMPRA -> hurt(world, projectile, owner, target, SECTUMSEMPRA_DAMAGE);
			case CORINFICUS -> world.createExplosion(projectile, pos.x, pos.y, pos.z,
					CORINFICUS_EXPLOSION_POWER, World.ExplosionSourceType.MOB);
			case CRUCIO -> {
				if (target instanceof LivingEntity living) {
					living.addStatusEffect(new StatusEffectInstance(StatusEffects.SLOWNESS, dur(owner, CRUCIO_SLOWNESS_TICKS), 0), owner);
					living.addStatusEffect(new StatusEffectInstance(StatusEffects.NAUSEA, dur(owner, CRUCIO_NAUSEA_TICKS), 0), owner);
					DamageOverTime.start(living, dur(owner, CRUCIO_DAMAGE_TICKS), owner);
				}
			}
			case PETRIFICUS_TOTALIS -> {
				if (target instanceof LivingEntity living) {
					living.addStatusEffect(new StatusEffectInstance(StatusEffects.SLOWNESS, dur(owner, PETRIFICUS_TICKS),
							PETRIFICUS_SLOWNESS_AMPLIFIER), owner);
				}
			}
			case IMPERIUS -> {
				// Reaproveita o efeito de Confusão já pronto (inversão feita no cliente via KeyBindingMixin).
				if (target instanceof LivingEntity living) {
					living.addStatusEffect(new StatusEffectInstance(ModEffects.CONFUSION, dur(owner, IMPERIUS_CONFUSION_TICKS), 0), owner);
				}
			}
			case EXPELLIARMUS -> expel(world, projectile, owner, pos);
			case ACCIO -> AccioPull.pullAround(world, owner, pos, ACCIO_IMPACT_RADIUS);
			case WINGARDIUM_LEVIOSA -> {
				if (target != null) {
					Leviosa.start(world, owner, target, dur(owner, LEVIOSA_TICKS));
				}
			}
			case INCENDIO -> {
				if (target instanceof LivingEntity living) {
					IncendioBurn.start(living, owner, dur(owner, INCENDIO_FIRE_TICKS));
				} else if (target == null) {
					ignite(world, projectile, pos);
				}
			}
			// Lumos, Expecto Patronum e Vortem não têm projétil: rodam direto em SpellCaster.
			case LUMOS, EXPECTO_PATRONUM, VORTEM -> { }
		}
	}

	// ---------------------------------------------------------------- Incendio

	/**
	 * Incendeia o chão no ponto de impacto: o bloco de ar logo antes da superfície atingida e os vizinhos em cruz,
	 * cada um descendo até 3 blocos para achar chão onde o fogo pode existir (fogo comum ou de almas).
	 */
	private static void ignite(ServerWorld world, SpellProjectileEntity projectile, Vec3d impact) {
		Vec3d travel = projectile.getVelocity();
		Vec3d back = travel.lengthSquared() > 1.0E-6 ? impact.subtract(travel.normalize().multiply(0.1)) : impact;
		BlockPos origin = BlockPos.ofFloored(back);
		BlockPos[] columns = {origin, origin.north(), origin.south(), origin.east(), origin.west()};
		for (BlockPos column : columns) {
			for (int dy = 0; dy >= -3; dy--) {
				BlockPos pos = column.add(0, dy, 0);
				if (!world.getBlockState(pos).isAir()) {
					break; // bateu em algo sólido ou líquido sem achar onde colocar fogo
				}
				BlockState fire = AbstractFireBlock.getState(world, pos);
				if (fire.canPlaceAt(world, pos)) {
					world.setBlockState(pos, fire);
					break;
				}
			}
		}
	}

	private static void hurt(ServerWorld world, SpellProjectileEntity projectile, Entity owner, Entity target, float amount) {
		if (target != null) {
			target.damage(world, world.getDamageSources().indirectMagic(projectile, owner), amount);
		}
	}

	// ---------------------------------------------------------------- Expelliarmus

	/**
	 * Arremessa para longe do ponto de impacto, dentro do raio, jogadores e mobs (menos o autor) e
	 * transforma os blocos expostos em blocos físicos (FallingBlockEntity) também arremessados.
	 * Quanto mais perto do impacto, mais forte o arremesso.
	 */
	private static void expel(ServerWorld world, SpellProjectileEntity projectile, Entity owner, Vec3d impact) {
		double radius = EXPELLIARMUS_RADIUS;
		Vec3d travel = projectile.getVelocity();
		Vec3d fallback = travel.lengthSquared() > 1.0E-6 ? travel.normalize() : new Vec3d(0, 0, 1);

		// Jogadores e mobs: ~8 blocos de distância no ponto mais próximo, menos longe do impacto.
		Box area = new Box(impact, impact).expand(radius);
		for (LivingEntity entity : world.getEntitiesByClass(LivingEntity.class, area,
				e -> e.isAlive() && !e.isSpectator() && e != owner)) {
			Vec3d center = entity.getBoundingBox().getCenter();
			double dist = center.distanceTo(impact);
			if (dist > radius) {
				continue;
			}
			Vec3d flat = horizontal(center.subtract(impact), fallback);
			double strength = 0.35 + 0.45 * (1.0 - dist / radius);
			launch(entity, new Vec3d(flat.x * strength, 0.45, flat.z * strength));
		}

		// Blocos físicos: somente os expostos ao ar (a "casca"), para não transformar o terreno todo.
		BlockPos origin = BlockPos.ofFloored(impact);
		int r = (int) Math.ceil(radius);
		List<BlockPos> blocks = new ArrayList<>();
		for (BlockPos pos : BlockPos.iterate(origin.add(-r, -r, -r), origin.add(r, r, r))) {
			if (Vec3d.ofCenter(pos).squaredDistanceTo(impact) > radius * radius) {
				continue;
			}
			BlockState state = world.getBlockState(pos);
			if (state.isAir() || state.hasBlockEntity() || !state.getFluidState().isEmpty()
					|| state.getHardness(world, pos) < 0 || !state.isFullCube(world, pos) || !exposed(world, pos)) {
				continue;
			}
			blocks.add(pos.toImmutable());
		}
		for (BlockPos pos : blocks) {
			BlockState state = world.getBlockState(pos);
			if (state.isAir()) {
				continue;
			}
			Vec3d center = Vec3d.ofCenter(pos);
			double dist = center.distanceTo(impact);
			Vec3d flat = horizontal(center.subtract(impact), fallback);
			double strength = 0.2 + 0.25 * (1.0 - dist / radius);
			FallingBlockEntity block = FallingBlockEntity.spawnFromBlock(world, pos, state);
			block.setVelocity(flat.x * strength, 0.4, flat.z * strength);
			block.velocityModified = true;
		}
	}

	private static boolean exposed(ServerWorld world, BlockPos pos) {
		for (Direction direction : Direction.values()) {
			if (world.getBlockState(pos.offset(direction)).isAir()) {
				return true;
			}
		}
		return false;
	}

	/** Direção horizontal normalizada de {@code v}; se for quase vertical, usa a de {@code fallback}. */
	private static Vec3d horizontal(Vec3d v, Vec3d fallback) {
		Vec3d flat = new Vec3d(v.x, 0, v.z);
		if (flat.lengthSquared() > 1.0E-4) {
			return flat.normalize();
		}
		flat = new Vec3d(fallback.x, 0, fallback.z);
		return flat.lengthSquared() > 1.0E-4 ? flat.normalize() : Vec3d.ZERO;
	}

	private static void launch(LivingEntity entity, Vec3d velocity) {
		entity.setVelocity(velocity);
		entity.velocityModified = true;
		if (entity instanceof ServerPlayerEntity player) {
			player.networkHandler.sendPacket(new EntityVelocityUpdateS2CPacket(player));
		}
	}
}
