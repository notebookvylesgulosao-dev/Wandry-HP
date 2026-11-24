package com.wandryhp.network;

import com.wandryhp.item.WandItem;
import com.wandryhp.item.WandType;
import com.wandryhp.mana.Mana;
import net.fabricmc.fabric.api.networking.v1.PlayerLookup;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Sequencia unica de conjuracao, compartilhada pelo lancamento dos props (botao esquerdo) e pelos
 * feitiços (botao direito):
 *
 * <ol>
 *   <li>o servidor valida o clique e chama {@link #begin}: envia a animacao de CONJURACAO a todos que veem o jogador;</li>
 *   <li>durante {@link #FIRE_DELAY_TICKS} ticks NENHUM projetil existe;</li>
 *   <li>so depois (a animacao de conjuracao de {@link #CAST_ANIM_TICKS} ticks ja terminou por completo) o servidor
 *       executa o lancamento (cria o projetil) e envia a animacao de LANCAMENTO.</li>
 * </ol>
 *
 * Como o inicio da animacao e o nascimento do projetil saem do mesmo servidor, com o mesmo atraso de rede,
 * a sincronia vale em primeira e terceira pessoa, para o proprio jogador e para os outros.
 */
public final class WandCasting {
	/** Duracao da animacao de conjuracao: 24 ticks = 1,2 s. */
	public static final int CAST_ANIM_TICKS = 24;
	/** Atraso ate o projetil: a animacao termina 2 ticks (0,1 s) antes, entao o projetil nunca nasce com ela em curso. */
	public static final int FIRE_DELAY_TICKS = CAST_ANIM_TICKS + 2;

	/** Cria o projetil. Chamado so no fim da conjuracao, com o tipo da varinha que o jogador segura nesse momento. */
	@FunctionalInterface
	public interface Fire {
		void run(ServerPlayerEntity player, WandType type);
	}

	/** manaCost: mana já gasta por este lançamento; devolvida se a conjuração for cancelada. */
	private record Pending(int fireAtTick, Fire fire, int manaCost) {}

	private static final Map<UUID, Pending> PENDING = new HashMap<>();

	private WandCasting() {}

	public static boolean isCasting(ServerPlayerEntity player) {
		return PENDING.containsKey(player.getUuid());
	}

	/**
	 * Inicia a conjuracao. Quem chama ja validou varinha, desbloqueio, recarga e mana, e ja gastou {@code manaCost}
	 * (devolvido aqui se a conjuracao for cancelada antes de sair o projetil).
	 */
	public static void begin(ServerPlayerEntity player, Fire fire, int manaCost) {
		if (isCasting(player)) {
			return;
		}
		int now = player.getServer().getTicks();
		PENDING.put(player.getUuid(), new Pending(now + FIRE_DELAY_TICKS, fire, manaCost));
		broadcast(player, WandAnimPayload.CAST);
	}

	/** Chamado a cada tick do servidor (fim do tick). */
	public static void tick(MinecraftServer server) {
		if (PENDING.isEmpty()) {
			return;
		}
		int now = server.getTicks();
		Iterator<Map.Entry<UUID, Pending>> it = PENDING.entrySet().iterator();
		while (it.hasNext()) {
			Map.Entry<UUID, Pending> entry = it.next();
			ServerPlayerEntity player = server.getPlayerManager().getPlayer(entry.getKey());
			if (player == null) {
				it.remove();
				continue;
			}
			boolean holdsWand = player.getMainHandStack().getItem() instanceof WandItem;
			if (!player.isAlive() || player.isSpectator() || !holdsWand) {
				it.remove();
				Mana.refund(player, entry.getValue().manaCost()); // cancelado: nada foi lancado
				broadcast(player, WandAnimPayload.CANCEL);
				continue;
			}
			Pending pending = entry.getValue();
			if (now >= pending.fireAtTick()) {
				it.remove(); // remove antes: o Fire pode, no futuro, iniciar outra conjuracao
				WandType type = ((WandItem) player.getMainHandStack().getItem()).getWandType();
				pending.fire().run(player, type);
				broadcast(player, WandAnimPayload.LAUNCH);
			}
		}
	}

	public static void forget(ServerPlayerEntity player) {
		PENDING.remove(player.getUuid());
	}

	public static void clear() {
		PENDING.clear();
	}

	private static void broadcast(ServerPlayerEntity player, int phase) {
		WandAnimPayload payload = new WandAnimPayload(player.getId(), phase);
		Set<ServerPlayerEntity> targets = new HashSet<>(PlayerLookup.tracking(player));
		targets.add(player); // o proprio jogador (primeira pessoa / camera de terceira pessoa)
		for (ServerPlayerEntity target : targets) {
			ServerPlayNetworking.send(target, payload);
		}
	}
}
