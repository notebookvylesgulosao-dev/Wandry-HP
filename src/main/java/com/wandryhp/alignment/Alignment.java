package com.wandryhp.alignment;

import com.mojang.serialization.Codec;

/**
 * Alinhamento do jogador. Todo jogador começa {@link #NEUTRAL}; os livros do Bem e do Mal mudam para {@link #GOOD}
 * e {@link #EVIL}. A ordem dos valores é usada na rede e nos ícones (ver PlayerBadge): não reordenar.
 */
public enum Alignment {
	NEUTRAL("neutral"),
	GOOD("good"),
	EVIL("evil");

	/** Gravado no mundo (data attachment): o id, não o índice, para a ordem poder mudar sem corromper saves. */
	public static final Codec<Alignment> CODEC = Codec.STRING.xmap(Alignment::byId, alignment -> alignment.id);

	public final String id;

	Alignment(String id) {
		this.id = id;
	}

	/** Chave de tradução do nome (alignment.wandryhp.&lt;id&gt;). */
	public String translationKey() {
		return "alignment.wandryhp." + id;
	}

	public static Alignment byId(String id) {
		for (Alignment alignment : values()) {
			if (alignment.id.equals(id)) {
				return alignment;
			}
		}
		return NEUTRAL;
	}

	public static Alignment byOrdinal(int ordinal) {
		Alignment[] all = values();
		return ordinal >= 0 && ordinal < all.length ? all[ordinal] : NEUTRAL;
	}
}
