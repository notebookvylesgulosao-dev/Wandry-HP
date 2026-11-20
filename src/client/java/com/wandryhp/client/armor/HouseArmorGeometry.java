package com.wandryhp.client.armor;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.util.math.MatrixStack;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * Geometria de um peitoral construida diretamente do .bbmodel original (cubos body / RightArm / LeftArm).
 *
 * <p>Le exatamente o que esta no arquivo: from/to, inflate e UV por face (box UV ja vem resolvido por face no
 * arquivo). Cubos ocultos no Blockbench (visibility: false) ou com export desligado nao sao desenhados; o unico
 * caso nos modelos atuais e um corpo vanilla escondido dentro do corpo inflado, que nunca seria visivel.
 *
 * <p>Sistema de coordenadas: o Blockbench usa +X = direita do personagem, +Y = cima, frente = norte (-Z), pes em
 * Y=0 e ombros em Y=24. Os modelos de entidade do Minecraft usam +X = esquerda do personagem, +Y = baixo, frente
 * tambem em -Z. A conversao (x, y, z) -> (-x, 24 - y, z) e uma rotacao de 180 graus em torno de Z, portanto
 * preserva a orientacao das faces e das UVs. O centro do personagem em X/Z e tirado do corpo (no .bbmodel e 0 ou 8).
 */
public final class HouseArmorGeometry {
	/** Partes do modelo do jogador em que o peitoral se encaixa. */
	public enum Part {
		BODY("body"),
		RIGHT_ARM("RightArm"),
		LEFT_ARM("LeftArm"),
		/** leg0 = perna direita do personagem, leg1 = esquerda (nos .bbmodel da calca e das botas). */
		RIGHT_LEG("leg0"),
		LEFT_LEG("leg1");

		/** Nome dos cubos desta parte no .bbmodel. */
		final String cubeName;

		Part(String cubeName) {
			this.cubeName = cubeName;
		}
	}

	/**
	 * Folga extra (em pixels) nos cubos dos bracos. Os bracos do .bbmodel tem exatamente o tamanho do braco da
	 * pele; sem folga as faces coincidem e piscam (z-fighting). 0,25 e o mesmo valor da camada de manga vanilla.
	 */
	private static final float ARM_EXTRA_INFLATE = 0.25f;

	/**
	 * Folga extra (em pixels) nos cubos das pernas, pelo mesmo motivo dos bracos: os cubos do .bbmodel tem o
	 * tamanho exato da perna da pele (z-fighting com a pele e com a camada de calca de 0,25). A calca fica um
	 * pouco acima da camada da pele e as botas um pouco acima da calca, para nao disputarem a mesma superficie.
	 */
	public static final float LEGGINGS_EXTRA_INFLATE = 0.3f;
	public static final float BOOTS_EXTRA_INFLATE = 0.5f;

	private static final String[] DIRECTIONS = {"down", "up", "north", "south", "west", "east"};

	private static final class Quad {
		final float[] pos = new float[12]; // espaco do modelo de entidade, em pixels
		final float[] uv = new float[8];
		float nx, ny, nz;
	}

	private final Map<Part, List<Quad>> quads = new EnumMap<>(Part.class);

	private HouseArmorGeometry() {
		for (Part part : Part.values()) {
			quads.put(part, new ArrayList<>());
		}
	}

	public static HouseArmorGeometry fromBbmodel(JsonObject root) {
		return fromBbmodel(root, 0f);
	}

	/** {@code legExtraInflate}: folga extra aplicada aos cubos leg0/leg1 (ignorada nos modelos de peitoral). */
	public static HouseArmorGeometry fromBbmodel(JsonObject root, float legExtraInflate) {
		JsonObject resolution = root.has("resolution") ? root.getAsJsonObject("resolution") : null;
		float resW = resolution != null ? resolution.get("width").getAsFloat() : 16f;
		float resH = resolution != null ? resolution.get("height").getAsFloat() : 16f;

		List<JsonObject> cubes = new ArrayList<>();
		for (JsonElement el : root.getAsJsonArray("elements")) {
			JsonObject cube = el.getAsJsonObject();
			if (!"cube".equals(cube.get("type").getAsString())) continue;
			if (cube.has("export") && !cube.get("export").getAsBoolean()) continue;
			if (cube.has("visibility") && !cube.get("visibility").getAsBoolean()) continue;
			if (cube.has("rotation")) {
				double[] rotation = array(cube.getAsJsonArray("rotation"));
				if (rotation[0] != 0 || rotation[1] != 0 || rotation[2] != 0) {
					System.err.println("[wandryhp] cubo '" + cube.get("name").getAsString()
							+ "' do peitoral tem rotacao; nao suportada, ignorada.");
				}
			}
			cubes.add(cube);
		}

		// Centro do personagem em X/Z: caixa dos cubos do corpo; sem corpo (calca/botas), a caixa das pernas.
		double centerX = 0, centerZ = 0;
		boolean found = false;
		for (int pass = 0; pass < 2 && !found; pass++) {
			double minX = Double.MAX_VALUE, maxX = -Double.MAX_VALUE, minZ = Double.MAX_VALUE, maxZ = -Double.MAX_VALUE;
			for (JsonObject cube : cubes) {
				String name = cube.get("name").getAsString();
				boolean use = pass == 0 ? Part.BODY.cubeName.equals(name)
						: Part.RIGHT_LEG.cubeName.equals(name) || Part.LEFT_LEG.cubeName.equals(name);
				if (!use) continue;
				double[] from = array(cube.getAsJsonArray("from"));
				double[] to = array(cube.getAsJsonArray("to"));
				minX = Math.min(minX, from[0]);
				maxX = Math.max(maxX, to[0]);
				minZ = Math.min(minZ, from[2]);
				maxZ = Math.max(maxZ, to[2]);
				found = true;
			}
			if (found) {
				centerX = (minX + maxX) / 2.0;
				centerZ = (minZ + maxZ) / 2.0;
			}
		}
		if (!found) {
			throw new IllegalArgumentException("bbmodel sem cubo 'body', 'leg0' ou 'leg1'");
		}

		HouseArmorGeometry geometry = new HouseArmorGeometry();
		for (JsonObject cube : cubes) {
			Part part = partOf(cube.get("name").getAsString());
			if (part == null) continue;

			double inflate = cube.has("inflate") ? cube.get("inflate").getAsDouble() : 0.0;
			if (part == Part.RIGHT_ARM || part == Part.LEFT_ARM) {
				inflate += ARM_EXTRA_INFLATE;
			} else if (part == Part.RIGHT_LEG || part == Part.LEFT_LEG) {
				inflate += legExtraInflate;
			}
			double[] from = array(cube.getAsJsonArray("from"));
			double[] to = array(cube.getAsJsonArray("to"));
			for (int a = 0; a < 3; a++) {
				from[a] -= inflate;
				to[a] += inflate;
			}
			JsonObject faces = cube.getAsJsonObject("faces");

			for (String dir : DIRECTIONS) {
				if (!faces.has(dir)) continue;
				JsonObject face = faces.getAsJsonObject(dir);
				if (!face.has("texture") || face.get("texture").isJsonNull()) continue;

				Quad quad = new Quad();
				double[][] corners = faceCorners(dir, from, to);
				for (int i = 0; i < 4; i++) {
					quad.pos[i * 3] = (float) -(corners[i][0] - centerX);
					quad.pos[i * 3 + 1] = (float) (24.0 - corners[i][1]);
					quad.pos[i * 3 + 2] = (float) (corners[i][2] - centerZ);
				}
				double[] n = normal(dir);
				quad.nx = (float) -n[0];
				quad.ny = (float) -n[1];
				quad.nz = (float) n[2];

				// UV: [u1, v1, u2, v2] em pixels da resolucao do projeto, normalizado para 0..1.
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
				geometry.quads.get(part).add(quad);
			}
		}
		return geometry;
	}

	private static Part partOf(String cubeName) {
		for (Part part : Part.values()) {
			if (part.cubeName.equals(cubeName)) return part;
		}
		return null;
	}

	/**
	 * Desenha uma parte. A matriz deve ja estar na origem da parte (ModelPart#rotate); (pivotX, pivotY, pivotZ) e o
	 * pivo padrao real da parte no modelo em uso (bracos largos ou finos), em pixels.
	 */
	public void render(Part part, MatrixStack matrices, VertexConsumer consumer, int light,
					   float pivotX, float pivotY, float pivotZ) {
		MatrixStack.Entry entry = matrices.peek();
		for (Quad q : quads.get(part)) {
			for (int i = 0; i < 4; i++) {
				consumer.vertex(entry,
								(q.pos[i * 3] - pivotX) / 16f,
								(q.pos[i * 3 + 1] - pivotY) / 16f,
								(q.pos[i * 3 + 2] - pivotZ) / 16f)
						.color(255, 255, 255, 255)
						.texture(q.uv[i * 2], q.uv[i * 2 + 1])
						.overlay(OverlayTexture.DEFAULT_UV)
						.light(light)
						.normal(entry, q.nx, q.ny, q.nz);
			}
		}
	}

	/** Mesma ordem de vertices e de UV do bake vanilla (igual a PropGeometry). */
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

	private static double[] array(JsonArray a) {
		double[] out = new double[a.size()];
		for (int i = 0; i < out.length; i++) out[i] = a.get(i).getAsDouble();
		return out;
	}
}
