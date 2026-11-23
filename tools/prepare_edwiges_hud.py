#!/usr/bin/env python3
"""Gera as texturas da HUD de chegada da Edwiges a partir da arte enviada (tools/source/edwiges_hud_original.png).

Uso: python3 -I tools/prepare_edwiges_hud.py <raiz do mod>

A arte original e pixel art ampliada (~13,3 px por pixel, com margem transparente). Como em prepare_mana_bar.py,
so se faz o necessario: corta a margem, reduz a grade ampliada para a grade nativa (148x26) amostrando o centro de
cada celula e separa o que e dinamico:
  - textures/gui/edwiges_hud.png       148x26: pergaminho + coruja + titulo + trilho VAZIO da barra
                                       (preenchimento e texto da distancia removidos; o pergaminho e reconstruido
                                       com a mediana dos pixels vizinhos).
  - textures/gui/edwiges_hud_fill.png   79x5: preenchimento da barra (cantos arredondados), recortado em runtime.
Constantes de layout correspondentes estao em EdwigesHud.java.
"""
import sys
from pathlib import Path
import numpy as np
from PIL import Image

root = Path(sys.argv[1])
src = root / "tools/source/edwiges_hud_original.png"
out = root / "src/main/resources/assets/wandryhp/textures/gui"

im = Image.open(src).convert("RGBA")
a = np.array(im)
ys, xs = np.where(a[..., 3] > 8)
im = im.crop((xs.min(), ys.min(), xs.max() + 1, ys.max() + 1))
w, h = im.size
H = 26
W = round(w / (h / H))  # 148
a = np.array(im)
g = np.zeros((H, W, 4), np.uint8)
for j in range(H):
    for i in range(W):
        g[j, i] = a[min(h - 1, int((j + .5) * h / H)), min(w - 1, int((i + .5) * w / W))]
g[..., 3] = np.where(g[..., 3] > 127, 255, 0)

# --- layout medido na grade nativa -------------------------------------------------------------------------
BAR_ROWS = range(16, 21)
INNER = {17: (32, 109), 18: (32, 110), 19: (32, 110), 20: (32, 109)}  # x inicial/final (inclusive) do preenchimento
TEXT_X0, DIST_X0, DIST_Y0, DIST_Y1 = 32, 112, 14, 22                    # area do texto da distancia
FILL_PROFILE = {r: tuple(int(v) for v in g[r, 60, :3]) for r in INNER}   # cor do preenchimento por linha
EMPTY_PROFILE = {r: tuple(int(v) for v in g[r, 105, :3]) for r in INNER} # cor do trilho vazio por linha

def is_dark(p):
    return p[3] > 0 and p[0] < 190 and p[1] < 100

# 1) trilho vazio: pinta o trecho preenchido com a cor do trilho
bg = g.copy()
for r, (x0, x1) in INNER.items():
    for x in range(x0, x1 + 1):
        bg[r, x, :3] = EMPTY_PROFILE[r]

# 2) remove o texto da distancia (direita da barra) reconstruindo o pergaminho
mask = np.zeros((H, W), bool)
for j in range(DIST_Y0, DIST_Y1 + 1):
    for i in range(DIST_X0, W - 1):
        if is_dark(bg[j, i]):
            mask[j, i] = True
big = mask.copy()
for dj in (-1, 0, 1):
    for di in (-1, 0, 1):
        big |= np.roll(np.roll(mask, dj, 0), di, 1)
big[:, :DIST_X0] = False
big[:DIST_Y0, :] = False
big[DIST_Y1 + 1:, :] = False
big[:, W - 1:] = False
ref = bg.copy()
for j, i in zip(*np.where(big)):
    vals = []
    for dj in range(-4, 5):
        for di in range(-4, 5):
            jj, ii = j + dj, i + di
            if 0 <= jj < H and 0 <= ii < W and not big[jj, ii] and ref[jj, ii, 3] and ref[jj, ii, 0] > 190 and ref[jj, ii, 1] > 150:
                vals.append(ref[jj, ii, :3])
    bg[j, i, :3] = np.median(np.array(vals), axis=0).astype(np.uint8) if vals else (230, 198, 152)

# 3) sprite do preenchimento (79x5, x 32..110, linhas 16..20)
fw = 110 - 32 + 1
fill = np.zeros((5, fw, 4), np.uint8)
for r, (x0, x1) in INNER.items():
    for x in range(x0, x1 + 1):
        fill[r - 16, x - 32] = (*FILL_PROFILE[r], 255)

out.mkdir(parents=True, exist_ok=True)
Image.fromarray(bg, "RGBA").save(out / "edwiges_hud.png")
Image.fromarray(fill, "RGBA").save(out / "edwiges_hud_fill.png")
print(f"{src.name}: {w}x{h} -> edwiges_hud.png {W}x{H}, edwiges_hud_fill.png {fw}x5")
