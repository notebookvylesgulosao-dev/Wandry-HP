package com.wandryhp.edwiges.model;

/**
 * Transformacao afim 3x4 (linha a linha, double). Matematica pura, sem dependencia do Minecraft, para ser
 * usada igual no servidor (ponto de soltura do envelope) e no cliente (renderizacao).
 */
public final class Affine {
	/** m[0..3] = linha X (a b c tx), m[4..7] = linha Y, m[8..11] = linha Z. */
	public final double[] m = new double[12];

	public static Affine identity() {
		Affine a = new Affine();
		a.m[0] = 1;
		a.m[5] = 1;
		a.m[10] = 1;
		return a;
	}

	public static Affine translation(double x, double y, double z) {
		Affine a = identity();
		a.m[3] = x;
		a.m[7] = y;
		a.m[11] = z;
		return a;
	}

	/** this * o (aplica {@code o} primeiro). */
	public Affine mul(Affine o) {
		Affine r = new Affine();
		for (int row = 0; row < 3; row++) {
			int i = row * 4;
			for (int col = 0; col < 3; col++) {
				r.m[i + col] = m[i] * o.m[col] + m[i + 1] * o.m[4 + col] + m[i + 2] * o.m[8 + col];
			}
			r.m[i + 3] = m[i] * o.m[3] + m[i + 1] * o.m[7] + m[i + 2] * o.m[11] + m[i + 3];
		}
		return r;
	}

	public void apply(double x, double y, double z, double[] out) {
		out[0] = m[0] * x + m[1] * y + m[2] * z + m[3];
		out[1] = m[4] * x + m[5] * y + m[6] * z + m[7];
		out[2] = m[8] * x + m[9] * y + m[10] * z + m[11];
	}

	/** Aplica so a parte linear (direcoes / normais de uma transformacao sem escala nao uniforme). */
	public void applyDirection(double x, double y, double z, double[] out) {
		out[0] = m[0] * x + m[1] * y + m[2] * z;
		out[1] = m[4] * x + m[5] * y + m[6] * z;
		out[2] = m[8] * x + m[9] * y + m[10] * z;
	}

	/** Rotacao do Blockbench: graus, ordem Euler ZYX (X aplicado primeiro): R = Rz * Ry * Rx. */
	public static double[] rotationZYX(double degX, double degY, double degZ) {
		double x = Math.toRadians(degX), y = Math.toRadians(degY), z = Math.toRadians(degZ);
		double cx = Math.cos(x), sx = Math.sin(x), cy = Math.cos(y), sy = Math.sin(y), cz = Math.cos(z), sz = Math.sin(z);
		return new double[] {
				cz * cy, cz * sy * sx - sz * cx, cz * sy * cx + sz * sx,
				sz * cy, sz * sy * sx + cz * cx, sz * sy * cx - cz * sx,
				-sy, cy * sx, cy * cx
		};
	}
}
