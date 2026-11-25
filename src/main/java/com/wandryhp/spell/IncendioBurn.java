package com.wandryhp.spell;

import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.server.world.ServerWorld;

import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Queimadura do Incendio: o alvo pega fogo pela duração do feitiço, mas o dano total é limitado a
 * {@link SpellEffects#INCENDIO_MAX_DAMAGE} (3 corações). Para isso o dano de fogo comum é cancelado enquanto a
 * queimadura está ativa (ver SpellsInit) e o feitiço aplica o seu, dividido igualmente em um golpe por segundo.
 * Dano mágico sem atacante, como o Crucio: não empurra o alvo; o autor segue creditado por {@link #ownerOf}.
 */
public final class IncendioBurn {
	private static final class Entry {
		final LivingEntity target;
		final Entity owner;
		final int duration;
		final float perHit;
		int elapsed;

		Entry(LivingEntity target, Entity owner, int duration, float perHit) {
			this.target = target;
			this.owner = owner;
			this.duration = duration;
			this.perHit = perHit;
		}
	}

	private static final Map<UUID, Entry> ACTIVE = new LinkedHashMap<>();

	private IncendioBurn() {}

	public static void start(LivingEntity target, Entity owner, int durationTicks) {
		if (target.isFireImmune() || target.hasStatusEffect(StatusEffects.FIRE_RESISTANCE)) {
			return;
		}
		target.setFireTicks(Math.max(target.getFireTicks(), durationTicks));
		float perHit = SpellEffects.INCENDIO_MAX_DAMAGE / (durationTicks / 20.0f);
		ACTIVE.put(target.getUuid(), new Entry(target, owner, durationTicks, perHit));
	}

	public static boolean isBurning(Entity entity) {
		return ACTIVE.containsKey(entity.getUuid());
	}

	public static Entity ownerOf(LivingEntity target) {
		Entry entry = ACTIVE.get(target.getUuid());
		return entry == null ? null : entry.owner;
	}

	public static void tick() {
		if (ACTIVE.isEmpty()) {
			return;
		}
		Iterator<Entry> it = ACTIVE.values().iterator();
		while (it.hasNext()) {
			Entry entry = it.next();
			LivingEntity target = entry.target;
			if (!target.isAlive() || target.isRemoved() || !(target.getWorld() instanceof ServerWorld world)
					|| !target.isOnFire()) {
				it.remove(); // morreu, saiu do mundo ou foi apagado (água etc.)
				continue;
			}
			entry.elapsed++;
			if (entry.elapsed % 20 == 0 && !target.isFireImmune()
					&& !target.hasStatusEffect(StatusEffects.FIRE_RESISTANCE)) {
				target.damage(world, world.getDamageSources().magic(), entry.perHit);
			}
			if (entry.elapsed >= entry.duration) {
				it.remove();
			}
		}
	}

	public static void clear() {
		ACTIVE.clear();
	}
}
