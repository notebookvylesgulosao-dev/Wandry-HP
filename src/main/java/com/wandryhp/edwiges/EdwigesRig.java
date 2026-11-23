package com.wandryhp.edwiges;

import com.wandryhp.WandryHP;
import com.wandryhp.edwiges.model.Affine;
import com.wandryhp.edwiges.model.BbModel;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

/**
 * Modelos da Edwiges e dos envelopes (lidos dos .bbmodel originais, no servidor e no cliente) e a regra de
 * ONDE o envelope fica preso: sempre no fundo do corpo (osso "Owl"), pendurado pela borda de cima, com a
 * face do brasao virada para a frente da coruja. Servidor (ponto de soltura) e cliente (desenho) usam
 * exatamente a mesma conta.
 */
public final class EdwigesRig {
	private EdwigesRig() {}

	public static final String ANIM_FLY = "fly";
	public static final String ANIM_POUSO = "pouso";
	public static final String ANIM_IDLE = "idle";

	/** Osso do corpo da coruja, ao qual o envelope fica preso. */
	public static final String BODY_BONE = "Owl";
	/** Quanto (px) a borda de cima do envelope entra no corpo/garras, para parecer seguro. */
	public static final double GRIP_OVERLAP_PX = 1.0;
	/** Escala do modelo: 1 px do modelo = 1/16 de bloco (tamanho original, sem redesenhar). */
	public static final double PX = 1.0 / 16.0;

	private static final String BASE = "/assets/" + WandryHP.MOD_ID + "/bbmodels/edwiges/";
	private static final Map<String, BbModel> CACHE = new HashMap<>();

	/** Modelo animado da Edwiges, ou null se nao puder ser lido. */
	public static BbModel owl() {
		return load("edwiges.bbmodel");
	}

	/** Chave do envelope da casa: o id da casa se existir modelo proprio, senao "generic". */
	public static String letterKey(String houseId) {
		return houseId != null && load("letters/" + houseId + ".bbmodel") != null ? houseId : "generic";
	}

	public static BbModel letter(String houseId) {
		String key = letterKey(houseId);
		return load("generic".equals(key) ? "envelope.bbmodel" : "letters/" + key + ".bbmodel");
	}

	private static synchronized BbModel load(String file) {
		if (CACHE.containsKey(file)) return CACHE.get(file);
		BbModel model = null;
		try (InputStream in = EdwigesRig.class.getResourceAsStream(BASE + file)) {
			if (in != null) {
				try (Reader reader = new InputStreamReader(in, StandardCharsets.UTF_8)) {
					model = BbModel.parse(reader);
				}
			} else {
				System.err.println("[wandryhp] bbmodel nao encontrado: " + BASE + file);
			}
		} catch (Exception e) {
			System.err.println("[wandryhp] Falha ao ler " + BASE + file + ": " + e);
		}
		CACHE.put(file, model);
		return model;
	}

	/** Nome da animacao para o id sincronizado (0 fly, 1 pouso, 2 idle). */
	public static String animationName(int id) {
		return id == 1 ? ANIM_POUSO : id == 2 ? ANIM_IDLE : ANIM_FLY;
	}

	/** Duracao (ticks) da animacao "pouso" no arquivo. */
	public static int landingTicks() {
		BbModel owl = owl();
		BbModel.Animation a = owl == null ? null : owl.animation(ANIM_POUSO);
		return a == null || a.length <= 0 ? EdwigesSettings.FALLBACK_LANDING_TICKS : (int) Math.round(a.length * 20.0);
	}

	/**
	 * Matriz (coordenadas do arquivo do envelope -> espaco do modelo da coruja, em px): centra o envelope e o
	 * pendura sob o fundo do corpo, seguindo a matriz do osso do corpo na pose atual.
	 */
	public static Affine letterBase(BbModel owl, BbModel letter, Affine bodyBone) {
		int body = owl.boneIndex(BODY_BONE);
		double[] ob = owl.bounds(body);
		double ax = (ob[0] + ob[3]) / 2.0, aTopY = ob[1] + GRIP_OVERLAP_PX, az = (ob[2] + ob[5]) / 2.0;
		double[] lb = letter.bounds(-1);
		double cx = (lb[0] + lb[3]) / 2.0, cy = (lb[1] + lb[4]) / 2.0, cz = (lb[2] + lb[5]) / 2.0;
		double h = lb[4] - lb[1];
		return bodyBone.mul(Affine.translation(ax, aTopY - h / 2.0, az)).mul(Affine.translation(-cx, -cy, -cz));
	}

	/** Centro do envelope preso (px, espaco do modelo): onde ele aparece e de onde cai quando solto. */
	public static double[] letterCenterModel(BbModel owl, BbModel letter, Affine bodyBone) {
		double[] lb = letter.bounds(-1);
		double[] out = new double[3];
		letterBase(owl, letter, bodyBone).apply((lb[0] + lb[3]) / 2.0, (lb[1] + lb[4]) / 2.0, (lb[2] + lb[5]) / 2.0, out);
		return out;
	}

	/**
	 * Deslocamento (blocos) do centro do envelope em relacao a posicao da entidade, no mundo, para a
	 * animacao/instante dados e a rotacao da coruja. Mesma ordem de rotacoes do renderizador: Ry(180 - yaw) * Rx(-pitch).
	 */
	public static double[] letterWorldOffset(String houseId, String animation, double seconds, boolean loop, float yaw, float pitch) {
		BbModel owl = owl();
		BbModel letter = letter(houseId);
		double[] p;
		if (owl == null || letter == null) {
			p = new double[] {0, 4, 0}; // sem modelo no servidor: abaixo do corpo, aproximado
		} else {
			Affine[] pose = owl.pose(owl.animation(animation), seconds, loop);
			p = letterCenterModel(owl, letter, pose[owl.boneIndex(BODY_BONE)]);
		}
		double x = p[0] * PX, y = p[1] * PX, z = p[2] * PX;
		double a = Math.toRadians(-pitch);
		double y2 = y * Math.cos(a) - z * Math.sin(a), z2 = y * Math.sin(a) + z * Math.cos(a);
		double t = Math.toRadians(180.0 - yaw);
		return new double[] {x * Math.cos(t) + z2 * Math.sin(t), y2, -x * Math.sin(t) + z2 * Math.cos(t)};
	}
}
