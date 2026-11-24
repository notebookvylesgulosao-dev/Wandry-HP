package com.wandryhp.client.edwiges;

import com.wandryhp.edwiges.EdwigesHudPayload;
import net.minecraft.util.Util;

/**
 * Estado e animacao da HUD de chegada da Edwiges (so cliente). O servidor manda a distancia ~4x por segundo; aqui
 * a barra e o numero acompanham suavemente, e a faixa desliza para dentro/fora da tela.
 *
 * <p>Fluxo: PROGRESS -> a faixa desce do topo; a barra comeca cheia (distancia inicial) e diminui ate a coruja
 * chegar. DELIVERED -> barra e distancia vao a zero, a faixa fica {@link #HOLD_MS} e sobe. STOP -> sobe na hora.
 */
public final class EdwigesHudState {
	/** Tempo que a HUD permanece visivel depois da entrega da carta. */
	public static final long HOLD_MS = 3000L;
	/** Duracao da entrada (de cima para baixo) e da saida (para cima). */
	private static final float SLIDE_IN_SECONDS = 0.55f;
	private static final float SLIDE_OUT_SECONDS = 0.45f;
	/** Rapidez com que a barra/numero alcancam o valor recebido (por segundo). */
	private static final float FOLLOW_RATE = 7.0f;

	private static boolean wanted;
	/** 0 = escondida acima da tela, 1 = no lugar. */
	private static float slide;
	private static float start = 1.0f;
	private static float targetDistance;
	private static float shownDistance;
	/** Instante (ms) em que a saida comeca depois da entrega; -1 = sem contagem. */
	private static long hideAtMs = -1L;
	private static long lastMs;

	private EdwigesHudState() {}

	public static void onPacket(EdwigesHudPayload payload) {
		switch (payload.state()) {
			case EdwigesHudPayload.PROGRESS -> {
				if (payload.start() != start || !wanted) {
					// Nova coruja (ou primeira mensagem): a barra recomeca cheia.
					start = Math.max(payload.start(), 1.0f);
					shownDistance = start;
				}
				wanted = true;
				hideAtMs = -1L;
				targetDistance = payload.distance();
				if (slide <= 0.0f) lastMs = Util.getMeasuringTimeMs();
			}
			case EdwigesHudPayload.DELIVERED -> {
				if (!wanted) return;
				targetDistance = 0.0f;
				hideAtMs = Util.getMeasuringTimeMs() + HOLD_MS;
			}
			default -> wanted = false; // STOP
		}
	}

	public static void clear() {
		wanted = false;
		slide = 0.0f;
		hideAtMs = -1L;
		shownDistance = 0.0f;
		targetDistance = 0.0f;
	}

	/** Avanca a animacao ate agora (chamado a cada quadro pela renderizacao). */
	public static void update() {
		long now = Util.getMeasuringTimeMs();
		float dt = Math.min(0.1f, Math.max(0.0f, (now - lastMs) / 1000.0f));
		lastMs = now;

		if (wanted && hideAtMs >= 0L && now >= hideAtMs) {
			wanted = false;
			hideAtMs = -1L;
		}
		if (wanted) {
			slide = Math.min(1.0f, slide + dt / SLIDE_IN_SECONDS);
		} else {
			slide = Math.max(0.0f, slide - dt / SLIDE_OUT_SECONDS);
		}
		float follow = 1.0f - (float) Math.exp(-FOLLOW_RATE * dt);
		shownDistance += (targetDistance - shownDistance) * follow;
	}

	public static boolean isVisible() {
		return slide > 0.0f;
	}

	/** Posicao da faixa, ja suavizada: 0 (fora, acima) .. 1 (no lugar). Entrada desacelera; saida acelera. */
	public static float eased() {
		float t = slide;
		return wanted ? 1.0f - (1.0f - t) * (1.0f - t) * (1.0f - t) : t * t * (3.0f - 2.0f * t);
	}

	/** Fracao da barra que ainda falta percorrer: 1 = cheia (inicio), 0 = vazia (chegou). */
	public static float progress() {
		float p = shownDistance / start;
		return p < 0.0f ? 0.0f : Math.min(p, 1.0f);
	}

	/** Distancia mostrada, em blocos inteiros. */
	public static int distanceBlocks() {
		return Math.max(0, Math.round(shownDistance));
	}
}
