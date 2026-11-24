package com.wandryhp.spell;

import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.server.world.ServerWorld;

import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Dano contínuo do Crucio. Aplica um pequeno dano em intervalos fixos durante a duração do feitiço.
 * Usa dano mágico sem atacante para não empurrar (knockback) o alvo a cada tique de dano.
 */
public final class DamageOverTime {
	private static final Map<UUID, Entry> ACTIVE = new LinkedHashMap<>();

	private DamageOverTime() {}

	private static final class Entry {
		final LivingEntity target;
		final int duration;
		final Entity owner;
		int elapsed;

		Entry(LivingEntity target, int duration, Entity owner) {
			this.target = target;
			this.duration = duration;
			this.owner = owner;
		}
	}

	/** Inicia (ou reinicia) o dano contínuo no alvo. */
	public static void start(LivingEntity target, int durationTicks) {
		start(target, durationTicks, null);
	}

	/** Igual a {@link #start(LivingEntity, int)}, guardando quem lancou o feitiço (para creditar abates). */
	public static void start(LivingEntity target, int durationTicks, Entity owner) {
		ACTIVE.put(target.getUuid(), new Entry(target, durationTicks, owner));
	}

	/** Quem lancou o dano continuo ativo neste alvo, ou null. O dano segue sem atacante (sem knockback). */
	public static Entity ownerOf(LivingEntity target) {
		Entry entry = ACTIVE.get(target.getUuid());
		return entry == null ? null : entry.owner;
	}

	/** Chamado uma vez por tique do servidor. */
	public static void tick() {
		if (ACTIVE.isEmpty()) {
			return;
		}
		Iterator<Entry> it = ACTIVE.values().iterator();
		while (it.hasNext()) {
			Entry entry = it.next();
			LivingEntity target = entry.target;
			if (!target.isAlive() || target.isRemoved() || !(target.getWorld() instanceof ServerWorld world)) {
				it.remove();
				continue;
			}
			entry.elapsed++;
			if (entry.elapsed % SpellEffects.CRUCIO_DAMAGE_INTERVAL_TICKS == 0) {
				DamageSource source = world.getDamageSources().magic();
				target.damage(world, source, SpellEffects.CRUCIO_DAMAGE_PER_HIT);
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
