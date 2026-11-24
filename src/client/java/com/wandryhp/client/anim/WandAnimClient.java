package com.wandryhp.client.anim;

import com.wandryhp.network.WandAnimPayload;
import com.wandryhp.network.WandCasting;
import net.minecraft.client.MinecraftClient;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * Estado cliente das animações da varinha, por entidade (jogador local e os outros jogadores).
 * O servidor decide a fase ({@link WandAnimPayload}); aqui só se calcula a pose a cada quadro.
 *
 * <p>O tempo é o tempo de jogo do mundo mais o {@code tickDelta} do quadro, então a animação é contínua entre ticks
 * (sem passos de 20 Hz). Ao trocar de fase a pose anterior é carregada e dissolvida em {@link #CARRY_TICKS},
 * garantindo que nunca haja salto, mesmo se um pacote chegar em momento inesperado.
 */
public final class WandAnimClient {
	private static final int LAUNCH_TICKS = 8;
	private static final int CARRY_TICKS = 6;
	/** Segurança: se o lançamento nunca chegar (ex.: lag extremo), solta o estado depois de um tempo. */
	private static final int HOLD_LIMIT_TICKS = 80;

	private static final class Entry {
		int phase;
		long start;
		WandPose carry = WandPose.ZERO;
	}

	private static final Map<Integer, Entry> ENTRIES = new HashMap<>();

	private WandAnimClient() {}

	public static void onPacket(WandAnimPayload payload, MinecraftClient client) {
		if (client.world == null) {
			return;
		}
		long now = client.world.getTime();
		Entry old = ENTRIES.get(payload.entityId());
		WandPose carry = old == null ? WandPose.ZERO : evaluate(old, now, 0f, true);
		Entry e = new Entry();
		e.phase = payload.phase();
		e.start = now;
		e.carry = carry;
		ENTRIES.put(payload.entityId(), e);
	}

	public static void clear() {
		ENTRIES.clear();
	}

	/** Pose do jogador neste quadro, ou null se ele não está animando. */
	public static WandPose poseFor(int entityId, float tickDelta) {
		Entry e = ENTRIES.get(entityId);
		MinecraftClient client = MinecraftClient.getInstance();
		if (e == null || client.world == null) {
			return null;
		}
		WandPose pose = evaluate(e, client.world.getTime(), tickDelta, false);
		if (pose == null) {
			ENTRIES.remove(entityId);
		}
		return pose;
	}

	/** Remove entradas vencidas (chamado de vez em quando; poseFor já limpa as consultadas). */
	public static void prune() {
		MinecraftClient client = MinecraftClient.getInstance();
		if (client.world == null) {
			ENTRIES.clear();
			return;
		}
		long now = client.world.getTime();
		Iterator<Map.Entry<Integer, Entry>> it = ENTRIES.entrySet().iterator();
		while (it.hasNext()) {
			if (evaluate(it.next().getValue(), now, 0f, false) == null) {
				it.remove();
			}
		}
	}

	/** @param keep true = nunca devolve null (usado só para capturar a pose atual ao trocar de fase) */
	private static WandPose evaluate(Entry e, long now, float tickDelta, boolean keep) {
		float elapsed = (now - e.start) + tickDelta; // em ticks
		if (elapsed < 0f) elapsed = 0f;

		WandPose base;
		switch (e.phase) {
			case WandAnimPayload.CAST -> {
				if (elapsed < WandCasting.CAST_ANIM_TICKS) {
					base = WandPose.cast(elapsed / WandCasting.CAST_ANIM_TICKS);
				} else if (elapsed < HOLD_LIMIT_TICKS) {
					base = WandPose.ZERO; // animação terminou: braço relaxado até o servidor lançar
				} else {
					return keep ? WandPose.ZERO : null;
				}
			}
			case WandAnimPayload.LAUNCH -> {
				if (elapsed < LAUNCH_TICKS) {
					base = WandPose.launch(elapsed / LAUNCH_TICKS);
				} else {
					return keep ? WandPose.ZERO : null;
				}
			}
			default -> base = WandPose.ZERO; // CANCEL: só a pose carregada se dissolvendo
		}

		if (elapsed < CARRY_TICKS && e.carry != WandPose.ZERO) {
			base = base.plus(e.carry.scale(1f - WandPose.smootherstep(elapsed / CARRY_TICKS)));
		} else if (e.phase == WandAnimPayload.CANCEL && elapsed >= CARRY_TICKS) {
			return keep ? WandPose.ZERO : null;
		}
		return base;
	}
}
