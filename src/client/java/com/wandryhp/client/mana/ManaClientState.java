package com.wandryhp.client.mana;

/** Mana do proprio jogador como o servidor informou (so para a HUD; quem decide e o servidor). */
public final class ManaClientState {
	private static int current;
	private static int max;
	private static boolean known;

	private ManaClientState() {}

	public static void set(int current, int max) {
		ManaClientState.max = Math.max(1, max);
		ManaClientState.current = Math.max(0, Math.min(ManaClientState.max, current));
		known = true;
	}

	public static void clear() {
		known = false;
	}

	/** false ate o servidor enviar o primeiro valor (a HUD nao desenha nada antes disso). */
	public static boolean isKnown() {
		return known;
	}

	public static int current() {
		return current;
	}

	public static int max() {
		return max;
	}
}
