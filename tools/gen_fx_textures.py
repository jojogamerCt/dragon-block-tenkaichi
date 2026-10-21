import json
import math
import os
import random

from PIL import Image, ImageDraw

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
ASSETS = os.path.join(ROOT, "common", "src", "main", "resources", "assets", "dbtenkaichi")


def path(*parts):
    p = os.path.join(ASSETS, *parts)
    os.makedirs(os.path.dirname(p), exist_ok=True)
    return p


def clamp(v, lo=0.0, hi=1.0):
    return max(lo, min(hi, v))


def shaded_item(size, mask_fn, color_fn, light=(-0.6, -0.7), outline=0.38):
    img = Image.new("RGBA", (size, size), (0, 0, 0, 0))
    px = img.load()
    inside = [[mask_fn((x + 0.5) / size, (y + 0.5) / size) for x in range(size)] for y in range(size)]
    for y in range(size):
        for x in range(size):
            if not inside[y][x]:
                continue
            edge = any(not (0 <= x + dx < size and 0 <= y + dy < size and inside[y + dy][x + dx]) for dx, dy in ((1, 0), (-1, 0), (0, 1), (0, -1)))
            u = (x + 0.5) / size
            v = (y + 0.5) / size
            base, nx, ny = color_fn(u, v)
            lam = clamp(0.62 - (nx * light[0] + ny * light[1]) * 0.5)
            r, g, b = (int(clamp(c / 255 * (0.55 + lam * 0.75)) * 255) for c in base)
            if edge:
                r, g, b = (int(c * outline) for c in base)
            px[x, y] = (r, g, b, 255)
    return img


def senzu():
    size = 32

    def mask(u, v):
        dx = (u - 0.5) / 0.36
        dy = (v - 0.52) / 0.24
        bend = 0.18 * math.sin((u - 0.5) * 3.0)
        dy2 = (v - 0.52 - bend * 0.3) / 0.24
        return dx * dx + dy2 * dy2 <= 1.0 and not (abs(u - 0.5) < 0.06 and v < 0.38)

    def color(u, v):
        nx = (u - 0.5) / 0.36
        ny = (v - 0.52) / 0.24
        crease = abs((u - 0.5) * 0.9 - (v - 0.5) * 0.4) < 0.025 and 0.3 < u < 0.7
        base = (74, 140, 52) if crease else (112, 184, 70)
        return base, nx, ny
    img = shaded_item(size, mask, color)
    d = ImageDraw.Draw(img)
    d.line([(11, 13), (15, 12)], fill=(214, 245, 170, 255))
    d.point((12, 12), fill=(240, 255, 220, 255))
    img.save(path("textures", "item", "senzu_bean.png"))


def egg(name, base, spot, accent):
    size = 32
    rng = random.Random(name)
    spots = [(rng.uniform(0.32, 0.68), rng.uniform(0.3, 0.8), rng.uniform(0.05, 0.09)) for _ in range(7)]

    def mask(u, v):
        dx = (u - 0.5) / (0.3 + (v - 0.2) * 0.12)
        dy = (v - 0.55) / 0.4
        return dx * dx + dy * dy <= 1.0

    def color(u, v):
        nx = (u - 0.5) / 0.36
        ny = (v - 0.55) / 0.4
        c = base
        for sx, sy, sr in spots:
            if math.hypot(u - sx, (v - sy) * 0.9) < sr:
                c = spot
        if abs(v - 0.62) < 0.035:
            c = accent
        return c, nx, ny
    img = shaded_item(size, mask, color)
    d = ImageDraw.Draw(img)
    d.ellipse([11, 8, 13, 11], fill=(255, 255, 255, 190))
    img.save(path("textures", "item", name + ".png"))


def item_models():
    for name in ["senzu_bean", "master_spawn_egg", "saiyan_raider_spawn_egg"]:
        with open(path("models", "item", name + ".json"), "w", encoding="utf-8") as f:
            json.dump({"parent": "minecraft:item/generated", "textures": {"layer0": "dbtenkaichi:item/" + name}}, f, indent=2)


if __name__ == "__main__":
    senzu()
    egg("master_spawn_egg", (240, 122, 26), (31, 63, 174), (250, 250, 250))
    egg("saiyan_raider_spawn_egg", (44, 40, 66), (176, 75, 255), (230, 220, 200))
    item_models()
    print("item textures generated")
