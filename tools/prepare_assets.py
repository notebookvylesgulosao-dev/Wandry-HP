#!/usr/bin/env python3
"""Gera os assets do mod a partir dos .bbmodel originais (sem alterar geometria/UV/texturas).

Uso: python3 -I tools/prepare_assets.py <pasta com wands/ e 'props base/'> <raiz do mod>

- wands/*.bbmodel  -> assets/wandryhp/models/item/<id>_wand.json (conversao exata, 1 eixo de rotacao)
                      + assets/wandryhp/items/<id>_wand.json
- texturas embutidas -> assets/wandryhp/textures/item/*.png (varinhas) e textures/entity/prop/*.png (props)
- props base/*.bbmodel -> assets/wandryhp/bbmodels/props/<id>.bbmodel (copia byte a byte; lida em runtime)
"""
import base64, json, shutil, sys
from pathlib import Path

src, root = Path(sys.argv[1]), Path(sys.argv[2])
A = root / "src/main/resources/assets/wandryhp"
MOD = "wandryhp"

WANDS = {  # id do item -> bbmodel
    "amethyst": "amethyst-rod.bbmodel",
    "bone": "bone-wand.bbmodel",
    "celestial": "celestial_wand.bbmodel",
    "widow": "widow-rod.bbmodel",
}
PROPS = {  # id do prop -> (bbmodel, textura embutida)
    "amethyst": "amethyst-prop.bbmodel",
    "bone": "bone_prop.bbmodel",
    "celestial": "celestial_prop.bbmodel",
    "universal": "Prop Universal.bbmodel",
}

def write_json(path, data):
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(json.dumps(data, indent=2, ensure_ascii=False) + "\n", encoding="utf-8")

def dump_texture(tex, dest):
    dest.parent.mkdir(parents=True, exist_ok=True)
    dest.write_bytes(base64.b64decode(tex["source"].split(",", 1)[1]))

def convert_wand(wand_id, bb):
    d = json.loads(bb.read_text(encoding="utf-8"))
    tex_by_idx = {str(i): t for i, t in enumerate(d["textures"])}  # faces referenciam o INDICE da textura
    used, elements = set(), []
    for e in d["elements"]:
        if e["type"] != "cube" or not e.get("export", True):
            continue
        el = {"name": e["name"], "from": e["from"], "to": e["to"]}
        rot = e.get("rotation", [0, 0, 0])
        nz = [i for i, a in enumerate(rot) if a]
        if len(nz) > 1:
            raise SystemExit(f"{bb.name}: '{e['name']}' tem rotacao multi-eixo; nao representavel em JSON vanilla")
        if nz:
            el["rotation"] = {"angle": rot[nz[0]], "axis": "xyz"[nz[0]], "origin": e["origin"]}
        if e.get("light_emission"):
            el["light_emission"] = e["light_emission"]
        faces = {}
        for name, f in e["faces"].items():
            if f.get("texture") is None:
                continue
            face = {"uv": f["uv"], "texture": f"#{f['texture']}"}
            if f.get("rotation"):
                face["rotation"] = f["rotation"]
            faces[name] = face
            used.add(str(f["texture"]))
        el["faces"] = faces
        elements.append(el)
    textures = {}
    for tid in sorted(used):
        tname = tex_by_idx[tid]["name"]
        textures[tid] = f"{MOD}:item/{tname}"
        dump_texture(tex_by_idx[tid], A / f"textures/item/{tname}.png")
    textures["particle"] = textures[sorted(used)[0]]
    model = {"textures": textures, "elements": elements, "display": d["display"]}
    write_json(A / f"models/item/{wand_id}_wand.json", model)
    write_json(A / f"items/{wand_id}_wand.json",
               {"model": {"type": "minecraft:model", "model": f"{MOD}:item/{wand_id}_wand"}})
    print(f"wand {wand_id}: {len(elements)} elements, textures {textures}")

def copy_prop(prop_id, bb):
    d = json.loads(bb.read_text(encoding="utf-8"))
    dest = A / f"bbmodels/props/{prop_id}.bbmodel"
    dest.parent.mkdir(parents=True, exist_ok=True)
    shutil.copyfile(bb, dest)
    dump_texture(d["textures"][0], A / f"textures/entity/prop/{prop_id}.png")
    print(f"prop {prop_id}: {len(d['elements'])} elements")

for wid, fn in WANDS.items():
    convert_wand(wid, src / "wands" / fn)
for pid, fn in PROPS.items():
    copy_prop(pid, src / "props base" / fn)
