package com.wandryhp.spell;

import com.wandryhp.WandryHP;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

/**
 * Servidor -> cliente: o feitiço (índice em {@link Spell}) entrou em recarga por esta quantidade de ticks.
 * Serve só para a HUD de recarga; quem valida a recarga continua sendo o servidor ({@link SpellCaster}).
 */
public record SpellCooldownPayload(int spell, int ticks) implements CustomPayload {
	public static final CustomPayload.Id<SpellCooldownPayload> ID =
			new CustomPayload.Id<>(Identifier.of(WandryHP.MOD_ID, "spell_cooldown"));
	public static final PacketCodec<RegistryByteBuf, SpellCooldownPayload> CODEC = PacketCodec.tuple(
			PacketCodecs.VAR_INT, SpellCooldownPayload::spell,
			PacketCodecs.VAR_INT, SpellCooldownPayload::ticks,
			SpellCooldownPayload::new);

	@Override
	public CustomPayload.Id<? extends CustomPayload> getId() {
		return ID;
	}
}
