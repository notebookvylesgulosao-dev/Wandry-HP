package com.wandryhp.edwiges;

/** Parametros da entrega da carta pela Edwiges (distancias em blocos, tempos em ticks; 20 ticks = 1 s). */
public final class EdwigesSettings {
	private EdwigesSettings() {}

	/** Raio maximo (3D) ao redor do jogador em que a Edwiges aparece. */
	public static final double SPAWN_RADIUS = 100.0;
	/** Altura minima acima do chao da coluna onde ela aparece. */
	public static final double MIN_ALTITUDE_ABOVE_GROUND = 80.0;

	/** 2 blocos por segundo. */
	public static final double SPEED_PER_TICK = 2.0 / 20.0;
	/** Ao chegar a esta distancia do jogador ela usa "pouso" e solta o envelope. */
	public static final double DELIVERY_DISTANCE = 5.0;
	/** Instante da animacao "pouso" em que as garras soltam o envelope (3,0 s: quando as patas se abrem). */
	public static final int DROP_TICK = 60;
	/** Duracao de "pouso" se o modelo nao puder ser lido no servidor (o arquivo diz 3,5 s). */
	public static final int FALLBACK_LANDING_TICKS = 70;

	/** Alem desta distancia (sem ver o jogador por perto) ela e considerada fora do campo de visao. */
	public static final double VISIBLE_RANGE = 128.0;
	/**
	 * Meio-angulo do cone de visao usado no servidor (nao conhece o FOV do cliente): propositalmente maior que
	 * qualquer FOV real, para que ela nunca suma enquanto ainda pode ser vista.
	 */
	public static final double VIEW_HALF_ANGLE_DEGREES = 100.0;
	/** Se o jogador a deixar para tras alem disto, ela e removida e uma nova aparece perto dele. */
	public static final double ABANDON_DISTANCE = 160.0;
	/** Teto de vida (ticks) por seguranca: 20 min. */
	public static final int MAX_LIFETIME_TICKS = 20 * 60 * 20;
	/** Espera antes de uma nova Edwiges se a anterior sumiu sem entregar a carta. */
	public static final int RETRY_DELAY_TICKS = 200;

	/** Giro maximo do corpo por tick (graus) e inclinacao visual maxima. */
	public static final float MAX_TURN_PER_TICK = 12.0f;
	public static final float MAX_VISUAL_PITCH = 25.0f;
}
