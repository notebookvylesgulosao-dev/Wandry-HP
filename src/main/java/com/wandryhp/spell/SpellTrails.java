package com.wandryhp.spell;

import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.particle.DustParticleEffect;
import net.minecraft.particle.ItemStackParticleEffect;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.random.Random;

/**
 * Rastro de partículas nas cores de um feitiço (itens de lã/concreto/corante + poeira colorida), o mesmo visual
 * do rastro dos projéteis, para os feitiços que não são só um projétil em linha reta (Accio, Wingardium Leviosa,
 * Lumos, Patrono). Somente visual.
 */
public final class SpellTrails {
	private SpellTrails() {}

	/** Solta {@code items} partículas de item e {@code dust} de poeira em volta de {@code pos}. */
	public static void spawn(ServerWorld world, Spell spell, Vec3d pos, double spread, int items, int dust) {
		Item[] trailItems = spell.trailItems();
		int[] colors = spell.trailColors();
		Random random = world.getRandom();
		for (int i = 0; i < items; i++) {
			Item item = trailItems[random.nextInt(trailItems.length)];
			world.spawnParticles(new ItemStackParticleEffect(ParticleTypes.ITEM, new ItemStack(item)),
					pos.x, pos.y, pos.z, 1, spread, spread, spread, 0.02);
		}
		for (int i = 0; i < dust; i++) {
			int color = colors[random.nextInt(colors.length)];
			world.spawnParticles(new DustParticleEffect(color, 1.0f),
					pos.x, pos.y, pos.z, 1, spread, spread, spread, 0.0);
		}
	}

	/** Linha de partículas de {@code from} até {@code to}. */
	public static void line(ServerWorld world, Spell spell, Vec3d from, Vec3d to) {
		int steps = Math.max(1, Math.min(24, (int) Math.ceil(from.distanceTo(to) / 0.5)));
		for (int i = 0; i <= steps; i++) {
			spawn(world, spell, from.lerp(to, (double) i / steps), 0.04, 1, 1);
		}
	}
}
