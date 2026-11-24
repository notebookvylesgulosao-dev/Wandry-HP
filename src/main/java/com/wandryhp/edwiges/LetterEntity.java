package com.wandryhp.edwiges;

import com.wandryhp.armor.HouseType;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.MovementType;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.data.DataTracker;
import net.minecraft.entity.data.TrackedData;
import net.minecraft.entity.data.TrackedDataHandlerRegistry;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

import java.util.UUID;

/**
 * Envelope solto pela Edwiges: cai com a gravidade normal ate o chao e fica ali (salvo no mundo). A leitura da
 * carta (HUD) ainda nao existe: {@link #getHouseId()} e {@link #getRecipient()} sao o ponto de ligacao.
 */
public class LetterEntity extends Entity {
	/** Altura (blocos) do centro do envelope sobre a posicao da entidade enquanto cai (em pe, como preso). */
	public static final float AIRBORNE_PIVOT = 0.1f;

	private static final TrackedData<String> HOUSE =
			DataTracker.registerData(LetterEntity.class, TrackedDataHandlerRegistry.STRING);
	private static final TrackedData<Boolean> LANDED =
			DataTracker.registerData(LetterEntity.class, TrackedDataHandlerRegistry.BOOLEAN);

	private UUID recipient;
	/** Cliente: ticks desde que pousou (anima o envelope deitando no chao). */
	private int landedTicks;

	public LetterEntity(EntityType<? extends LetterEntity> type, World world) {
		super(type, world);
	}

	@Override
	protected void initDataTracker(DataTracker.Builder builder) {
		builder.add(HOUSE, "");
		builder.add(LANDED, false);
	}

	public String getHouseId() {
		String id = this.dataTracker.get(HOUSE);
		return id.isEmpty() ? null : id;
	}

	public void setHouseId(String id) {
		this.dataTracker.set(HOUSE, id == null ? "" : id);
	}

	public UUID getRecipient() {
		return recipient;
	}

	public void setRecipient(UUID recipient) {
		this.recipient = recipient;
	}

	public boolean hasLanded() {
		return this.dataTracker.get(LANDED);
	}

	public int getLandedTicks() {
		return landedTicks;
	}

	@Override
	public void tick() {
		super.tick();
		// Mesma fisica nos dois lados (como ItemEntity): gravidade, colisao com blocos e arrasto.
		this.applyGravity();
		this.move(MovementType.SELF, this.getVelocity());
		Vec3d v = this.getVelocity().multiply(0.98, 0.98, 0.98);
		if (this.isOnGround()) {
			v = Vec3d.ZERO; // pousou: para; a gravidade do proximo tick mantem o contato
		}
		this.setVelocity(v);

		if (this.getWorld().isClient) {
			this.landedTicks = this.hasLanded() ? this.landedTicks + 1 : 0;
		} else if (this.isOnGround() != this.hasLanded()) {
			this.dataTracker.set(LANDED, this.isOnGround());
		}
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
	protected void readCustomDataFromNbt(NbtCompound nbt) {
		setHouseId(nbt.getString("House"));
		if (nbt.containsUuid("Recipient")) {
			this.recipient = nbt.getUuid("Recipient");
		}
	}

	@Override
	protected void writeCustomDataToNbt(NbtCompound nbt) {
		String id = getHouseId();
		if (id != null) nbt.putString("House", id);
		if (this.recipient != null) nbt.putUuid("Recipient", this.recipient);
	}

	/** Casa da carta como {@link HouseType}, ou null. */
	public HouseType getHouse() {
		String id = getHouseId();
		for (HouseType h : HouseType.values()) {
			if (h.id.equals(id)) return h;
		}
		return null;
	}
}
