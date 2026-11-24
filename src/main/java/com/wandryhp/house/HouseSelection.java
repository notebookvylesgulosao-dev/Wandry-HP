package com.wandryhp.house;

import com.wandryhp.WandryHP;
import com.wandryhp.armor.HouseType;
import com.wandryhp.spell.DamageOverTime;
import net.fabricmc.fabric.api.attachment.v1.AttachmentRegistry;
import net.fabricmc.fabric.api.attachment.v1.AttachmentType;
import net.fabricmc.fabric.api.tag.convention.v2.ConventionalBlockTags;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.CropBlock;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.damage.DamageTypes;
import net.minecraft.entity.mob.Monster;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.registry.Registries;
import net.minecraft.registry.tag.BlockTags;
import net.minecraft.registry.tag.ItemTags;
import net.minecraft.screen.BlastFurnaceScreenHandler;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.stat.StatHandler;
import net.minecraft.stat.StatType;
import net.minecraft.stat.Stats;
import net.minecraft.util.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * Selecao de casas (somente servidor). Analisa o jogador desde a primeira entrada no mundo ate ele derrotar o
 * {@link HouseScoring#HOSTILE_KILLS_TO_FINISH}o mob hostil; nesse momento a analise termina e a casa de maior
 * pontuacao (empate: sorteio entre as empatadas) e gravada como resultado.
 *
 * <p>O progresso e um data attachment persistente do jogador (ver {@link HouseProgress}), salvo na pasta do mundo:
 * cada mundo tem progresso e resultado proprios. Nao ha estado estatico por jogador nesta classe.
 *
 * <p>Deteccao: acoes com evento proprio (abates, dano bloqueado, blocos quebrados, acordar) pontuam na hora; o que
 * o vanilla ja contabiliza em estatisticas (distancia, itens coletados/fabricados) e lido por diferenca de tempos
 * em tempos; comer, fabricar e retirar da fornalha vem de tres mixins pequenos que chamam os metodos "on..." daqui.
 */
public final class HouseSelection {
	private static final Logger LOGGER = LoggerFactory.getLogger(WandryHP.MOD_ID);

	public static final AttachmentType<HouseProgress> DATA = AttachmentRegistry.create(
			Identifier.of(WandryHP.MOD_ID, "house_progress"),
			builder -> builder
					.initializer(HouseProgress::fresh)
					.persistent(HouseProgress.CODEC));

	/** A cada quantos ticks as estatisticas e o relogio sao lidos (o sono e checado todo tick). */
	private static final int POLL_INTERVAL_TICKS = 10;

	/**
	 * Estatisticas vanilla de distancia HORIZONTAL que contam como "blocos percorridos": a pe, correndo, agachado,
	 * nadando, na agua, no ar (pulo/voo), elytra e montado/em veiculo. Ficam de fora FALL e CLIMB (distancia
	 * vertical). O vanilla ja nao conta teleporte e cada estatistica e por mundo.
	 */
	private static final Identifier[] TRAVEL_STATS = {
			Stats.WALK_ONE_CM, Stats.SPRINT_ONE_CM, Stats.CROUCH_ONE_CM,
			Stats.SWIM_ONE_CM, Stats.WALK_ON_WATER_ONE_CM, Stats.WALK_UNDER_WATER_ONE_CM,
			Stats.FLY_ONE_CM, Stats.AVIATE_ONE_CM,
			Stats.BOAT_ONE_CM, Stats.MINECART_ONE_CM, Stats.HORSE_ONE_CM, Stats.PIG_ONE_CM, Stats.STRIDER_ONE_CM
	};

	/** Itens valiosos da Sonserina. */
	private static final List<Item> VALUABLE_ITEMS = List.of(
			Items.DIAMOND, Items.EMERALD, Items.GOLD_INGOT, Items.NETHERITE_INGOT, Items.ANCIENT_DEBRIS);

	/** Animais passivos da Sonserina. */
	private static final Set<EntityType<?>> PASSIVE_ANIMALS = Set.of(
			EntityType.COW, EntityType.PIG, EntityType.SHEEP, EntityType.CHICKEN);

	/** Todos os itens com componente de comida (carregado so no primeiro uso, com os registros ja prontos). */
	private static final class FoodItems {
		static final List<Item> ALL = Registries.ITEM.stream()
				.filter(item -> item.getComponents().contains(DataComponentTypes.FOOD))
				.toList();
	}

	private HouseSelection() {}

	// ---------------------------------------------------------------- leitura (API publica)

	public static HouseProgress get(PlayerEntity player) {
		return player.getAttachedOrCreate(DATA);
	}

	/** true quando o jogador ja derrotou o 50o mob hostil neste mundo. */
	public static boolean isFinished(PlayerEntity player) {
		return get(player).finished();
	}

	/** Casa resultante neste mundo; vazio enquanto a analise nao terminou. */
	public static Optional<HouseType> getResult(PlayerEntity player) {
		return Optional.ofNullable(get(player).result());
	}

	public static double getScore(PlayerEntity player, HouseType house) {
		return get(player).score(house);
	}

	public static int getHostileKills(PlayerEntity player) {
		return get(player).hostileKills();
	}

	// ---------------------------------------------------------------- escrita basica

	/** Pontua uma casa. Ignorado depois que a analise terminou. */
	public static void addPoints(ServerPlayerEntity player, HouseType house, double points) {
		HouseProgress progress = get(player);
		if (!progress.finished() && points != 0) {
			player.setAttached(DATA, progress.withScoreAdded(house, points));
		}
	}

	private static HouseProgress add(HouseProgress progress, HouseType house, double points) {
		return points == 0 ? progress : progress.withScoreAdded(house, points);
	}

	// ---------------------------------------------------------------- tick (leituras periodicas)

	public static void tick(MinecraftServer server) {
		boolean fullPoll = server.getTicks() % POLL_INTERVAL_TICKS == 0;
		for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
			HouseProgress progress = get(player);
			if (progress.finished()) {
				if (fullPoll) {
					clearCampfireMarks(player);
				}
				continue;
			}
			trackSleep(player, progress);
			if (fullPoll) {
				poll(player);
			}
		}
	}

	/** Marca que o jogador dormiu o tempo suficiente (feito todo tick: a noite pode ser pulada logo em seguida). */
	private static void trackSleep(ServerPlayerEntity player, HouseProgress progress) {
		if (!progress.sleepArmed() && player.isSleeping() && player.getSleepTimer() >= HouseScoring.SLEEP_TICKS_REQUIRED) {
			player.setAttached(DATA, progress.withSleepArmed(true));
		}
	}

	/** Le exploracao, itens e relogio; grava uma vez so se algo mudou. */
	private static void poll(ServerPlayerEntity player) {
		HouseProgress before = get(player);
		if (before.finished()) {
			return;
		}
		clearCampfireMarks(player);
		HouseProgress progress = pollTravel(player, before);
		progress = pollItems(player, progress);
		progress = pollNight(player, progress);
		if (progress != before) {
			player.setAttached(DATA, progress);
		}
	}

	/** Itens marcados que chegaram ao jogador sem ser pela coleta do chao (funil, bau) nao pontuam e voltam a empilhar. */
	private static void clearCampfireMarks(ServerPlayerEntity player) {
		for (int slot = 0; slot < player.getInventory().size(); slot++) {
			CampfireCooking.clear(player.getInventory().getStack(slot));
		}
	}

	private static long travelledCm(StatHandler stats) {
		long total = 0;
		for (Identifier stat : TRAVEL_STATS) {
			total += stats.getStat(Stats.CUSTOM, stat);
		}
		return total;
	}

	private static long sum(StatHandler stats, StatType<Item> type, Collection<Item> items) {
		long total = 0;
		for (Item item : items) {
			total += stats.getStat(type, item);
		}
		return total;
	}

	/**
	 * Exploracao: credita o que o jogador percorreu desde a ultima leitura. Na primeira leitura (primeira entrada
	 * no mundo com este sistema) so grava a linha de base: nada do que veio antes conta.
	 */
	private static HouseProgress pollTravel(ServerPlayerEntity player, HouseProgress progress) {
		long total = travelledCm(player.getStatHandler());
		long last = progress.lastTravelCm();
		if (total == last) {
			return progress;
		}
		if (last < 0 || total < last) { // primeira leitura, ou estatisticas apagadas: so reajusta a base
			return progress.withLastTravelCm(total);
		}
		HouseProgress next = progress.withLastTravelCm(total);
		return player.isSpectator() ? next : next.withExploration(total - last);
	}

	/**
	 * Itens por diferenca das estatisticas vanilla: valiosos (coletados + fabricados/fundidos, Sonserina, por
	 * unidade) e alimentos coletados do chao (Lufa-Lufa, por unidade).
	 */
	private static HouseProgress pollItems(ServerPlayerEntity player, HouseProgress progress) {
		StatHandler stats = player.getStatHandler();
		long valuables = sum(stats, Stats.PICKED_UP, VALUABLE_ITEMS) + sum(stats, Stats.CRAFTED, VALUABLE_ITEMS);
		long food = sum(stats, Stats.PICKED_UP, FoodItems.ALL);

		HouseProgress next = progress;
		if (progress.lastValuables() >= 0 && valuables > progress.lastValuables()) {
			next = add(next, HouseScoring.SLYTHERIN, (valuables - progress.lastValuables()) * HouseScoring.VALUABLE_ITEM_POINTS);
		}
		if (progress.lastFoodPickups() >= 0 && food > progress.lastFoodPickups()) {
			next = add(next, HouseScoring.HUFFLEPUFF, (food - progress.lastFoodPickups()) * HouseScoring.FOOD_COLLECTED_POINTS);
		}
		if (valuables != progress.lastValuables() || food != progress.lastFoodPickups()) {
			next = next.withBaselines(valuables, food);
		}
		return pollEquipment(player, next);
	}

	/**
	 * Melhorar equipamento (Sonserina): +1 quando a maior categoria obtida supera a melhor ja registrada. A
	 * primeira leitura so grava a linha de base (o que o jogador ja tinha nao conta). Pular categorias vale +1 e
	 * a mesma categoria nunca pontua duas vezes, pois a melhor registrada so sobe.
	 */
	private static HouseProgress pollEquipment(ServerPlayerEntity player, HouseProgress progress) {
		int obtained = EquipmentTiers.highestObtained(player);
		int best = progress.bestEquipmentTier();
		if (best == HouseProgress.TIER_UNREAD) {
			return progress.withBestEquipmentTier(obtained);
		}
		if (obtained <= best) {
			return progress;
		}
		return add(progress.withBestEquipmentTier(obtained), HouseScoring.SLYTHERIN,
				HouseScoring.equipmentTierPoints(best, obtained));
	}

	/**
	 * Sobreviver a uma noite inteira: usa o relogio do Overworld (igual em qualquer dimensao). Arma ao ver o jogador
	 * vivo no comeco da noite; ao amanhecer (inclusive quando pulado dormindo) credita o ponto. Morrer ou entrar no
	 * mundo no meio da noite desarma (ver {@link #onJoin} e {@link #onEntityDeath}).
	 */
	private static HouseProgress pollNight(ServerPlayerEntity player, HouseProgress progress) {
		MinecraftServer server = player.getServer();
		if (server == null) {
			return progress;
		}
		long time = server.getOverworld().getTimeOfDay() % 24000L;
		if (progress.nightArmed()) {
			if (time < HouseScoring.NIGHT_START_TICK) {
				return add(progress.withNightArmed(false), HouseScoring.GRYFFINDOR, HouseScoring.SURVIVE_NIGHT_POINTS);
			}
		} else if (player.isAlive() && time >= HouseScoring.NIGHT_START_TICK && time < HouseScoring.NIGHT_ARM_END_TICK) {
			return progress.withNightArmed(true);
		}
		return progress;
	}

	// ---------------------------------------------------------------- eventos de conexao / sono / escudo

	/** Entrar no mundo no meio de uma noite ou de um sono nao conta como "noite inteira" / "dormiu". */
	public static void onJoin(ServerPlayerEntity player) {
		HouseProgress progress = get(player);
		if (!progress.finished() && (progress.nightArmed() || progress.sleepArmed())) {
			player.setAttached(DATA, progress.withNightArmed(false).withSleepArmed(false));
		}
	}

	/** Ao acordar: se dormiu o tempo suficiente, +0,5 para a Lufa-Lufa (uma vez). */
	public static void onWake(ServerPlayerEntity player) {
		HouseProgress progress = get(player);
		if (!progress.finished() && progress.sleepArmed()) {
			player.setAttached(DATA, add(progress.withSleepArmed(false), HouseScoring.HUFFLEPUFF, HouseScoring.SLEEP_POINTS));
		}
	}

	/** AFTER_DAMAGE: ataque bloqueado com escudo, +1 para a Grifinoria por ataque. */
	public static void onDamaged(LivingEntity entity, DamageSource source, float baseDamage, float damageTaken, boolean blocked) {
		if (blocked && entity instanceof ServerPlayerEntity player) {
			addPoints(player, HouseScoring.GRYFFINDOR, HouseScoring.SHIELD_BLOCK_POINTS);
		}
	}

	// ---------------------------------------------------------------- blocos

	/** Depois de quebrar um bloco: minerio (Sonserina + Corvinal), tronco e plantacao madura (Lufa-Lufa). */
	public static void onBlockBroken(ServerPlayerEntity player, BlockState state) {
		if (get(player).finished()) {
			return;
		}
		if (state.isIn(ConventionalBlockTags.ORES)) {
			addPoints(player, HouseScoring.SLYTHERIN, HouseScoring.ORE_MINED_SLYTHERIN_POINTS);
			addPoints(player, HouseScoring.RAVENCLAW, HouseScoring.ORE_MINED_RAVENCLAW_POINTS);
		}
		if (state.isIn(BlockTags.LOGS)) {
			addPoints(player, HouseScoring.HUFFLEPUFF, HouseScoring.LOG_BROKEN_POINTS);
		}
		if (isMatureFoodCrop(state)) {
			addPoints(player, HouseScoring.HUFFLEPUFF, HouseScoring.FOOD_COLLECTED_POINTS);
		}
	}

	private static boolean isMatureFoodCrop(BlockState state) {
		return (state.isOf(Blocks.WHEAT) || state.isOf(Blocks.CARROTS) || state.isOf(Blocks.POTATOES) || state.isOf(Blocks.BEETROOTS))
				&& state.getBlock() instanceof CropBlock crop && crop.isMature(state);
	}

	// ---------------------------------------------------------------- itens (chamados pelos mixins)

	/** ItemStackMixin: um alimento acabou de ser consumido. */
	public static void onFoodEaten(ServerPlayerEntity player) {
		addPoints(player, HouseScoring.HUFFLEPUFF, HouseScoring.EAT_POINTS);
	}

	/** CraftingResultSlotMixin: uma operacao de fabricacao (resultado retirado de bancada/grade de fabricacao). */
	public static void onItemCrafted(ServerPlayerEntity player, ItemStack result) {
		double points = HouseScoring.CRAFT_POINTS;
		if (result.isIn(ItemTags.PICKAXES) || result.isIn(ItemTags.SHOVELS) || result.isIn(ItemTags.AXES) || result.isIn(ItemTags.HOES)) {
			points += HouseScoring.TOOL_CRAFT_POINTS;
		}
		addPoints(player, HouseScoring.RAVENCLAW, points);
	}

	/**
	 * ItemEntityMixin: {@code count} itens cozidos na fogueira acabaram de ser coletados do chao pelo jogador
	 * (cada unidade chega aqui uma unica vez, pois a marca e retirada na coleta).
	 */
	public static void onCampfireItemsCollected(ServerPlayerEntity player, int count) {
		addPoints(player, HouseScoring.HUFFLEPUFF, count * HouseScoring.CAMPFIRE_COOK_POINTS);
	}

	/** FurnaceOutputSlotMixin: itens retirados prontos da fornalha ou defumador (o alto-forno nao conta). */
	public static void onFurnaceOutput(ServerPlayerEntity player, int count) {
		if (player.currentScreenHandler instanceof BlastFurnaceScreenHandler) {
			return;
		}
		addPoints(player, HouseScoring.HUFFLEPUFF, count * HouseScoring.COOK_ITEM_POINTS);
	}

	// ---------------------------------------------------------------- mortes

	/** Jogador que levou o credito pela morte: atacante direto (inclui feitico/prop) ou dono do Crucio ativo. */
	private static ServerPlayerEntity killerOf(LivingEntity victim, DamageSource source) {
		if (source.getAttacker() instanceof ServerPlayerEntity player) {
			return player;
		}
		if (source.isOf(DamageTypes.MAGIC)
				&& DamageOverTime.ownerOf(victim) instanceof ServerPlayerEntity player && !player.isRemoved()) {
			return player;
		}
		return null;
	}

	/** AFTER_DEATH. */
	public static void onEntityDeath(LivingEntity entity, DamageSource source) {
		if (entity instanceof ServerPlayerEntity dead) {
			// Morrer cancela a noite e o sono em andamento.
			HouseProgress progress = get(dead);
			if (!progress.finished() && (progress.nightArmed() || progress.sleepArmed())) {
				dead.setAttached(DATA, progress.withNightArmed(false).withSleepArmed(false));
			}
			return;
		}
		ServerPlayerEntity killer = killerOf(entity, source);
		if (killer == null) {
			return;
		}
		if (entity instanceof Monster) {
			registerHostileKill(killer, entity);
		} else if (PASSIVE_ANIMALS.contains(entity.getType())) {
			addPoints(killer, HouseScoring.SLYTHERIN, HouseScoring.PASSIVE_ANIMAL_KILL_POINTS);
		}
	}

	private static void registerHostileKill(ServerPlayerEntity player, LivingEntity victim) {
		if (get(player).finished()) {
			return;
		}
		// Fecha leituras pendentes antes de encerrar a analise.
		poll(player);

		HouseProgress progress = get(player);
		progress = progress.withHostileKills(progress.hostileKills() + 1);
		progress = add(progress, HouseScoring.GRYFFINDOR, HouseScoring.HOSTILE_KILL_POINTS);

		String typeId = Registries.ENTITY_TYPE.getId(victim.getType()).toString();
		if (!progress.hasKilledType(typeId)) {
			progress = add(progress.withKilledType(typeId), HouseScoring.GRYFFINDOR, HouseScoring.FIRST_KILL_OF_TYPE_POINTS);
		}

		if (progress.hostileKills() >= HouseScoring.HOSTILE_KILLS_TO_FINISH) {
			HouseType result = HouseScoring.winner(progress::score, bound -> player.getRandom().nextInt(bound));
			progress = progress.finishedWith(result);
			LOGGER.info("Selecao de casas de {}: {} (G {} / S {} / H {} / R {})", player.getName().getString(), result.id,
					progress.score(HouseType.GRYFFINDOR), progress.score(HouseType.SLYTHERIN),
					progress.score(HouseType.HUFFLEPUFF), progress.score(HouseType.RAVENCLAW));
		}
		player.setAttached(DATA, progress);
	}
}
