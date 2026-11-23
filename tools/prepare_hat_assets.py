#!/usr/bin/env python3
"""Prepara os assets dos chapeus (itens enchanter_<cor>_cape) a partir dos .bbmodel de origem.

Para cada cor (red, blue, green, yellow, purple):
  * copia o .bbmodel byte a byte para assets/wandryhp/bbmodels/accessory/ (o arquivo nunca e alterado);
  * extrai a textura embutida (atlas.png) para textures/entity/accessory/;
  * renderiza o icone do item (64x64, amostragem nearest) para textures/item/.

A rasterizacao replica a matematica de CapeGeometry.java (ancora na aba, rotacao X->Y->Z do cubo, Blockbench
(x, y, z) -> entidade (-x, -y, z)), entao o icone tambem serve para conferir a geometria sem abrir o jogo.

Uso: python3 tools/prepare_hat_assets.py <pasta com witch-<cor>.bbmodel>
"""
import base64
import io
import json
import math
import shutil
import sys
from pathlib import Path

import numpy as np
from PIL import Image

ROOT = Path(__file__).resolve().parent.parent
ASSETS = ROOT / "src/main/resources/assets/wandryhp"
COLORS = ["red", "blue", "green", "yellow", "purple"]
DIRECTIONS = ["down", "up", "north", "south", "west", "east"]
ICON = 64


def face_corners(d, f, t):
    return {
        "down": [(f[0], f[1], t[2]), (f[0], f[1], f[2]), (t[0], f[1], f[2]), (t[0], f[1], t[2])],
        "up": [(f[0], t[1], f[2]), (f[0], t[1], t[2]), (t[0], t[1], t[2]), (t[0], t[1], f[2])],
        "north": [(t[0], t[1], f[2]), (t[0], f[1], f[2]), (f[0], f[1], f[2]), (f[0], t[1], f[2])],
        "south": [(f[0], t[1], t[2]), (f[0], f[1], t[2]), (t[0], f[1], t[2]), (t[0], t[1], t[2])],
        "west": [(f[0], t[1], f[2]), (f[0], f[1], f[2]), (f[0], f[1], t[2]), (f[0], t[1], t[2])],
        "east": [(t[0], t[1], t[2]), (t[0], f[1], t[2]), (t[0], f[1], f[2]), (t[0], t[1], f[2])],
    }[d]


def rotate(p, origin, rot):
    x, y, z = (p[i] - origin[i] for i in range(3))
    ax, ay, az = (math.radians(r) for r in rot)
    c, s = math.cos(ax), math.sin(ax)
    y, z = y * c - z * s, y * s + z * c
    c, s = math.cos(ay), math.sin(ay)
    x, z = x * c + z * s, -x * s + z * c
    c, s = math.cos(az), math.sin(az)
    x, y = x * c - y * s, x * s + y * c
    return (x + origin[0], y + origin[1], z + origin[2])


def load_quads(model):
    res_w, res_h = model["resolution"]["width"], model["resolution"]["height"]
    raw, best, anchor = [], -1.0, (0, 0, 0)
    for cube in model["elements"]:
        if cube.get("type") != "cube" or not cube.get("export", True) or not cube.get("visibility", True):
            continue
        inflate = cube.get("inflate", 0.0)
        f = [v - inflate for v in cube["from"]]
        t = [v + inflate for v in cube["to"]]
        foot = (t[0] - f[0]) * (t[2] - f[2])
        if foot > best:
            best, anchor = foot, ((f[0] + t[0]) / 2, f[1], (f[2] + t[2]) / 2)
        rot = cube.get("rotation", [0, 0, 0])
        origin = cube.get("origin", [0, 0, 0])
        for d in DIRECTIONS:
            face = cube["faces"].get(d)
            if not face or face.get("texture") is None:
                continue
            pts = [rotate(c, origin, rot) for c in face_corners(d, f, t)]
            u1, v1, u2, v2 = face["uv"]
            u1, u2 = u1 / res_w, u2 / res_w
            v1, v2 = v1 / res_h, v2 / res_h
            corner = [(u1, v1), (u1, v2), (u2, v2), (u2, v1)]
            shift = (face.get("rotation", 0) // 90) % 4
            uvs = [corner[(i + shift) % 4] for i in range(4)]
            raw.append((pts, uvs))
    out = []
    for pts, uvs in raw:
        out.append(([(-(p[0] - anchor[0]), -(p[1] - anchor[1]), p[2] - anchor[2]) for p in pts], uvs))
    return out


def render_icon(quads, tex, yaw=-35.0, pitch=28.0):
    th, ph = math.radians(yaw), math.radians(pitch)

    def project(p):
        x, y, z = p
        x, z = x * math.cos(th) + z * math.sin(th), -x * math.sin(th) + z * math.cos(th)
        ys = y * math.cos(ph) - z * math.sin(ph)
        depth = y * math.sin(ph) + z * math.cos(ph)
        return x, ys, depth

    proj = [([project(p) for p in pts], uvs) for pts, uvs in quads]
    xs = [p[0] for pts, _ in proj for p in pts]
    ys = [p[1] for pts, _ in proj for p in pts]
    scale = (ICON - 6) / max(max(xs) - min(xs), max(ys) - min(ys))
    cx, cy = (max(xs) + min(xs)) / 2, (max(ys) + min(ys)) / 2

    tw, thh = tex.size
    texa = np.asarray(tex.convert("RGBA"))
    img = np.zeros((ICON, ICON, 4), np.uint8)
    zbuf = np.full((ICON, ICON), np.inf)
    gy, gx = np.mgrid[0:ICON, 0:ICON]
    px, py = gx + 0.5, gy + 0.5

    for pts, uvs in proj:
        scr = [((p[0] - cx) * scale + ICON / 2, (p[1] - cy) * scale + ICON / 2, p[2]) for p in pts]
        for tri in ((0, 1, 2), (0, 2, 3)):
            (x0, y0, d0), (x1, y1, d1), (x2, y2, d2) = (scr[i] for i in tri)
            det = (y1 - y2) * (x0 - x2) + (x2 - x1) * (y0 - y2)
            if abs(det) < 1e-9:
                continue
            l0 = ((y1 - y2) * (px - x2) + (x2 - x1) * (py - y2)) / det
            l1 = ((y2 - y0) * (px - x2) + (x0 - x2) * (py - y2)) / det
            l2 = 1 - l0 - l1
            inside = (l0 >= 0) & (l1 >= 0) & (l2 >= 0)
            if not inside.any():
                continue
            depth = l0 * d0 + l1 * d1 + l2 * d2
            u = l0 * uvs[tri[0]][0] + l1 * uvs[tri[1]][0] + l2 * uvs[tri[2]][0]
            v = l0 * uvs[tri[0]][1] + l1 * uvs[tri[1]][1] + l2 * uvs[tri[2]][1]
            tx = np.clip((u * tw).astype(int), 0, tw - 1)
            ty = np.clip((v * thh).astype(int), 0, thh - 1)
            col = texa[ty, tx]
            ok = inside & (col[..., 3] > 10) & (depth < zbuf - 1e-6)
            img[ok] = col[ok]
            zbuf[ok] = depth[ok]
    return Image.fromarray(img, "RGBA")


def main():
    if len(sys.argv) != 2:
        sys.exit(__doc__)
    src = Path(sys.argv[1])
    for color in COLORS:
        bb = src / f"witch-{color}.bbmodel"
        model = json.loads(bb.read_text(encoding="utf-8"))
        shutil.copyfile(bb, ASSETS / f"bbmodels/accessory/enchanter_{color}_cape.bbmodel")

        data = model["textures"][0]["source"].split(",", 1)[1]
        tex = Image.open(io.BytesIO(base64.b64decode(data))).convert("RGBA")
        tex.save(ASSETS / f"textures/entity/accessory/enchanter_{color}_cape.png")

        render_icon(load_quads(model), tex).save(ASSETS / f"textures/item/enchanter_{color}_cape.png")
        print("ok", color)


if __name__ == "__main__":
    main()
