package com.wandryhp.spell;

import net.minecraft.entity.Entity;
import net.minecraft.entity.boss.dragon.EnderDragonEntity;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.particle.DustParticleEffect;
import net.minecraft.particle.ItemStackParticleEffect;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.random.Random;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Vortem: não causa dano. Atrai todos os mobs num raio de {@link #RANGE} blocos, vindos de todas as direções,
 * direto para o centro (a posição do autor ao conjurar). Ao chegarem, os mobs sobem na vertical, ficam
 * suspensos e, quando o efeito termina, caem (sem dano de queda, ver {@link #protects}).
 *
 * <p>Visual: círculo horizontal de raio {@link #RING_RADIUS} com fragmentos de lã, concreto e corante e poeira de
 * energia girando muito rápido, e fragmentos/partículas orbitando cada mob enquanto ele sobe. Tudo em tons de
 * azul-bebê e turquesa (paleta do {@link Spell#VORTEM}), sem roxo.
 *
 * <p>Valores "assumidos" (o pedido não os definiu): duração total, altura da subida e velocidades.
 */
public final class Vortem {
	/** Definido: alcance de 15 blocos. */
	public static final double RANGE = 15.0;
	/** Definido: círculo de partículas com raio de 7 blocos. */
	public static final double RING_RADIUS = 7.0;

	/** Assumido: duração total do efeito (8 s), contada a partir do lançamento. */
	public static final int EFFECT_TICKS = 8 * 20;
	/** Assumido: tempo máximo de atração; quem não chegou ao centro (obstáculo) começa a subir mesmo assim. */
	private static final int PULL_MAX_TICKS = 50;
	/** Assumido: velocidade máxima de atração (blocos por tique). */
	private static final double PULL_SPEED = 0.8;
	/** Distância horizontal ao centro em que o mob conta como "chegou". */
	private static final double ARRIVE_DISTANCE = 1.2;
	/** Assumido: altura da subida (blocos) e tempo até chegar lá (ticks). */
	private static final double RISE_HEIGHT = 5.0;
	private static final int RISE_TICKS = 40;
	/** Limite (ticks) da proteção contra queda depois de soltar, caso o mob nunca pouse. */
	private static final int LANDING_TIMEOUT_TICKS = 600;

	// Visual do círculo
	private static final double RING_ADVANCE = 1.35; // radianos por tique (extremamente rápido: ~4 voltas/s)
	private static final int RING_ARMS = 4;
	private static final int RING_STEPS = 8;

	private static final double TWO_PI = Math.PI * 2.0;

	private static final class Held {
		final MobEntity mob;
		final boolean hadNoGravity;
		final double phase;
		boolean arrived;
		int pullTicks;
		int riseTicks;
		double baseY;

		Held(MobEntity mob, boolean hadNoGravity, double phase) {
			this.mob = mob;
			this.hadNoGravity = hadNoGravity;
			this.phase = phase;
		}
	}

	private static final class Session {
		final ServerWorld world;
		final Vec3d center;
		final List<Held> held = new ArrayList<>();
		final int duration;
		int age;

		Session(ServerWorld world, Vec3d center, int duration) {
			this.world = world;
			this.center = center;
			this.duration = duration;
		}
	}

	private static final class Landing {
		final MobEntity mob;
		int ticksLeft = LANDING_TIMEOUT_TICKS;

		Landing(MobEntity mob) {
			this.mob = mob;
		}
	}

	private static final List<Session> SESSIONS = new ArrayList<>();
	/** Mobs que já estão sob controle de um Vortem (evita dois feitiços disputando o mesmo mob). */
	private static final Set<UUID> CONTROLLED = new HashSet<>();
	private static final Map<UUID, Landing> LANDING = new HashMap<>();

	private Vortem() {}

	// ---------------------------------------------------------------- lançamento

	/** Chamado por {@link SpellCaster} no fim da animação de conjuração. */
	public static void cast(ServerPlayerEntity player) {
		if (!(player.getWorld() instanceof ServerWorld world)) {
			return;
		}
		Vec3d center = player.getPos();
		int duration = com.wandryhp.accessory.AccessoryEffects.scaleEffectDuration(player, EFFECT_TICKS);
		Session session = new Session(world, center, duration);

		Box area = new Box(center, center).expand(RANGE);
		for (MobEntity mob : world.getEntitiesByClass(MobEntity.class, area,
				e -> e.isAlive() && !e.isRemoved() && !(e instanceof EnderDragonEntity))) {
			if (mob.getBoundingBox().getCenter().distanceTo(center) > RANGE) {
				continue;
			}
			if (CONTROLLED.contains(mob.getUuid()) || Leviosa.protects(mob)) {
				continue;
			}
			mob.stopRiding();
			boolean hadNoGravity = mob.hasNoGravity();
			mob.setNoGravity(true);
			LANDING.remove(mob.getUuid());
			CONTROLLED.add(mob.getUuid());
			session.held.add(new Held(mob, hadNoGravity, world.getRandom().nextDouble() * TWO_PI));
		}
		SESSIONS.add(session);

		world.playSound(null, center.x, center.y, center.z, SoundEvents.BLOCK_BEACON_ACTIVATE,
				SoundCategory.PLAYERS, 1.6f, 1.5f);
		castBurst(world, center);
	}

	/** Explosão de partículas expandindo até a borda do círculo, no instante do lançamento. */
	private static void castBurst(ServerWorld world, Vec3d center) {
		Random random = world.getRandom();
		for (int i = 0; i < 160; i++) {
			double angle = random.nextDouble() * TWO_PI;
			double dist = random.nextDouble() * RING_RADIUS;
			Vec3d pos = new Vec3d(center.x + Math.cos(angle) * dist, center.y + 0.2 + random.nextDouble() * 0.8,
					center.z + Math.sin(angle) * dist);
			SpellTrails.spawn(world, Spell.VORTEM, pos, 0.15, 1, 2);
		}
	}

	// ---------------------------------------------------------------- tique

	/** Chamado uma vez por tique do servidor. */
	public static void tick() {
		if (!SESSIONS.isEmpty()) {
			Iterator<Session> it = SESSIONS.iterator();
			while (it.hasNext()) {
				Session session = it.next();
				session.age++;
				if (session.age > session.duration) {
					releaseAll(session);
					it.remove();
					continue;
				}
				tickSession(session);
			}
		}
		if (!LANDING.isEmpty()) {
			Iterator<Landing> it = LANDING.values().iterator();
			while (it.hasNext()) {
				Landing landing = it.next();
				MobEntity mob = landing.mob;
				mob.fallDistance = 0;
				if (mob.isRemoved() || !mob.isAlive() || --landing.ticksLeft < 0 || mob.isOnGround()
						|| mob.isTouchingWater() || mob.isInLava()) {
					it.remove();
				}
			}
		}
	}

	private static void tickSession(Session session) {
		ServerWorld world = session.world;
		spawnRing(world, session.center, session.age);

		Iterator<Held> it = session.held.iterator();
		int index = 0;
		int visualStride = session.held.size() > 20 ? 2 : 1; // muitos mobs: metade deles por tique
		while (it.hasNext()) {
			Held held = it.next();
			MobEntity mob = held.mob;
			if (mob.isRemoved() || !mob.isAlive() || mob.getWorld() != world) {
				CONTROLLED.remove(mob.getUuid());
				if (!mob.isRemoved() && !held.hadNoGravity) {
					mob.setNoGravity(false);
				}
				it.remove();
				continue;
			}
			step(session, held);
			if (held.arrived && (index + session.age) % visualStride == 0) {
				spawnOrbit(world, mob, session.age, held.phase, session.held.size() > 20 ? 3 : 6);
			}
			index++;
		}
	}

	/** Atrai (direto ao centro), faz subir na vertical e mantém suspenso. */
	private static void step(Session session, Held held) {
		MobEntity mob = held.mob;
		Vec3d center = session.center;
		Vec3d pos = mob.getPos();
		double vx;
		double vy;
		double vz;

		if (!held.arrived) {
			held.pullTicks++;
			Vec3d delta = center.subtract(pos);
			double horizontal = Math.hypot(delta.x, delta.z);
			if (horizontal < ARRIVE_DISTANCE || held.pullTicks > PULL_MAX_TICKS) {
				held.arrived = true;
				held.baseY = pos.y;
				held.riseTicks = 0;
				vx = 0;
				vy = 0;
				vz = 0;
			} else {
				double dist = delta.length();
				double speed = Math.min(PULL_SPEED, 0.15 + dist * 0.2);
				Vec3d v = delta.multiply(speed / dist);
				vx = v.x;
				vy = v.y;
				vz = v.z;
			}
		} else {
			held.riseTicks++;
			double t = Math.min(1.0, (double) held.riseTicks / RISE_TICKS);
			double eased = t * t * (3.0 - 2.0 * t); // sobe suave, sem arrancada
			double targetY = held.baseY + RISE_HEIGHT * eased;
			if (held.riseTicks > RISE_TICKS) {
				targetY += Math.sin(session.age * 0.15 + held.phase) * 0.12; // leve flutuação enquanto suspenso
			}
			vy = MathHelper.clamp((targetY - pos.y) * 0.3, -0.5, 0.5);
			// Mantém a coluna central; quem ficou preso longe vai se juntando devagar.
			vx = MathHelper.clamp((center.x - pos.x) * 0.15, -0.3, 0.3);
			vz = MathHelper.clamp((center.z - pos.z) * 0.15, -0.3, 0.3);
		}

		mob.setVelocity(vx, vy, vz);
		mob.velocityModified = true;
		mob.fallDistance = 0;
		mob.getNavigation().stop();
	}

	// ---------------------------------------------------------------- fim

	private static void releaseAll(Session session) {
		ServerWorld world = session.world;
		for (Held held : session.held) {
			release(world, held);
		}
		session.held.clear();
		world.playSound(null, session.center.x, session.center.y, session.center.z,
				SoundEvents.BLOCK_BEACON_DEACTIVATE, SoundCategory.PLAYERS, 1.2f, 1.6f);
	}

	/** Devolve a gravidade e deixa o mob cair, protegido contra dano de queda até pousar. */
	private static void release(ServerWorld world, Held held) {
		MobEntity mob = held.mob;
		CONTROLLED.remove(mob.getUuid());
		if (mob.isRemoved()) {
			return;
		}
		if (!held.hadNoGravity) {
			mob.setNoGravity(false);
		}
		if (!mob.isAlive()) {
			return;
		}
		mob.setVelocity(0.0, 0.0, 0.0);
		mob.velocityModified = true;
		LANDING.put(mob.getUuid(), new Landing(mob));
		Vec3d c = mob.getBoundingBox().getCenter();
		SpellTrails.spawn(world, Spell.VORTEM, c, Math.max(0.3, mob.getWidth() / 2.0), 6, 8);
	}

	/** Verdadeiro enquanto o mob está sob o Vortem ou até pousar depois de solto: o dano de queda é cancelado. */
	public static boolean protects(Entity entity) {
		return CONTROLLED.contains(entity.getUuid()) || LANDING.containsKey(entity.getUuid());
	}

	/** Servidor parando: devolve a gravidade de todos (o NoGravity seria salvo no mundo). */
	public static void releaseAllNow() {
		for (Session session : SESSIONS) {
			for (Held held : session.held) {
				if (!held.hadNoGravity && !held.mob.isRemoved()) {
					held.mob.setNoGravity(false);
				}
			}
		}
		clear();
	}

	public static void clear() {
		SESSIONS.clear();
		CONTROLLED.clear();
		LANDING.clear();
	}

	// ---------------------------------------------------------------- visual

	private static int dust(Random random) {
		int[] colors = Spell.VORTEM.trailColors();
		return colors[random.nextInt(colors.length)];
	}

	private static Item fragment(Random random) {
		Item[] items = Spell.VORTEM.trailItems();
		return items[random.nextInt(items.length)];
	}

	private static void fragmentAt(ServerWorld world, Random random, double x, double y, double z, double spread) {
		world.spawnParticles(new ItemStackParticleEffect(ParticleTypes.ITEM, new ItemStack(fragment(random))),
				x, y, z, 1, spread, spread, spread, 0.03);
	}

	private static void dustAt(ServerWorld world, Random random, double x, double y, double z, double spread) {
		world.spawnParticles(new DustParticleEffect(dust(random), 0.8f + random.nextFloat() * 0.7f),
				x, y, z, 1, spread, spread, spread, 0.0);
	}

	/** Energia: chama de almas (azul-turquesa) subindo devagar. */
	private static void energyAt(ServerWorld world, double x, double y, double z) {
		world.spawnParticles(ParticleTypes.SOUL_FIRE_FLAME, x, y, z, 0, 0.0, 1.0, 0.0, 0.04);
	}

	/**
	 * Círculo horizontal de raio {@link #RING_RADIUS}: vários braços que avançam {@link #RING_ADVANCE} rad por tique.
	 * Cada braço é varrido em passos pequenos para o arco ficar contínuo mesmo girando tão rápido.
	 */
	private static void spawnRing(ServerWorld world, Vec3d center, int age) {
		Random random = world.getRandom();
		double baseY = center.y + 0.15;
		for (int arm = 0; arm < RING_ARMS; arm++) {
			double armBase = age * RING_ADVANCE + arm * (TWO_PI / RING_ARMS);
			for (int step = 0; step < RING_STEPS; step++) {
				double angle = armBase - RING_ADVANCE * step / RING_STEPS;
				double radius = RING_RADIUS + (random.nextDouble() - 0.5) * 0.6;
				double x = center.x + Math.cos(angle) * radius;
				double z = center.z + Math.sin(angle) * radius;
				double y = baseY + random.nextDouble() * 0.5;
				fragmentAt(world, random, x, y, z, 0.08);
				dustAt(world, random, x, y, z, 0.1);
				if (step % 2 == 0) {
					energyAt(world, x, y, z);
				}
			}
		}
		// Faixa interna de energia e poeira, girando no sentido contrário.
		for (int i = 0; i < 12; i++) {
			double angle = -age * RING_ADVANCE * 0.8 + i * (TWO_PI / 12.0);
			double radius = RING_RADIUS - 0.8 - random.nextDouble() * 0.8;
			double x = center.x + Math.cos(angle) * radius;
			double z = center.z + Math.sin(angle) * radius;
			double y = baseY + random.nextDouble() * 0.4;
			dustAt(world, random, x, y, z, 0.05);
			if (i % 3 == 0) {
				energyAt(world, x, y, z);
			}
		}
	}

	/** Fragmentos e partículas girando rápido em volta do mob, seguindo a posição atual dele. */
	private static void spawnOrbit(ServerWorld world, MobEntity mob, int age, double phase, int samples) {
		Random random = world.getRandom();
		Vec3d c = mob.getBoundingBox().getCenter();
		double radius = mob.getWidth() / 2.0 + 0.5;
		double height = Math.max(0.6, mob.getHeight());
		for (int i = 0; i < samples; i++) {
			int ring = i % 3;
			double angle = phase + age * 1.5 + i * (TWO_PI / samples) + ring * 2.1;
			double y = c.y + (ring - 1) * height * 0.35 + (random.nextDouble() - 0.5) * 0.2;
			double x = c.x + Math.cos(angle) * radius;
			double z = c.z + Math.sin(angle) * radius;
			fragmentAt(world, random, x, y, z, 0.05);
			dustAt(world, random, x, y, z, 0.08);
			if (i % 2 == 0) {
				energyAt(world, x, y, z);
			}
		}
	}
}
