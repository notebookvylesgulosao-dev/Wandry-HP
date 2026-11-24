package com.wandryhp.spell;

import com.wandryhp.WandryHP;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.data.DataTracker;
import net.minecraft.entity.data.TrackedData;
import net.minecraft.entity.data.TrackedDataHandlerRegistry;
import net.minecraft.entity.projectile.ProjectileEntity;
import net.minecraft.entity.projectile.ProjectileUtil;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.particle.DustParticleEffect;
import net.minecraft.particle.ItemStackParticleEffect;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.World;

/**
 * Projétil de feitiço. Voa em linha reta (sem gravidade), deixa o rastro visual (partículas de item de
 * lã/concreto/corante alternados + poeira colorida) e aplica o efeito do feitiço ao atingir algo.
 * Velocidade e tempo de vida vêm da mesma config dos props base.
 */
public class SpellProjectileEntity extends ProjectileEntity {
	private static final TrackedData<Integer> SPELL =
			DataTracker.registerData(SpellProjectileEntity.class, TrackedDataHandlerRegistry.INTEGER);

	/** Contador para alternar os itens do rastro (só usado no servidor). */
	private int trailCounter;

	public SpellProjectileEntity(EntityType<? extends SpellProjectileEntity> type, World world) {
		super(type, world);
	}

	@Override
	protected void initDataTracker(DataTracker.Builder builder) {
		builder.add(SPELL, 0);
	}

	public Spell getSpell() {
		Spell spell = Spell.byOrdinal(this.dataTracker.get(SPELL));
		return spell != null ? spell : Spell.AVADA_KEDAVRA;
	}

	public void setSpell(Spell spell) {
		this.dataTracker.set(SPELL, spell.ordinal());
	}

	@Override
	public void tick() {
		super.tick();
		World world = this.getWorld();
		Vec3d velocity = this.getVelocity();

		if (!world.isClient) {
			if (this.age > WandryHP.CONFIG.maxLifetimeTicks) {
				this.discard();
				return;
			}
			HitResult hit = ProjectileUtil.getCollision(this, this::canHit);
			if (hit.getType() != HitResult.Type.MISS) {
				this.onCollision(hit);
				if (this.isRemoved()) {
					return;
				}
			}
		}

		Vec3d from = this.getPos();
		this.setPosition(from.add(velocity));

		if (world instanceof ServerWorld serverWorld) {
			spawnTrail(serverWorld, from, this.getPos());
		}
	}

	@Override
	protected void onEntityHit(EntityHitResult hit) {
		super.onEntityHit(hit);
		if (this.getWorld() instanceof ServerWorld world) {
			SpellEffects.impact(world, this, getSpell(), this.getOwner(), hit.getEntity(), hit.getPos());
		}
	}

	@Override
	protected void onBlockHit(BlockHitResult hit) {
		super.onBlockHit(hit);
		if (this.getWorld() instanceof ServerWorld world) {
			SpellEffects.impact(world, this, getSpell(), this.getOwner(), null, hit.getPos());
		}
	}

	@Override
	protected void onCollision(HitResult hit) {
		super.onCollision(hit);
		if (!this.getWorld().isClient) {
			this.discard();
		}
	}

	/** Rastro: partículas de item (lã/concreto/corante, alternando) + poeira, nas cores do feitiço. Somente visual. */
	private void spawnTrail(ServerWorld world, Vec3d from, Vec3d to) {
		Spell spell = getSpell();
		Item[] items = spell.trailItems();
		int[] colors = spell.trailColors();
		Random random = world.getRandom();

		int steps = Math.max(1, (int) Math.ceil(from.distanceTo(to) / 0.4));
		for (int i = 0; i < steps; i++) {
			Vec3d p = from.lerp(to, (double) i / steps);

			for (int k = 0; k < 2; k++) {
				Item item = items[Math.floorMod(trailCounter++, items.length)];
				world.spawnParticles(new ItemStackParticleEffect(ParticleTypes.ITEM, new ItemStack(item)),
						p.x, p.y, p.z, 1, 0.06, 0.06, 0.06, 0.02);
			}

			int color = colors[random.nextInt(colors.length)];
			world.spawnParticles(new DustParticleEffect(color, 1.0f),
					p.x, p.y, p.z, 2, 0.08, 0.08, 0.08, 0.0);
		}
	}
}
