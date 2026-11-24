package com.wandryhp.client.accessory;

import com.google.gson.JsonParser;
import com.wandryhp.WandryHP;
import com.wandryhp.accessory.CapeType;
import net.minecraft.resource.Resource;
import net.minecraft.resource.ResourceManager;
import net.minecraft.util.Identifier;

import java.io.Reader;
import java.util.EnumMap;
import java.util.Map;
import java.util.Optional;

/** Geometrias dos chapeus (itens enchanter_*_cape), lidas dos .bbmodel em assets/wandryhp/bbmodels/accessory/. */
public final class CapeModels {
	private static final Map<CapeType, CapeGeometry> MODELS = new EnumMap<>(CapeType.class);

	private CapeModels() {}

	public static void reload(ResourceManager manager) {
		MODELS.clear();
		for (CapeType type : CapeType.values()) {
			Identifier path = Identifier.of(WandryHP.MOD_ID, "bbmodels/accessory/" + type.itemId() + ".bbmodel");
			Optional<Resource> resource = manager.getResource(path);
			if (resource.isEmpty()) {
				System.err.println("[wandryhp] bbmodel do chapeu nao encontrado: " + path);
				continue;
			}
			try (Reader reader = resource.get().getReader()) {
				MODELS.put(type, CapeGeometry.fromBbmodel(JsonParser.parseReader(reader).getAsJsonObject()));
			} catch (Exception e) {
				System.err.println("[wandryhp] Falha ao ler " + path + ": " + e);
			}
		}
	}

	public static CapeGeometry get(CapeType type) {
		return MODELS.get(type);
	}

	public static Identifier texture(CapeType type) {
		return Identifier.of(WandryHP.MOD_ID, "textures/entity/accessory/" + type.itemId() + ".png");
	}
}
