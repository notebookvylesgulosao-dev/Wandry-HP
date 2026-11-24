package com.wandryhp.client.spell;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.wandryhp.WandryHP;
import com.wandryhp.client.render.PropGeometry;
import com.wandryhp.spell.Spell;
import net.minecraft.resource.Resource;
import net.minecraft.resource.ResourceManager;
import net.minecraft.util.Identifier;

import java.io.Reader;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * Geometrias dos projéteis dos feitiços, lidas dos .bbmodel originais em assets/wandryhp/bbmodels/spells/
 * com o mesmo leitor dos props (PropGeometry), sem converter nem alterar o modelo.
 */
public final class SpellModels {
	private static final Map<String, PropGeometry> MODELS = new HashMap<>();

	private SpellModels() {}

	public static void reload(ResourceManager manager) {
		MODELS.clear();
		Set<String> ids = new LinkedHashSet<>();
		for (Spell spell : Spell.values()) {
			ids.add(spell.modelId); // Corinficus e Crucio compartilham o modelo vermelho
		}
		for (String id : ids) {
			Identifier path = Identifier.of(WandryHP.MOD_ID, "bbmodels/spells/" + id + ".bbmodel");
			Optional<Resource> resource = manager.getResource(path);
			if (resource.isEmpty()) {
				System.err.println("[wandryhp] bbmodel do feitiço não encontrado: " + path);
				continue;
			}
			try (Reader reader = resource.get().getReader()) {
				JsonObject root = JsonParser.parseReader(reader).getAsJsonObject();
				MODELS.put(id, PropGeometry.fromBbmodel(root));
			} catch (Exception e) {
				System.err.println("[wandryhp] Falha ao ler " + path + ": " + e);
			}
		}
	}

	public static PropGeometry get(String modelId) {
		return MODELS.get(modelId);
	}

	public static Identifier texture(String modelId) {
		return Identifier.of(WandryHP.MOD_ID, "textures/entity/spell/" + modelId + ".png");
	}
}
