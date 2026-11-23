package com.wandryhp.accessory;

import com.wandryhp.WandryHP;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.entity.Entity;
import net.minecraft.entity.attribute.EntityAttributeInstance;
import net.minecraft.entity.attribute.EntityAttributeModifier;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;
import net.minecraft.world.GameRules;

/**
 * Bonus dos chapeus Enchanter (itens enchanter_*_cape). Mesmo modelo do conjunto das casas: a cada tick (e ao equipar/desequipar) o estado do jogador e
 * reconciliado com o chapeu realmente equipado, usando modificadores com id fixo. Como a checagem e por id, nao ha
 * duplicacao; como parte do estado real (o slot), trocar ou retirar a capa sempre remove o bonus anterior.
 *
 * <p>Somente Vermelho, Amarelo e Roxo sao atributos vanilla. Azul (duracao dos efeitos) e Verde (dano dos props das
 * varinhas) nao existem como atributo e sao consultados pelos pontos de dano/efeito do mod.
 */
public final class AccessoryEffects {
	/** +2 coracoes = +4 pontos de vida maxima. */
	public static final double RED_MAX_HEALTH_BONUS = 4.0;
	/** +30% de velocidade de movimento (multiplica o valor base). */
	public static final double YELLOW_SPEED_BONUS = 0.30;
	/** +30% de velocidade de movimento (multiplica o valor base). */
	public static final double PURPLE_SPEED_BONUS = 0.30;
	/** +10% de dano dos props das varinhas. */
	public static final float GREEN_WAND_DAMAGE_MULTIPLIER = 1.10f;
	/** +20% de duracao dos efeitos aplicados com varinhas e feiticos. */
	public static final float BLUE_EFFECT_DURATION_MULTIPLIER = 1.20f;

	private static final Identifier RED_ID = Identifier.of(WandryHP.MOD_ID, "accessory_red_cape_max_health");
	private static final Identifier YELLOW_ID = Identifier.of(WandryHP.MOD_ID, "accessory_yellow_cape_speed");
	private static final Identifier PURPLE_ID = Identifier.of(WandryHP.MOD_ID, "accessory_purple_cape_speed");

	private AccessoryEffects() {}

	public static void register() {
		ServerTickEvents.END_SERVER_TICK.register(server -> {
			for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
				update(player);
			}
		});

		// Morte: sem keepInventory os acessorios caem no chao (como a armadura); com keepInventory ficam.
		ServerLivingEntityEvents.AFTER_DEATH.register((entity, source) -> {
			if (entity instanceof ServerPlayerEntity player && !player.getServerWorld().getGameRules().getBoolean(GameRules.KEEP_INVENTORY)) {
				AccessoryData data = Accessories.get(player);
				for (int i = 0; i < AccessoryData.SIZE; i++) {
					ItemStack stack = data.get(i);
					if (!stack.isEmpty()) {
						player.dropItem(stack, true, false);
					}
				}
				Accessories.set(player, AccessoryData.EMPTY);
			}
		});

		// Renascer / voltar do End: o novo jogador herda os acessorios que sobraram (keepInventory ou nao-morte).
		ServerPlayerEvents.COPY_FROM.register((oldPlayer, newPlayer, alive) -> {
			AccessoryData data = Accessories.get(oldPlayer);
			if (!data.isEmpty()) {
				Accessories.set(newPlayer, data);
			}
		});
		ServerPlayerEvents.AFTER_RESPAWN.register((oldPlayer, newPlayer, alive) -> {
			update(newPlayer);
			if (!alive) {
				newPlayer.setHealth(newPlayer.getMaxHealth());
			}
		});
	}

	/** Reconcilia os modificadores do jogador com o chapeu equipado. Idempotente. */
	public static void update(PlayerEntity player) {
		CapeType cape = Accessories.equippedCape(player);
		boolean removedAny = false;
		removedAny |= sync(player, EntityAttributes.MAX_HEALTH, RED_ID, RED_MAX_HEALTH_BONUS,
				EntityAttributeModifier.Operation.ADD_VALUE, cape == CapeType.RED);
		removedAny |= sync(player, EntityAttributes.MOVEMENT_SPEED, YELLOW_ID, YELLOW_SPEED_BONUS,
				EntityAttributeModifier.Operation.ADD_MULTIPLIED_BASE, cape == CapeType.YELLOW);
		removedAny |= sync(player, EntityAttributes.MOVEMENT_SPEED, PURPLE_ID, PURPLE_SPEED_BONUS,
				EntityAttributeModifier.Operation.ADD_MULTIPLIED_BASE, cape == CapeType.PURPLE);

		// Ao perder a vida maxima extra, a vida atual nao pode ficar acima do novo maximo.
		if (removedAny && player.getHealth() > player.getMaxHealth()) {
			player.setHealth(player.getMaxHealth());
		}
	}

	/** @return true se removeu um modificador. */
	private static boolean sync(PlayerEntity player, net.minecraft.registry.entry.RegistryEntry<net.minecraft.entity.attribute.EntityAttribute> attribute,
								Identifier id, double value, EntityAttributeModifier.Operation operation, boolean active) {
		EntityAttributeInstance instance = player.getAttributeInstance(attribute);
		if (instance == null) {
			return false;
		}
		boolean present = instance.hasModifier(id);
		if (active && !present) {
			// Persistente: a vida maxima extra ja existe ao carregar o jogador (senao a vida salva seria cortada).
			instance.addPersistentModifier(new EntityAttributeModifier(id, value, operation));
		} else if (!active && present) {
			instance.removeModifier(id);
			return true;
		}
		return false;
	}

	/** Multiplicador do dano dos props das varinhas lancados por {@code owner} (Verde). */
	public static float wandDamageMultiplier(Entity owner) {
		return owner instanceof PlayerEntity player && Accessories.equippedCape(player) == CapeType.GREEN
				? GREEN_WAND_DAMAGE_MULTIPLIER : 1.0f;
	}

	/** Duracao (em ticks) de um efeito aplicado por {@code owner}, ja com o bonus do chapeu Azul. */
	public static int scaleEffectDuration(Entity owner, int ticks) {
		if (owner instanceof PlayerEntity player && Accessories.equippedCape(player) == CapeType.BLUE) {
			return Math.round(ticks * BLUE_EFFECT_DURATION_MULTIPLIER);
		}
		return ticks;
	}
}
