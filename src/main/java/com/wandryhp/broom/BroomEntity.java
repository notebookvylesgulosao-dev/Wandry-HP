package com.wandryhp.broom;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.MovementType;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.data.DataTracker;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.particle.DustParticleEffect;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

/**
 * Vassoura voadora montavel. Sem gravidade: quando o jogador desmonta ela fica parada onde esta.
 *
 * <p>O servidor decide tudo: o cliente do jogador manda so as teclas ({@link BroomInputPayload}) e a vassoura
 * voa para onde ele olha (W/S = frente/tras, A/D = lados, Espaco sobe, Ctrl desce), no maximo
 * {@link #MAX_SPEED_BLOCKS_PER_SECOND} blocos por segundo. Desmontar = Shift (padrao do jogo);
 * pegar de volta = agachar + botao direito nela.
 */
public class BroomEntity extends Entity {
	/** Bits de {@link BroomInputPayload#flags()}. */
	public static final int FORWARD = 1, BACK = 2, LEFT = 4, RIGHT = 8, UP = 16, DOWN = 32;

	public static final double MAX_SPEED_BLOCKS_PER_SECOND = 8.0;
	private static final double MAX_SPEED_PER_TICK = MAX_SPEED_BLOCKS_PER_SECOND / 20.0;
	/** Fracao da diferenca ate a velocidade desejada que e corrigida por tick (nunca passa do maximo). */
	private static final double ACCELERATION = 0.25;
	/** Freio quando nenhuma tecla esta pressionada. */
	private static final double DRAG = 0.8;
	/** Inclinacao visual maxima (a direcao de voo continua sendo a do olhar). */
	private static final float MAX_VISUAL_PITCH = 35.0f;
	private static final int PURPLE = 0x9B30FF;

	/** Teclas do cavaleiro (ultimo estado recebido do cliente). */
	private int input;

	public BroomEntity(EntityType<? extends BroomEntity> type, World world) {
		super(type, world);
		this.setNoGravity(true);
	}

	@Override
	protected void initDataTracker(DataTracker.Builder builder) {}

	public void setInput(int flags) {
		this.input = flags;
	}

	private boolean pressed(int flag) {
		return (this.input & flag) != 0;
	}

	// ---------------------------------------------------------------- voo (so no servidor)

	/** O jogador montado controla a vassoura (como um barco): o movimento roda no cliente dele, sem atraso. */
	@Override
	public LivingEntity getControllingPassenger() {
		return this.getFirstPassenger() instanceof PlayerEntity player ? player : null;
	}

	@Override
	public void tick() {
		super.tick();
		Entity rider = this.getFirstPassenger();
		if (rider != null) {
			// Montado: a altura de queda e zerada, entao aterrissar nao causa dano.
			rider.fallDistance = 0;
			if (this.getWorld().isClient) {
				spawnTrailClient();
			}
		}
		// So o lado que controla (o cliente do cavaleiro; ou o servidor quando nao ha jogador) calcula o voo.
		if (!this.isLogicalSideForUpdatingMovement()) {
			return;
		}
		if (rider == null) {
			// Sem cavaleiro: fica exatamente onde esta.
			this.input = 0;
			this.setVelocity(Vec3d.ZERO);
			return;
		}

		Vec3d look = rider.getRotationVec(1.0F);
		double yaw = Math.toRadians(rider.getYaw());
		Vec3d right = new Vec3d(-Math.cos(yaw), 0.0, -Math.sin(yaw));
		Vec3d wish = Vec3d.ZERO;
		if (pressed(FORWARD)) wish = wish.add(look);
		if (pressed(BACK)) wish = wish.subtract(look);
		if (pressed(RIGHT)) wish = wish.add(right);
		if (pressed(LEFT)) wish = wish.subtract(right);
		if (pressed(UP)) wish = wish.add(0.0, 1.0, 0.0);
		if (pressed(DOWN)) wish = wish.add(0.0, -1.0, 0.0);

		Vec3d velocity = this.getVelocity();
		if (wish.lengthSquared() > 1.0E-6) {
			Vec3d target = wish.normalize().multiply(MAX_SPEED_PER_TICK);
			velocity = velocity.add(target.subtract(velocity).multiply(ACCELERATION));
		} else {
			velocity = velocity.multiply(DRAG);
		}
		if (velocity.lengthSquared() < 1.0E-6) {
			velocity = Vec3d.ZERO;
		}
		this.setVelocity(velocity);
		if (!velocity.equals(Vec3d.ZERO)) {
			this.move(MovementType.SELF, velocity);
		}

		this.setYaw(rider.getYaw());
		this.setPitch(MathHelper.clamp(rider.getPitch(), -MAX_VISUAL_PITCH, MAX_VISUAL_PITCH));
	}

	/** Particulas roxas no cliente (cada jogador as ve localmente). Somente visual. */
	private void spawnTrailClient() {
		World world = this.getWorld();
		Vec3d forward = Vec3d.fromPolar(this.getPitch(), this.getYaw());
		Vec3d tail = this.getPos().add(0.0, 0.5, 0.0).subtract(forward.multiply(0.75));
		for (int i = 0; i < 3; i++) {
			world.addParticle(new DustParticleEffect(PURPLE, 1.0f),
					tail.x + (world.random.nextDouble() - 0.5) * 0.2, tail.y + (world.random.nextDouble() - 0.5) * 0.2,
					tail.z + (world.random.nextDouble() - 0.5) * 0.2, 0.0, 0.0, 0.0);
		}
		if (world.random.nextInt(3) == 0) {
			world.addParticle(ParticleTypes.WITCH, tail.x, tail.y, tail.z, 0.0, 0.0, 0.0);
		}
	}

	// ---------------------------------------------------------------- interacao

	/** Botao direito: monta. Agachado + botao direito: pega a vassoura de volta (vazia). */
	@Override
	public ActionResult interact(PlayerEntity player, Hand hand) {
		if (this.hasPassengers()) {
			return ActionResult.PASS;
		}
		if (player.isSneaking()) {
			if (!this.getWorld().isClient) {
				player.getInventory().offerOrDrop(new ItemStack(BroomInit.ITEM));
				this.discard();
			}
			return ActionResult.SUCCESS;
		}
		if (!this.getWorld().isClient) {
			player.startRiding(this);
		}
		return ActionResult.SUCCESS;
	}

	/** Ao desmontar o jogador fica no ponto em que estava sentado (e nao em cima da caixa da vassoura). */
	@Override
	public Vec3d updatePassengerForDismount(LivingEntity passenger) {
		return this.getPos();
	}

	@Override
	public boolean canHit() {
		return !this.isRemoved();
	}

	@Override
	public boolean damage(ServerWorld world, DamageSource source, float amount) {
		return false;
	}

	@Override
	public boolean isAttackable() {
		return false;
	}

	@Override
	public boolean isPushable() {
		return false;
	}

	@Override
	protected void readCustomDataFromNbt(NbtCompound nbt) {}

	@Override
	protected void writeCustomDataToNbt(NbtCompound nbt) {}
}
