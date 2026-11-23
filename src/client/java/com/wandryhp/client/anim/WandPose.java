package com.wandryhp.client.anim;

/**
 * Pose da conjuração em radianos, SEMPRE relativa ao repouso (tudo zero = nenhum efeito). Assim o início e o fim
 * das animações coincidem exatamente com a pose normal e não há saltos.
 *
 * <p>Convenção (mesma em primeira e terceira pessoa, o lado da mão é aplicado por quem usa a pose):
 * <ul>
 *   <li>{@code shoulderPitch}: levanta o braço à frente (+ = sobe);</li>
 *   <li>{@code shoulderYaw}: varre o braço lateralmente (+ = para o centro do corpo);</li>
 *   <li>{@code shoulderRoll}: leve giro do braço;</li>
 *   <li>{@code elbow}: flexão do antebraço (+ = dobra para cima);</li>
 *   <li>{@code wristYaw}: movimento lateral do pulso (+ = para o centro);</li>
 *   <li>{@code wristPitch}: ponta da varinha sobe (+) / desce (-);</li>
 *   <li>{@code wristRoll}: giro do pulso;</li>
 *   <li>{@code wandYaw}: chicote lateral extra da varinha, o movimento mais acentuado (+ = para o centro).</li>
 * </ul>
 *
 * <p>As animações são tabelas de quadros-chave interpoladas por splines cúbicas de Hermite (tangentes de
 * Catmull-Rom, velocidade zero no início e no fim): a velocidade é contínua em todos os instantes, sem movimento
 * robótico. O pulso e a varinha usam quadros-chave em tempos diferentes do braço, então a ponta "chicoteia" depois
 * do ombro, como num gesto real.
 */
public record WandPose(float shoulderPitch, float shoulderYaw, float shoulderRoll, float elbow,
					   float wristYaw, float wristPitch, float wristRoll, float wandYaw) {
	public static final WandPose ZERO = new WandPose(0, 0, 0, 0, 0, 0, 0, 0);

	public WandPose scale(float k) {
		return new WandPose(shoulderPitch * k, shoulderYaw * k, shoulderRoll * k, elbow * k,
				wristYaw * k, wristPitch * k, wristRoll * k, wandYaw * k);
	}

	public WandPose plus(WandPose o) {
		return new WandPose(shoulderPitch + o.shoulderPitch, shoulderYaw + o.shoulderYaw,
				shoulderRoll + o.shoulderRoll, elbow + o.elbow, wristYaw + o.wristYaw,
				wristPitch + o.wristPitch, wristRoll + o.wristRoll, wandYaw + o.wandYaw);
	}

	// ----------------------------------------------------------------------------------------------- CONJURAÇÃO
	// t normalizado 0..1 (24 ticks = 1,2 s). Fases: sobe (0-.30) -> arma o pulso (.30-.56) -> chicote (.56-.73)
	// -> continua naturalmente (.73-.90) -> relaxa (.90-1).
	private static final float[] CAST_T = {0f, .14f, .30f, .46f, .56f, .66f, .78f, .90f, 1f};
	private static final float[] CAST_SH_PITCH = {0f, .22f, .62f, .78f, .80f, .72f, .45f, .14f, 0f};
	private static final float[] CAST_ELBOW = {0f, .15f, .50f, .62f, .55f, .35f, .22f, .07f, 0f};
	private static final float[] CAST_SH_YAW = {0f, -.04f, -.12f, -.22f, -.20f, .28f, .34f, .12f, 0f};
	private static final float[] CAST_SH_ROLL = {0f, 0f, -.02f, -.05f, -.04f, .06f, .08f, .03f, 0f};
	private static final float[] CAST_WRIST_YAW = {0f, -.05f, -.20f, -.45f, -.50f, .30f, .45f, .16f, 0f};
	private static final float[] CAST_WRIST_ROLL = {0f, 0f, -.08f, -.15f, -.22f, .12f, .25f, .06f, 0f};
	private static final float[] CAST_WRIST_PITCH = {0f, .03f, .10f, .16f, .22f, -.10f, -.25f, -.05f, 0f};
	// chicote da varinha: mais amplo e com tempos próprios (armar -> estalo -> pico -> assentar)
	private static final float[] WAND_T = {0f, .30f, .46f, .56f, .66f, .73f, .82f, .92f, 1f};
	private static final float[] WAND_YAW = {0f, -.12f, -.40f, -.72f, .20f, .95f, .55f, .12f, 0f};

	// ----------------------------------------------------------------------------------------------- LANÇAMENTO
	// Curto "empurrão" de soltura, tocado no instante em que o servidor cria o projétil.
	private static final float[] LAUNCH_T = {0f, .35f, 1f};
	private static final float[] LAUNCH_SH_PITCH = {0f, .18f, 0f};
	private static final float[] LAUNCH_ELBOW = {0f, -.12f, 0f};
	private static final float[] LAUNCH_WRIST_PITCH = {0f, .25f, 0f};
	private static final float[] LAUNCH_WRIST_YAW = {0f, .10f, 0f};
	private static final float[] LAUNCH_WAND_YAW = {0f, .22f, 0f};

	/** Pose da animação de conjuração em {@code t} (0..1). */
	public static WandPose cast(float t) {
		t = clamp01(t);
		return new WandPose(
				spline(CAST_T, CAST_SH_PITCH, t), spline(CAST_T, CAST_SH_YAW, t), spline(CAST_T, CAST_SH_ROLL, t),
				spline(CAST_T, CAST_ELBOW, t), spline(CAST_T, CAST_WRIST_YAW, t), spline(CAST_T, CAST_WRIST_PITCH, t),
				spline(CAST_T, CAST_WRIST_ROLL, t), spline(WAND_T, WAND_YAW, t));
	}

	/** Pose da animação de lançamento em {@code t} (0..1). */
	public static WandPose launch(float t) {
		t = clamp01(t);
		return new WandPose(spline(LAUNCH_T, LAUNCH_SH_PITCH, t), 0, 0, spline(LAUNCH_T, LAUNCH_ELBOW, t),
				spline(LAUNCH_T, LAUNCH_WRIST_YAW, t), spline(LAUNCH_T, LAUNCH_WRIST_PITCH, t), 0,
				spline(LAUNCH_T, LAUNCH_WAND_YAW, t));
	}

	/** Suavização 0..1 com velocidade e aceleração zero nas pontas. */
	public static float smootherstep(float x) {
		x = clamp01(x);
		return x * x * x * (x * (x * 6f - 15f) + 10f);
	}

	private static float clamp01(float x) {
		return x < 0f ? 0f : Math.min(x, 1f);
	}

	/** Spline cúbica de Hermite por quadros-chave; tangentes de Catmull-Rom, zero nas duas pontas. */
	private static float spline(float[] ts, float[] vs, float t) {
		int n = ts.length;
		if (t <= ts[0]) return vs[0];
		if (t >= ts[n - 1]) return vs[n - 1];
		int i = 0;
		while (t > ts[i + 1]) i++;
		float t0 = ts[i], t1 = ts[i + 1], h = t1 - t0;
		float m0 = tangent(ts, vs, i), m1 = tangent(ts, vs, i + 1);
		float s = (t - t0) / h, s2 = s * s, s3 = s2 * s;
		return (2 * s3 - 3 * s2 + 1) * vs[i] + (s3 - 2 * s2 + s) * h * m0
				+ (-2 * s3 + 3 * s2) * vs[i + 1] + (s3 - s2) * h * m1;
	}

	private static float tangent(float[] ts, float[] vs, int i) {
		if (i == 0 || i == ts.length - 1) return 0f;
		return (vs[i + 1] - vs[i - 1]) / (ts[i + 1] - ts[i - 1]);
	}
}
