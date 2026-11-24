package com.wandryhp.house;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.wandryhp.armor.HouseType;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Progresso da selecao de casas de um jogador em UM mundo (data attachment persistente: e gravado junto com o
 * jogador, na pasta do proprio mundo). Imutavel de proposito: toda alteracao cria um novo valor, gravado com
 * setAttached.
 *
 * @param hostileKills    mobs hostis derrotados desde a primeira entrada neste mundo
 * @param exploredCm      distancia percorrida (cm) contabilizada para exploracao
 * @param lastTravelCm    ultimo total das estatisticas de distancia lido (-1 = ainda nao lido): linha de base
 * @param lastValuables   idem, para itens valiosos obtidos (coletados + fabricados/fundidos)
 * @param lastFoodPickups idem, para alimentos coletados do chao
 * @param scores          pontos por casa (chave = id da casa)
 * @param finished        true depois do 50o mob hostil: a analise terminou e o resultado nao muda mais
 * @param house           id da casa resultante ("" enquanto a analise nao terminou)
 * @param killedTypes     ids dos tipos de mob hostil ja derrotados (para o ponto de "primeira vez")
 * @param nightArmed      o jogador esta vivendo uma noite desde o comeco; ao amanhecer ganha o ponto
 * @param sleepArmed      o jogador dormiu o tempo suficiente; ao acordar ganha o ponto
 * @param bestEquipmentTier melhor categoria de equipamento ja obtida (ver {@link EquipmentTiers}):
 *                        {@link #TIER_UNREAD} = ainda nao lida (linha de base), -1 = nenhum, 0..5 = madeira..netherite
 */
public record HouseProgress(int hostileKills, long exploredCm, long lastTravelCm, long lastValuables,
							long lastFoodPickups, Map<String, Double> scores, boolean finished, String house,
							List<String> killedTypes, boolean nightArmed, boolean sleepArmed,
							int bestEquipmentTier) {
	public static final int TIER_UNREAD = -2;
	public static final int TIER_NONE = -1;

	public static final Codec<HouseProgress> CODEC = RecordCodecBuilder.create(instance -> instance.group(
			Codec.INT.optionalFieldOf("hostile_kills", 0).forGetter(HouseProgress::hostileKills),
			Codec.LONG.optionalFieldOf("explored_cm", 0L).forGetter(HouseProgress::exploredCm),
			Codec.LONG.optionalFieldOf("last_travel_cm", -1L).forGetter(HouseProgress::lastTravelCm),
			Codec.LONG.optionalFieldOf("last_valuables", -1L).forGetter(HouseProgress::lastValuables),
			Codec.LONG.optionalFieldOf("last_food_pickups", -1L).forGetter(HouseProgress::lastFoodPickups),
			Codec.unboundedMap(Codec.STRING, Codec.DOUBLE).optionalFieldOf("scores", Map.of()).forGetter(HouseProgress::scores),
			Codec.BOOL.optionalFieldOf("finished", false).forGetter(HouseProgress::finished),
			Codec.STRING.optionalFieldOf("house", "").forGetter(HouseProgress::house),
			Codec.STRING.listOf().optionalFieldOf("killed_types", List.of()).forGetter(HouseProgress::killedTypes),
			Codec.BOOL.optionalFieldOf("night_armed", false).forGetter(HouseProgress::nightArmed),
			Codec.BOOL.optionalFieldOf("sleep_armed", false).forGetter(HouseProgress::sleepArmed),
			Codec.INT.optionalFieldOf("best_equipment_tier", TIER_UNREAD).forGetter(HouseProgress::bestEquipmentTier)
	).apply(instance, HouseProgress::new));

	public HouseProgress {
		scores = Map.copyOf(scores);
		killedTypes = List.copyOf(killedTypes);
		house = house == null ? "" : house;
	}

	public static HouseProgress fresh() {
		return new HouseProgress(0, 0L, -1L, -1L, -1L, Map.of(), false, "", List.of(), false, false, TIER_UNREAD);
	}

	public double score(HouseType type) {
		return scores.getOrDefault(type.id, 0.0);
	}

	/** Casa resultante, ou null se a analise ainda nao terminou. */
	public HouseType result() {
		for (HouseType type : HouseType.values()) {
			if (type.id.equals(house)) {
				return type;
			}
		}
		return null;
	}

	public boolean hasKilledType(String typeId) {
		return killedTypes.contains(typeId);
	}

	public HouseProgress withScoreAdded(HouseType type, double points) {
		Map<String, Double> copy = new HashMap<>(scores);
		copy.merge(type.id, points, Double::sum);
		return new HouseProgress(hostileKills, exploredCm, lastTravelCm, lastValuables, lastFoodPickups, copy,
				finished, house, killedTypes, nightArmed, sleepArmed, bestEquipmentTier);
	}

	public HouseProgress withHostileKills(int kills) {
		return new HouseProgress(kills, exploredCm, lastTravelCm, lastValuables, lastFoodPickups, scores,
				finished, house, killedTypes, nightArmed, sleepArmed, bestEquipmentTier);
	}

	public HouseProgress withKilledType(String typeId) {
		List<String> copy = new ArrayList<>(killedTypes);
		copy.add(typeId);
		return new HouseProgress(hostileKills, exploredCm, lastTravelCm, lastValuables, lastFoodPickups, scores,
				finished, house, copy, nightArmed, sleepArmed, bestEquipmentTier);
	}

	public HouseProgress withLastTravelCm(long cm) {
		return new HouseProgress(hostileKills, exploredCm, cm, lastValuables, lastFoodPickups, scores,
				finished, house, killedTypes, nightArmed, sleepArmed, bestEquipmentTier);
	}

	public HouseProgress withBaselines(long valuables, long foodPickups) {
		return new HouseProgress(hostileKills, exploredCm, lastTravelCm, valuables, foodPickups, scores,
				finished, house, killedTypes, nightArmed, sleepArmed, bestEquipmentTier);
	}

	public HouseProgress withBestEquipmentTier(int tier) {
		return new HouseProgress(hostileKills, exploredCm, lastTravelCm, lastValuables, lastFoodPickups, scores,
				finished, house, killedTypes, nightArmed, sleepArmed, tier);
	}

	public HouseProgress withNightArmed(boolean armed) {
		return new HouseProgress(hostileKills, exploredCm, lastTravelCm, lastValuables, lastFoodPickups, scores,
				finished, house, killedTypes, armed, sleepArmed, bestEquipmentTier);
	}

	public HouseProgress withSleepArmed(boolean armed) {
		return new HouseProgress(hostileKills, exploredCm, lastTravelCm, lastValuables, lastFoodPickups, scores,
				finished, house, killedTypes, nightArmed, armed, bestEquipmentTier);
	}

	/** Soma {@code deltaCm} a distancia explorada e credita os pontos dos novos blocos de 20 completados. */
	public HouseProgress withExploration(long deltaCm) {
		long after = exploredCm + deltaCm;
		double points = HouseScoring.explorationPoints(exploredCm, after);
		HouseProgress next = new HouseProgress(hostileKills, after, lastTravelCm, lastValuables, lastFoodPickups,
				scores, finished, house, killedTypes, nightArmed, sleepArmed, bestEquipmentTier);
		return points > 0 ? next.withScoreAdded(HouseScoring.RAVENCLAW, points) : next;
	}

	public HouseProgress finishedWith(HouseType result) {
		return new HouseProgress(hostileKills, exploredCm, lastTravelCm, lastValuables, lastFoodPickups, scores,
				true, result.id, killedTypes, nightArmed, sleepArmed, bestEquipmentTier);
	}
}
