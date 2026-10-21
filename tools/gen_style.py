import json
import math
import os
import random

from PIL import Image

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
ASSETS = os.path.join(ROOT, "common", "src", "main", "resources", "assets", "dbtenkaichi")
SPRITES = ("textures", "gui", "sprites")

INK = (24, 12, 6, 255)
NAVY_TOP = (30, 41, 86, 238)
NAVY_BOTTOM = (15, 21, 52, 242)
NAVY_LIGHT = (78, 108, 212, 255)
NAVY_DARK = (8, 12, 34, 255)
GOLD = (255, 177, 43, 255)
GOLD_DARK = (192, 112, 22, 255)
GOLD_LIGHT = (255, 236, 150, 255)
ORANGE_TOP = (247, 150, 44, 255)
ORANGE_BOTTOM = (226, 112, 26, 255)
ORANGE_LIGHT = (255, 206, 120, 255)
ORANGE_DARK = (150, 64, 14, 255)
BLUE_TOP = (62, 104, 228, 255)
BLUE_BOTTOM = (38, 70, 186, 255)
BLUE_LIGHT = (150, 186, 255, 255)
BLUE_DARK = (20, 38, 108, 255)
STAR_RED = (214, 34, 26, 255)
STAR_DARK = (120, 14, 10, 255)
BALL_LIGHT = (255, 226, 120, 255)
BALL = (250, 160, 36, 255)
BALL_SHADE = (214, 104, 20, 255)
BALL_EDGE = (120, 46, 10, 255)


def path(*parts):
    p = os.path.join(ASSETS, *parts)
    os.makedirs(os.path.dirname(p), exist_ok=True)
    return p


def clamp(v, lo=0.0, hi=1.0):
    return max(lo, min(hi, v))


def lerp(a, b, t):
    return tuple(int(round(a[i] + (b[i] - a[i]) * t)) for i in range(len(a)))


def save_sprite(img, name, border=None, stretch=True):
    img.save(path(*SPRITES, name + ".png"))
    meta = path(*SPRITES, name + ".png.mcmeta")
    if border is None:
        if os.path.exists(meta):
            os.remove(meta)
        return
    with open(meta, "w", encoding="utf-8") as f:
        json.dump({"gui": {"scaling": {"type": "nine_slice", "width": img.width, "height": img.height, "border": border, "stretch_inner": stretch}}}, f, indent=2)


def bevel_box(w, h, top, bottom, light, dark, outline=INK, trim=None, trim_dark=None, inset=False, shadow=1):
    img = Image.new("RGBA", (w, h), (0, 0, 0, 0))
    px = img.load()
    for y in range(h):
        for x in range(w):
            corner = (x in (0, w - 1)) and (y in (0, h - 1))
            if corner:
                continue
            if x == 0 or y == 0 or x == w - 1 or y == h - 1:
                px[x, y] = outline
                continue
            lo = min(x - 1, y - 1)
            hi_x = w - 2 - x
            hi_y = h - 2 - y
            if trim is not None and (x == 2 or y == 2 or x == w - 3 or y == h - 3) and 2 <= x <= w - 3 and 2 <= y <= h - 3:
                px[x, y] = trim_dark if (y == h - 3 or x == w - 3) and trim_dark else trim
                continue
            if lo == 0 and hi_x >= 0 and hi_y >= 0:
                px[x, y] = dark if inset else light
                continue
            if hi_x < shadow or hi_y < shadow:
                px[x, y] = light if inset else dark
                continue
            t = (y - 1) / max(1, h - 3)
            px[x, y] = lerp(top, bottom, t)
    return img


def ui_sprites():
    save_sprite(bevel_box(64, 64, NAVY_TOP, NAVY_BOTTOM, NAVY_LIGHT, NAVY_DARK, trim=GOLD, trim_dark=GOLD_DARK), "panel", 4)
    save_sprite(bevel_box(32, 32, (18, 26, 62, 214), (12, 18, 44, 224), (8, 12, 30, 255), (52, 70, 140, 255), outline=(6, 8, 20, 255), inset=True), "card", 3)
    save_sprite(bevel_box(64, 20, ORANGE_TOP, ORANGE_BOTTOM, ORANGE_LIGHT, ORANGE_DARK, shadow=2), "button", 3)
    save_sprite(bevel_box(64, 20, (255, 198, 76, 255), (255, 156, 40, 255), (255, 246, 196, 255), (176, 84, 16, 255), shadow=2), "button_hover", 3)
    save_sprite(bevel_box(64, 20, (92, 96, 116, 255), (72, 76, 94, 255), (128, 134, 156, 255), (40, 42, 54, 255), outline=(20, 20, 26, 255), shadow=2), "button_disabled", 3)
    save_sprite(bevel_box(64, 20, BLUE_TOP, BLUE_BOTTOM, BLUE_LIGHT, BLUE_DARK, shadow=2), "button_selected", 3)
    save_sprite(bevel_box(24, 24, (18, 26, 60, 228), (10, 15, 38, 236), (6, 9, 24, 255), (60, 80, 150, 255), outline=(6, 8, 20, 255), inset=True), "slot", 3)
    save_sprite(bevel_box(24, 24, (104, 66, 16, 236), (70, 42, 8, 240), GOLD_LIGHT, GOLD_DARK, outline=(60, 30, 4, 255), trim=GOLD), "slot_selected", 3)
    save_sprite(bevel_box(28, 28, (22, 30, 70, 230), (14, 20, 48, 236), (70, 96, 190, 255), NAVY_DARK, shadow=2), "tab", 4)
    save_sprite(bevel_box(28, 28, ORANGE_TOP, ORANGE_BOTTOM, ORANGE_LIGHT, ORANGE_DARK, trim=GOLD_LIGHT, shadow=2), "tab_selected", 4)
    save_sprite(bevel_box(32, 12, (14, 18, 40, 236), (8, 10, 26, 240), (54, 70, 140, 255), (4, 6, 16, 255), outline=(4, 4, 10, 255)), "chip", 3)
    portrait()


def portrait():
    size = 48
    img = Image.new("RGBA", (size, size), (0, 0, 0, 0))
    px = img.load()
    c = (size - 1) / 2
    for y in range(size):
        for x in range(size):
            r = math.hypot(x - c, y - c)
            if r > 23.6:
                continue
            if r > 22.4:
                px[x, y] = INK
            elif r > 19.6:
                light = clamp(0.55 + ((c - y) - (x - c) * 0.3) / 40.0)
                g = int(150 + 105 * light)
                px[x, y] = (g, g, g, 255)
            elif r > 18.6:
                px[x, y] = (70, 70, 70, 255)
            else:
                t = y / size
                px[x, y] = lerp((40, 52, 104, 238), (16, 22, 54, 242), t)
    for sx in (-1, 0, 1):
        px[int(c) + sx * 6, size - 3] = (255, 255, 255, 255)
    save_sprite(img, "portrait_frame")


def put(img, x, y, color):
    if 0 <= x < img.width and 0 <= y < img.height:
        img.load()[x, y] = color


def outline_shape(img, color=INK):
    src = img.copy()
    sp = src.load()
    for y in range(img.height):
        for x in range(img.width):
            if sp[x, y][3] > 0:
                continue
            for dx, dy in ((1, 0), (-1, 0), (0, 1), (0, -1)):
                nx, ny = x + dx, y + dy
                if 0 <= nx < img.width and 0 <= ny < img.height and sp[nx, ny][3] > 200 and sp[nx, ny][:3] != color[:3]:
                    put(img, x, y, color)
                    break
    return img


def disc(img, cx, cy, r, fn):
    for y in range(img.height):
        for x in range(img.width):
            d = math.hypot(x + 0.5 - cx, y + 0.5 - cy)
            if d <= r:
                c = fn(x, y, d / r)
                if c is not None:
                    put(img, x, y, c)


STAR_LAYOUTS = {
    1: [(0.0, 0.0)],
    2: [(-0.28, -0.16), (0.28, 0.16)],
    3: [(0.0, -0.3), (-0.28, 0.2), (0.28, 0.2)],
    4: [(0.0, -0.34), (-0.32, 0.0), (0.32, 0.0), (0.0, 0.34)],
    5: [(0.0, -0.36), (-0.34, -0.08), (0.34, -0.08), (-0.2, 0.32), (0.2, 0.32)],
    6: [(-0.2, -0.32), (0.2, -0.32), (-0.38, 0.02), (0.38, 0.02), (-0.2, 0.36), (0.2, 0.36)],
    7: [(0.0, 0.0), (0.0, -0.4), (-0.36, -0.2), (0.36, -0.2), (-0.36, 0.22), (0.36, 0.22), (0.0, 0.42)],
}


SMALL_STARS = {
    1: [(0, 0)],
    2: [(-2, -2), (2, 2)],
    3: [(0, -3), (-3, 2), (3, 2)],
    4: [(0, -3), (-3, 0), (3, 0), (0, 3)],
    5: [(0, -3), (-3, -1), (3, -1), (-2, 3), (2, 3)],
    6: [(-2, -3), (2, -3), (-4, 0), (4, 0), (-2, 3), (2, 3)],
    7: [(0, 0), (0, -4), (-3, -2), (3, -2), (-3, 2), (3, 2), (0, 4)],
}


def dragon_ball(size, stars):
    img = Image.new("RGBA", (size, size), (0, 0, 0, 0))
    c = size / 2
    r = size / 2 - 1

    def shade(x, y, k):
        hx = (x + 0.5 - c) / r + 0.35
        hy = (y + 0.5 - c) / r + 0.4
        spec = math.hypot(hx, hy)
        if spec < 0.22:
            return (255, 252, 230, 255)
        if spec < 0.42:
            return BALL_LIGHT
        lit = (x + 0.5 - c) * 0.5 + (y + 0.5 - c)
        return BALL_SHADE if lit > r * 0.55 else BALL
    disc(img, c, c, r, shade)
    if size >= 32:
        for sx, sy in STAR_LAYOUTS[stars]:
            star(img, c + sx * r, c + sy * r, size / 9.0 if stars > 1 else size / 5.0)
    else:
        for sx, sy in SMALL_STARS[stars]:
            x, y = int(c + sx), int(c + sy)
            put(img, x, y, STAR_RED)
            if stars <= 3:
                for dx, dy in ((1, 0), (-1, 0), (0, 1), (0, -1)):
                    put(img, x + dx, y + dy, STAR_RED)
    return outline_shape(img, BALL_EDGE)


def star(img, cx, cy, r):
    pts = []
    for i in range(10):
        a = -math.pi / 2 + i * math.pi / 5
        rr = r if i % 2 == 0 else r * 0.45
        pts.append((cx + math.cos(a) * rr, cy + math.sin(a) * rr))
    for y in range(img.height):
        for x in range(img.width):
            if inside(pts, x + 0.5, y + 0.5):
                put(img, x, y, STAR_RED)
    if r < 2.2:
        put(img, int(cx), int(cy), STAR_RED)
        for dx, dy in ((1, 0), (-1, 0), (0, 1), (0, -1)):
            if r >= 1.2:
                put(img, int(cx) + dx, int(cy) + dy, STAR_RED)


def inside(pts, x, y):
    c = False
    n = len(pts)
    for k in range(n):
        x1, y1 = pts[k]
        x2, y2 = pts[(k + 1) % n]
        if (y1 > y) != (y2 > y) and x < (x2 - x1) * (y - y1) / (y2 - y1 + 1e-9) + x1:
            c = not c
    return c


def balls():
    for n in range(1, 8):
        save_sprite(dragon_ball(16, n), "ball/%d" % n)
    save_sprite(dragon_ball(64, 4), "emblem")


def grid(rows, palette):
    h = len(rows)
    w = max(len(r) for r in rows)
    img = Image.new("RGBA", (w, h), (0, 0, 0, 0))
    for y, row in enumerate(rows):
        for x, ch in enumerate(row):
            if ch in palette:
                put(img, x, y, palette[ch])
    return img


def tab_icons():
    status = grid([
        "................",
        "................",
        "..kkkkkkkkkk....",
        ".kggggggggggk...",
        ".kgGGGGGGGggkk..",
        ".kgGwGGGGGGgkwk.",
        ".kgGGGGGGGGgkwk.",
        ".kggGGGGGGggkwk.",
        "..kggggggggkkwk.",
        "...kkkkkkkk.kwk.",
        ".............kwk",
        "............kssk",
        "............kssk",
        ".............kk.",
        "................",
        "................",
    ], {"k": INK, "g": (40, 180, 70, 255), "G": (120, 240, 120, 220), "w": (230, 250, 230, 255), "s": (150, 150, 160, 255)})
    save_sprite(status, "icon/status")
    tech = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    disc(tech, 8, 8, 6.5, lambda x, y, k: (255, 255, 255, 255) if k < 0.35 else (150, 220, 255, 255) if k < 0.7 else (50, 150, 255, 255))
    for x, y in ((1, 1), (14, 2), (2, 14), (14, 14), (8, 0), (0, 8), (15, 8), (8, 15)):
        put(tech, x, y, (150, 220, 255, 255))
    save_sprite(outline_shape(tech), "icon/techniques")
    forms = grid([
        "................",
        "......y.........",
        ".....yy....y....",
        "..y..yyy..yy....",
        "..yy.yYyy.yy..y.",
        "...yyyYYyyyy.yy.",
        "y...yyYYYYyyyyy.",
        "yy..yYYYYYYyyy..",
        ".yyyyYYYYYYYyy..",
        "..yyYYYYYYYYyyyy",
        "...yYYYYYYYYYyy.",
        "..yyYYYYYYYYYy..",
        "..yyy.ssssss.yy.",
        ".yy...ssssss..y.",
        "......ssssss....",
        "......ssssss....",
    ], {"y": (240, 190, 30, 255), "Y": (255, 240, 120, 255), "s": (244, 200, 150, 255)})
    save_sprite(outline_shape(forms), "icon/forms")
    capsule = grid([
        "................",
        "................",
        "......wwww......",
        ".....wWWWWw.....",
        "....wWWWWWWw....",
        "....wWWbbWWw....",
        "....wWWbbWWw....",
        "....wWWWWWWw....",
        "....rrrrrrrr....",
        "....wWWWWWWw....",
        "....wWWWWWWw....",
        "....wWWgWWWw....",
        "....wWWWWWWw....",
        ".....wWWWWw.....",
        "......wwww......",
        "................",
    ], {"w": (150, 156, 176, 255), "W": (240, 242, 248, 255), "b": (60, 110, 230, 255), "r": (60, 110, 230, 255), "g": (120, 126, 146, 255)})
    save_sprite(outline_shape(capsule), "icon/settings")
    close = Image.new("RGBA", (12, 12), (0, 0, 0, 0))
    for i in range(1, 10):
        for o in (0, 1):
            put(close, i + o, i, (255, 90, 70, 255))
            put(close, 10 - i - o, i, (255, 90, 70, 255))
    save_sprite(outline_shape(close), "icon/close")
    ki = grid([
        ".........",
        "...kkk...",
        "..kbbbk..",
        ".kbwwbbk.",
        ".kbwbbbk.",
        ".kbbbbBk.",
        "..kbbBk..",
        "...kkk...",
        ".........",
    ], {"k": INK, "b": (80, 176, 255, 255), "B": (30, 100, 210, 255), "w": (230, 248, 255, 255)})
    save_sprite(ki, "icon/ki")
    bolt = grid([
        "....kkk..",
        "...kyyk..",
        "..kyyk...",
        ".kyyykkk.",
        ".kkkyyyk.",
        "...kyyk..",
        "..kyyk...",
        "..kyk....",
        "..kk.....",
    ], {"k": INK, "y": (200, 246, 90, 255)})
    save_sprite(bolt, "icon/stamina")


def beam_icon(core, glow, dark, wide=False):
    img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    half = 3 if wide else 2
    for x in range(5, 16):
        spread = half + (1 if x > 11 else 0)
        for y in range(8 - spread, 8 + spread):
            put(img, x, y, glow)
        for y in range(8 - max(1, spread - 2), 8 + max(1, spread - 2)):
            put(img, x, y, core)
    disc(img, 5, 8, 4.6, lambda x, y, k: core if k < 0.5 else glow)
    for x in range(7, 16, 3):
        put(img, x, 8 - half - 1, glow)
    return outline_shape(img, dark)


def thin_icon(core, glow, dark, spiral):
    img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    for i in range(2, 15):
        put(img, i, 15 - i, core)
        put(img, i + 1, 15 - i, glow)
    if spiral:
        for k in range(4):
            cx, cy = 4 + k * 3, 11 - k * 3
            for dx, dy in ((-1, -1), (1, -1), (-1, 1), (1, 1)):
                put(img, cx + dx, cy + dy, glow)
    disc(img, 3, 13, 2.4, lambda x, y, k: (255, 255, 255, 255) if k < 0.5 else glow)
    return outline_shape(img, dark)


def orb_icon(core, glow, dark, rays):
    img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    disc(img, 8, 8, 5.6, lambda x, y, k: (255, 255, 255, 255) if k < 0.3 else core if k < 0.62 else glow)
    if rays:
        for a in range(8):
            ang = a * math.pi / 4
            for rr in (6.6, 7.4):
                put(img, int(8 + math.cos(ang) * rr), int(8 + math.sin(ang) * rr), glow)
    else:
        for x, y in ((1, 13), (2, 12), (3, 11)):
            put(img, x, y, glow)
    return outline_shape(img, dark)


def skill_icons():
    icons = {
        "ki_blast": orb_icon((170, 225, 255, 255), (60, 160, 255, 255), (12, 36, 90, 255), False),
        "kamehameha": beam_icon((225, 248, 255, 255), (63, 168, 255, 255), (10, 40, 100, 255)),
        "raider_cannon": beam_icon((246, 220, 255, 255), (176, 75, 255, 255), (60, 16, 90, 255)),
        "galick_gun": beam_icon((246, 226, 255, 255), (150, 60, 250, 255), (54, 14, 96, 255)),
        "masenko": beam_icon((255, 250, 210, 255), (255, 206, 50, 255), (110, 70, 6, 255)),
        "special_beam_cannon": thin_icon((255, 250, 200, 255), (200, 110, 255, 255), (60, 20, 80, 255), True),
        "final_flash": beam_icon((255, 255, 228, 255), (255, 226, 60, 255), (110, 80, 6, 255), True),
        "big_bang_attack": orb_icon((190, 230, 255, 255), (110, 190, 255, 255), (16, 46, 100, 255), True),
        "death_beam": thin_icon((255, 220, 250, 255), (255, 90, 216, 255), (90, 10, 70, 255), False),
    }
    for name, img in icons.items():
        save_sprite(img, "skill/" + name)


def particle_json(name, frames):
    with open(path("particles", name + ".json"), "w", encoding="utf-8") as f:
        json.dump({"textures": ["dbtenkaichi:%s_%d" % (name, i) for i in range(frames)]}, f, indent=2)


def save_particle(name, index, img):
    img.save(path("textures", "particle", "%s_%d.png" % (name, index)))


def gray(v, a=255):
    g = int(clamp(v) * 255)
    return (g, g, g, a)


def pixel_particle(size, fn):
    img = Image.new("RGBA", (size, size), (0, 0, 0, 0))
    for y in range(size):
        for x in range(size):
            u = (x + 0.5) / size * 2 - 1
            v = (y + 0.5) / size * 2 - 1
            res = fn(x, y, u, v)
            if res is not None:
                put(img, x, y, res)
    return img


def ki_spark():
    shapes = [
        ["...w...", "...w...", "..www..", "wwwWwww", "..www..", "...w...", "...w..."],
        [".......", "...w...", "..wWw..", ".wWWWw.", "..wWw..", "...w...", "......."],
        [".......", ".......", "...w...", "..wWw..", "...w...", ".......", "......."],
        [".......", ".......", ".......", "...W...", ".......", ".......", "......."],
    ]
    for i, rows in enumerate(shapes):
        img = Image.new("RGBA", (8, 8), (0, 0, 0, 0))
        small = grid(rows, {"w": (220, 220, 220, 255), "W": (255, 255, 255, 255)})
        img.alpha_composite(small, (0, 0))
        save_particle("ki_spark", i, img)
    particle_json("ki_spark", len(shapes))


FLAMES = [
    ["...w....", "..ww....", "..wWw...", ".wWWw.w.", ".wWWWww.", "wWWWWWw.", "wWWWWWw.", ".wWWWw.."],
    ["....w...", "...ww...", "..wWww..", "..wWWw..", ".wWWWWw.", ".wWWWWw.", "wWWWWWWw", ".wWWWWw."],
    [".....w..", "....ww..", "...wWw..", "..wWWw..", ".wwWWWw.", ".wWWWWw.", ".wWWWWw.", "..wWWw.."],
    ["........", "...w....", "...ww...", "..wWw...", "..wWWw..", ".wWWWw..", ".wWWWw..", "..www..."],
    ["........", "........", "....w...", "...ww...", "...wWw..", "..wWWw..", "..wWWw..", "...ww..."],
    ["........", "........", "........", "...w....", "...ww...", "..wWw...", "..www...", "........"],
]


def aura_flame():
    for i, rows in enumerate(FLAMES):
        save_particle("aura_flame", i, grid(rows, {"w": (205, 205, 205, 220), "W": (255, 255, 255, 255)}))
    particle_json("aura_flame", len(FLAMES))


def energy_orb():
    frames = 4
    for i in range(frames):
        r = 0.95 - i * 0.12

        def fn(x, y, u, v, r=r):
            d = math.hypot(u, v) / r
            if d > 1.0:
                return None
            if d > 0.78:
                return gray(0.7, 150 if (x + y) % 2 == 0 else 0)
            if d > 0.5:
                return gray(0.85, 220)
            return gray(1.0, 255)
        save_particle("energy_orb", i, pixel_particle(8, fn))
    particle_json("energy_orb", frames)


def impact_star():
    variants = 3
    for i in range(variants):
        rng = random.Random(700 + i)
        spikes = 6 + i * 2
        lengths = [rng.uniform(0.62, 1.0) for _ in range(spikes)]
        rot = rng.uniform(0, math.pi)

        def fn(x, y, u, v, spikes=spikes, lengths=lengths, rot=rot):
            r = math.hypot(u, v)
            ang = (math.atan2(v, u) + rot) % (math.pi * 2)
            seg = ang / (math.pi * 2) * spikes
            idx = int(seg) % spikes
            frac = seg - int(seg)
            spike = (1.0 - abs(frac - 0.5) * 2) ** 2.2
            reach = lengths[idx] * (0.28 + 0.72 * spike)
            if r > reach:
                return None
            return gray(1.0 if r < reach * 0.55 else 0.8, 255)
        save_particle("impact_star", i, pixel_particle(16, fn))
    particle_json("impact_star", variants)


def shock_ring():
    variants = 2
    for i in range(variants):
        inner = 0.72 - i * 0.12

        def fn(x, y, u, v, inner=inner):
            r = math.hypot(u, v)
            if inner <= r <= 0.98:
                return gray(1.0 if r < inner + 0.1 else 0.8, 255 if r < 0.9 else 170)
            return None
        save_particle("shock_ring", i, pixel_particle(16, fn))
    particle_json("shock_ring", variants)


def dust_puff():
    frames = 4
    for i in range(frames):
        rng = random.Random(900 + i)
        blobs = [(rng.uniform(-0.25, 0.25), rng.uniform(-0.2, 0.25), rng.uniform(0.3, 0.5) * (1 - i * 0.12)) for _ in range(3)]

        def fn(x, y, u, v, blobs=blobs):
            inside_any = False
            for bx, by, br in blobs:
                if math.hypot(u - bx, v - by) < br:
                    inside_any = True
            if not inside_any or math.hypot(u, v) > 0.92:
                return None
            shade = 0.95 if v < 0.0 else 0.75
            return gray(shade, 170 if v < 0.0 else 140)
        save_particle("dust_puff", i, pixel_particle(8, fn))
    particle_json("dust_puff", frames)


def lightning():
    variants = 4
    size = 16
    for i in range(variants):
        rng = random.Random(1100 + i)
        img = Image.new("RGBA", (size, size), (0, 0, 0, 0))
        x = rng.randint(5, 10)
        for y in range(size):
            if y % 3 == 0 and y > 0:
                x = max(1, min(size - 2, x + rng.choice([-2, -1, 1, 2])))
            put(img, x, y, (255, 255, 255, 255))
            put(img, x + 1, y, (200, 200, 200, 170))
        bx = x
        for y in range(size // 2, size // 2 + 5):
            bx = max(0, min(size - 1, bx + rng.choice([-1, 1])))
            put(img, bx, y, (230, 230, 230, 220))
        save_particle("lightning", i, img)
    particle_json("lightning", variants)


def speed_line():
    img = Image.new("RGBA", (8, 8), (0, 0, 0, 0))
    for y in range(8):
        put(img, 3, y, (255, 255, 255, 255 if 1 <= y <= 6 else 140))
        if 2 <= y <= 5:
            put(img, 4, y, (220, 220, 220, 200))
    save_particle("speed_line", 0, img)
    particle_json("speed_line", 1)


def rock():
    variants = 3
    for i in range(variants):
        rng = random.Random(1500 + i)
        w = rng.randint(4, 6)
        h = rng.randint(3, 5)
        img = Image.new("RGBA", (8, 8), (0, 0, 0, 0))
        x0 = (8 - w) // 2
        y0 = (8 - h) // 2
        for y in range(h):
            for x in range(w):
                if (x, y) in ((0, 0), (w - 1, h - 1)) and rng.random() < 0.7:
                    continue
                v = 0.95 if y == 0 else 0.55 if y == h - 1 or x == w - 1 else 0.78
                if rng.random() < 0.2:
                    v -= 0.1
                put(img, x0 + x, y0 + y, gray(v))
        save_particle("rock", i, img)
    particle_json("rock", variants)


def roshi_gear():
    img = Image.new("RGBA", (64, 64), (0, 0, 0, 0))
    base = (142, 124, 224, 255)
    seam = (88, 72, 158, 255)
    light = (178, 166, 244, 255)
    for y in range(24):
        for x in range(40):
            row = y // 4
            col_off = 3 if row % 2 else 0
            if y % 4 == 0 or (x + col_off) % 6 == 0:
                put(img, x, y, seam)
            elif y % 4 == 1 and (x + col_off) % 6 in (1, 2):
                put(img, x, y, light)
            else:
                put(img, x, y, base)
    for y in range(24, 40):
        for x in range(40):
            put(img, x, y, (96, 80, 170, 255) if (x + y) % 5 else (78, 64, 146, 255))
    for y in range(0, 16):
        for x in range(40, 64):
            put(img, x, y, (214, 190, 140, 255) if (x + y) % 3 else (170, 146, 100, 255))
    for y in range(16, 24):
        for x in range(40, 64):
            put(img, x, y, (226, 52, 52, 255) if y % 4 else (160, 28, 30, 255))
    for y in range(24, 32):
        for x in range(40, 64):
            put(img, x, y, (34, 26, 40, 255))
        put(img, 41, y, (110, 100, 130, 255))
    img.save(path("textures", "entity", "roshi_gear.png"))


if __name__ == "__main__":
    roshi_gear()
    ui_sprites()
    balls()
    tab_icons()
    skill_icons()
    ki_spark()
    aura_flame()
    energy_orb()
    impact_star()
    shock_ring()
    dust_puff()
    lightning()
    speed_line()
    rock()
    print("ui sprites, icons and pixel particles generated")
