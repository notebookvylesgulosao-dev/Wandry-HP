"""Gera as texturas das varinhas (Amethyst, Bone, Celestial) mantendo o atlas/UV de cada modelo.
Uso: python3 gen_textures.py <pasta de saida>"""
import sys, math, random
from PIL import Image

def hexc(h):
    h = h.lstrip('#'); return tuple(int(h[i:i+2], 16) for i in (0, 2, 4)) + (255,)

def h2(x, y, seed):
    n = (x * 374761393 + y * 668265263 + seed * 2147483647) & 0xFFFFFFFF
    n = (n ^ (n >> 13)) * 1274126177 & 0xFFFFFFFF
    return ((n ^ (n >> 16)) & 0xFFFF) / 65535.0

class Atlas:
    def __init__(self): self.im = Image.new('RGBA', (64, 64), (0, 0, 0, 0)); self.px = self.im.load()
    def paint(self, rect, fn):
        x0, y0, x1, y1 = rect
        for y in range(int(math.floor(y0)), int(math.ceil(y1))):
            for x in range(int(math.floor(x0)), int(math.ceil(x1))):
                if 0 <= x < 64 and 0 <= y < 64:
                    u = (x + .5 - x0) / max(1e-6, x1 - x0); v = (y + .5 - y0) / max(1e-6, y1 - y0)
                    self.px[x, y] = fn(x, y, u, v)
    def save(self, p): self.im.save(p)

def pal(colors): return [hexc(c) for c in colors]
def pick(p, t):
    i = max(0, min(len(p) - 1, int(round(t * (len(p) - 1))))); return p[i]

def cyl(u, light=0.32, width=1.5):
    """Sombreamento de cilindro: 0..1, luz vinda da esquerda."""
    return max(0.0, min(1.0, 1.0 - abs(u - light) * width))

# --------------------------------------------------------------------------- AMETHYST
def amethyst(out):
    A = Atlas()
    body = pal(['#1b1426', '#2a2038', '#3b2f50', '#52436e', '#6e5c92', '#8f7db5'])
    # corpo: haste + empunhadura (colunas 48..52.8) – pedra escura violeta com veios claros
    def body_fn(x, y, u, v):
        c = 48 + 0  # col base
        t = cyl((x - 48 + .5) / 4.8, 0.30, 1.35)
        t = 0.18 + 0.72 * t
        t += (h2(x, y, 1) - .5) * 0.16
        if (y % 8) == 3 and 0 < (x - 48) < 4: t -= 0.12          # sulcos finos
        if h2(x, y // 3, 7) > 0.93: t += 0.20                      # faiscas de cristal
        if y < 2 or y >= 14: t -= 0.10                              # extremidades
        return pick(body, t)
    A.paint((48, 0, 53, 16), body_fn)
    # topo (tampa 4.8 x 4.8) usa a mesma região: mais claro, faceta
    # veio luminoso (emissivo): gradiente violeta brilhante
    vein = pal(['#8a4fe0', '#a468ff', '#c291ff', '#e2c4ff', '#f6e9ff'])
    A.paint((32, 0, 33, 16), lambda x, y, u, v: pick(vein, 0.55 + 0.45 * math.sin(y * 0.9) ** 2 * 0.6 + (h2(x, y, 3) - .5) * 0.2))
    # anéis de metal prateado/violeta + colar (bloco 0..6.4 x 16..22)
    metal = pal(['#3a3550', '#5d5878', '#8a86a6', '#bcb8d6', '#e8e5f5', '#ffffff'])
    def ring(x, y, u, v):
        t = 0.2 + 0.7 * (1 - abs(v - 0.35) * 1.6)
        t += (h2(x, y, 5) - .5) * 0.1
        if y == 16: t = 0.8
        if y >= 20: t = 0.1 + t * 0.4
        return pick(metal, max(0, min(0.78, t)))
    A.paint((0, 16, 6.4, 22), ring)
    # geode (pomo): rocha escura com borda; movido para x=56..62.8 (antes colidia com haste/empunhadura)
    rock = pal(['#17121f', '#251d33', '#3a2e50', '#54447a'])
    A.paint((56, 0, 63, 7), lambda x, y, u, v: pick(rock, 0.25 + 0.55 * cyl(u, .35, 1.2) + (h2(x, y, 9) - .5) * 0.35))
    # calcita (cristal branco-lilás), movida para x=8..13.2
    calc = pal(['#9b8bbf', '#c6b8e4', '#e6dcf6', '#faf5ff'])
    A.paint((8, 16, 13.2, 21.2), lambda x, y, u, v: pick(calc, 0.3 + 0.6 * (1 - abs(u - .35) * 1.5) + (h2(x, y, 11) - .5) * 0.3))
    # cristal interno (ametista saturada)
    cry = pal(['#5b2aa8', '#7d3fd8', '#a064f5', '#c895ff', '#ecd2ff'])
    def cry_fn(x, y, u, v):
        t = 0.3 + 0.55 * (1 - abs(u - .4) * 1.4) + 0.25 * (1 - v)
        t += (h2(x, y, 13) - .5) * 0.25
        if (x + y) % 5 == 0: t += 0.2
        return pick(cry, max(0, min(1, t)))
    A.paint((16, 0, 22.4, 6.4), cry_fn)
    A.save(out + '/amethyst_rod.png')

# --------------------------------------------------------------------------- BONE
def bone(out):
    A = Atlas()
    B = pal(['#3d3326', '#6f6150', '#9a8c74', '#c2b697', '#e0d6ba', '#f4eed8', '#fffbed'])
    def bone_fn(x, y, u, v):
        t = 0.15 + 0.78 * cyl(x / 4.6, 0.28, 1.3 if x < 5 else 0.6)
        if x >= 4.6: t = 0.22
        t += (h2(x, y, 21) - .5) * 0.14
        if y % 6 == 2 and h2(x, y, 22) > .35: t -= 0.13          # fibras
        if h2(x // 1, y // 2, 23) > 0.95: t -= 0.3                # poros
        if y in (0, 15): t -= 0.1
        if 7 <= y <= 8 and x < 4: t += 0.06
        return pick(B, max(0, min(1, t)))
    A.paint((0, 0, 8, 16), bone_fn)
    # tendões/espinhos (x16..18.1) + núcleo do carretel (x16..19.6, y0..3.6)
    D = pal(['#120e0a', '#221a14', '#352a21', '#4d3f33', '#6b5a48', '#8a7660'])
    def spine_fn(x, y, u, v):
        t = 0.1 + 0.7 * cyl((x - 16 + .5) / 2.2, 0.3, 1.5) + (h2(x, y, 31) - .5) * 0.2
        if y < 3.6: t = 0.25 + 0.5 * cyl((x - 16 + .5) / 3.6, 0.3, 1.1) + (h2(x, y, 32) - .5) * 0.2
        if y % 4 == 1: t += 0.08
        return pick(D, max(0, min(1, t)))
    A.paint((16, 0, 19.6, 15.1), spine_fn)
    # couro: envolvimento e tira (x32..37.8, y0..9.7)
    L = pal(['#24150c', '#3f2615', '#5e3a20', '#7c4f2b', '#9b6a3d', '#b98856'])
    def leather(x, y, u, v):
        t = 0.2 + 0.55 * cyl((x - 32 + .5) / 5.8, 0.32, 1.0)
        diag = (x + y) % 4
        t += {0: -0.18, 1: 0.0, 2: 0.1, 3: 0.04}[diag]
        t += (h2(x, y, 41) - .5) * 0.14
        if y == 0 or y >= 9: t -= 0.12
        return pick(L, max(0, min(1, t)))
    A.paint((32, 0, 38, 10), leather)
    # costelas (x32..33.1, y16..23.8): osso claro
    A.paint((32, 16, 33.1, 24), lambda x, y, u, v: pick(B, 0.62 + (h2(x, y, 51) - .5) * 0.25 + (0.15 if y % 5 == 0 else 0)))
    # fio/guias/olhal (x0..2.8, y16..30.7): fio marfim fino + argola
    S = pal(['#8f8566', '#c4b994', '#e9dfc0', '#fffaea'])
    A.paint((0, 16, 3, 31), lambda x, y, u, v: pick(S, 0.35 + 0.5 * cyl((x + .5) / 2.8, .3, 1.2) + (h2(x, y, 61) - .5) * 0.15 + (0.1 if y % 6 == 0 else 0)))
    A.save(out + '/bone_rod.png')

# --------------------------------------------------------------------------- CELESTIAL
def celestial(out):
    A = Atlas()
    G = pal(['#4a3210', '#7a5518', '#a87a22', '#d4a735', '#f2cb55', '#ffe58a', '#fff6c8'])
    def gold(x, y, u, v):
        t = 0.12 + 0.82 * cyl(x / 4.6, 0.28, 1.35)
        if x >= 4.6: t = 0.2
        t += (h2(x, y, 71) - .5) * 0.10
        if y % 8 == 4 and x < 4.6: t -= 0.14                      # filetes
        if y in (0, 15): t -= 0.1
        if h2(x, y, 72) > 0.965: t = 1.0                           # brilho
        return pick(G, max(0, min(1, t)))
    A.paint((0, 0, 8, 16), gold)
    N = pal(['#070b1f', '#0e1536', '#16214f', '#223070', '#334590', '#4c5fb0'])
    def night(x, y, u, v):
        t = 0.08 + 0.6 * cyl((x - 16 + .5) / 2.2, 0.3, 1.3) + (h2(x, y, 81) - .5) * 0.12
        c = pick(N, max(0, min(1, t)))
        r = h2(x, y, 82)
        if r > 0.93: return hexc('#fff6c8')                          # estrelas
        if r > 0.89: return hexc('#bfe6ff')                          # brilho azul-gelo
        return c
    A.paint((16, 0, 18.4, 15.1), night)
    # núcleo do carretel (16..19.6 x 0..3.6): anel dourado com centro azul-gelo
    def hub(x, y, u, v):
        d = math.hypot(u - .5, v - .5)
        if d < 0.28: return hexc('#bfe6ff') if d < 0.16 else hexc('#7fb8e6')
        return pick(G, 0.35 + 0.5 * cyl(u, .3, 1.2) + (h2(x, y, 83) - .5) * .2)
    A.paint((16, 0, 19.6, 3.6), hub)
    # tira (32..37.8, 0..5.8): azul-noite com faixas douradas diagonais
    def strap(x, y, u, v):
        if (x + y) % 5 == 0: return pick(G, 0.55 + 0.4 * cyl(u, .3, 1.0))
        return pick(N, 0.2 + 0.55 * cyl(u, .3, 1.0) + (h2(x, y, 91) - .5) * 0.12)
    A.paint((32, 0, 38, 10), strap)
    # costelas (32..33.1 x 16..23.8): ouro claro
    A.paint((32, 16, 33.1, 24), lambda x, y, u, v: pick(G, 0.65 + (h2(x, y, 92) - .5) * 0.2))
    # olhal da ponta (0..2.8 x 16..18.8): azul-gelo luminoso
    I = pal(['#6aa6d6', '#a5d3f5', '#d7efff', '#ffffff'])
    A.paint((0, 16, 3, 19), lambda x, y, u, v: pick(I, 0.35 + 0.55 * cyl(u, .35, 1.1) + (h2(x, y, 93) - .5) * 0.15))
    A.save(out + '/celestial_rod.png')

if __name__ == '__main__':
    o = sys.argv[1]; amethyst(o); bone(o); celestial(o)
