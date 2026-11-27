package com.wandryhp.alignment;

import com.wandryhp.WandryHP;
import com.wandryhp.armor.HouseType;
import com.wandryhp.house.HouseSelection;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.MutableText;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Ícones ao lado do nick: o da casa (brasão) e o do alinhamento, já nas cores da casa. São caracteres de uma fonte
 * própria (assets/wandryhp/font/badges.json, gerada por tools/prepare_alignment_assets.py), então valem em qualquer
 * lugar que mostre texto: chat, TAB e nome acima da cabeça. Sem casa definida só aparece o ícone do alinhamento,
 * em cinza. Os códigos abaixo devem bater com o script.
 */
public final class PlayerBadge {
	public static final Identifier FONT = Identifier.of(WandryHP.MOD_ID, "badges");

	private static final int HOUSE_BASE = 0xE000;
	private static final int ALIGNMENT_BASE = 0xE010;
	/** Posição do "sem casa" entre as variações de cor do ícone de alinhamento (0..3 = casas). */
	private static final int NO_HOUSE_SLOT = 4;

	/** Último valor recebido do servidor para cada jogador (só usado no cliente). */
	private static final Map<UUID, Badge> CLIENT = new ConcurrentHashMap<>();

	private PlayerBadge() {}

	/** @param house null enquanto o jogador ainda não tem casa */
	public record Badge(HouseType house, Alignment alignment) {
		public int houseCode() {
			return house == null ? 0 : house.ordinal() + 1;
		}
	}

	// ---------------------------------------------------------------- leitura

	/** Casa e alinhamento do jogador: do servidor direto; no cliente, do que o servidor informou (pode ser null). */
	public static Badge of(PlayerEntity player) {
		if (player instanceof ServerPlayerEntity) {
			return new Badge(HouseSelection.getResult(player).orElse(null), AlignmentData.of(player));
		}
		return cached(player.getUuid());
	}

	public static Badge cached(UUID uuid) {
		return CLIENT.get(uuid);
	}

	public static void putClient(UUID uuid, int houseCode, int alignmentOrdinal) {
		HouseType[] houses = HouseType.values();
		HouseType house = houseCode >= 1 && houseCode <= houses.length ? houses[houseCode - 1] : null;
		CLIENT.put(uuid, new Badge(house, Alignment.byOrdinal(alignmentOrdinal)));
	}

	public static void clearClient() {
		CLIENT.clear();
	}

	// ---------------------------------------------------------------- texto

	/** Ícones + espaço, para ficar antes do nick. */
	public static Text prefix(Badge badge) {
		MutableText text = Text.empty();
		if (badge.house() != null) {
			text.append(glyph(HOUSE_BASE + badge.house().ordinal()));
		}
		int slot = badge.house() == null ? NO_HOUSE_SLOT : badge.house().ordinal();
		text.append(glyph(ALIGNMENT_BASE + badge.alignment().ordinal() * 8 + slot));
		text.append(Text.literal(" "));
		return text;
	}

	/** O nome com os ícones na frente (o nome original fica intacto, com a cor/hover/clique que já tinha). */
	public static Text decorate(Badge badge, Text name) {
		if (badge == null) {
			return name;
		}
		return Text.empty().append(prefix(badge)).append(name);
	}

	public static Text decorate(PlayerEntity player, Text name) {
		return decorate(of(player), name);
	}

	private static Text glyph(int codePoint) {
		return Text.literal(String.valueOf((char) codePoint)).setStyle(Style.EMPTY.withFont(FONT));
	}
}
