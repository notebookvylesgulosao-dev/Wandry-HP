#!/usr/bin/env python3
"""Gera os assets do sistema de alinhamento a partir da arte enviada (nada e redesenhado, so convertido).

Uso: python3 -I tools/prepare_alignment_assets.py <raiz do mod>

Entradas:
  tools/source/house_icons/{gryffindor,hufflepuff,ravenclaw,slytherin}.png   icones das casas (1254x1254)
  source_models/books/{good_book,bad_book}.bbmodel                           modelos dos livros (Blockbench)

Saidas (em src/main/resources/assets/wandryhp/):
  textures/font/house_<casa>.png            icone da casa reduzido para 16x16 (fundo removido)
  textures/font/align_<alinh>_<casa>.png    icone do alinhamento (16x16) ja nas cores da casa; "none" = sem casa
  font/badges.json                          fonte (bitmap) que mapeia cada icone a um caractere privado
  textures/item/{good,bad}_book*.png        texturas EMBUTIDAS nos .bbmodel dos livros (extraidas, sem alterar)
  models/item/{good,bad}_book.json          o modelo do livro convertido em modelo de item vanilla (mesmos cubos/UV)
  items/{good,bad}_book.json                definicao do item

Os codigos dos caracteres devem bater com PlayerBadge.java.
"""
import base64
import io
import json
import sys
from pathlib import Path

import numpy as np
from PIL import Image
from scipy import ndimage

root = Path(sys.argv[1])
res = root / "src/main/resources/assets/wandryhp"
(res / "textures/font").mkdir(parents=True, exist_ok=True)
(res / "font").mkdir(parents=True, exist_ok=True)

HOUSES = ["gryffindor", "hufflepuff", "ravenclaw", "slytherin"]  # = ordem de HouseType
ALIGNS = ["neutral", "good", "evil"]                              # = ordem de Alignment
SIZE = 16
OUTLINE = (26, 26, 26)

# Cores de cada casa, medidas nos proprios icones (tom dominante e tom secundario). "none" = jogador sem casa.
COLORS = {
    "gryffindor": ((0xD0, 0x10, 0x30), (0xF0, 0xD0, 0x30)),
    "hufflepuff": ((0xF0, 0xB0, 0x10), (0x70, 0x50, 0x10)),
    "ravenclaw": ((0x50, 0x90, 0xD0), (0xF0, 0x90, 0x30)),
    "slytherin": ((0x10, 0x90, 0x50), (0xC0, 0xC0, 0xC0)),
    "none": ((0xB0, 0xB0, 0xB0), (0xF0, 0xF0, 0xF0)),
}

# ---------------------------------------------------------------- icones das casas
def house_icon(name):
    im = Image.open(root / f"tools/source/house_icons/{name}.png").convert("RGBA")
    a = np.array(im).astype(int)
    if a[2, 2, 3] < 20:
        bg = a[..., 3] < 20
    else:
        bg = np.abs(a[..., :3] - a[2, 2, :3]).sum(-1) < 40
    lab, _ = ndimage.label(bg)  # fundo = so o que toca a borda (o preto/branco interno do brasao fica)
    border = (set(lab[0, :]) | set(lab[-1, :]) | set(lab[:, 0]) | set(lab[:, -1])) - {0}
    a[np.isin(lab, list(border)), 3] = 0
    im = Image.fromarray(a.astype(np.uint8), "RGBA")
    im = im.crop(im.getbbox())
    side = max(im.size)
    sq = Image.new("RGBA", (side, side), (0, 0, 0, 0))
    sq.paste(im, ((side - im.width) // 2, (side - im.height) // 2))
    out = sq.convert("RGBa").resize((SIZE, SIZE), Image.LANCZOS).convert("RGBA")
    o = np.array(out)
    o[..., 3] = np.where(o[..., 3] < 40, 0, o[..., 3])
    return Image.fromarray(o, "RGBA")


for h in HOUSES:
    house_icon(h).save(res / f"textures/font/house_{h}.png")

# ---------------------------------------------------------------- icones do alinhamento
# P = tom principal da casa, S = tom secundario, K = escuro. Contorno escuro e adicionado em volta.
ART = {
    "neutral": [
        "...PPPPP...",
        "..PP...PP..",
        ".PP.....PP.",
        "PP.......PP",
        "P....S....P",
        "P...SSS...P",
        "P....S....P",
        "PP.......PP",
        ".PP.....PP.",
        "..PP...PP..",
        "...PPPPP...",
    ],
    "good": [
        ".....P.....",
        ".....P.....",
        "....PPP....",
        "....PSP....",
        ".PPPPSPPPP.",
        "PPPSSSSSPPP",
        ".PPPPSPPPP.",
        "....PSP....",
        "....PPP....",
        ".....P.....",
        ".....P.....",
    ],
    "evil": [
        "..PPPPPPP..",
        ".PPPPPPPPP.",
        "PPPPPPPPPPP",
        "PPKKPPPKKPP",
        "PPKKPPPKKPP",
        "PPPPPKPPPPP",
        ".PPPPPPPPP.",
        "..PPPPPPP..",
        "..PSPSPSP..",
        "..PSPSPSP..",
        "...........",
    ],
}


def align_icon(align, house):
    prim, sec = COLORS[house]
    art = ART[align]
    h, w = len(art), len(art[0])
    img = np.zeros((SIZE, SIZE, 4), np.uint8)
    ox, oy = (SIZE - w) // 2, (SIZE - h) // 2
    filled = np.zeros((SIZE, SIZE), bool)
    for y, row in enumerate(art):
        for x, c in enumerate(row):
            if c == ".":
                continue
            color = {"P": prim, "S": sec, "K": OUTLINE}[c]
            img[oy + y, ox + x] = (*color, 255)
            filled[oy + y, ox + x] = True
    ring = ndimage.binary_dilation(filled, structure=[[0, 1, 0], [1, 1, 1], [0, 1, 0]]) & ~filled
    img[ring] = (*OUTLINE, 255)
    return Image.fromarray(img, "RGBA")


for al in ALIGNS:
    for h in HOUSES + ["none"]:
        align_icon(al, h).save(res / f"textures/font/align_{al}_{h}.png")

# ---------------------------------------------------------------- fonte
providers = []
for i, h in enumerate(HOUSES):
    providers.append({"type": "bitmap", "file": f"wandryhp:font/house_{h}.png", "ascent": 7, "height": 8,
                      "chars": [chr(0xE000 + i)]})
for ai, al in enumerate(ALIGNS):
    for hi, h in enumerate(HOUSES + ["none"]):
        providers.append({"type": "bitmap", "file": f"wandryhp:font/align_{al}_{h}.png", "ascent": 7, "height": 8,
                          "chars": [chr(0xE010 + ai * 8 + hi)]})
(res / "font/badges.json").write_text(json.dumps({"providers": providers}, indent=2) + "\n", encoding="utf-8")

# ---------------------------------------------------------------- livros (bbmodel -> modelo de item)
BOOKS = {"good_book": "good_book.bbmodel", "bad_book": "bad_book.bbmodel"}
(res / "models/item").mkdir(parents=True, exist_ok=True)
(res / "items").mkdir(parents=True, exist_ok=True)
(res / "textures/item").mkdir(parents=True, exist_ok=True)

# Texturas usadas pelas faces do modelo salvo: 0 = book.png, 5 = ribbon_tear_v.png, 7 = text_a.png.
TEX_NAMES = {0: "", 5: "_ribbon", 7: "_text"}
DISPLAY = {
    "gui": {"rotation": [30, 225, 0], "translation": [0, 0, 0], "scale": [0.9, 0.9, 0.9]},
    "ground": {"rotation": [0, 0, 0], "translation": [0, 2, 0], "scale": [0.5, 0.5, 0.5]},
    "fixed": {"rotation": [0, 180, 0], "translation": [0, 0, 0], "scale": [0.8, 0.8, 0.8]},
    "thirdperson_righthand": {"rotation": [0, 0, 0], "translation": [0, 3, 1], "scale": [0.55, 0.55, 0.55]},
    "thirdperson_lefthand": {"rotation": [0, 0, 0], "translation": [0, 3, 1], "scale": [0.55, 0.55, 0.55]},
    "firstperson_righthand": {"rotation": [0, -90, 25], "translation": [1.13, 3.2, 1.13], "scale": [0.68, 0.68, 0.68]},
    "firstperson_lefthand": {"rotation": [0, -90, 25], "translation": [1.13, 3.2, 1.13], "scale": [0.68, 0.68, 0.68]},
}
SKIP = {"inside", "page"}  # parte interna do livro: nunca aparece (e coplanar com a capa, causaria z-fighting)

for item_id, fname in BOOKS.items():
    bb = json.loads((root / "source_models/books" / fname).read_text(encoding="utf-8"))
    textures = {}
    for idx, suffix in TEX_NAMES.items():
        src = bb["textures"][idx]["source"].split(",", 1)[1]
        Image.open(io.BytesIO(base64.b64decode(src))).convert("RGBA").save(res / f"textures/item/{item_id}{suffix}.png")
        textures[str(idx)] = f"wandryhp:item/{item_id}{suffix}"
    textures["particle"] = textures["0"]
    scale = 16.0 / bb["textures"][0]["width"]  # UV do Blockbench em pixels da textura -> faixa 0..16 do vanilla
    elements = []
    for el in bb["elements"]:
        if el["name"] in SKIP:
            continue
        lo = [min(el["from"][i], el["to"][i]) for i in range(3)]
        hi = [max(el["from"][i], el["to"][i]) for i in range(3)]
        shift = (8, 5, 8)  # centraliza o livro (origem do bbmodel em 0,3,0) no bloco de 16
        faces = {}
        for side, f in el["faces"].items():
            if f.get("texture") is None or not any(f["uv"]):
                continue
            face = {"uv": [round(v * scale, 4) for v in f["uv"]], "texture": f"#{f['texture']}"}
            if f.get("rotation"):
                face["rotation"] = f["rotation"]
            faces[side] = face
        if faces:
            elements.append({"name": el["name"], "from": [round(lo[i] + shift[i], 4) for i in range(3)],
                             "to": [round(hi[i] + shift[i], 4) for i in range(3)], "faces": faces})
    model = {"textures": textures, "elements": elements, "display": DISPLAY}
    (res / f"models/item/{item_id}.json").write_text(json.dumps(model, indent=2) + "\n", encoding="utf-8")
    (res / f"items/{item_id}.json").write_text(json.dumps(
        {"model": {"type": "minecraft:model", "model": f"wandryhp:item/{item_id}"}}, indent=2) + "\n", encoding="utf-8")
print("ok")
