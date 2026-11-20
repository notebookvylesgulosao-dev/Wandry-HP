package com.wandryhp.armor;

import com.wandryhp.WandryHP;
import net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.attribute.EntityAttribute;
import net.minecraft.entity.attribute.EntityAttributeInstance;
import net.minecraft.entity.attribute.EntityAttributeModifier;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;

import java.util.List;

/**
 * Bonus do conjunto das casas. Ativos somente com, simultaneamente: um dos quatro peitorais (slot do peito),
 * a calca compartilhada (slot das pernas) e as botas compartilhadas (slot dos pes).
 *
 * <p>Os bonus sao modificadores de atributo com id fixo. A cada tick do servidor o estado e reconciliado com o
 * equipamento atual: se o conjunto esta completo e o modificador falta, ele e adicionado; se o conjunto esta
 * incompleto e o modificador existe, ele e removido. Como a verificacao e por id, nunca ha duplicacao, e como
 * parte do estado real (o equipamento) e nao de eventos de equipar/desequipar, tambem cobre trocar de peca,
 * morrer, renascer, trocar de dimensao, /item replace, dispenser etc.
 *
 * <p>Os modificadores sao persistentes (salvos no jogador) para que a vida maxima extra ja exista quando a vida
 * salva e carregada ao entrar no mundo; senao ela seria cortada para 20 a cada login.
 */
public final class HouseArmorSet {
	/** 15% menos knockback = +0,15 de resistencia a repulsao (a formula vanilla multiplica o recuo por 1 - resistencia). */
	public static final double KNOCKBACK_RESISTANCE_BONUS = 0.15;
	/** +5 coracoes = +10 pontos de vida maxima. */
	public static final double MAX_HEALTH_BONUS = 10.0;
	/** +30% de velocidade de movimento (multiplica o valor base). */
	public static final double MOVEMENT_SPEED_BONUS = 0.30;

	private record Bonus(RegistryEntry<EntityAttribute> attribute, Identifier id, double value,
						 EntityAttributeModifier.Operation operation) {
		EntityAttributeModifier modifier() {
			return new EntityAttributeModifier(id, value, operation);
		}
	}

	private static final List<Bonus> BONUSES = List.of(
			new Bonus(EntityAttributes.KNOCKBACK_RESISTANCE, Identifier.of(WandryHP.MOD_ID, "house_set_knockback"),
					KNOCKBACK_RESISTANCE_BONUS, EntityAttributeModifier.Operation.ADD_VALUE),
			new Bonus(EntityAttributes.MAX_HEALTH, Identifier.of(WandryHP.MOD_ID, "house_set_max_health"),
					MAX_HEALTH_BONUS, EntityAttributeModifier.Operation.ADD_VALUE),
			new Bonus(EntityAttributes.MOVEMENT_SPEED, Identifier.of(WandryHP.MOD_ID, "house_set_speed"),
					MOVEMENT_SPEED_BONUS, EntityAttributeModifier.Operation.ADD_MULTIPLIED_BASE));

	private HouseArmorSet() {}

	public static void register() {
		ServerTickEvents.END_SERVER_TICK.register(server -> {
			for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
				update(player);
			}
		});

		// Renascer: aplica ja no novo jogador (com keepInventory o conjunto continua equipado) e, se morreu,
		// comeca com a vida cheia considerando a vida maxima com o bonus. Voltar do End (alive) mantem a vida.
		ServerPlayerEvents.AFTER_RESPAWN.register((oldPlayer, newPlayer, alive) -> {
			update(newPlayer);
			if (!alive) {
				newPlayer.setHealth(newPlayer.getMaxHealth());
			}
		});
	}

	/** Conjunto completo: qualquer peitoral das casas + calca + botas, cada um no seu slot. */
	public static boolean hasFullSet(PlayerEntity player) {
		return player.getEquippedStack(EquipmentSlot.CHEST).getItem() instanceof HouseChestplateItem
				&& player.getEquippedStack(EquipmentSlot.LEGS).isOf(ModArmorItems.LEGGINGS)
				&& player.getEquippedStack(EquipmentSlot.FEET).isOf(ModArmorItems.BOOTS);
	}

	/** Reconcilia os modificadores do jogador com o equipamento atual. Idempotente. */
	public static void update(PlayerEntity player) {
		boolean active = hasFullSet(player);
		boolean removedAny = false;

		for (Bonus bonus : BONUSES) {
			EntityAttributeInstance instance = player.getAttributeInstance(bonus.attribute());
			if (instance == null) {
				continue;
			}
			boolean present = instance.hasModifier(bonus.id());
			if (active && !present) {
				instance.addPersistentModifier(bonus.modifier());
			} else if (!active && present) {
				instance.removeModifier(bonus.id());
				removedAny = true;
			}
		}

		// Ao perder a vida maxima extra, a vida atual nao pode ficar acima do novo maximo.
		if (removedAny && player.getHealth() > player.getMaxHealth()) {
			player.setHealth(player.getMaxHealth());
		}
	}
}
