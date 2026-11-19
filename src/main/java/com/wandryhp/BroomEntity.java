package com.wandryhp;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.MovementType;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.data.DataTracker;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

/**
 * Vassoura montável. Movimento controlado pelo servidor: W/A/S/D andam na direção do olhar (plano horizontal),
 * Espaço sobe e Ctrl desce, a 5 blocos por segundo. Shift desmonta e devolve a vassoura ao inventário.
 */
public final class BroomEntity extends Entity {
    public static final double SPEED = 5.0 / 20.0; // 5 blocos por segundo
    public static final int FORWARD = 1, BACK = 2, LEFT = 4, RIGHT = 8, UP = 16, DOWN = 32;

    private int input;

    public BroomEntity(EntityType<? extends BroomEntity> type, World world) {
        super(type, world);
        setNoGravity(true);
    }

    public void setInput(int flags) { this.input = flags; }

    @Override protected void initDataTracker(DataTracker.Builder builder) {}
    @Override protected void readCustomDataFromNbt(NbtCompound nbt) {}
    @Override protected void writeCustomDataToNbt(NbtCompound nbt) {}

    @Override public boolean canHit() { return !isRemoved(); }
    @Override protected boolean canAddPassenger(Entity passenger) { return getPassengerList().isEmpty(); }

    @Override public ActionResult interact(PlayerEntity player, Hand hand) {
        if (player.isSneaking()) return ActionResult.PASS;
        if (getWorld().isClient) return ActionResult.SUCCESS;
        return player.startRiding(this) ? ActionResult.CONSUME : ActionResult.PASS;
    }

    @Override public void tick() {
        super.tick();
        if (getWorld().isClient) return;
        Entity rider = getFirstPassenger();
        Vec3d velocity = Vec3d.ZERO;
        if (rider != null) {
            setYaw(rider.getYaw());
            double forward = ((input & FORWARD) != 0 ? 1 : 0) - ((input & BACK) != 0 ? 1 : 0);
            double left = ((input & LEFT) != 0 ? 1 : 0) - ((input & RIGHT) != 0 ? 1 : 0);
            double up = ((input & UP) != 0 ? 1 : 0) - ((input & DOWN) != 0 ? 1 : 0);
            double rad = Math.toRadians(rider.getYaw());
            double sin = Math.sin(rad), cos = Math.cos(rad);
            Vec3d dir = new Vec3d(-sin * forward + cos * left, up, cos * forward + sin * left);
            if (dir.lengthSquared() > 1.0E-6) velocity = dir.normalize().multiply(SPEED);
        } else {
            input = 0;
        }
        setVelocity(velocity);
        if (velocity.lengthSquared() > 0) move(MovementType.SELF, velocity);
    }

    /** Desmontar (Shift) devolve a vassoura ao inventário de quem estava nela. */
    @Override protected void removePassenger(Entity passenger) {
        super.removePassenger(passenger);
        if (!getWorld().isClient && !isRemoved() && getPassengerList().isEmpty()) {
            if (passenger instanceof PlayerEntity player && !player.getAbilities().creativeMode) {
                player.getInventory().offerOrDrop(new ItemStack(WandryHP.WITCH_BROOM));
            }
            discard();
        }
    }

    /** Bater na vassoura vazia recupera o item. */
    @Override public boolean damage(ServerWorld world, DamageSource source, float amount) {
        if (isRemoved() || hasPassengers()) return false;
        if (source.getAttacker() instanceof PlayerEntity player) {
            if (!player.getAbilities().creativeMode) player.getInventory().offerOrDrop(new ItemStack(WandryHP.WITCH_BROOM));
            discard();
            return true;
        }
        return false;
    }
}
