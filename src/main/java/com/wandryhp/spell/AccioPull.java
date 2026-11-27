package com.wandryhp.spell;

import net.minecraft.entity.Entity;
import net.minecraft.entity.ItemEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;

import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Accio: puxa itens (ItemEntity) até o jogador. Cada item marcado voa em direção ao autor, com rastro roxo,
 * até chegar perto (a coleta normal do jogo faz o resto) ou até {@link #MAX_TICKS}.
 */
public final class AccioPull {
	private static final double SPEED = 0.55;
	private static final double ARRIVE_DISTANCE = 1.0;
	private static final int MAX_TICKS = 100;

	private static final class Entry {
		final ItemEntity item;
		final Entity owner;
		int ticks;

		Entry(ItemEntity item, Entity owner) {
			this.item = item;
			this.owner = owner;
		}
	}

	private static final Map<UUID, Entry> ACTIVE = new LinkedHashMap<>();

	private AccioPull() {}

	/** Marca os itens dentro de {@code radius} de {@code center} para serem puxados até {@code owner}. */
	public static void pullAround(ServerWorld world, Entity owner, Vec3d center, double radius) {
		if (owner == null || owner.isRemoved()) {
			return;
		}
		Box area = new Box(center, center).expand(radius);
		for (ItemEntity item : world.getEntitiesByClass(ItemEntity.class, area,
				e -> e.isAlive() && e.getPos().squaredDistanceTo(center) <= radius * radius)) {
			ACTIVE.put(item.getUuid(), new Entry(item, owner));
		}
	}

	/** Chamado uma vez por tique do servidor. */
	public static void tick() {
		if (ACTIVE.isEmpty()) {
			return;
		}
		Iterator<Entry> it = ACTIVE.values().iterator();
		while (it.hasNext()) {
			Entry entry = it.next();
			ItemEntity item = entry.item;
			Entity owner = entry.owner;
			if (item.isRemoved() || !item.isAlive() || owner.isRemoved() || !owner.isAlive()
					|| item.getWorld() != owner.getWorld() || !(item.getWorld() instanceof ServerWorld world)
					|| ++entry.ticks > MAX_TICKS) {
				it.remove();
				continue;
			}
			Vec3d target = owner.getPos().add(0.0, owner.getHeight() * 0.5, 0.0);
			Vec3d delta = target.subtract(item.getPos());
			double distance = delta.length();
			if (distance < ARRIVE_DISTANCE) {
				it.remove();
				continue;
			}
			double speed = Math.min(SPEED, 0.15 + distance * 0.1);
			Vec3d v = delta.multiply(speed / distance);
			item.setPickupDelay(0);
			// +0.04 em Y compensa a gravidade que o ItemEntity aplica antes de se mover.
			item.setVelocity(v.x, v.y + 0.04, v.z);
			item.velocityModified = true;
			SpellTrails.spawn(world, Spell.ACCIO, item.getPos().add(0.0, 0.2, 0.0), 0.05, 1, 1);
		}
	}

	public static void clear() {
		ACTIVE.clear();
	}
}
