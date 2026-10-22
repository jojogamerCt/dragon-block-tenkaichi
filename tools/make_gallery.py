import math
import os
import sys

from PIL import Image, ImageDraw, ImageFilter, ImageFont, ImageEnhance

SRC = sys.argv[1] if len(sys.argv) > 1 else "fabric/runs/client/screenshots"
OUT = sys.argv[2] if len(sys.argv) > 2 else "marketing/curseforge"
W, H = 1920, 1080
TITLE_FONT = "C:/Windows/Fonts/impact.ttf"
SUB_FONT = "C:/Windows/Fonts/bahnschrift.ttf"
BRAND = "DRAGON BLOCK: TENKAICHI"


def shot(name):
    return Image.open(os.path.join(SRC, "mkt_" + name + ".png")).convert("RGB")


def cover(img, w, h, cx=0.5, cy=0.5, zoom=1.0):
    scale = max(w / img.width, h / img.height) * zoom
    resized = img.resize((max(w, int(img.width * scale)), max(h, int(img.height * scale))), Image.LANCZOS)
    x = int((resized.width - w) * cx)
    y = int((resized.height - h) * cy)
    return resized.crop((x, y, x + w, y + h))


def grade(img, saturation=1.25, contrast=1.1):
    img = ImageEnhance.Color(img).enhance(saturation)
    return ImageEnhance.Contrast(img).enhance(contrast)


def vignette(img, strength=0.65):
    mask = Image.new("L", img.size, 0)
    d = ImageDraw.Draw(mask)
    w, h = img.size
    for i in range(60):
        t = i / 60
        alpha = int(255 * strength * (1 - t) ** 2)
        d.rectangle((int(w * t * 0.18), int(h * t * 0.18), int(w - w * t * 0.18), int(h - h * t * 0.18)), outline=alpha, width=int(w * 0.004) + 1)
    mask = mask.filter(ImageFilter.GaussianBlur(60))
    dark = Image.new("RGB", img.size, (0, 0, 0))
    return Image.composite(dark, img, mask)


def bands(img, top=210, bottom=120):
    over = Image.new("RGBA", img.size, (0, 0, 0, 0))
    d = ImageDraw.Draw(over)
    for y in range(top):
        d.line((0, y, W, y), fill=(0, 0, 0, int(200 * (1 - y / top) ** 1.4)))
    for y in range(bottom):
        d.line((0, H - 1 - y, W, H - 1 - y), fill=(0, 0, 0, int(190 * (1 - y / bottom) ** 1.4)))
    return Image.alpha_composite(img.convert("RGBA"), over)


def burst(img, cx, cy, color=(255, 200, 60), rays=48, length=900, alpha=70):
    over = Image.new("RGBA", img.size, (0, 0, 0, 0))
    d = ImageDraw.Draw(over)
    for i in range(rays):
        a = 2 * math.pi * i / rays
        spread = 0.035 if i % 2 else 0.02
        p1 = (cx + math.cos(a - spread) * length, cy + math.sin(a - spread) * length * 0.55)
        p2 = (cx + math.cos(a + spread) * length, cy + math.sin(a + spread) * length * 0.55)
        d.polygon([(cx, cy), p1, p2], fill=color + (alpha if i % 2 else alpha // 2,))
    over = over.filter(ImageFilter.GaussianBlur(6))
    return Image.alpha_composite(img.convert("RGBA"), over)


def gradient_text(img, text, size, x, y, top=(255, 245, 140), bottom=(255, 120, 10), stroke=10, anchor="mt", glow=(255, 150, 0)):
    font = ImageFont.truetype(TITLE_FONT, size)
    mask = Image.new("L", img.size, 0)
    ImageDraw.Draw(mask).text((x, y), text, font=font, fill=255, anchor=anchor)
    box = mask.getbbox()
    if box is None:
        return img
    glow_layer = Image.new("RGBA", img.size, glow + (0,))
    glow_mask = Image.new("L", img.size, 0)
    ImageDraw.Draw(glow_mask).text((x, y), text, font=font, fill=230, anchor=anchor, stroke_width=stroke + 14)
    glow_layer.putalpha(glow_mask.filter(ImageFilter.GaussianBlur(22)))
    img = Image.alpha_composite(img.convert("RGBA"), glow_layer)
    shadow = Image.new("RGBA", img.size, (0, 0, 0, 0))
    ImageDraw.Draw(shadow).text((x + 8, y + 10), text, font=font, fill=(0, 0, 0, 200), anchor=anchor, stroke_width=stroke, stroke_fill=(0, 0, 0, 200))
    img = Image.alpha_composite(img, shadow.filter(ImageFilter.GaussianBlur(4)))
    outline = Image.new("RGBA", img.size, (0, 0, 0, 0))
    ImageDraw.Draw(outline).text((x, y), text, font=font, fill=(20, 8, 0, 255), anchor=anchor, stroke_width=stroke, stroke_fill=(20, 8, 0, 255))
    img = Image.alpha_composite(img, outline)
    grad = Image.new("RGBA", img.size, (0, 0, 0, 0))
    gd = ImageDraw.Draw(grad)
    for yy in range(box[1], box[3] + 1):
        t = (yy - box[1]) / max(1, box[3] - box[1])
        c = tuple(int(top[i] + (bottom[i] - top[i]) * t) for i in range(3))
        gd.line((box[0], yy, box[2], yy), fill=c + (255,))
    shine = Image.new("L", img.size, 0)
    ImageDraw.Draw(shine).rectangle((box[0], box[1], box[2], box[1] + (box[3] - box[1]) * 0.45), fill=70)
    grad.putalpha(mask)
    img = Image.alpha_composite(img, grad)
    white = Image.new("RGBA", img.size, (255, 255, 255, 0))
    from PIL import ImageChops
    white.putalpha(ImageChops.multiply(shine, mask))
    return Image.alpha_composite(img, white)


def plain_text(img, text, size, x, y, fill=(255, 255, 255), anchor="mt", stroke=4, font_path=SUB_FONT):
    font = ImageFont.truetype(font_path, size)
    layer = Image.new("RGBA", img.size, (0, 0, 0, 0))
    ImageDraw.Draw(layer).text((x, y), text, font=font, fill=fill + (255,), anchor=anchor, stroke_width=stroke, stroke_fill=(0, 0, 0, 255))
    return Image.alpha_composite(img.convert("RGBA"), layer)


def brand(img):
    img = plain_text(img, BRAND, 34, W - 40, H - 30, fill=(255, 214, 90), anchor="rs", stroke=3, font_path=TITLE_FONT)
    return img


def frame(img, box, border=(255, 190, 40), width=6):
    d = ImageDraw.Draw(img)
    d.rectangle(box, outline=(0, 0, 0), width=width + 6)
    d.rectangle(box, outline=border, width=width)
    return img


def paste_card(canvas, img, box, border=(255, 190, 40), tilt=0.0):
    w, h = box[2] - box[0], box[3] - box[1]
    card = img.resize((w, h), Image.LANCZOS).convert("RGBA")
    card = frame(card, (0, 0, w - 1, h - 1), border)
    shadow = Image.new("RGBA", canvas.size, (0, 0, 0, 0))
    ImageDraw.Draw(shadow).rectangle((box[0] + 14, box[1] + 18, box[2] + 14, box[3] + 18), fill=(0, 0, 0, 170))
    canvas = Image.alpha_composite(canvas, shadow.filter(ImageFilter.GaussianBlur(12)))
    if tilt:
        card = card.rotate(tilt, expand=True, resample=Image.BICUBIC)
        canvas.alpha_composite(card, (box[0] - (card.width - w) // 2, box[1] - (card.height - h) // 2))
    else:
        canvas.alpha_composite(card, (box[0], box[1]))
    return canvas


def finish(img, name):
    os.makedirs(OUT, exist_ok=True)
    img.convert("RGB").save(os.path.join(OUT, name + ".png"), optimize=True)
    print("wrote", name)


def hero(background, title, subtitle, name, cx=0.5, cy=0.5, zoom=1.0, color=(255, 200, 60), top=None, bottom=None):
    img = grade(cover(shot(background), W, H, cx, cy, zoom))
    img = vignette(img)
    img = bands(img)
    img = burst(img, W // 2, 60, color, length=1100, alpha=55)
    kwargs = {}
    if top:
        kwargs["top"] = top
    if bottom:
        kwargs["bottom"] = bottom
    img = gradient_text(img, title, 150, W // 2, 22, **kwargs)
    img = plain_text(img, subtitle, 44, W // 2, 196)
    return finish(brand(img), name)


def transformations():
    img = grade(cover(shot("ssj2_full"), W, H, zoom=1.05)).filter(ImageFilter.GaussianBlur(18))
    img = ImageEnhance.Brightness(img).enhance(0.55)
    img = burst(img.convert("RGBA"), W // 2, H // 2 + 80, (255, 210, 60), rays=64, length=1400, alpha=60)
    labels = [("ssj_full", "SUPER SAIYAN"), ("ssj2_full", "SUPER SAIYAN 2"), ("ssj3_full", "SUPER SAIYAN 3")]
    cw, ch = 540, 660
    gap = 60
    x0 = (W - cw * 3 - gap * 2) // 2
    for i, (src, label) in enumerate(labels):
        card = cover(shot(src), cw, ch, 0.5, 0.18, zoom=1.45)
        box = (x0 + i * (cw + gap), 300, x0 + i * (cw + gap) + cw, 300 + ch)
        img = paste_card(img, grade(card), box, tilt=(-2.5, 0, 2.5)[i])
        img = plain_text(img, label, 50, box[0] + cw // 2, box[3] - 20, fill=(255, 226, 90), anchor="ms", stroke=5, font_path=TITLE_FONT)
    img = gradient_text(img, "TRANSFORMATIONS!", 170, W // 2, 30)
    img = plain_text(img, "Unlock every form by defeating yourself in a trial", 42, W // 2, 222)
    finish(brand(img), "02_transformations")


def races():
    order = ["saiyan", "half_saiyan", "human", "namekian", "frieza_race", "majin", "android", "bio_android", "kaioshin", "demon"]
    names = ["SAIYAN", "HALF-SAIYAN", "HUMAN", "NAMEKIAN", "FROST DEMON", "MAJIN", "ANDROID", "BIO-ANDROID", "KAIOSHIN", "DEMON"]
    img = grade(cover(shot("race_saiyan"), W, H)).filter(ImageFilter.GaussianBlur(24))
    img = ImageEnhance.Brightness(img).enhance(0.45).convert("RGBA")
    img = burst(img, W // 2, 140, (90, 170, 255), length=1200, alpha=40)
    cw, ch = 330, 360
    gap = 28
    x0 = (W - cw * 5 - gap * 4) // 2
    for i, race in enumerate(order):
        variant = "_f" if i % 2 else ""
        card = cover(shot("race_" + race + variant), cw, ch, 0.5, 0.42, zoom=1.75)
        col, row = i % 5, i // 5
        box = (x0 + col * (cw + gap), 260 + row * (ch + 30), x0 + col * (cw + gap) + cw, 260 + row * (ch + 30) + ch)
        img = paste_card(img, grade(card), box, border=(120, 190, 255))
        img = plain_text(img, names[i], 36, box[0] + cw // 2, box[3] - 16, fill=(255, 255, 255), anchor="ms", stroke=4, font_path=TITLE_FONT)
    img = gradient_text(img, "10 PLAYABLE RACES", 150, W // 2, 24, top=(220, 245, 255), bottom=(60, 150, 255), glow=(40, 120, 255))
    img = plain_text(img, "Masculine and feminine looks, canonical hairstyles, racial techniques", 40, W // 2, 196)
    finish(brand(img), "03_races")


def locations():
    img = grade(cover(shot("kami_lookout"), W, H)).filter(ImageFilter.GaussianBlur(10))
    img = ImageEnhance.Brightness(img).enhance(0.6).convert("RGBA")
    items = [("kame_house", "KAME HOUSE", 0.5, 0.55, 2.2), ("korin_tower", "KORIN TOWER", 0.5, 0.3, 1.5), ("kami_lookout", "KAMI'S LOOKOUT", 0.5, 0.45, 2.4)]
    cw, ch = 580, 620
    gap = 40
    x0 = (W - cw * 3 - gap * 2) // 2
    for i, (src, label, cx, cy, zoom) in enumerate(items):
        card = cover(shot(src), cw, ch, cx, cy, zoom=zoom)
        box = (x0 + i * (cw + gap), 300, x0 + i * (cw + gap) + cw, 300 + ch)
        img = paste_card(img, grade(card), box)
        img = plain_text(img, label, 50, box[0] + cw // 2, box[3] - 20, fill=(255, 226, 90), anchor="ms", stroke=5, font_path=TITLE_FONT)
    img = gradient_text(img, "ICONIC LOCATIONS", 160, W // 2, 26)
    img = plain_text(img, "Indestructible landmarks with their Masters: Roshi, Korin and Kami", 42, W // 2, 212)
    finish(brand(img), "05_locations")


def trials():
    img = grade(cover(shot("trial_cell_fight"), W, H, 0.5, 0.6, zoom=1.15))
    img = vignette(img)
    img = bands(img)
    inset = cover(shot("trial_namek_fight"), 620, 350, 0.5, 0.6, zoom=1.3)
    img = paste_card(img, grade(inset), (W - 700, H - 470, W - 80, H - 120), tilt=3)
    img = gradient_text(img, "TRIALS!", 160, W // 2, 22)
    img = plain_text(img, "Face yourself on the Cell Games ring, Namek and more", 44, W // 2, 196)
    finish(brand(img), "06_trials")


def cutscenes():
    img = grade(cover(shot("ssj2_streak"), W, H, 0.5, 0.5, zoom=1.0), 1.15)
    img = bands(img, 230, 120)
    inset = cover(shot("ssj_face"), 560, 315, 0.5, 0.5)
    img = paste_card(img, grade(inset), (80, H - 420, 640, H - 105), tilt=-3)
    img = gradient_text(img, "CANON CUTSCENES", 150, W // 2, 24, top=(255, 240, 240), bottom=(255, 40, 40), glow=(255, 30, 0))
    img = plain_text(img, "Every transformation has its own anime-style cinematic", 44, W // 2, 196)
    finish(brand(img), "01_cutscenes")


def techniques():
    img = grade(cover(shot("kamehameha_charge"), W, H, 0.55, 0.5, zoom=1.1))
    img = vignette(img)
    img = bands(img)
    inset = cover(shot("beam_clash"), 620, 350, 0.5, 0.5, zoom=1.2)
    img = paste_card(img, grade(inset), (W - 700, H - 470, W - 80, H - 120), border=(90, 180, 255), tilt=3)
    img = gradient_text(img, "KAMEHAMEHA!", 160, W // 2, 22, top=(225, 248, 255), bottom=(40, 140, 255), glow=(30, 120, 255))
    img = plain_text(img, "Beam clashes, Final Flash, Big Bang Attack, Galick Gun and more", 42, W // 2, 196)
    finish(brand(img), "04_techniques")


def kaioken():
    hero("kaioken", "POWER UP!", "Charge your Ki and stack the Kaioken up to x20 on top of your forms", "07_power_up", 0.5, 0.3, 1.05,
         color=(255, 70, 40), top=(255, 230, 210), bottom=(230, 20, 10))


def creator():
    img = grade(cover(shot("creator"), W, H), 1.1, 1.05)
    img = ImageEnhance.Brightness(img).enhance(0.9)
    img = bands(img, 250, 120)
    img = gradient_text(img, "CREATE YOUR FIGHTER", 140, W // 2, 24)
    img = plain_text(img, "Race, build, face, hair, outfit and aura color", 42, W // 2, 190)
    finish(brand(img), "08_character_creator")


def menu():
    img = grade(cover(shot("menu_status"), W, H), 1.05, 1.02)
    img = bands(img, 250, 120)
    img = gradient_text(img, "CHARACTER MENU", 150, W // 2, 22)
    img = plain_text(img, "Full screen: your fighter, stats, techniques and transformations", 42, W // 2, 190)
    techniques = cover(shot("menu_techniques"), 820, 462)
    img = paste_card(img.convert("RGBA"), techniques, (60, 560, 880, 1022))
    finish(brand(img), "08_character_menu")


def roshi():
    hero("roshi", "MASTER ROSHI", "The Turtle Hermit of Kame House teaches the Kamehameha", "09_master_roshi", 0.5, 0.5, 1.0,
         color=(255, 170, 40))


def majin():
    img = grade(cover(shot("kid_buu_full"), W, H)).filter(ImageFilter.GaussianBlur(22))
    img = ImageEnhance.Brightness(img).enhance(0.5).convert("RGBA")
    img = burst(img, W // 2, 140, (255, 110, 190), length=1200, alpha=55)
    cards = [("majin_fat", "MAJIN"), ("evil_buu_full", "EVIL BUU"), ("super_buu_full", "SUPER BUU"), ("kid_buu_full", "KID BUU")]
    cw, ch = 380, 440
    gap = 34
    x0 = (W - cw * 4 - gap * 3) // 2
    for i, (src, label) in enumerate(cards):
        card = cover(shot(src), cw, ch, 0.5, 0.36, zoom=1.5)
        box = (x0 + i * (cw + gap), 250, x0 + i * (cw + gap) + cw, 250 + ch)
        img = paste_card(img, grade(card), box, border=(255, 140, 200), tilt=(-2.0, -0.7, 0.7, 2.0)[i])
        img = plain_text(img, label, 44, box[0] + cw // 2, box[3] - 18, fill=(255, 210, 235), anchor="ms", stroke=5, font_path=TITLE_FONT)
    stills = ["evil_buu_cloud", "super_buu_smoke", "kid_buu_swell"]
    sw, sh = 520, 292
    sx0 = (W - sw * 3 - gap * 2) // 2
    for i, src in enumerate(stills):
        box = (sx0 + i * (sw + gap), 740, sx0 + i * (sw + gap) + sw, 740 + sh)
        img = paste_card(img, grade(cover(shot(src), sw, sh)), box, border=(255, 140, 200))
    img = gradient_text(img, "MAJIN BUU!", 160, W // 2, 24, top=(255, 230, 245), bottom=(255, 70, 160), glow=(255, 60, 150))
    img = plain_text(img, "Majins are fat by default and transform into Evil, Super and Kid Buu", 42, W // 2, 196)
    finish(brand(img), "10_majin_buu")


def destruction():
    hero("big_bang_crater", "DESTRUCTION!", "Big Bang Attack craters and explosions that grow with your power", "09_destruction", 0.5, 0.5, 1.1)


if __name__ == "__main__":
    cutscenes()
    transformations()
    races()
    techniques()
    locations()
    trials()
    kaioken()
    menu()
    roshi()
    majin()
