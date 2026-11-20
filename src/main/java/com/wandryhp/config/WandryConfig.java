package com.wandryhp.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.wandryhp.WandryHP;
import com.wandryhp.item.WandType;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Parametros configuraveis (config/wandryhp.json). Criado com os padroes na primeira execucao.
 *
 * <p>Dano padrao: 2.0 (1 coracao) para todas as varinhas.
 */
public class WandryConfig {
	private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

	public String _notes = "damage em pontos de vida (2.0 = 1 coracao). Duracoes em ticks (20 = 1s). "
			+ "A Confusao esta registrada mas nao e usada por nenhuma varinha por enquanto.";

	/** Intervalo minimo entre lancamentos (30 ticks = 1,5 s). */
	public int cooldownTicks = 30;
	/** Velocidade do prop em blocos/tick. */
	public double projectileSpeed = 1.5;
	/** Tempo de vida maximo do prop em ticks. */
	public int maxLifetimeTicks = 60;
	/** Multiplicador visual do modelo do prop (os modelos originais sao muito pequenos). */
	public float propScale = 2.0f;
	/** Confusao tambem inverte pular <-> agachar. */
	public boolean confusionSwapJumpSneak = true;

	public Map<String, WandSettings> wands = new LinkedHashMap<>();

	public static class WandSettings {
		/** Dano ao atingir, em pontos de vida (2.0 = 1 coracao). */
		public float damage = 2.0f;
		/**
		 * Duracao do efeito principal em ticks.
		 * Amethyst: Lentidao. Bone: Cegueira. Celestial: Brilho (Glowing). Widow: Lentidao.
		 */
		public int effectDurationTicks = 0;
		/** Amplificador do efeito principal (0 = nivel I, 1 = nivel II). */
		public int effectAmplifier = 0;
		/** Segundos de fogo aplicados ao alvo (usado pela Widow). */
		public int fireSeconds = 0;

		WandSettings() {}

		WandSettings(int effectDurationTicks, int effectAmplifier, int fireSeconds) {
			this.effectDurationTicks = effectDurationTicks;
			this.effectAmplifier = effectAmplifier;
			this.fireSeconds = fireSeconds;
		}
	}

	public WandSettings settings(WandType type) {
		return wands.computeIfAbsent(type.id, k -> defaultsFor(type));
	}

	private static WandSettings defaultsFor(WandType type) {
		return switch (type) {
			case AMETHYST -> new WandSettings(20, 1, 0);   // Lentidao II por 1 s
			case BONE -> new WandSettings(20, 0, 0);       // Cegueira por 1 s
			case CELESTIAL -> new WandSettings(200, 0, 0); // Brilho (Glowing) por 10 s
			case WIDOW -> new WandSettings(40, 0, 2);      // Fogo + Lentidao por 2 s (nivel da Lentidao nao especificado)
		};
	}

	public static WandryConfig loadOrCreate() {
		Path path = FabricLoader.getInstance().getConfigDir().resolve(WandryHP.MOD_ID + ".json");
		WandryConfig config = null;
		if (Files.exists(path)) {
			try (Reader reader = Files.newBufferedReader(path)) {
				config = GSON.fromJson(reader, WandryConfig.class);
			} catch (Exception e) {
				System.err.println("[wandryhp] Falha ao ler " + path + ", usando padroes: " + e);
			}
		}
		if (config == null) {
			config = new WandryConfig();
		}
		for (WandType type : WandType.values()) {
			config.settings(type); // preenche entradas ausentes com os padroes
		}
		try (Writer writer = Files.newBufferedWriter(path)) {
			GSON.toJson(config, writer);
		} catch (IOException e) {
			System.err.println("[wandryhp] Falha ao gravar " + path + ": " + e);
		}
		return config;
	}
}
