package com.wandryhp.spell;

import com.wandryhp.alignment.AlignmentData;
import net.minecraft.entity.player.PlayerEntity;

import java.util.ArrayList;
import java.util.List;

/**
 * Ponto único de decisão sobre quais feitiços estão desbloqueados para um jogador.
 * Ainda não há outra forma de desbloquear: o único critério é o alinhamento do jogador ({@link SpellAlignment}):
 * Normais sempre; Bons só para Bom; Maus só para Mau; Neutro só os Normais. Como nada é gravado por feitiço, trocar
 * de alinhamento apenas bloqueia os incompatíveis (voltam sozinhos se o jogador voltar ao alinhamento deles).
 * Tanto a roda (cliente) quanto o lançamento (servidor) consultam este método.
 */
public final class SpellUnlocks {
	private SpellUnlocks() {}

	public static boolean isUnlocked(PlayerEntity player, Spell spell) {
		return SpellAlignment.of(spell).allowedFor(AlignmentData.of(player));
	}

	public static List<Spell> unlocked(PlayerEntity player) {
		List<Spell> result = new ArrayList<>();
		for (Spell spell : Spell.values()) {
			if (isUnlocked(player, spell)) {
				result.add(spell);
			}
		}
		return result;
	}
}
