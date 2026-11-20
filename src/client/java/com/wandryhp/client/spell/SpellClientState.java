package com.wandryhp.client.spell;

import com.wandryhp.spell.Spell;

/** Feitiço atualmente ativo no cliente (escolhido na roda). Null enquanto nenhum foi escolhido. */
public final class SpellClientState {
	private static Spell selected;

	private SpellClientState() {}

	public static Spell selected() {
		return selected;
	}

	public static void select(Spell spell) {
		selected = spell;
	}
}
