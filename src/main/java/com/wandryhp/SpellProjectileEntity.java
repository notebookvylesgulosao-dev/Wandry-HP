package com.wandryhp;

import net.minecraft.entity.EntityType;
import net.minecraft.entity.data.DataTracker;
import net.minecraft.entity.data.TrackedData;
import net.minecraft.entity.data.TrackedDataHandlerRegistry;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.projectile.ProjectileEntity;
import net.minecraft.entity.projectile.ProjectileUtil;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.particle.*;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

/**
 * Projétil visual: modelo original (feitiço ou prop de ataque base) + rastro do feitiço + rastro da varinha.
 * Só colide e some; não causa dano nem efeitos.
 */
public final class SpellProjectileEntity extends ProjectileEntity {
    private static final TrackedData<Integer> MODEL = DataTracker.registerData(SpellProjectileEntity.class, TrackedDataHandlerRegistry.INTEGER);
    private static final TrackedData<Integer> SPELL = DataTracker.registerData(SpellProjectileEntity.class, TrackedDataHandlerRegistry.INTEGER);
    private static final TrackedData<Integer> STYLE = DataTracker.registerData(SpellProjectileEntity.class, TrackedDataHandlerRegistry.INTEGER);
    private static final int LIFETIME_TICKS = 50;
    private static final double SPEED = 1.4;

    public SpellProjectileEntity(EntityType<? extends SpellProjectileEntity> type, World world) {
        super(type, world);
        setNoGravity(true);
    }

    /** model: 0-6 = feitiços, 7-11 = props. spell: índice do feitiço ou -1 (ataque base). style: ordinal de Style. */
    public static void launch(ServerWorld world, PlayerEntity caster, int model, int spell, int style) {
        SpellProjectileEntity p = new SpellProjectileEntity(WandryHP.SPELL_PROJECTILE, world);
        Vec3d dir = caster.getRotationVec(1f).normalize();
        Vec3d pos = caster.getEyePos().add(dir.multiply(0.8)).subtract(0, 0.15, 0);
        float yaw = (float) (MathHelper.atan2(dir.x, dir.z) * 57.29578);
        float pitch = (float) (MathHelper.atan2(dir.y, Math.sqrt(dir.x * dir.x + dir.z * dir.z)) * 57.29578);
        p.refreshPositionAndAngles(pos.x, pos.y, pos.z, yaw, pitch);
        p.setOwner(caster);
        p.setVelocity(dir.multiply(SPEED));
        p.dataTracker.set(MODEL, model);
        p.dataTracker.set(SPELL, spell);
        p.dataTracker.set(STYLE, style);
        world.spawnEntity(p);
    }

    public int modelIndex() { return dataTracker.get(MODEL); }

    @Override protected void initDataTracker(DataTracker.Builder builder) {
        builder.add(MODEL, 0);
        builder.add(SPELL, -1);
        builder.add(STYLE, 0);
    }

    @Override protected void writeCustomDataToNbt(NbtCompound nbt) {
        super.writeCustomDataToNbt(nbt);
        nbt.putInt("Model", dataTracker.get(MODEL));
        nbt.putInt("Spell", dataTracker.get(SPELL));
        nbt.putInt("Style", dataTracker.get(STYLE));
    }

    @Override protected void readCustomDataFromNbt(NbtCompound nbt) {
        super.readCustomDataFromNbt(nbt);
        dataTracker.set(MODEL, nbt.getInt("Model"));
        dataTracker.set(SPELL, nbt.getInt("Spell"));
        dataTracker.set(STYLE, nbt.getInt("Style"));
    }

    @Override public void tick() {
        super.tick();
        World world = getWorld();
        if (!world.isClient) {
            if (age > LIFETIME_TICKS) { discard(); return; }
            HitResult hit = ProjectileUtil.getCollision(this, this::canHit);
            if (hit.getType() != HitResult.Type.MISS) hitOrDeflect(hit);
            if (isRemoved()) return;
        }
        Vec3d v = getVelocity();
        setPosition(getX() + v.x, getY() + v.y, getZ() + v.z);
        prevYaw = getYaw();
        prevPitch = getPitch();
        setYaw((float) (MathHelper.atan2(v.x, v.z) * 57.29578));
        setPitch((float) (MathHelper.atan2(v.y, Math.sqrt(v.x * v.x + v.z * v.z)) * 57.29578));
        if (world.isClient) spawnTrail();
    }

    @Override protected void onEntityHit(EntityHitResult hit) {
        super.onEntityHit(hit);
        if (!getWorld().isClient) discard();
    }

    @Override protected void onBlockHit(BlockHitResult hit) {
        super.onBlockHit(hit);
        if (!getWorld().isClient) discard();
    }

    // ------------------------------------------------------------------ rastros (client)

    private void spawnTrail() {
        Vec3d c = getPos().add(0, getHeight() / 2, 0);
        int spell = dataTracker.get(SPELL);
        if (spell >= 0 && spell < MagicWandItem.Spell.values().length) spellTrail(MagicWandItem.Spell.values()[spell], c);
        wandTrail(MagicWandItem.Style.values()[MathHelper.clamp(dataTracker.get(STYLE), 0, MagicWandItem.Style.values().length - 1)], c);
    }

    private double jitter(double s) { return random.nextGaussian() * s; }

    private void dust(int rgb, float scale, Vec3d c, double spread) {
        getWorld().addParticle(new DustParticleEffect(rgb, scale), c.x + jitter(spread), c.y + jitter(spread), c.z + jitter(spread), 0, 0, 0);
    }

    private void puff(ParticleEffect effect, Vec3d c, double spread, double vx, double vy, double vz) {
        getWorld().addParticle(effect, c.x + jitter(spread), c.y + jitter(spread), c.z + jitter(spread), vx, vy, vz);
    }

    private void spellTrail(MagicWandItem.Spell spell, Vec3d c) {
        switch (spell) {
            case AVADA_KEDAVRA -> { // verde, fumaça e partículas verdes
                dust(0x1FE04A, 1.0f, c, 0.12);
                dust(0x1FE04A, 0.8f, c, 0.12);
                dust(0x0B5E20, 2.0f, c, 0.15);
                puff(ParticleTypes.SMOKE, c, 0.1, 0, 0.01, 0);
            }
            case CORINFICUS -> { // vermelho, partículas vermelhas e energia explosiva
                dust(0xFF2A2A, 1.1f, c, 0.12);
                dust(0xFF2A2A, 0.8f, c, 0.15);
                if (random.nextInt(5) == 0) puff(ParticleTypes.EXPLOSION, c, 0.1, 0.4, 0, 0);
            }
            case CRUCIO -> { // vermelho intenso, partículas que se dissipam
                for (int i = 0; i < 3; i++)
                    getWorld().addParticle(new DustColorTransitionParticleEffect(0xFF0000, 0x1A0000, 1.4f),
                        c.x + jitter(0.12), c.y + jitter(0.12), c.z + jitter(0.12), 0, 0, 0);
            }
            case EXPELLIARMUS -> { // branco, partículas luminosas
                puff(ParticleTypes.END_ROD, c, 0.08, 0, 0, 0);
                dust(0xFFFFFF, 0.8f, c, 0.1);
            }
            case IMPERIUS -> { // amarelo e dourado, partículas brilhantes
                dust(0xFFD84A, 1.0f, c, 0.12);
                dust(0xFFF04A, 0.8f, c, 0.12);
                puff(ParticleTypes.WAX_ON, c, 0.12, 0, 0, 0);
            }
            case PETRIFICUS_TOTALIS -> { // cinza, partículas acinzentadas
                dust(0x9E9E9E, 1.1f, c, 0.12);
                dust(0x7A7A7A, 0.9f, c, 0.12);
                puff(ParticleTypes.ASH, c, 0.12, 0, 0, 0);
            }
            case SECTUMSEMPRA -> { // roxo, partículas violetas
                dust(0x8B2BE2, 1.1f, c, 0.12);
                dust(0xB35CFF, 0.8f, c, 0.12);
                puff(ParticleTypes.WITCH, c, 0.1, 0, 0, 0);
            }
        }
    }

    private void wandTrail(MagicWandItem.Style style, Vec3d c) {
        switch (style) {
            case AMETHYST -> { // cristais roxos
                puff(new ItemStackParticleEffect(ParticleTypes.ITEM, new ItemStack(Items.AMETHYST_SHARD)), c, 0.1, 0, 0, 0);
                dust(0x9B59F0, 0.6f, c, 0.1);
            }
            case BONE -> { // fragmentos brancos e cinzas de ossos
                puff(new ItemStackParticleEffect(ParticleTypes.ITEM, new ItemStack(Items.BONE_MEAL)), c, 0.1, 0, 0, 0);
                if (random.nextBoolean()) puff(ParticleTypes.ASH, c, 0.1, 0, 0, 0);
            }
            case CELESTIAL -> { // estrelas brancas, douradas e azuis
                puff(ParticleTypes.END_ROD, c, 0.1, 0, 0, 0);
                dust(random.nextBoolean() ? 0xFFD700 : 0x55AAFF, 0.7f, c, 0.1);
            }
            case DRAGON -> { // pequenas chamas verdes e laranjas
                puff(ParticleTypes.SMALL_FLAME, c, 0.08, 0, 0.01, 0);
                dust(0x33FF55, 0.6f, c, 0.1);
            }
            case EMERALD -> { // cristais verde-esmeralda
                puff(new ItemStackParticleEffect(ParticleTypes.ITEM, new ItemStack(Items.EMERALD)), c, 0.1, 0, 0, 0);
                dust(0x2ECC71, 0.6f, c, 0.1);
            }
            case MAGMA -> { // brasas vermelhas, laranjas e amarelas
                int[] embers = { 0xFF2A00, 0xFF8C00, 0xFFD21E };
                dust(embers[random.nextInt(3)], 0.7f, c, 0.12);
                if (random.nextInt(4) == 0) puff(ParticleTypes.LAVA, c, 0.1, 0, 0, 0);
            }
            case WIDOW -> { // fios e partículas pretas, brancas, cinzas e vermelhas
                int[] colors = { 0x111111, 0xFFFFFF, 0x888888, 0xAA0000 };
                dust(colors[random.nextInt(4)], 0.6f, c, 0.12);
                if (random.nextBoolean()) puff(new ItemStackParticleEffect(ParticleTypes.ITEM, new ItemStack(Items.STRING)), c, 0.1, 0, 0, 0);
            }
        }
    }
}
