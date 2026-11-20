package com.wandryhp.client.render;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.minecraft.client.render.LightmapTextureManager;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.util.math.MatrixStack;

import java.util.ArrayList;
import java.util.List;

/**
 * Geometria de um prop construida diretamente do .bbmodel original (java_block).
 *
 * <p>Reproduz exatamente o que esta no arquivo: cubos from/to, rotacao em ate 3 eixos em torno do
 * "origin" (o JSON vanilla so suporta 1 eixo, por isso nao ha conversao), UV por face (com rotacao de
 * face) e light_emission. A geometria e centralizada no centro da sua caixa e fica em unidades de bloco.
 */
public final class PropGeometry {
	private static final String[] DIRECTIONS = {"down", "up", "north", "south", "west", "east"};

	private static final class Quad {
		final float[] pos = new float[12];
		final float[] uv = new float[8];
		float nx, ny, nz;
		int emission;
	}

	private final List<Quad> quads = new ArrayList<>();

	private PropGeometry() {}

	public static PropGeometry fromBbmodel(JsonObject root) {
		JsonObject resolution = root.has("resolution") ? root.getAsJsonObject("resolution") : null;
		float resW = resolution != null ? resolution.get("width").getAsFloat() : 16f;
		float resH = resolution != null ? resolution.get("height").getAsFloat() : 16f;

		PropGeometry geometry = new PropGeometry();
		double[] min = {Double.MAX_VALUE, Double.MAX_VALUE, Double.MAX_VALUE};
		double[] max = {-Double.MAX_VALUE, -Double.MAX_VALUE, -Double.MAX_VALUE};

		for (JsonElement el : root.getAsJsonArray("elements")) {
			JsonObject cube = el.getAsJsonObject();
			if (!"cube".equals(cube.get("type").getAsString())) continue;
			if (cube.has("export") && !cube.get("export").getAsBoolean()) continue;

			double[] from = array(cube.getAsJsonArray("from"));
			double[] to = array(cube.getAsJsonArray("to"));
			double[] origin = cube.has("origin") ? array(cube.getAsJsonArray("origin")) : new double[] {0, 0, 0};
			double[] rotation = cube.has("rotation") ? array(cube.getAsJsonArray("rotation")) : new double[] {0, 0, 0};
			double[][] r = rotationMatrix(rotation);
			int emission = cube.has("light_emission") ? cube.get("light_emission").getAsInt() : 0;
			JsonObject faces = cube.getAsJsonObject("faces");

			for (String dir : DIRECTIONS) {
				if (!faces.has(dir)) continue;
				JsonObject face = faces.getAsJsonObject(dir);
				if (!face.has("texture") || face.get("texture").isJsonNull()) continue;

				Quad quad = new Quad();
				quad.emission = emission;

				double[][] corners = faceCorners(dir, from, to);
				for (int i = 0; i < 4; i++) {
					double[] c = corners[i];
					double[] d = {c[0] - origin[0], c[1] - origin[1], c[2] - origin[2]};
					for (int a = 0; a < 3; a++) {
						double v = r[a][0] * d[0] + r[a][1] * d[1] + r[a][2] * d[2] + origin[a];
						quad.pos[i * 3 + a] = (float) v;
						min[a] = Math.min(min[a], v);
						max[a] = Math.max(max[a], v);
					}
				}

				double[] n = normal(dir);
				quad.nx = (float) (r[0][0] * n[0] + r[0][1] * n[1] + r[0][2] * n[2]);
				quad.ny = (float) (r[1][0] * n[0] + r[1][1] * n[1] + r[1][2] * n[2]);
				quad.nz = (float) (r[2][0] * n[0] + r[2][1] * n[1] + r[2][2] * n[2]);

				// UV: [u1, v1, u2, v2] no espaco da resolucao do projeto (16x16), normalizado para 0..1.
				double[] uv = array(face.getAsJsonArray("uv"));
				float u1 = (float) (uv[0] / resW), v1 = (float) (uv[1] / resH);
				float u2 = (float) (uv[2] / resW), v2 = (float) (uv[3] / resH);
				float[][] corner = {{u1, v1}, {u1, v2}, {u2, v2}, {u2, v1}};
				int shift = face.has("rotation") ? Math.floorMod(face.get("rotation").getAsInt() / 90, 4) : 0;
				for (int i = 0; i < 4; i++) {
					float[] c = corner[(i + shift) % 4];
					quad.uv[i * 2] = c[0];
					quad.uv[i * 2 + 1] = c[1];
				}
				geometry.quads.add(quad);
			}
		}

		// Centraliza na caixa da geometria e converte de unidades de modelo (1/16) para blocos.
		for (Quad q : geometry.quads) {
			for (int i = 0; i < 4; i++) {
				for (int a = 0; a < 3; a++) {
					double center = (min[a] + max[a]) / 2.0;
					q.pos[i * 3 + a] = (float) ((q.pos[i * 3 + a] - center) / 16.0);
				}
			}
		}
		return geometry;
	}

	public void render(MatrixStack matrices, VertexConsumer consumer, int light) {
		MatrixStack.Entry entry = matrices.peek();
		for (Quad q : quads) {
			int l = light;
			if (q.emission > 0) {
				l = LightmapTextureManager.pack(
						Math.max(q.emission, LightmapTextureManager.getBlockLightCoordinates(light)),
						LightmapTextureManager.getSkyLightCoordinates(light));
			}
			for (int i = 0; i < 4; i++) {
				consumer.vertex(entry, q.pos[i * 3], q.pos[i * 3 + 1], q.pos[i * 3 + 2])
						.color(255, 255, 255, 255)
						.texture(q.uv[i * 2], q.uv[i * 2 + 1])
						.overlay(OverlayTexture.DEFAULT_UV)
						.light(l)
						.normal(entry, q.nx, q.ny, q.nz);
			}
		}
	}

	/** Mesma ordem de vertices e de UV do bake vanilla (CubeFace). */
	private static double[][] faceCorners(String dir, double[] f, double[] t) {
		return switch (dir) {
			case "down" -> new double[][] {{f[0], f[1], t[2]}, {f[0], f[1], f[2]}, {t[0], f[1], f[2]}, {t[0], f[1], t[2]}};
			case "up" -> new double[][] {{f[0], t[1], f[2]}, {f[0], t[1], t[2]}, {t[0], t[1], t[2]}, {t[0], t[1], f[2]}};
			case "north" -> new double[][] {{t[0], t[1], f[2]}, {t[0], f[1], f[2]}, {f[0], f[1], f[2]}, {f[0], t[1], f[2]}};
			case "south" -> new double[][] {{f[0], t[1], t[2]}, {f[0], f[1], t[2]}, {t[0], f[1], t[2]}, {t[0], t[1], t[2]}};
			case "west" -> new double[][] {{f[0], t[1], f[2]}, {f[0], f[1], f[2]}, {f[0], f[1], t[2]}, {f[0], t[1], t[2]}};
			default -> new double[][] {{t[0], t[1], t[2]}, {t[0], f[1], t[2]}, {t[0], f[1], f[2]}, {t[0], t[1], f[2]}}; // east
		};
	}

	private static double[] normal(String dir) {
		return switch (dir) {
			case "down" -> new double[] {0, -1, 0};
			case "up" -> new double[] {0, 1, 0};
			case "north" -> new double[] {0, 0, -1};
			case "south" -> new double[] {0, 0, 1};
			case "west" -> new double[] {-1, 0, 0};
			default -> new double[] {1, 0, 0};
		};
	}

	/** Rotacao do Blockbench: graus, ordem Euler ZYX (X aplicado primeiro): R = Rz * Ry * Rx. */
	private static double[][] rotationMatrix(double[] deg) {
		double x = Math.toRadians(deg[0]), y = Math.toRadians(deg[1]), z = Math.toRadians(deg[2]);
		double cx = Math.cos(x), sx = Math.sin(x), cy = Math.cos(y), sy = Math.sin(y), cz = Math.cos(z), sz = Math.sin(z);
		return new double[][] {
				{cz * cy, cz * sy * sx - sz * cx, cz * sy * cx + sz * sx},
				{sz * cy, sz * sy * sx + cz * cx, sz * sy * cx - cz * sx},
				{-sy, cy * sx, cy * cx}
		};
	}

	private static double[] array(JsonArray a) {
		double[] out = new double[a.size()];
		for (int i = 0; i < out.length; i++) out[i] = a.get(i).getAsDouble();
		return out;
	}
}
