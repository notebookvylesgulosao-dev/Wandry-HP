package com.wandryhp.house;

import com.wandryhp.armor.HouseType;

import java.util.ArrayList;
import java.util.List;
import java.util.function.IntUnaryOperator;
import java.util.function.ToDoubleFunction;

/**
 * Regras da selecao de casas, juntas num so lugar. Sem dependencia do Minecraft (so logica pura).
 * Todos os valores vem da tabela de criterios definida no pedido; o que e interpretacao esta marcado como "Assumido".
 * Acoes contaveis pontuam por quantidade realizada (por bloco, por item, por animal...).
 */
public final class HouseScoring {
	/** A analise termina ao derrotar o 50o mob hostil. */
	public static final int HOSTILE_KILLS_TO_FINISH = 50;

	// ---------------------------------------------------------------- Grifinoria
	public static final HouseType GRYFFINDOR = HouseType.GRYFFINDOR;
	public static final double HOSTILE_KILL_POINTS = 1.0;
	/** Uma vez por tipo de mob hostil, alem do ponto normal do abate. */
	public static final double FIRST_KILL_OF_TYPE_POINTS = 1.0;
	/** Conta ao amanhecer, mesmo que o jogador tenha dormido; morrer na noite anula. */
	public static final double SURVIVE_NIGHT_POINTS = 1.0;
	/** Por ataque bloqueado com escudo. */
	public static final double SHIELD_BLOCK_POINTS = 1.0;
	/** Assumido: a noite comeca em 12542 (hora em que o vanilla permite dormir). */
	public static final long NIGHT_START_TICK = 12542L;
	/** Assumido: so "viveu a noite inteira" quem estava presente no comeco dela (janela de entrada na noite). */
	public static final long NIGHT_ARM_END_TICK = 13000L;

	// ---------------------------------------------------------------- Sonserina
	public static final HouseType SLYTHERIN = HouseType.SLYTHERIN;
	/** Por minerio quebrado. */
	public static final double ORE_MINED_SLYTHERIN_POINTS = 1.0;
	/** Por unidade de item valioso (diamante, esmeralda, lingote de ouro, lingote de netherite, ancient debris). */
	public static final double VALUABLE_ITEM_POINTS = 1.0;
	/** Por vaca, porco, ovelha ou galinha morta. */
	public static final double PASSIVE_ANIMAL_KILL_POINTS = 0.5;

	/** Por categoria de equipamento (ferramenta, arma ou armadura) superior a melhor ja obtida; pular categorias vale so isto. */
	public static final double EQUIPMENT_TIER_UP_POINTS = 1.0;

	// ---------------------------------------------------------------- Lufa-Lufa
	public static final HouseType HUFFLEPUFF = HouseType.HUFFLEPUFF;
	/** Por alimento consumido. */
	public static final double EAT_POINTS = 1.0;
	/** Por item retirado pronto da fornalha ou defumador. */
	public static final double COOK_ITEM_POINTS = 0.5;
	/** Por item cozido numa fogueira e coletado do chao (cada item conta uma unica vez). */
	public static final double CAMPFIRE_COOK_POINTS = 0.5;
	/** Uma vez ao acordar. */
	public static final double SLEEP_POINTS = 0.5;
	/** Assumido: so conta o sono em que o jogador ficou deitado o tempo vanilla de "dormiu de verdade" (100 ticks). */
	public static final int SLEEP_TICKS_REQUIRED = 100;
	/** Por bloco de tronco quebrado. */
	public static final double LOG_BROKEN_POINTS = 0.5;
	/** Por plantacao madura colhida e por unidade de alimento coletada do chao. */
	public static final double FOOD_COLLECTED_POINTS = 0.5;

	// ---------------------------------------------------------------- Corvinal
	public static final HouseType RAVENCLAW = HouseType.RAVENCLAW;
	public static final int EXPLORATION_BLOCKS_PER_STEP = 20;
	public static final double EXPLORATION_POINTS_PER_STEP = 0.5;
	/** Por operacao de fabricacao (cada vez que o resultado e retirado de uma bancada de fabricacao). */
	public static final double CRAFT_POINTS = 0.5;
	/** Por minerio quebrado (a mesma quebra tambem pontua a Sonserina). */
	public static final double ORE_MINED_RAVENCLAW_POINTS = 0.5;
	/** Picareta, pa, machado ou enxada fabricados (somado ao ponto de fabricar item). */
	public static final double TOOL_CRAFT_POINTS = 0.5;

	private static final long CM_PER_STEP = EXPLORATION_BLOCKS_PER_STEP * 100L;

	private HouseScoring() {}

	/** Pontos de exploracao ganhos ao ir de {@code cmBefore} a {@code cmAfter} centimetros percorridos no total. */
	public static double explorationPoints(long cmBefore, long cmAfter) {
		return (cmAfter / CM_PER_STEP - cmBefore / CM_PER_STEP) * EXPLORATION_POINTS_PER_STEP;
	}

	/**
	 * Pontos de melhoria de equipamento: {@link #EQUIPMENT_TIER_UP_POINTS} se {@code obtainedTier} for maior que
	 * {@code bestTier}, senao 0 (nunca acumula categorias puladas nem repete a mesma categoria).
	 */
	public static double equipmentTierPoints(int bestTier, int obtainedTier) {
		return obtainedTier > bestTier ? EQUIPMENT_TIER_UP_POINTS : 0.0;
	}

	/**
	 * Casa de maior pontuacao; em empate, sorteia entre as empatadas.
	 *
	 * @param randomIndex devolve um indice aleatorio em [0, n) para n empatadas
	 */
	public static HouseType winner(ToDoubleFunction<HouseType> score, IntUnaryOperator randomIndex) {
		double best = Double.NEGATIVE_INFINITY;
		for (HouseType type : HouseType.values()) {
			best = Math.max(best, score.applyAsDouble(type));
		}
		List<HouseType> tied = new ArrayList<>();
		for (HouseType type : HouseType.values()) {
			if (Math.abs(score.applyAsDouble(type) - best) < 1.0E-9) {
				tied.add(type);
			}
		}
		return tied.size() == 1 ? tied.get(0) : tied.get(randomIndex.applyAsInt(tied.size()));
	}
}
