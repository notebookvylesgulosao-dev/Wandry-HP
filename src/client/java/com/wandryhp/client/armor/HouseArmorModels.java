package com.wandryhp.client.armor;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.wandryhp.WandryHP;
import com.wandryhp.armor.HouseType;
import net.minecraft.resource.Resource;
import net.minecraft.resource.ResourceManager;
import net.minecraft.util.Identifier;

import java.io.Reader;
import java.util.EnumMap;
import java.util.Map;
import java.util.Optional;

/** Geometrias dos peitorais, lidas dos .bbmodel originais em assets/wandryhp/bbmodels/armor/. */
public final class HouseArmorModels {
	private static final Map<HouseType, HouseArmorGeometry> MODELS = new EnumMap<>(HouseType.class);

	private static HouseArmorGeometry leggings;
	private static HouseArmorGeometry boots;

	public static final String LEGGINGS_ID = "house_leggings";
	public static final String BOOTS_ID = "house_boots";

	private HouseArmorModels() {}

	public static void reload(ResourceManager manager) {
		MODELS.clear();
		leggings = load(manager, LEGGINGS_ID, HouseArmorGeometry.LEGGINGS_EXTRA_INFLATE);
		boots = load(manager, BOOTS_ID, HouseArmorGeometry.BOOTS_EXTRA_INFLATE);
		for (HouseType house : HouseType.values()) {
			Identifier path = Identifier.of(WandryHP.MOD_ID, "bbmodels/armor/" + house.id + ".bbmodel");
			Optional<Resource> resource = manager.getResource(path);
			if (resource.isEmpty()) {
				System.err.println("[wandryhp] bbmodel do peitoral nao encontrado: " + path);
				continue;
			}
			try (Reader reader = resource.get().getReader()) {
				JsonObject root = JsonParser.parseReader(reader).getAsJsonObject();
				MODELS.put(house, HouseArmorGeometry.fromBbmodel(root));
			} catch (Exception e) {
				System.err.println("[wandryhp] Falha ao ler " + path + ": " + e);
			}
		}
	}

	private static HouseArmorGeometry load(ResourceManager manager, String id, float legExtraInflate) {
		Identifier path = Identifier.of(WandryHP.MOD_ID, "bbmodels/armor/" + id + ".bbmodel");
		Optional<Resource> resource = manager.getResource(path);
		if (resource.isEmpty()) {
			System.err.println("[wandryhp] bbmodel nao encontrado: " + path);
			return null;
		}
		try (Reader reader = resource.get().getReader()) {
			return HouseArmorGeometry.fromBbmodel(JsonParser.parseReader(reader).getAsJsonObject(), legExtraInflate);
		} catch (Exception e) {
			System.err.println("[wandryhp] Falha ao ler " + path + ": " + e);
			return null;
		}
	}

	public static HouseArmorGeometry leggings() {
		return leggings;
	}

	public static HouseArmorGeometry boots() {
		return boots;
	}

	public static Identifier texture(String id) {
		return Identifier.of(WandryHP.MOD_ID, "textures/entity/armor/" + id + ".png");
	}

	public static HouseArmorGeometry get(HouseType house) {
		return MODELS.get(house);
	}

	public static Identifier texture(HouseType house) {
		return Identifier.of(WandryHP.MOD_ID, "textures/entity/armor/" + house.id + ".png");
	}
}
