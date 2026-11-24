package com.wandryhp.client.accessory;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.util.math.MatrixStack;

import java.util.ArrayList;
import java.util.List;

/**
 * Geometria do acessorio (hoje os chapeus de bruxa) lida direto do .bbmodel: cubos, UV por face, inflate e rotacao
 * do cubo nos tres eixos, sem alterar tamanho nem proporcoes. O nome da classe e mantido por compatibilidade
 * (os itens ainda se chamam enchanter_*_cape).
 *
 * <p>Os modelos novos sao java_block (UV por face, coordenadas 0..16 com o modelo centrado em x = z = 8). Para
 * encaixar o chapeu na cabeca, a geometria e re-ancorada: o cubo de maior pegada no plano XZ (a aba) vira a ancora,
 * seu centro vai para x = z = 0 e sua face inferior para y = 0. Convencao de eixos igual a de HouseArmorGeometry:
 * Blockbench (x, y, z) -> modelo de entidade (-x, -y, z), ou seja, uma rotacao propria de 180 graus em torno de Z.
 */
public final class CapeGeometry {
	private static final String[] DIRECTIONS = {"down", "up", "north", "south", "west", "east"};

	private static final class Quad {
		final float[] pos = new float[12];
		final float[] uv = new float[8];
		float nx, ny, nz;
	}

	private final List<Quad> quads = new ArrayList<>();

	private CapeGeometry() {}

	public static CapeGeometry fromBbmodel(JsonObject root) {
		JsonObject resolution = root.getAsJsonObject("resolution");
		float resW = resolution.get("width").getAsFloat();
		float resH = resolution.get("height").getAsFloat();
		CapeGeometry geometry = new CapeGeometry();

		// Ancora: cubo com a maior pegada em XZ (a aba do chapeu).
		double bestFootprint = -1.0;
		double anchorX = 0, anchorY = 0, anchorZ = 0;
		List<Quad> raw = new ArrayList<>();

		for (JsonElement el : root.getAsJsonArray("elements")) {
			JsonObject cube = el.getAsJsonObject();
			if (!"cube".equals(cube.get("type").getAsString())) continue;
			if (cube.has("export") && !cube.get("export").getAsBoolean()) continue;
			if (cube.has("visibility") && !cube.get("visibility").getAsBoolean()) continue;

			double[] from = array(cube.getAsJsonArray("from"));
			double[] to = array(cube.getAsJsonArray("to"));
			double inflate = cube.has("inflate") ? cube.get("inflate").getAsDouble() : 0.0;
			for (int a = 0; a < 3; a++) {
				from[a] -= inflate;
				to[a] += inflate;
			}

			double footprint = (to[0] - from[0]) * (to[2] - from[2]);
			if (footprint > bestFootprint) {
				bestFootprint = footprint;
				anchorX = (from[0] + to[0]) / 2.0;
				anchorY = from[1];
				anchorZ = (from[2] + to[2]) / 2.0;
			}

			double[] rotation = cube.has("rotation") ? array(cube.getAsJsonArray("rotation")) : new double[3];
			double[] origin = cube.has("origin") ? array(cube.getAsJsonArray("origin")) : new double[3];
			JsonObject faces = cube.getAsJsonObject("faces");

			for (String dir : DIRECTIONS) {
				if (!faces.has(dir)) continue;
				JsonObject face = faces.getAsJsonObject(dir);
				if (!face.has("texture") || face.get("texture").isJsonNull()) continue;

				Quad quad = new Quad();
				double[][] corners = faceCorners(dir, from, to);
				for (int i = 0; i < 4; i++) {
					double[] p = rotate(corners[i], origin, rotation);
					quad.pos[i * 3] = (float) p[0];
					quad.pos[i * 3 + 1] = (float) p[1];
					quad.pos[i * 3 + 2] = (float) p[2];
				}
				double[] n = rotate(normal(dir), new double[3], rotation);
				quad.nx = (float) -n[0];
				quad.ny = (float) -n[1];
				quad.nz = (float) n[2];

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
				raw.add(quad);
			}
		}

		// Re-ancora (Blockbench -> espaco da entidade, em pixels, relativo a ancora).
		for (Quad q : raw) {
			for (int i = 0; i < 4; i++) {
				q.pos[i * 3] = (float) -(q.pos[i * 3] - anchorX);
				q.pos[i * 3 + 1] = (float) -(q.pos[i * 3 + 1] - anchorY);
				q.pos[i * 3 + 2] = (float) (q.pos[i * 3 + 2] - anchorZ);
			}
			geometry.quads.add(q);
		}
		return geometry;
	}

	/**
	 * Desenha com a matriz ja na origem da parte (ModelPart#rotate). (offsetX, offsetY, offsetZ), em pixels, e onde a
	 * ancora fica em relacao a origem da parte; Y negativo sobe.
	 */
	public void render(MatrixStack matrices, VertexConsumer consumer, int light, float offsetX, float offsetY, float offsetZ) {
		MatrixStack.Entry entry = matrices.peek();
		for (Quad q : quads) {
			for (int i = 0; i < 4; i++) {
				consumer.vertex(entry,
								(q.pos[i * 3] + offsetX) / 16f,
								(q.pos[i * 3 + 1] + offsetY) / 16f,
								(q.pos[i * 3 + 2] + offsetZ) / 16f)
						.color(255, 255, 255, 255)
						.texture(q.uv[i * 2], q.uv[i * 2 + 1])
						.overlay(OverlayTexture.DEFAULT_UV)
						.light(light)
						.normal(entry, q.nx, q.ny, q.nz);
			}
		}
	}

	/** Rotacao do cubo em torno da origem dele, na ordem do Blockbench (X, depois Y, depois Z), em graus. */
	private static double[] rotate(double[] point, double[] origin, double[] rotationDeg) {
		double x = point[0] - origin[0], y = point[1] - origin[1], z = point[2] - origin[2];
		double ax = Math.toRadians(rotationDeg[0]), ay = Math.toRadians(rotationDeg[1]), az = Math.toRadians(rotationDeg[2]);

		double cos = Math.cos(ax), sin = Math.sin(ax);
		double y1 = y * cos - z * sin;
		double z1 = y * sin + z * cos;
		y = y1;
		z = z1;

		cos = Math.cos(ay);
		sin = Math.sin(ay);
		double x2 = x * cos + z * sin;
		double z2 = -x * sin + z * cos;
		x = x2;
		z = z2;

		cos = Math.cos(az);
		sin = Math.sin(az);
		double x3 = x * cos - y * sin;
		double y3 = x * sin + y * cos;
		x = x3;
		y = y3;

		return new double[] {x + origin[0], y + origin[1], z + origin[2]};
	}

	private static double[][] faceCorners(String dir, double[] f, double[] t) {
		return switch (dir) {
			case "down" -> new double[][] {{f[0], f[1], t[2]}, {f[0], f[1], f[2]}, {t[0], f[1], f[2]}, {t[0], f[1], t[2]}};
			case "up" -> new double[][] {{f[0], t[1], f[2]}, {f[0], t[1], t[2]}, {t[0], t[1], t[2]}, {t[0], t[1], f[2]}};
			case "north" -> new double[][] {{t[0], t[1], f[2]}, {t[0], f[1], f[2]}, {f[0], f[1], f[2]}, {f[0], t[1], f[2]}};
			case "south" -> new double[][] {{f[0], t[1], t[2]}, {f[0], f[1], t[2]}, {t[0], f[1], t[2]}, {t[0], t[1], t[2]}};
			case "west" -> new double[][] {{f[0], t[1], f[2]}, {f[0], f[1], f[2]}, {f[0], f[1], t[2]}, {f[0], t[1], t[2]}};
			default -> new double[][] {{t[0], t[1], t[2]}, {t[0], f[1], t[2]}, {t[0], f[1], f[2]}, {t[0], t[1], f[2]}};
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

	private static double[] array(JsonArray a) {
		double[] out = new double[a.size()];
		for (int i = 0; i < out.length; i++) out[i] = a.get(i).getAsDouble();
		return out;
	}
}
