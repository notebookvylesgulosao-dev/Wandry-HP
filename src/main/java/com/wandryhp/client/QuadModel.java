package com.wandryhp.client;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.wandryhp.WandryHP;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

/**
 * Modelo feito de quads pré-calculados a partir dos .bbmodel originais (tools/convert_models.py),
 * o que preserva rotações em vários eixos e UV por face que o formato JSON de item não suporta.
 * Dados em assets/wandry_hp/quads/<nome>.json, textura em textures/entity/<nome>.png.
 */
public final class QuadModel {
    /** 0-6 = feitiços (ordem de MagicWandItem.Spell), 7-11 = props de ataque base. */
    public static final String[] NAMES = {
        "avada_kedavra", "corinficus", "crucio", "expelliarmus", "imperius", "petrificus_totalis", "sectumsempra",
        "prop_amethyst", "prop_bone", "prop_celestial", "prop_dragon", "prop_widow"
    };
    private static final int STRIDE = 24; // 4 vértices x (x,y,z,u,v) + normal(3) + emissivo(1)
    private static final Map<String, QuadModel> CACHE = new HashMap<>();

    private final float[] data;
    private final RenderLayer layer;

    private QuadModel(float[] data, Identifier texture) {
        this.data = data;
        this.layer = RenderLayer.getEntityCutoutNoCull(texture);
    }

    public static QuadModel get(String name) {
        return CACHE.computeIfAbsent(name, QuadModel::load);
    }

    private static QuadModel load(String name) {
        try (InputStream in = QuadModel.class.getResourceAsStream("/assets/wandry_hp/quads/" + name + ".json")) {
            if (in == null) throw new IllegalStateException("Quads não encontrados: " + name);
            JsonObject root = JsonParser.parseReader(new InputStreamReader(in, StandardCharsets.UTF_8)).getAsJsonObject();
            JsonArray array = root.getAsJsonArray("quads");
            float[] data = new float[array.size()];
            for (int i = 0; i < data.length; i++) data[i] = array.get(i).getAsFloat();
            return new QuadModel(data, WandryHP.id("textures/entity/" + name + ".png"));
        } catch (java.io.IOException e) {
            throw new IllegalStateException(e);
        }
    }

    public void render(MatrixStack matrices, VertexConsumerProvider vertexConsumers, int light) {
        VertexConsumer consumer = vertexConsumers.getBuffer(layer);
        MatrixStack.Entry entry = matrices.peek();
        for (int o = 0; o + STRIDE <= data.length; o += STRIDE) {
            float nx = data[o + 20], ny = data[o + 21], nz = data[o + 22];
            int quadLight = data[o + 23] > 0.5f ? 0xF000F0 : light;
            for (int v = 0; v < 4; v++) {
                int i = o + v * 5;
                consumer.vertex(entry, data[i], data[i + 1], data[i + 2])
                    .color(255, 255, 255, 255)
                    .texture(data[i + 3], data[i + 4])
                    .overlay(OverlayTexture.DEFAULT_UV)
                    .light(quadLight)
                    .normal(entry, nx, ny, nz);
            }
        }
    }
}
