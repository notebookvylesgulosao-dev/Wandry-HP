package com.wandryhp.entity;

import com.wandryhp.WandryHP;
import com.wandryhp.config.WandryConfig;
import com.wandryhp.item.WandType;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
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
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.World;

/**
 * Prop lancado pela varinha. Voa em linha reta (sem gravidade), deixa rastro visual de particulas
 * (itens de la/concreto/corante + poeira colorida) e aplica os efeitos da varinha ao atingir um alvo.
 */
public class WandPropEntity extends ProjectileEntity {
	private static final TrackedData<Integer> WAND_TYPE =
			DataTracker.registerData(WandPropEntity.class, TrackedDataHandlerRegistry.INTEGER);

	public WandPropEntity(EntityType<? extends WandPropEntity> type, World world) {
		super(type, world);
	}

	@Override
	protected void initDataTracker(DataTracker.Builder builder) {
		builder.add(WAND_TYPE, 0);
	}

	public WandType getWandType() {
		return WandType.byOrdinal(this.dataTracker.get(WAND_TYPE));
	}

	public void setWandType(WandType type) {
		this.dataTracker.set(WAND_TYPE, type.ordinal());
	}

	/** Id do prop a renderizar (cai no universal quando a varinha nao tem prop proprio). */
	public String getPropId() {
		return getWandType().propId();
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
		if (!(this.getWorld() instanceof ServerWorld world)) {
			return;
		}
		WandType type = getWandType();
		WandryConfig.WandSettings settings = WandryHP.CONFIG.settings(type);
		Entity target = hit.getEntity();
		Entity owner = this.getOwner();

		if (settings.damage > 0f) {
			target.damage(world, this.getDamageSources().indirectMagic(this, owner), settings.damage * com.wandryhp.accessory.AccessoryEffects.wandDamageMultiplier(owner));
		}
		if (target instanceof LivingEntity living) {
			WandEffects.apply(type, settings, living, owner);
		}
	}

	@Override
	protected void onCollision(HitResult hit) {
		super.onCollision(hit);
		if (!this.getWorld().isClient) {
			this.discard();
		}
	}

	/** Rastro: particulas de item (la/concreto/corante) + poeira, nas cores da varinha. Somente visual. */
	private void spawnTrail(ServerWorld world, Vec3d from, Vec3d to) {
		WandType type = getWandType();
		Item[] items = type.trailItems();
		int[] colors = type.trailColors();
		Random random = world.getRandom();

		int steps = Math.max(1, (int) Math.ceil(from.distanceTo(to) / 0.5));
		for (int i = 0; i < steps; i++) {
			Vec3d p = from.lerp(to, (double) i / steps);

			Item item = items[random.nextInt(items.length)];
			world.spawnParticles(new ItemStackParticleEffect(ParticleTypes.ITEM, new ItemStack(item)),
					p.x, p.y, p.z, 1, 0.06, 0.06, 0.06, 0.02);

			int color = colors[random.nextInt(colors.length)];
			world.spawnParticles(new DustParticleEffect(color, 1.0f),
					p.x, p.y, p.z, 2, 0.08, 0.08, 0.08, 0.0);
		}
	}
}
