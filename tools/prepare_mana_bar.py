#!/usr/bin/env python3
"""Gera a textura da barra de mana da HUD a partir da arte enviada (tools/source/mana_bar_original.png).

Uso: python3 -I tools/prepare_mana_bar.py <raiz do mod>

A arte original e pixel art ampliada (~17,9 px por pixel, com margem transparente). Aqui so se faz o necessario
para usa-la na HUD: corta a margem transparente e reduz a grade ampliada para a grade nativa 108x12, amostrando o
centro de cada celula (sem inventar cores nem redesenhar). Saida: textures/gui/mana_bar.png.
"""
import sys
from pathlib import Path
import numpy as np
from PIL import Image

root = Path(sys.argv[1])
src = root / "tools/source/mana_bar_original.png"
out = root / "src/main/resources/assets/wandryhp/textures/gui/mana_bar.png"

im = Image.open(src).convert("RGBA")
a = np.array(im)
ys, xs = np.where(a[..., 3] > 8)
im = im.crop((xs.min(), ys.min(), xs.max() + 1, ys.max() + 1))
w, h = im.size
H = 12                      # linhas da grade nativa (cada celula ~ h/12 px)
cell = h / H
W = round(w / cell)
a = np.array(im)
res = np.zeros((H, W, 4), np.uint8)
for j in range(H):
    for i in range(W):
        res[j, i] = a[min(h - 1, int((j + .5) * cell)), min(w - 1, int((i + .5) * w / W))]
res[..., 3] = np.where(res[..., 3] > 127, 255, 0)
Image.fromarray(res, "RGBA").save(out)
print(f"{src.name}: {w}x{h} -> {out.name}: {W}x{H}")
