#!/usr/bin/env python3
"""Gera os assets da Edwiges a partir dos .bbmodel originais (sem alterar geometria/UV/animacoes/texturas).

Uso: python3 -I tools/prepare_edwiges_assets.py <pasta do WandryHP_Edwiges> <raiz do mod>

- EdwigesAnimada.bbmodel     -> assets/wandryhp/bbmodels/edwiges/edwiges.bbmodel (copia byte a byte; lida em runtime)
- Envelope.bbmodel           -> assets/wandryhp/bbmodels/edwiges/envelope.bbmodel (envelope generico, reserva)
- cartas/envelope_*.bbmodel  -> assets/wandryhp/bbmodels/edwiges/letters/<casa>.bbmodel (copia byte a byte)
- texturas embutidas         -> textures/entity/edwiges/owl.png e textures/entity/edwiges/letter/<casa>_<indice>.png
                                (faces referenciam o INDICE da textura no .bbmodel)
"""
import base64, json, shutil, sys
from pathlib import Path

src, root = Path(sys.argv[1]), Path(sys.argv[2])
A = root / "src/main/resources/assets/wandryhp"

OWL = ("EdwigesAnimada.bbmodel", "edwiges", "owl")
LETTERS = {  # id da casa (HouseType.id) -> bbmodel
    "gryffindor": "cartas/envelope_grifinoria.bbmodel",
    "hufflepuff": "cartas/envelope_hufflepuff.bbmodel",
    "ravenclaw": "cartas/envelope_raven.bbmodel",
    "slytherin": "cartas/envelope_slytherin_corrigido.bbmodel",
}
GENERIC = "Envelope.bbmodel"

def dump(bb, model_dest, tex_name):
    model_dest.parent.mkdir(parents=True, exist_ok=True)
    shutil.copyfile(bb, model_dest)
    d = json.loads(bb.read_text(encoding="utf-8"))
    for i, t in enumerate(d["textures"]):
        dest = A / "textures/entity/edwiges" / tex_name(i)
        dest.parent.mkdir(parents=True, exist_ok=True)
        dest.write_bytes(base64.b64decode(t["source"].split(",", 1)[1]))
        print(f"{bb.name}: textura {i} -> {dest.relative_to(root)}")

dump(src / OWL[0], A / "bbmodels/edwiges/edwiges.bbmodel", lambda i: "owl.png")
dump(src / GENERIC, A / "bbmodels/edwiges/envelope.bbmodel", lambda i: f"letter/generic_{i}.png")
for house, f in LETTERS.items():
    dump(src / f, A / f"bbmodels/edwiges/letters/{house}.bbmodel", lambda i, h=house: f"letter/{h}_{i}.png")
