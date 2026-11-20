package com.wandryhp.client.render;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.wandryhp.WandryHP;
import net.minecraft.resource.ResourceManager;
import net.minecraft.util.Identifier;

import java.io.Reader;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/** Geometrias dos props, lidas dos .bbmodel originais em assets/wandryhp/bbmodels/props/. */
public final class PropModels {
	/** Props existentes nesta etapa (a varinha Widow usa o universal). */
	private static final String[] PROP_IDS = {"amethyst", "bone", "celestial", WandryHP.UNIVERSAL_PROP_ID};

	private static final Map<String, PropGeometry> MODELS = new HashMap<>();

	private PropModels() {}

	public static void reload(ResourceManager manager) {
		MODELS.clear();
		for (String id : PROP_IDS) {
			Identifier path = Identifier.of(WandryHP.MOD_ID, "bbmodels/props/" + id + ".bbmodel");
			Optional<net.minecraft.resource.Resource> resource = manager.getResource(path);
			if (resource.isEmpty()) {
				System.err.println("[wandryhp] bbmodel do prop nao encontrado: " + path);
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

	public static PropGeometry get(String propId) {
		return MODELS.get(propId);
	}

	public static Identifier texture(String propId) {
		return Identifier.of(WandryHP.MOD_ID, "textures/entity/prop/" + propId + ".png");
	}
}
