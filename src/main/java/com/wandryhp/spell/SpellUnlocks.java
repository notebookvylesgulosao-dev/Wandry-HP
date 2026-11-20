package com.wandryhp.spell;

import net.minecraft.entity.player.PlayerEntity;

import java.util.ArrayList;
import java.util.List;

/**
 * Ponto único de decisão sobre quais feitiços estão desbloqueados para um jogador.
 * Ainda não foi definida a forma de desbloquear: por enquanto todos os sete estão desbloqueados.
 * Tanto a roda (cliente) quanto o lançamento (servidor) consultam este método.
 */
public final class SpellUnlocks {
	private SpellUnlocks() {}

	public static boolean isUnlocked(PlayerEntity player, Spell spell) {
		return true;
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
