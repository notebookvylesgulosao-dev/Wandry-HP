package com.wandryhp.edwiges;

import com.wandryhp.armor.HouseType;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.ItemEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.data.DataTracker;
import net.minecraft.entity.data.TrackedData;
import net.minecraft.entity.data.TrackedDataHandlerRegistry;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

import java.util.UUID;

/**
 * Edwiges: voa ate o jogador, usa "pouso" e solta o envelope da casa, depois voa embora e some quando sai do
 * campo de visao dele. Sem IA nem colisao (atravessa blocos); o servidor decide tudo e o cliente so anima.
 *
 * <p>Fases (servidor): APPROACH (voa a 2 blocos/s na direcao do jogador, reajustando a rota todo tick) ->
 * LANDING (a 5 blocos: "pouso", solta o envelope na hora certa da animacao) -> LEAVING ("fly", direcao aleatoria,
 * removida ao sair do campo de visao).
 */
public class EdwigesEntity extends Entity {
	public static final int ANIM_FLY = 0;
	public static final int ANIM_POUSO = 1;
	public static final int ANIM_IDLE = 2;

	private static final TrackedData<Integer> ANIMATION =
			DataTracker.registerData(EdwigesEntity.class, TrackedDataHandlerRegistry.INTEGER);
	private static final TrackedData<Boolean> HAS_LETTER =
			DataTracker.registerData(EdwigesEntity.class, TrackedDataHandlerRegistry.BOOLEAN);
	/** Ordinal da {@link HouseType} da carta (-1 = nenhuma). */
	private static final TrackedData<Integer> HOUSE =
			DataTracker.registerData(EdwigesEntity.class, TrackedDataHandlerRegistry.INTEGER);

	/** A cada quantos ticks a distancia e enviada para a HUD (o cliente suaviza entre os envios). */
	private static final int HUD_INTERVAL_TICKS = 5;

	private enum Phase { APPROACH, LANDING, LEAVING }

	private Phase phase = Phase.APPROACH;
	private UUID target;
	private boolean dropped;
	private Vec3d leaveDirection = Vec3d.ZERO;
	/** Ticks desde a ultima troca de animacao (nos dois lados): relogio da animacao. */
	private int stateTicks;
	/** Distancia (blocos) em que a HUD de chegada comecou (-1 = ainda nao medida) e contador de envio dela. */
	private double hudStart = -1.0;
	private int hudTimer;

	public EdwigesEntity(EntityType<? extends EdwigesEntity> type, World world) {
		super(type, world);
		this.noClip = true;
		this.setNoGravity(true);
	}

	@Override
	protected void initDataTracker(DataTracker.Builder builder) {
		builder.add(ANIMATION, ANIM_FLY);
		builder.add(HAS_LETTER, false);
		builder.add(HOUSE, -1);
	}

	// ---------------------------------------------------------------- estado sincronizado

	public int getAnimation() {
		return this.dataTracker.get(ANIMATION);
	}

	private void setAnimation(int animation) {
		this.dataTracker.set(ANIMATION, animation);
		this.stateTicks = 0;
	}

	public boolean hasLetter() {
		return this.dataTracker.get(HAS_LETTER);
	}

	public void setHasLetter(boolean value) {
		this.dataTracker.set(HAS_LETTER, value);
	}

	public void setHouse(HouseType house) {
		this.dataTracker.set(HOUSE, house == null ? -1 : house.ordinal());
	}

	/** Id da casa da carta, ou null. */
	public String getHouseId() {
		int ordinal = this.dataTracker.get(HOUSE);
		HouseType[] all = HouseType.values();
		return ordinal >= 0 && ordinal < all.length ? all[ordinal].id : null;
	}

	/** Ticks na animacao atual (relogio da animacao). */
	public int getStateTicks() {
		return stateTicks;
	}

	public void setTarget(UUID player) {
		this.target = player;
	}

	@Override
	public void onTrackedDataSet(TrackedData<?> data) {
		if (ANIMATION.equals(data)) {
			this.stateTicks = 0; // cliente: reinicia o relogio quando o servidor troca de animacao
		}
		super.onTrackedDataSet(data);
	}

	// ---------------------------------------------------------------- tick

	@Override
	public void tick() {
		super.tick();
		World world = this.getWorld();
		if (world.isClient) {
			// Predicao: segue a velocidade sincronizada (o servidor corrige a posicao), como um item/projetil.
			this.stateTicks++;
			this.setPosition(this.getPos().add(this.getVelocity()));
			return;
		}
		if (world instanceof ServerWorld serverWorld) {
			serverTick(serverWorld);
		}
	}

	private void serverTick(ServerWorld world) {
		this.stateTicks++;
		ServerPlayerEntity player = this.target == null || this.getServer() == null ? null
				: this.getServer().getPlayerManager().getPlayer(this.target);
		// Sem jogador valido (saiu, morreu, mudou de dimensao): some; EdwigesDelivery cria outra se a carta nao foi entregue.
		if (player == null || !player.isAlive() || player.getServerWorld() != world || this.age > EdwigesSettings.MAX_LIFETIME_TICKS) {
			this.discard();
			return;
		}
		switch (this.phase) {
			case APPROACH -> approach(player);
			case LANDING -> land(world, player);
			case LEAVING -> leave(player);
		}
	}

	/** Voa direto ao jogador (posicao dele a cada tick = acompanha os movimentos). */
	private void approach(ServerPlayerEntity player) {
		Vec3d toPlayer = player.getEyePos().subtract(this.getPos());
		double distance = toPlayer.length();
		// HUD de chegada: o primeiro envio (distancia inicial = barra cheia) e depois 4x por segundo.
		if (this.hudStart < 0.0) this.hudStart = Math.max(distance, 1.0);
		if (this.hudTimer++ % HUD_INTERVAL_TICKS == 0 || distance <= EdwigesSettings.DELIVERY_DISTANCE) {
			EdwigesHudPayload.sendProgress(player, distance, this.hudStart);
		}
		if (distance <= EdwigesSettings.DELIVERY_DISTANCE) {
			this.phase = Phase.LANDING;
			this.setAnimation(ANIM_POUSO);
			return;
		}
		if (distance > EdwigesSettings.ABANDON_DISTANCE) {
			this.discard(); // jogador deixou ela para tras: EdwigesDelivery chama outra perto dele
			return;
		}
		Vec3d velocity = toPlayer.multiply(EdwigesSettings.SPEED_PER_TICK / distance);
		fly(velocity);
		faceDirection(velocity);
	}

	/** "pouso": para no ar olhando para o jogador, solta o envelope e, ao fim da animacao, vai embora. */
	private void land(ServerWorld world, ServerPlayerEntity player) {
		Vec3d velocity = this.getVelocity().multiply(0.8);
		fly(velocity.lengthSquared() < 1.0E-6 ? Vec3d.ZERO : velocity);
		faceDirection(player.getEyePos().subtract(this.getPos()));

		if (!this.dropped && this.stateTicks >= EdwigesSettings.DROP_TICK) {
			dropLetter(world, player);
		}
		if (this.stateTicks >= EdwigesRig.landingTicks()) {
			startLeaving(world);
		}
	}

	private void startLeaving(ServerWorld world) {
		// Direcao aleatoria: azimute qualquer e subida de 0 a 30 graus (nunca mergulha no terreno).
		double azimuth = world.getRandom().nextDouble() * Math.PI * 2.0;
		double elevation = Math.toRadians(world.getRandom().nextDouble() * 30.0);
		this.leaveDirection = new Vec3d(Math.cos(elevation) * Math.cos(azimuth), Math.sin(elevation), Math.cos(elevation) * Math.sin(azimuth));
		this.phase = Phase.LEAVING;
		this.setAnimation(ANIM_FLY);
	}

	private void leave(ServerPlayerEntity player) {
		Vec3d velocity = this.leaveDirection.multiply(EdwigesSettings.SPEED_PER_TICK);
		fly(velocity);
		faceDirection(velocity);
		// So some quando sai do campo de visao do jogador.
		if (!isInView(player)) {
			this.discard();
		}
	}

	/** Dentro do alcance e do cone de visao (voltado para onde o jogador olha agora). */
	private boolean isInView(ServerPlayerEntity player) {
		Vec3d toOwl = this.getPos().add(0, 0.6, 0).subtract(player.getEyePos());
		double distance = toOwl.length();
		if (distance > EdwigesSettings.VISIBLE_RANGE) return false;
		if (distance < 1.0E-3) return true;
		double cos = player.getRotationVec(1.0F).dotProduct(toOwl.multiply(1.0 / distance));
		return cos > Math.cos(Math.toRadians(EdwigesSettings.VIEW_HALF_ANGLE_DEGREES));
	}

	private void fly(Vec3d velocity) {
		this.setVelocity(velocity);
		this.setPosition(this.getPos().add(velocity));
	}

	/** Gira o corpo ate a direcao (suavizado); a inclinacao visual e limitada para parecer planeio. */
	private void faceDirection(Vec3d direction) {
		double horizontal = Math.sqrt(direction.x * direction.x + direction.z * direction.z);
		if (horizontal > 1.0E-4) {
			float targetYaw = (float) Math.toDegrees(Math.atan2(-direction.x, direction.z));
			float diff = MathHelper.wrapDegrees(targetYaw - this.getYaw());
			this.setYaw(this.getYaw() + MathHelper.clamp(diff, -EdwigesSettings.MAX_TURN_PER_TICK, EdwigesSettings.MAX_TURN_PER_TICK));
		}
		float targetPitch = (float) -Math.toDegrees(Math.atan2(direction.y, Math.max(horizontal, 1.0E-4)));
		targetPitch = MathHelper.clamp(targetPitch, -EdwigesSettings.MAX_VISUAL_PITCH, EdwigesSettings.MAX_VISUAL_PITCH);
		this.setPitch(this.getPitch() + MathHelper.clamp(targetPitch - this.getPitch(), -3.0f, 3.0f));
	}

	/** Solta o envelope (item fisico, com gravidade) no ponto exato em que ele esta preso e marca a entrega como feita. */
	private void dropLetter(ServerWorld world, ServerPlayerEntity player) {
		this.dropped = true;
		// Ela sempre carrega o envelope generico (a carta da casa so surge no clique direito).
		double[] offset = EdwigesRig.letterWorldOffset(null, EdwigesRig.ANIM_POUSO, this.stateTicks / 20.0, false, this.getYaw(), this.getPitch());
		Vec3d center = this.getPos().add(offset[0], offset[1], offset[2]);

		ItemEntity envelope = new ItemEntity(world, center.x, center.y, center.z,
				new ItemStack(ModEdwigesItems.ENVELOPE), 0.0, 0.0, 0.0);
		envelope.setPickupDelay(20);
		envelope.setNeverDespawn();
		envelope.setInvulnerable(true);
		world.spawnEntity(envelope);

		this.setHasLetter(false);
		EdwigesDelivery.markDelivered(player);
		EdwigesHudPayload.sendDelivered(player);
	}

	// ---------------------------------------------------------------- renderizacao / interacao

	/** O corpo e pequeno mas ela e vista de longe: renderiza ate o alcance de rastreamento. */
	@Override
	public boolean shouldRender(double distance) {
		return distance < 160.0 * 160.0;
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
