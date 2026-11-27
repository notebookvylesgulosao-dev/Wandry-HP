package com.wandryhp.spell;

import com.wandryhp.alignment.Alignment;

import java.util.EnumSet;
import java.util.Set;

/**
 * Categoria de cada feitiço quanto ao alinhamento: Bons, Maus e Normais (todos os demais). Fica fora de {@link Spell}
 * para não mexer nos feitiços. Bom só usa Bons + Normais, Mau só Maus + Normais, Neutro só Normais.
 */
public enum SpellAlignment {
	GOOD,
	EVIL,
	NORMAL;

	private static final Set<Spell> GOOD_SPELLS = EnumSet.of(Spell.EXPECTO_PATRONUM, Spell.EXPELLIARMUS, Spell.VORTEM);
	private static final Set<Spell> EVIL_SPELLS = EnumSet.of(Spell.AVADA_KEDAVRA, Spell.CRUCIO, Spell.IMPERIUS);

	public static SpellAlignment of(Spell spell) {
		if (GOOD_SPELLS.contains(spell)) {
			return GOOD;
		}
		if (EVIL_SPELLS.contains(spell)) {
			return EVIL;
		}
		return NORMAL;
	}

	/** O feitiço dessa categoria está liberado para esse alinhamento? */
	public boolean allowedFor(Alignment alignment) {
		return switch (this) {
			case NORMAL -> true;
			case GOOD -> alignment == Alignment.GOOD;
			case EVIL -> alignment == Alignment.EVIL;
		};
	}
}
