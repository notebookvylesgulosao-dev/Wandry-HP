#!/usr/bin/env python3
"""Gera os assets das armaduras das casas a partir dos .bbmodel originais (sem alterar geometria/UV/texturas).

Uso: python3 -I tools/prepare_armor_assets.py <pasta com os .bbmodel das casas, calça e botas> <raiz do mod>
Requer Pillow (pip install pillow).

- <casa>.bbmodel -> assets/wandryhp/bbmodels/armor/<id>.bbmodel (copia byte a byte; lida em runtime pelo renderizador)
- textura embutida -> assets/wandryhp/textures/entity/armor/<id>.png
- icone do peitoral -> assets/wandryhp/textures/item/<id>_chestplate.png (recortado da propria textura da casa:
  frente dos bracos + frente do corpo)
- calca e botas compartilhadas: calça_universal.bbmodel / botas_universal.bbmodel -> bbmodels/armor/house_leggings|
  house_boots.bbmodel + textura embutida; icones desenhados na paleta escura/prata; texturas de equipamento vanilla
  transparentes (so existem por exigencia do ArmorMaterial)
- items/*.json, models/item/*.json e equipment/house.json
"""
import base64, io, json, shutil, sys
from pathlib import Path
from PIL import Image

src, root = Path(sys.argv[1]), Path(sys.argv[2])
A = root / "src/main/resources/assets/wandryhp"
MOD = "wandryhp"

HOUSES = {  # id da casa -> bbmodel
    "gryffindor": "grifinoria.bbmodel",
    "hufflepuff": "lufalufa.bbmodel",
    "ravenclaw": "corvinal.bbmodel",
    "slytherin": "sonserina.bbmodel",
}

# Paleta tirada das texturas originais (partes escuras e prata)
CLOTH, SHADE, TRIM = (0x13, 0x10, 0x1b, 255), (0x06, 0x06, 0x0a, 255), (0x2e, 0x24, 0x43, 255)
SILVER, SILVER_HI, SILVER_LO = (0x79, 0x7f, 0x7f, 255), (0x95, 0x9b, 0x9b, 255), (0x5a, 0x5f, 0x5f, 255)
LEATHER = (0x1a, 0x18, 0x17, 255)


def write_json(path, data):
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(json.dumps(data, indent=2, ensure_ascii=False) + "\n", encoding="utf-8")


def save(img, path):
    path.parent.mkdir(parents=True, exist_ok=True)
    img.save(path)


def item_files(item_id):
    write_json(A / f"items/{item_id}.json",
               {"model": {"type": "minecraft:model", "model": f"{MOD}:item/{item_id}"}})
    write_json(A / f"models/item/{item_id}.json",
               {"parent": "minecraft:item/generated", "textures": {"layer0": f"{MOD}:item/{item_id}"}})


def house(house_id, bb):
    d = json.loads(bb.read_text(encoding="utf-8"))
    dest = A / f"bbmodels/armor/{house_id}.bbmodel"
    dest.parent.mkdir(parents=True, exist_ok=True)
    shutil.copyfile(bb, dest)

    raw = base64.b64decode(d["textures"][0]["source"].split(",", 1)[1])
    tex_path = A / f"textures/entity/armor/{house_id}.png"
    tex_path.parent.mkdir(parents=True, exist_ok=True)
    tex_path.write_bytes(raw)

    # Icone: frente do braco direito | frente do corpo (15 linhas de cima) | frente do braco esquerdo,
    # nas mesmas coordenadas de UV das faces "north" dos cubos RightArm / body / LeftArm do .bbmodel.
    tex = Image.open(io.BytesIO(raw)).convert("RGBA")
    icon = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    icon.alpha_composite(tex.crop((32, 63, 36, 75)), (0, 1))
    icon.alpha_composite(tex.crop((38, 24, 46, 39)), (4, 1))
    icon.alpha_composite(tex.crop((48, 63, 52, 75)), (12, 1))
    save(icon, A / f"textures/item/{house_id}_chestplate.png")
    item_files(f"{house_id}_chestplate")
    print(f"casa {house_id}: {len(d['elements'])} elementos, textura {tex.size}")


def px(img, x, y, c):
    img.putpixel((x, y), c)


def rect(img, x0, y0, x1, y1, c):  # inclusivo
    for y in range(y0, y1 + 1):
        for x in range(x0, x1 + 1):
            px(img, x, y, c)


def leggings_icon():
    im = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    rect(im, 3, 1, 12, 2, SILVER)
    rect(im, 3, 1, 12, 1, SILVER_HI)
    rect(im, 7, 1, 8, 2, SILVER_LO)
    rect(im, 3, 3, 12, 5, CLOTH)
    for x0 in (3, 9):  # pernas
        rect(im, x0, 6, x0 + 3, 13, CLOTH)
        rect(im, x0 + 3 if x0 == 3 else x0, 6, x0 + 3 if x0 == 3 else x0, 13, SHADE)  # lado interno
        rect(im, x0, 14, x0 + 3, 14, TRIM)
    rect(im, 3, 3, 3, 13, SHADE)
    rect(im, 12, 3, 12, 13, SHADE)
    return im


def boots_icon():
    im = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    for x0 in (1, 9):
        rect(im, x0 + 1, 3, x0 + 4, 4, SILVER)          # cano (punho prata)
        rect(im, x0 + 1, 3, x0 + 4, 3, SILVER_HI)
        rect(im, x0 + 1, 5, x0 + 4, 10, LEATHER)        # cano
        rect(im, x0 + 1, 5, x0 + 1, 10, SHADE)
        rect(im, x0 + 2, 7, x0 + 3, 7, SILVER_LO)       # fivela
        rect(im, x0, 11, x0 + 5, 13, LEATHER)           # pe
        rect(im, x0, 11, x0 + 5, 11, TRIM)
        rect(im, x0, 14, x0 + 5, 14, SHADE)             # sola
    return im


def legwear(item_id, bb):
    """Calca/botas: copia o .bbmodel (cubos leg0/leg1) e extrai a textura embutida, sem alterar geometria/UV."""
    d = json.loads(bb.read_text(encoding="utf-8"))
    dest = A / f"bbmodels/armor/{item_id}.bbmodel"
    dest.parent.mkdir(parents=True, exist_ok=True)
    shutil.copyfile(bb, dest)
    raw = base64.b64decode(d["textures"][0]["source"].split(",", 1)[1])
    tex_path = A / f"textures/entity/armor/{item_id}.png"
    tex_path.parent.mkdir(parents=True, exist_ok=True)
    tex_path.write_bytes(raw)


for hid, fn in HOUSES.items():
    house(hid, src / fn)

save(leggings_icon(), A / "textures/item/house_leggings.png")
save(boots_icon(), A / "textures/item/house_boots.png")
item_files("house_leggings")
item_files("house_boots")
legwear("house_leggings", src / "cal\u00e7a_universal.bbmodel")
legwear("house_boots", src / "botas_universal.bbmodel")
# Texturas de equipamento vanilla totalmente transparentes: o ArmorMaterial exige o asset "house", mas a calca e as
# botas sao desenhadas pelo HouseLegwearRenderer a partir dos .bbmodel.
for layer in ("humanoid", "humanoid_leggings"):
    save(Image.new("RGBA", (64, 32), (0, 0, 0, 0)), A / f"textures/entity/equipment/{layer}/house.png")
write_json(A / "equipment/house.json", {"layers": {
    "humanoid": [{"texture": f"{MOD}:house"}],
    "humanoid_leggings": [{"texture": f"{MOD}:house"}],
}})
print("calca/botas (bbmodel) e equipment/house.json gerados")
