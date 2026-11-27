package com.wandryhp.edwiges.model;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.Reader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Modelo animado lido diretamente de um .bbmodel original do Blockbench (sem converter nem alterar nada):
 * cubos e meshes, grupos (ossos), varias texturas por face e animacoes por keyframes.
 *
 * <p>Classe pura (so Gson): o servidor usa para saber onde o envelope esta preso a coruja no instante da
 * soltura e o cliente usa para desenhar. Unidades: pixels do modelo (1 px = 1/16 de bloco).
 *
 * <p>Convencao de animacao (validada contra o modelo da Edwiges): valores dos keyframes aplicados direto no
 * espaco do modelo. Para um osso com pivo O, posicao P, rotacao R (Euler ZYX, graus) e escala S:
 * {@code p' = O + P + R*S*(p - O)}; osso filho compoe com o pai.
 */
public final class BbModel {
	public static final class Poly {
		/** 4 vertices (xyz). Triangulos repetem o ultimo vertice. */
		public final float[] pos = new float[12];
		/** UV normalizada (0..1) dos 4 vertices. */
		public final float[] uv = new float[8];
		/** Normal de repouso (para fallback de faces degeneradas). */
		public float nx, ny, nz;
		public int texture;
		public int emission;
		public int bone;
	}

	public static final class Bone {
		public final int index;
		public final String name;
		public final String uuid;
		public final int parent;
		public final double[] origin;
		public final double[] restRotation;

		Bone(int index, String name, String uuid, int parent, double[] origin, double[] restRotation) {
			this.index = index;
			this.name = name;
			this.uuid = uuid;
			this.parent = parent;
			this.origin = origin;
			this.restRotation = restRotation;
		}
	}

	private static final class Track {
		final double[] times;
		final double[][] values;
		final boolean[] step;

		Track(double[] times, double[][] values, boolean[] step) {
			this.times = times;
			this.values = values;
			this.step = step;
		}

		double[] sample(double t, double[] fallback) {
			int n = times.length;
			if (n == 0) return fallback;
			if (t <= times[0]) return values[0];
			if (t >= times[n - 1]) return values[n - 1];
			for (int i = 0; i < n - 1; i++) {
				if (t >= times[i] && t <= times[i + 1]) {
					double span = times[i + 1] - times[i];
					if (span <= 0 || step[i]) return values[i + (span <= 0 ? 1 : 0)];
					double f = (t - times[i]) / span;
					double[] a = values[i], b = values[i + 1];
					return new double[] {a[0] + (b[0] - a[0]) * f, a[1] + (b[1] - a[1]) * f, a[2] + (b[2] - a[2]) * f};
				}
			}
			return values[n - 1];
		}
	}

	private static final class BoneAnim {
		Track position, rotation, scale;
	}

	public static final class Animation {
		public final String name;
		public final double length;
		/** "loop", "once" ou "hold", como no arquivo. */
		public final String loopMode;
		private final Map<Integer, BoneAnim> bones = new HashMap<>();

		Animation(String name, double length, String loopMode) {
			this.name = name;
			this.length = length;
			this.loopMode = loopMode;
		}

		public boolean hasKeyframes() {
			return !bones.isEmpty();
		}
	}

	public final List<Bone> bones = new ArrayList<>();
	public final List<Poly> polys = new ArrayList<>();
	public final Map<String, Animation> animations = new HashMap<>();
	public int textureCount;

	private final Map<String, Integer> boneByName = new HashMap<>();

	private BbModel() {}

	// ------------------------------------------------------------------ leitura

	public static BbModel parse(Reader reader) {
		return parse(JsonParser.parseReader(reader).getAsJsonObject());
	}

	public static BbModel parse(JsonObject root) {
		BbModel model = new BbModel();

		double resW = 16, resH = 16;
		if (root.has("resolution")) {
			JsonObject r = root.getAsJsonObject("resolution");
			resW = r.get("width").getAsDouble();
			resH = r.get("height").getAsDouble();
		}
		List<double[]> uvSize = new ArrayList<>();
		if (root.has("textures")) {
			for (JsonElement te : root.getAsJsonArray("textures")) {
				JsonObject t = te.getAsJsonObject();
				double w = t.has("uv_width") && !t.get("uv_width").isJsonNull() ? t.get("uv_width").getAsDouble() : resW;
				double h = t.has("uv_height") && !t.get("uv_height").isJsonNull() ? t.get("uv_height").getAsDouble() : resH;
				uvSize.add(new double[] {w, h});
			}
		}
		model.textureCount = Math.max(1, uvSize.size());
		while (uvSize.size() < model.textureCount) uvSize.add(new double[] {resW, resH});

		Map<String, JsonObject> elements = new HashMap<>();
		for (JsonElement e : root.getAsJsonArray("elements")) {
			JsonObject o = e.getAsJsonObject();
			elements.put(o.get("uuid").getAsString(), o);
		}
		Map<String, JsonObject> groups = new HashMap<>();
		if (root.has("groups")) {
			for (JsonElement g : root.getAsJsonArray("groups")) {
				if (g.isJsonObject()) {
					JsonObject o = g.getAsJsonObject();
					if (o.has("uuid")) groups.put(o.get("uuid").getAsString(), o);
				}
			}
		}

		// Osso 0 sintetico: elementos soltos na raiz (identidade).
		model.addBone("", "", -1, new double[3], new double[3]);
		for (JsonElement item : root.getAsJsonArray("outliner")) {
			model.readOutlinerItem(item, 0, elements, groups, uvSize);
		}

		if (root.has("animations")) {
			for (JsonElement ae : root.getAsJsonArray("animations")) {
				model.readAnimation(ae.getAsJsonObject());
			}
		}
		return model;
	}

	private int addBone(String name, String uuid, int parent, double[] origin, double[] rot) {
		int index = bones.size();
		bones.add(new Bone(index, name, uuid, parent, origin, rot));
		if (!name.isEmpty()) boneByName.putIfAbsent(name, index);
		return index;
	}

	private void readOutlinerItem(JsonElement item, int parentBone, Map<String, JsonObject> elements,
								  Map<String, JsonObject> groups, List<double[]> uvSize) {
		if (item.isJsonPrimitive()) {
			JsonObject el = elements.get(item.getAsString());
			if (el != null) addElement(el, parentBone, uvSize);
			return;
		}
		JsonObject node = item.getAsJsonObject();
		JsonObject def = node.has("uuid") && groups.containsKey(node.get("uuid").getAsString())
				? groups.get(node.get("uuid").getAsString()) : node;
		String name = def.has("name") ? def.get("name").getAsString() : (node.has("name") ? node.get("name").getAsString() : "");
		String uuid = node.has("uuid") ? node.get("uuid").getAsString() : "";
		double[] origin = def.has("origin") ? array(def.getAsJsonArray("origin")) : new double[3];
		double[] rot = def.has("rotation") ? array(def.getAsJsonArray("rotation")) : new double[3];
		int bone = addBone(name, uuid, parentBone, origin, rot);
		JsonArray children = node.has("children") ? node.getAsJsonArray("children") : new JsonArray();
		for (JsonElement child : children) {
			readOutlinerItem(child, bone, elements, groups, uvSize);
		}
	}

	private void addElement(JsonObject el, int bone, List<double[]> uvSize) {
		if (el.has("export") && !el.get("export").getAsBoolean()) return;
		String type = el.get("type").getAsString();
		if ("cube".equals(type)) {
			addCube(el, bone, uvSize);
		} else if ("mesh".equals(type)) {
			addMesh(el, bone, uvSize);
		}
	}

	private float[] normUv(JsonArray uv, int tex, List<double[]> uvSize, int i) {
		double[] s = uvSize.get(Math.min(tex, uvSize.size() - 1));
		return new float[] {(float) (uv.get(i).getAsDouble() / s[0]), (float) (uv.get(i + 1).getAsDouble() / s[1])};
	}

	private static final String[] DIRECTIONS = {"down", "up", "north", "south", "west", "east"};

	private void addCube(JsonObject cube, int bone, List<double[]> uvSize) {
		double[] from = array(cube.getAsJsonArray("from"));
		double[] to = array(cube.getAsJsonArray("to"));
		// "inflate" (camadas externas, como o chapeu/jaqueta do Steve): engorda o cubo em todos os lados.
		double inflate = cube.has("inflate") && !cube.get("inflate").isJsonNull() ? cube.get("inflate").getAsDouble() : 0.0;
		if (inflate != 0.0) {
			for (int a = 0; a < 3; a++) {
				from[a] -= inflate;
				to[a] += inflate;
			}
		}
		double[] origin = cube.has("origin") ? array(cube.getAsJsonArray("origin")) : new double[3];
		double[] rotation = cube.has("rotation") && !cube.get("rotation").isJsonNull() ? array(cube.getAsJsonArray("rotation")) : new double[3];
		double[] r = Affine.rotationZYX(rotation[0], rotation[1], rotation[2]);
		int emission = cube.has("light_emission") ? cube.get("light_emission").getAsInt() : 0;
		JsonObject faces = cube.getAsJsonObject("faces");

		for (String dir : DIRECTIONS) {
			if (!faces.has(dir)) continue;
			JsonObject face = faces.getAsJsonObject(dir);
			if (!face.has("texture") || face.get("texture").isJsonNull()) continue;
			int tex = face.get("texture").getAsInt();

			Poly p = new Poly();
			p.texture = tex;
			p.emission = emission;
			p.bone = bone;
			double[][] corners = faceCorners(dir, from, to);
			for (int i = 0; i < 4; i++) {
				double[] d = {corners[i][0] - origin[0], corners[i][1] - origin[1], corners[i][2] - origin[2]};
				for (int a = 0; a < 3; a++) {
					p.pos[i * 3 + a] = (float) (r[a * 3] * d[0] + r[a * 3 + 1] * d[1] + r[a * 3 + 2] * d[2] + origin[a]);
				}
			}
			double[] n = normal(dir);
			p.nx = (float) (r[0] * n[0] + r[1] * n[1] + r[2] * n[2]);
			p.ny = (float) (r[3] * n[0] + r[4] * n[1] + r[5] * n[2]);
			p.nz = (float) (r[6] * n[0] + r[7] * n[1] + r[8] * n[2]);

			JsonArray uv = face.getAsJsonArray("uv");
			float[] a = normUv(uv, tex, uvSize, 0);
			float[] b = normUv(uv, tex, uvSize, 2);
			float[][] corner = {{a[0], a[1]}, {a[0], b[1]}, {b[0], b[1]}, {b[0], a[1]}};
			int shift = face.has("rotation") ? Math.floorMod(face.get("rotation").getAsInt() / 90, 4) : 0;
			for (int i = 0; i < 4; i++) {
				float[] c = corner[(i + shift) % 4];
				p.uv[i * 2] = c[0];
				p.uv[i * 2 + 1] = c[1];
			}
			polys.add(p);
		}
	}

	private void addMesh(JsonObject mesh, int bone, List<double[]> uvSize) {
		JsonObject vertices = mesh.getAsJsonObject("vertices");
		Map<String, double[]> v = new HashMap<>();
		for (Map.Entry<String, JsonElement> e : vertices.entrySet()) v.put(e.getKey(), array(e.getValue().getAsJsonArray()));
		double[] origin = mesh.has("origin") ? array(mesh.getAsJsonArray("origin")) : new double[3];
		double[] rotation = mesh.has("rotation") ? array(mesh.getAsJsonArray("rotation")) : new double[3];
		double[] r = Affine.rotationZYX(rotation[0], rotation[1], rotation[2]);

		for (Map.Entry<String, JsonElement> fentry : mesh.getAsJsonObject("faces").entrySet()) {
			JsonObject face = fentry.getValue().getAsJsonObject();
			if (!face.has("texture") || face.get("texture").isJsonNull()) continue;
			int tex = face.get("texture").getAsInt();
			JsonArray ids = face.getAsJsonArray("vertices");
			JsonObject uvs = face.getAsJsonObject("uv");
			int n = ids.size();
			if (n < 3) continue;
			// Fan: (0,k,k+1). Triangulo vira poly de 4 vertices repetindo o ultimo.
			for (int k = 1; k < n - 1; k++) {
				int[] pick = (n == 4) ? new int[] {0, 1, 2, 3} : new int[] {0, k, k + 1, k + 1};
				Poly p = new Poly();
				p.texture = tex;
				p.bone = bone;
				for (int i = 0; i < 4; i++) {
					String id = ids.get(pick[i]).getAsString();
					double[] pt = v.get(id);
					double[] d = {pt[0] - origin[0], pt[1] - origin[1], pt[2] - origin[2]};
					for (int a = 0; a < 3; a++) {
						p.pos[i * 3 + a] = (float) (r[a * 3] * d[0] + r[a * 3 + 1] * d[1] + r[a * 3 + 2] * d[2] + origin[a]);
					}
					double[] st = array(uvs.getAsJsonArray(id));
					double[] size = uvSize.get(Math.min(tex, uvSize.size() - 1));
					p.uv[i * 2] = (float) (st[0] / size[0]);
					p.uv[i * 2 + 1] = (float) (st[1] / size[1]);
				}
				computeRestNormal(p);
				polys.add(p);
				if (n == 4) break; // quad inteiro de uma vez
			}
		}
	}

	private static void computeRestNormal(Poly p) {
		double ax = p.pos[3] - p.pos[0], ay = p.pos[4] - p.pos[1], az = p.pos[5] - p.pos[2];
		double bx = p.pos[6] - p.pos[0], by = p.pos[7] - p.pos[1], bz = p.pos[8] - p.pos[2];
		double nx = ay * bz - az * by, ny = az * bx - ax * bz, nz = ax * by - ay * bx;
		double len = Math.sqrt(nx * nx + ny * ny + nz * nz);
		if (len > 1.0E-9) {
			p.nx = (float) (nx / len);
			p.ny = (float) (ny / len);
			p.nz = (float) (nz / len);
		} else {
			p.ny = 1;
		}
	}

	/** Mesma ordem de vertices e de UV do bake vanilla (CubeFace); cross(v1-v0, v2-v0) aponta para fora. */
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

	// ------------------------------------------------------------------ animacoes

	private void readAnimation(JsonObject a) {
		String name = a.get("name").getAsString();
		double length = a.has("length") ? a.get("length").getAsDouble() : 0;
		String loop = a.has("loop") ? a.get("loop").getAsString() : "once";
		Animation anim = new Animation(name, length, loop);
		if (a.has("animators")) {
			for (Map.Entry<String, JsonElement> entry : a.getAsJsonObject("animators").entrySet()) {
				JsonObject an = entry.getValue().getAsJsonObject();
				if (!an.has("keyframes")) continue; // ex.: "idle" (so pose de repouso) e o animator de efeitos
				if (an.has("type") && !"bone".equals(an.get("type").getAsString())) continue;
				int bone = -1;
				for (Bone b : bones) {
					if (b.uuid.equals(entry.getKey())) {
						bone = b.index;
						break;
					}
				}
				if (bone < 0 && an.has("name")) bone = boneByName.getOrDefault(an.get("name").getAsString(), -1);
				if (bone < 0) continue;
				BoneAnim ba = new BoneAnim();
				ba.position = readTrack(an.getAsJsonArray("keyframes"), "position");
				ba.rotation = readTrack(an.getAsJsonArray("keyframes"), "rotation");
				ba.scale = readTrack(an.getAsJsonArray("keyframes"), "scale");
				anim.bones.put(bone, ba);
			}
		}
		animations.put(name, anim);
	}

	private static Track readTrack(JsonArray keyframes, String channel) {
		List<double[]> list = new ArrayList<>(); // {time, x, y, z, step}
		for (JsonElement ke : keyframes) {
			JsonObject kf = ke.getAsJsonObject();
			if (!channel.equals(kf.get("channel").getAsString())) continue;
			JsonArray dp = kf.getAsJsonArray("data_points");
			if (dp.size() == 0) continue;
			JsonObject p = dp.get(0).getAsJsonObject();
			boolean step = kf.has("interpolation") && "step".equals(kf.get("interpolation").getAsString());
			list.add(new double[] {kf.get("time").getAsDouble(), number(p, "x"), number(p, "y"), number(p, "z"), step ? 1 : 0});
		}
		// O Blockbench pode gravar keyframes fora de ordem.
		list.sort((x, y) -> Double.compare(x[0], y[0]));
		int n = list.size();
		double[] times = new double[n];
		double[][] values = new double[n][];
		boolean[] step = new boolean[n];
		for (int i = 0; i < n; i++) {
			double[] k = list.get(i);
			times[i] = k[0];
			values[i] = new double[] {k[1], k[2], k[3]};
			step[i] = k[4] > 0;
		}
		return new Track(times, values, step);
	}

	/** Valores podem vir como numero ou texto com espacos/quebras de linha ("145\n\n"); Molang nao e suportado (0). */
	private static double number(JsonObject p, String key) {
		if (!p.has(key) || p.get(key).isJsonNull()) return 0;
		try {
			return Double.parseDouble(p.get(key).getAsString().trim());
		} catch (NumberFormatException e) {
			return 0;
		}
	}

	// ------------------------------------------------------------------ pose

	public Animation animation(String name) {
		return animations.get(name);
	}

	public int boneIndex(String name) {
		return boneByName.getOrDefault(name, -1);
	}

	/**
	 * Matriz de cada osso (modelo -> espaco do modelo, em pixels) na animacao e no instante dados (segundos).
	 * {@code anim == null} ou sem keyframes = pose de repouso. Com {@code loop} o tempo volta ao inicio ao
	 * passar do fim; sem, trava no ultimo quadro.
	 */
	public Affine[] pose(Animation anim, double time, boolean loop) {
		double t = time;
		if (anim != null && anim.length > 0) {
			t = loop ? ((t % anim.length) + anim.length) % anim.length : Math.max(0, Math.min(t, anim.length));
		}
		Affine[] world = new Affine[bones.size()];
		for (Bone b : bones) {
			Affine local = Affine.identity();
			if (b.index != 0) {
				double[] pos = {0, 0, 0}, rot = b.restRotation.clone(), scl = {1, 1, 1};
				BoneAnim ba = anim == null ? null : anim.bones.get(b.index);
				if (ba != null) {
					if (ba.position != null) pos = ba.position.sample(t, pos);
					if (ba.rotation != null) {
						double[] ar = ba.rotation.sample(t, new double[3]);
						rot = new double[] {rot[0] + ar[0], rot[1] + ar[1], rot[2] + ar[2]};
					}
					if (ba.scale != null) scl = ba.scale.sample(t, scl);
				}
				double[] r = Affine.rotationZYX(rot[0], rot[1], rot[2]);
				// p' = O + P + R*S*(p - O)
				for (int row = 0; row < 3; row++) {
					for (int col = 0; col < 3; col++) local.m[row * 4 + col] = r[row * 3 + col] * scl[col];
					local.m[row * 4 + 3] = b.origin[row] + pos[row]
							- (local.m[row * 4] * b.origin[0] + local.m[row * 4 + 1] * b.origin[1] + local.m[row * 4 + 2] * b.origin[2]);
				}
			}
			world[b.index] = b.parent >= 0 ? world[b.parent].mul(local) : local;
		}
		return world;
	}

	// ------------------------------------------------------------------ medidas (pose de repouso)

	/** {minX, minY, minZ, maxX, maxY, maxZ} da geometria de repouso do osso (ou de tudo se {@code bone < 0}). */
	public double[] bounds(int bone) {
		double[] b = {Double.MAX_VALUE, Double.MAX_VALUE, Double.MAX_VALUE, -Double.MAX_VALUE, -Double.MAX_VALUE, -Double.MAX_VALUE};
		for (Poly p : polys) {
			if (bone >= 0 && p.bone != bone) continue;
			for (int i = 0; i < 4; i++) {
				for (int a = 0; a < 3; a++) {
					b[a] = Math.min(b[a], p.pos[i * 3 + a]);
					b[3 + a] = Math.max(b[3 + a], p.pos[i * 3 + a]);
				}
			}
		}
		return b;
	}

	private static double[] array(JsonArray a) {
		double[] out = new double[a.size()];
		for (int i = 0; i < out.length; i++) out[i] = a.get(i).getAsDouble();
		return out;
	}

	@Override
	public String toString() {
		return "BbModel{bones=" + bones.size() + ", polys=" + polys.size() + ", animations=" + Arrays.toString(animations.keySet().toArray()) + "}";
	}
}
