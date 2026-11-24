"""Ajusta os UVs dos modelos das varinhas para o novo atlas (geometria intacta).
- amethyst: pomo (geode) e calcita saem das regioes compartilhadas com haste/empunhadura/aneis.
- celestial: UVs identicos aos do bone para os elementos de mesmo nome (geometria e igual)."""
import json, sys, copy
src, out = sys.argv[1], sys.argv[2]   # src=.../models/item
def load(n): return json.load(open(f'{src}/{n}_wand.json'))
def save(n, m): json.dump(m, open(f'{out}/{n}_wand.json', 'w'), indent=2, ensure_ascii=False); open(f'{out}/{n}_wand.json','a').write('\n')

a = load('amethyst')
for e in a['elements']:
    if e['name'].startswith('pommel_geode'):
        for f in e['faces'].values(): f['uv'] = [round(f['uv'][0] + 2, 5), f['uv'][1], round(f['uv'][2] + 2, 5), f['uv'][3]]
    if e['name'] == 'pommel_calcite':
        for f in e['faces'].values(): f['uv'] = [round(f['uv'][0] + 2, 5), f['uv'][1], round(f['uv'][2] + 2, 5), f['uv'][3]]
save('amethyst', a)

b = load('bone'); c = load('celestial')
bm = {e['name']: e for e in b['elements']}
for e in c['elements']:
    for fn, f in e['faces'].items():
        bf = bm[e['name']]['faces'][fn]
        f['uv'] = copy.deepcopy(bf['uv'])
        f.pop('rotation', None)
        if 'rotation' in bf: f['rotation'] = bf['rotation']
        f['texture'] = '#2'
save('celestial', c)
