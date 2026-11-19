#!/usr/bin/env python3
"""Converte os .bbmodel originais em assets do mod (rode na raiz do projeto).

- Projéteis de feitiço, props de ataque e vassoura (entidade): gera
  assets/wandry_hp/quads/<nome>.json (quads já com todas as rotações aplicadas)
  e assets/wandry_hp/textures/entity/<nome>.png.
- Chapéu, vassoura (item) e Emerald Wand: regenera models/item/*.json com UV
  escalada corretamente (16 / uv_width) e o bloco "display" original.
"""
import base64, json, math, os

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
SRC = os.path.join(ROOT, 'source_models')
ASSETS = os.path.join(ROOT, 'src/main/resources/assets/wandry_hp')

QUAD_MODELS = {
    'feitiços/avada_kedavra_projectile': 'avada_kedavra',
    'feitiços/corinficus_projectile': 'corinficus',
    'feitiços/crucio_projectile': 'crucio',
    'feitiços/expeliarmos_projectile': 'expelliarmus',
    'feitiços/imperius_projectile': 'imperius',
    'feitiços/petrificus_totalis_projectile': 'petrificus_totalis',
    'feitiços/sectumsempra_projectile': 'sectumsempra',
    'prop ataque base/amethyst-prop': 'prop_amethyst',
    'prop ataque base/bone_prop': 'prop_bone',
    'prop ataque base/celestial_prop': 'prop_celestial',
    'prop ataque base/dragon-prop': 'prop_dragon',
    'prop ataque base/widow-prop': 'prop_widow',
    'items/witch-broom': 'witch_broom',
}
# ponto do modelo (em unidades de 1/16) que fica na origem da entidade; None = centro da caixa
ANCHOR = {'witch_broom': (8.0, 7.0, 6.0)}

ITEM_MODELS = {'items/witch-hat': 'witch_hat', 'items/witch-broom': 'witch_broom',
               'wands/emerald_wand_refined': 'emerald_wand'}
WITH_DISPLAY = {'witch_hat', 'witch_broom'}

# vértices de cada face na mesma ordem do baker do Minecraft; UV: (u1,v1)(u1,v2)(u2,v2)(u2,v1)
def face_corners(face, a, b):
    x1, y1, z1 = a; x2, y2, z2 = b
    return {
        'down':  [(x1,y1,z2),(x1,y1,z1),(x2,y1,z1),(x2,y1,z2)],
        'up':    [(x1,y2,z1),(x1,y2,z2),(x2,y2,z2),(x2,y2,z1)],
        'north': [(x2,y2,z1),(x2,y1,z1),(x1,y1,z1),(x1,y2,z1)],
        'south': [(x1,y2,z2),(x1,y1,z2),(x2,y1,z2),(x2,y2,z2)],
        'west':  [(x1,y2,z1),(x1,y1,z1),(x1,y1,z2),(x1,y2,z2)],
        'east':  [(x2,y2,z2),(x2,y1,z2),(x2,y1,z1),(x2,y2,z1)],
    }[face]

NORMALS = {'down': (0,-1,0), 'up': (0,1,0), 'north': (0,0,-1), 'south': (0,0,1), 'west': (-1,0,0), 'east': (1,0,0)}

def rot_matrix(rx, ry, rz):  # ordem ZYX do Blockbench: M = Rz * Ry * Rx
    rx, ry, rz = map(math.radians, (rx, ry, rz))
    cx, sx, cy, sy, cz, sz = math.cos(rx), math.sin(rx), math.cos(ry), math.sin(ry), math.cos(rz), math.sin(rz)
    Rx = [[1,0,0],[0,cx,-sx],[0,sx,cx]]
    Ry = [[cy,0,sy],[0,1,0],[-sy,0,cy]]
    Rz = [[cz,-sz,0],[sz,cz,0],[0,0,1]]
    def mul(A, B): return [[sum(A[i][k]*B[k][j] for k in range(3)) for j in range(3)] for i in range(3)]
    return mul(Rz, mul(Ry, Rx))

def apply(M, v): return tuple(sum(M[i][k]*v[k] for k in range(3)) for i in range(3))

def load(path):
    with open(os.path.join(SRC, path + '.bbmodel'), encoding='utf-8') as f:
        return json.load(f)

def build_quads(model, name):
    tex = model['textures'][0]
    uw, uh = tex['uv_width'], tex['uv_height']
    quads = []
    for e in model['elements']:
        inf = e.get('inflate', 0) or 0
        a = [e['from'][i] - inf for i in range(3)]; b = [e['to'][i] + inf for i in range(3)]
        rot = e.get('rotation') or [0, 0, 0]; org = e.get('origin') or [0, 0, 0]
        M = rot_matrix(*rot)
        emissive = 1.0 if (e.get('light_emission') or 0) > 0 else 0.0
        for fname, fc in e['faces'].items():
            if fc.get('texture') is None: continue
            u1, v1, u2, v2 = fc['uv']; uvs = [u1, v1, u2, v2]
            r = (fc.get('rotation') or 0) // 90
            verts = []
            for i, c in enumerate(face_corners(fname, a, b)):
                p = tuple(c[k] - org[k] for k in range(3)); p = apply(M, p); p = tuple(p[k] + org[k] for k in range(3))
                idx = (i + r) % 4
                u = uvs[0] if idx in (0, 1) else uvs[2]
                v = uvs[1] if idx in (0, 3) else uvs[3]
                verts.append((p, u / uw, v / uh))
            n = apply(M, NORMALS[fname])
            quads.append((verts, n, emissive))
    pts = [v[0] for q in quads for v in q[0]]
    if name in ANCHOR: c = ANCHOR[name]
    else: c = tuple((min(p[k] for p in pts) + max(p[k] for p in pts)) / 2 for k in range(3))
    flat = []
    for verts, n, em in quads:
        for p, u, v in verts:
            flat += [round((p[0]-c[0])/16, 5), round((p[1]-c[1])/16, 5), round((p[2]-c[2])/16, 5), round(u, 6), round(v, 6)]
        flat += [round(n[0], 5), round(n[1], 5), round(n[2], 5), em]
    return flat, len(quads)

def save_texture(model, rel_out, index=0):
    src = model['textures'][index]['source']
    data = base64.b64decode(src.split(',', 1)[1])
    os.makedirs(os.path.dirname(rel_out), exist_ok=True)
    with open(rel_out, 'wb') as f: f.write(data)

def item_json(model, name):
    uw, uh = model['textures'][0]['uv_width'], model['textures'][0]['uv_height']
    out = {'textures': {}, 'elements': []}
    for i in range(len(model['textures'])): out['textures'][f'tex{i}'] = f'wandry_hp:item/{name}_{i}'
    out['textures']['particle'] = '#tex0'
    for e in model['elements']:
        el = {'from': [round(x, 4) for x in e['from']], 'to': [round(x, 4) for x in e['to']], 'faces': {}}
        for fname, fc in e['faces'].items():
            if fc.get('texture') is None: continue
            u1, v1, u2, v2 = fc['uv']
            face = {'uv': [round(u1*16/uw, 4), round(v1*16/uh, 4), round(u2*16/uw, 4), round(v2*16/uh, 4)],
                    'texture': f"#tex{fc['texture']}"}
            if fc.get('rotation'): face['rotation'] = fc['rotation']
            el['faces'][fname] = face
        rot = e.get('rotation') or [0, 0, 0]
        nz = [i for i in range(3) if abs(rot[i]) > 1e-6]
        if len(nz) == 1:
            el['rotation'] = {'origin': [round(x, 4) for x in e['origin']], 'axis': 'xyz'[nz[0]], 'angle': rot[nz[0]]}
        elif len(nz) > 1: raise SystemExit(f'{name}: rotação em mais de um eixo não cabe em modelo de item')
        if e.get('shade') is False: el['shade'] = False
        out['elements'].append(el)
    if name in WITH_DISPLAY and model.get('display'):
        out['display'] = {k: {kk: vv for kk, vv in v.items() if kk in ('rotation', 'translation', 'scale')}
                          for k, v in model['display'].items()}
    return out

def main():
    os.makedirs(os.path.join(ASSETS, 'quads'), exist_ok=True)
    for path, name in QUAD_MODELS.items():
        m = load(path)
        flat, n = build_quads(m, name)
        with open(os.path.join(ASSETS, 'quads', name + '.json'), 'w') as f:
            json.dump({'quads': flat}, f, separators=(',', ':'))
        save_texture(m, os.path.join(ASSETS, 'textures/entity', name + '.png'))
        print('quads', name, n)
    for path, name in ITEM_MODELS.items():
        m = load(path)
        with open(os.path.join(ASSETS, 'models/item', name + '.json'), 'w') as f:
            json.dump(item_json(m, name), f, indent=2)
        print('item', name)

if __name__ == '__main__':
    main()
