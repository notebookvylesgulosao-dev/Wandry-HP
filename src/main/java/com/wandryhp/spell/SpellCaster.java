package com.wandryhp.spell;

import com.wandryhp.WandryHP;
import com.wandryhp.item.WandItem;
import com.wandryhp.item.WandType;
import com.wandryhp.mana.Mana;
import com.wandryhp.network.WandCasting;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.entity.Entity;
import net.minecraft.entity.projectile.ProjectileUtil;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Arm;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.RaycastContext;

import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Lógica de servidor do lançamento de feitiços: valida varinha, desbloqueio e recarga do feitiço,
 * calcula a ponta da varinha e a direção (mesma lógica do lançamento dos props base) e cria o projétil.
 */
public final class SpellCaster {
	/** Tick do servidor do último lançamento, por jogador e por feitiço (cada feitiço tem a sua recarga). */
	private static final Map<UUID, Map<Spell, Integer>> LAST_CAST = new HashMap<>();

	private SpellCaster() {}

	public static void forget(ServerPlayerEntity player) {
		LAST_CAST.remove(player.getUuid());
	}

	public static void clear() {
		LAST_CAST.clear();
	}

	public static void tryCast(ServerPlayerEntity player, int spellOrdinal) {
		Spell spell = Spell.byOrdinal(spellOrdinal);
		if (spell == null || player.isSpectator() || !SpellUnlocks.isUnlocked(player, spell)) {
			return;
		}
		ItemStack stack = player.getMainHandStack();
		if (!(stack.getItem() instanceof WandItem)) {
			return;
		}
		if (!(player.getWorld() instanceof ServerWorld world)) {
			return;
		}

		// Lumos aceso: lançar de novo apaga na hora (sem animação, mana nem recarga).
		if (spell == Spell.LUMOS && LumosLight.isActive(player)) {
			LumosLight.turnOff(player);
			return;
		}

		if (WandCasting.isCasting(player)) {
			return; // já conjurando: ignora cliques até o lançamento
		}

		int now = world.getServer().getTicks();
		Map<Spell, Integer> casts = LAST_CAST.computeIfAbsent(player.getUuid(), k -> new EnumMap<>(Spell.class));
		Integer last = casts.get(spell);
		if (last != null && now >= last && now - last < spell.cooldownTicks) {
			return; // ainda em recarga
		}

		// Mana: o servidor valida e gasta. Lançamento recusado (sem mana) não gasta nada nem inicia a recarga.
		if (!Mana.tryConsume(player, spell.manaCost)) {
			return;
		}
		casts.put(spell, now);
		ServerPlayNetworking.send(player, new SpellCooldownPayload(spell.ordinal(), spell.cooldownTicks)); // HUD de recarga

		// Animação de conjuração primeiro; o projétil só nasce quando ela terminar (ver WandCasting).
		WandCasting.begin(player, (caster, type) -> launch(caster, type, spell), spell.manaCost);
	}

	/** Cria o projétil. Chamado por {@link WandCasting} somente depois que a animação de conjuração terminou. */
	private static void launch(ServerPlayerEntity player, WandType type, Spell spell) {
		if (!(player.getWorld() instanceof ServerWorld world)) {
			return;
		}
		// Feitiços sem projétil: o efeito acontece no fim da animação de conjuração.
		if (spell == Spell.LUMOS) {
			LumosLight.turnOn(player);
			return;
		}
		if (spell == Spell.EXPECTO_PATRONUM) {
			PatronusEntity.summon(player);
			return;
		}
		if (spell == Spell.VORTEM) {
			Vortem.cast(player);
			return;
		}

		Vec3d eye = player.getEyePos();
		Vec3d forward = player.getRotationVec(1.0f);

		// Base da câmera do jogador: direita (a partir do yaw) e cima.
		double yaw = Math.toRadians(player.getYaw());
		Vec3d right = new Vec3d(-Math.cos(yaw), 0.0, -Math.sin(yaw));
		Vec3d up = right.crossProduct(forward);
		double side = player.getMainArm() == Arm.RIGHT ? 1.0 : -1.0;

		// Ponta da varinha; se houver parede entre o olho e a ponta, sai do olho.
		Vec3d tip = eye
				.add(right.multiply(side * type.tipRight))
				.add(up.multiply(type.tipUp))
				.add(forward.multiply(type.tipForward));
		if (world.raycast(new RaycastContext(eye, tip, RaycastContext.ShapeType.COLLIDER,
				RaycastContext.FluidHandling.NONE, player)).getType() != HitResult.Type.MISS) {
			tip = eye;
		}

		Vec3d direction = aimDirection(world, player, eye, forward, tip);

		SpellProjectileEntity projectile = new SpellProjectileEntity(ModSpellEntities.SPELL_PROJECTILE, world);
		projectile.setOwner(player);
		projectile.setSpell(spell);
		projectile.setPosition(tip);
		projectile.setVelocity(direction.multiply(WandryHP.CONFIG.projectileSpeed));
		world.spawnEntity(projectile);
	}

	/** Posição da ponta da varinha (mesma conta do lançamento), sem o teste de parede. */
	public static Vec3d wandTip(ServerPlayerEntity player, WandType type) {
		Vec3d eye = player.getEyePos();
		Vec3d forward = player.getRotationVec(1.0f);
		double yaw = Math.toRadians(player.getYaw());
		Vec3d right = new Vec3d(-Math.cos(yaw), 0.0, -Math.sin(yaw));
		Vec3d up = right.crossProduct(forward);
		double side = player.getMainArm() == Arm.RIGHT ? 1.0 : -1.0;
		return eye
				.add(right.multiply(side * type.tipRight))
				.add(up.multiply(type.tipUp))
				.add(forward.multiply(type.tipForward));
	}

	/** Direção da ponta até o ponto mirado; sem alvo, segue a direção do olhar. */
	private static Vec3d aimDirection(ServerWorld world, ServerPlayerEntity player, Vec3d eye, Vec3d forward, Vec3d tip) {
		double range = WandryHP.CONFIG.projectileSpeed * WandryHP.CONFIG.maxLifetimeTicks;
		Vec3d end = eye.add(forward.multiply(range));

		Vec3d aim = end;
		double bestSq = range * range;

		HitResult block = world.raycast(new RaycastContext(eye, end, RaycastContext.ShapeType.COLLIDER,
				RaycastContext.FluidHandling.NONE, player));
		if (block.getType() != HitResult.Type.MISS) {
			aim = block.getPos();
			bestSq = eye.squaredDistanceTo(aim);
		}

		Box box = player.getBoundingBox().stretch(forward.multiply(range)).expand(1.0);
		EntityHitResult entity = ProjectileUtil.raycast(player, eye, end, box,
				(Entity e) -> !e.isSpectator() && e.canHit(), bestSq);
		if (entity != null) {
			aim = entity.getPos();
		}

		Vec3d dir = aim.subtract(tip);
		if (dir.lengthSquared() < 1.0E-4) {
			return forward;
		}
		dir = dir.normalize();
		// Alvo muito próximo/atrás da ponta geraria uma direção estranha: nesse caso segue o olhar.
		return dir.dotProduct(forward) < 0.5 ? forward : dir;
	}
}
