package com.wandryhp.client.edwiges;

import com.wandryhp.edwiges.model.Affine;
import com.wandryhp.edwiges.model.BbModel;
import net.minecraft.client.render.LightmapTextureManager;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.util.math.MatrixStack;

/**
 * Desenha um {@link BbModel} na pose dada: cada vertice e transformado pela matriz do osso (e por uma matriz
 * base opcional) na CPU e enviado ao consumidor. Um consumidor por vez, com as faces de UMA textura.
 */
public final class BbModelDrawer {
	private BbModelDrawer() {}

	/**
	 * @param pose  matrizes dos ossos (de {@link BbModel#pose})
	 * @param base  aplicada depois do osso (px do modelo); null = identidade
	 * @param scale px -> blocos
	 */
	public static void draw(BbModel model, Affine[] pose, Affine base, int texture, MatrixStack.Entry entry,
					 VertexConsumer consumer, int light, double scale) {
		Affine[] mats = new Affine[pose.length];
		for (int i = 0; i < pose.length; i++) {
			mats[i] = base == null ? pose[i] : base.mul(pose[i]);
		}
		double[] o = new double[3];
		double[] n = new double[3];
		float[] v = new float[12];

		for (BbModel.Poly p : model.polys) {
			if (Math.min(p.texture, model.textureCount - 1) != texture) continue;
			Affine m = mats[p.bone];
			for (int i = 0; i < 4; i++) {
				m.apply(p.pos[i * 3], p.pos[i * 3 + 1], p.pos[i * 3 + 2], o);
				v[i * 3] = (float) (o[0] * scale);
				v[i * 3 + 1] = (float) (o[1] * scale);
				v[i * 3 + 2] = (float) (o[2] * scale);
			}
			// Normal geometrica (cross(v1-v0, v2-v0) aponta para fora); correta mesmo com escala nao uniforme.
			double ax = v[3] - v[0], ay = v[4] - v[1], az = v[5] - v[2];
			double bx = v[6] - v[0], by = v[7] - v[1], bz = v[8] - v[2];
			n[0] = ay * bz - az * by;
			n[1] = az * bx - ax * bz;
			n[2] = ax * by - ay * bx;
			double len = Math.sqrt(n[0] * n[0] + n[1] * n[1] + n[2] * n[2]);
			if (len < 1.0E-12) { // face degenerada (ex.: escala zero): usa a normal de repouso
				m.applyDirection(p.nx, p.ny, p.nz, n);
				len = Math.sqrt(n[0] * n[0] + n[1] * n[1] + n[2] * n[2]);
			}
			if (len < 1.0E-12) {
				n[0] = 0;
				n[1] = 1;
				n[2] = 0;
				len = 1;
			}
			float nx = (float) (n[0] / len), ny = (float) (n[1] / len), nz = (float) (n[2] / len);

			int l = light;
			if (p.emission > 0) {
				l = LightmapTextureManager.pack(
						Math.max(p.emission, LightmapTextureManager.getBlockLightCoordinates(light)),
						LightmapTextureManager.getSkyLightCoordinates(light));
			}
			for (int i = 0; i < 4; i++) {
				consumer.vertex(entry, v[i * 3], v[i * 3 + 1], v[i * 3 + 2])
						.color(255, 255, 255, 255)
						.texture(p.uv[i * 2], p.uv[i * 2 + 1])
						.overlay(OverlayTexture.DEFAULT_UV)
						.light(l)
						.normal(entry, nx, ny, nz);
			}
		}
	}
}
