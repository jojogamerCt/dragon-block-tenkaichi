package com.dbtenkaichi.client.render;

import com.dbtenkaichi.Tenkaichi;
import com.dbtenkaichi.player.Appearance;
import com.dbtenkaichi.player.HairStyle;
import com.dbtenkaichi.race.Race;
import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.Identifier;

import java.util.LinkedHashMap;
import java.util.Map;

public final class CustomSkins {
    private static final int MAX_CACHED = 48;
    public static final int ROSHI_OUTFIT = 9;
    private static final Map<Long, Identifier> CACHE = new LinkedHashMap<>(16, 0.75f, true);
    private static int counter;

    private CustomSkins() {
    }

    public static Identifier textureFor(Race race, Appearance a, int eyeColor, int hairColor) {
        return textureFor(race, a, eyeColor, hairColor, a.skinColor, -1);
    }

    public static Identifier textureFor(Race race, Appearance a, int eyeColor, int hairColor, int skinColor, int scleraColor) {
        long key = hash(race, a, eyeColor, hairColor) * 31L + skinColor * 1_000_003L + scleraColor;
        Identifier existing = CACHE.get(key);
        if (existing != null) {
            return existing;
        }
        NativeImage image = new SkinPainter(race, a, eyeColor, hairColor, skinColor, scleraColor).paint();
        Identifier id = Tenkaichi.id("dynamic/skin_" + (counter++));
        Minecraft.getInstance().getTextureManager().register(id, new DynamicTexture(id::toString, image));
        CACHE.put(key, id);
        if (CACHE.size() > MAX_CACHED) {
            Map.Entry<Long, Identifier> eldest = CACHE.entrySet().iterator().next();
            CACHE.remove(eldest.getKey());
            Minecraft.getInstance().getTextureManager().release(eldest.getValue());
        }
        return id;
    }

    private static final Map<Long, Identifier> OUTFITS = new LinkedHashMap<>();
    private static final int[][] TOP_REGIONS = {{16, 16, 40, 32}, {16, 32, 40, 48}, {40, 16, 56, 32}, {40, 32, 56, 48}, {32, 48, 48, 64}, {48, 48, 64, 64}};
    private static final int[][] ARM_REGIONS = {{40, 16}, {40, 32}, {32, 48}, {48, 48}};
    private static final int[][] LEG_REGIONS = {{0, 16}, {0, 32}, {16, 48}, {0, 48}};

    public static Identifier outfitTexture(Race race, Appearance a, int[] spec) {
        long key = 31L + race.ordinal() * 7L + a.presentation * 131L + a.build * 1009L;
        for (int v : spec) {
            key = key * 1_000_003L + v;
        }
        Identifier existing = OUTFITS.get(key);
        if (existing != null) {
            return existing;
        }
        NativeImage result = new NativeImage(128, 128, true);
        for (int piece = 0; piece < 3; piece++) {
            int style = spec[piece * 3];
            if (style < 0) {
                continue;
            }
            Appearance copy = Appearance.defaultsFor(race);
            copy.load(a.save());
            copy.outfit = style;
            copy.outfitPrimary = spec[piece * 3 + 1];
            copy.outfitSecondary = spec[piece * 3 + 2];
            NativeImage layer = new SkinPainter(race, copy, 0, 0, copy.skinColor, -1).paintOutfit();
            for (int y = 0; y < 128; y++) {
                for (int x = 0; x < 128; x++) {
                    if (piece(x / 2, y / 2) == piece) {
                        result.setPixel(x, y, layer.getPixel(x, y));
                    }
                }
            }
            layer.close();
        }
        for (int[] arm : ARM_REGIONS) {
            for (int y = arm[1] * 2; y < (arm[1] + 4) * 2; y++) {
                for (int x = (arm[0] + 8) * 2; x < (arm[0] + 12) * 2; x++) {
                    result.setPixel(x, y, 0);
                }
            }
        }
        Identifier id = Tenkaichi.id("dynamic/outfit_" + (counter++));
        Minecraft.getInstance().getTextureManager().register(id, new DynamicTexture(id::toString, result));
        OUTFITS.put(key, id);
        if (OUTFITS.size() > MAX_CACHED) {
            Map.Entry<Long, Identifier> eldest = OUTFITS.entrySet().iterator().next();
            OUTFITS.remove(eldest.getKey());
            Minecraft.getInstance().getTextureManager().release(eldest.getValue());
        }
        return id;
    }

    private static int piece(int x, int y) {
        for (int[] r : TOP_REGIONS) {
            if (x >= r[0] && x < r[2] && y >= r[1] && y < r[3]) {
                return 0;
            }
        }
        for (int[] r : LEG_REGIONS) {
            int lx = x - r[0];
            int ly = y - r[1];
            if (lx >= 0 && lx < 16 && ly >= 0 && ly < 16) {
                if (ly < 4) {
                    return lx >= 8 && lx < 12 ? 2 : 1;
                }
                return ly - 4 < 9 ? 1 : 2;
            }
        }
        return -1;
    }

    public static void clear() {
        for (Identifier id : OUTFITS.values()) {
            Minecraft.getInstance().getTextureManager().release(id);
        }
        OUTFITS.clear();
        for (Identifier id : CACHE.values()) {
            Minecraft.getInstance().getTextureManager().release(id);
        }
        CACHE.clear();
    }

    private static long hash(Race race, Appearance a, int eyeColor, int hairColor) {
        long h = 17 + race.ordinal();
        int[] values = {a.presentation, a.build, a.skinColor, a.eyeStyle, eyeColor, a.eyebrowStyle, a.hairStyle, hairColor, a.expression, a.outfit,
                a.outfitPrimary, a.outfitSecondary, a.featureColor};
        for (int v : values) {
            h = h * 1_000_003L + v;
        }
        return h;
    }

    static int shade(int rgb, float factor) {
        int r = Math.min(255, Math.max(0, Math.round((rgb >> 16 & 255) * factor)));
        int g = Math.min(255, Math.max(0, Math.round((rgb >> 8 & 255) * factor)));
        int b = Math.min(255, Math.max(0, Math.round((rgb & 255) * factor)));
        return r << 16 | g << 8 | b;
    }

    static int mix(int a, int b, float t) {
        int r = Math.round((a >> 16 & 255) + ((b >> 16 & 255) - (a >> 16 & 255)) * t);
        int g = Math.round((a >> 8 & 255) + ((b >> 8 & 255) - (a >> 8 & 255)) * t);
        int bl = Math.round((a & 255) + ((b & 255) - (a & 255)) * t);
        return r << 16 | g << 8 | bl;
    }

    static final class SkinPainter {
        private static final int S = 2;
        private final NativeImage img = new NativeImage(128, 128, true);
        private final Race race;
        private final Appearance a;
        private final int eyeColor;
        private final int hairColor;
        private final boolean bald;
        private final int skin;
        private final int white;
        private final int whiteShade;

        SkinPainter(Race race, Appearance a, int eyeColor, int hairColor, int skin, int sclera) {
            this.race = race;
            this.a = a;
            this.eyeColor = eyeColor;
            this.hairColor = hairColor;
            this.bald = HairStyle.byOrdinal(a.hairStyle) == HairStyle.BALD || !race.hasHair();
            this.skin = skin;
            this.white = sclera < 0 ? 0xF4F4F4 : sclera;
            this.whiteShade = sclera < 0 ? 0xE6E6E6 : shade(sclera, 0.85f);
        }

        NativeImage paint() {
            body(0, 0, 8, 8, 8, true);
            body(16, 16, 8, 12, 4, false);
            body(40, 16, 4, 12, 4, false);
            body(32, 48, 4, 12, 4, false);
            body(0, 16, 4, 12, 4, false);
            body(16, 48, 4, 12, 4, false);
            if (race == Race.BIO_ANDROID) {
                bioBase();
            }
            torsoAnatomy();
            if (a.presentation == 1) {
                feminineBody();
            }
            limbAnatomy(40, 16);
            limbAnatomy(32, 48);
            scalp();
            face();
            ears();
            raceDetails();
            natural();
            return img;
        }

        NativeImage paintOutfit() {
            switch (a.outfit) {
                case 0 -> gi();
                case 1 -> armor();
                case 2 -> casual();
                case ROSHI_OUTFIT -> roshi();
                default -> {
                }
            }
            return img;
        }

        private void px(int x, int y, int rgb) {
            if (x >= 0 && y >= 0 && x < 128 && y < 128) {
                img.setPixel(x, y, 0xFF000000 | (rgb & 0xFFFFFF));
            }
        }

        private void clear(int x, int y) {
            if (x >= 0 && y >= 0 && x < 128 && y < 128) {
                img.setPixel(x, y, 0);
            }
        }

        private int noise(int x, int y) {
            int h = x * 374761393 + y * 668265263 + race.ordinal() * 2147483;
            h = (h ^ (h >>> 13)) * 1274126177;
            return (h ^ (h >>> 16)) & 255;
        }

        private void rect(int x, int y, int w, int h, int rgb, float top, float bottom, float grain) {
            for (int dy = 0; dy < h; dy++) {
                float t = h <= 1 ? 0 : (float) dy / (h - 1);
                float f = top + (bottom - top) * t;
                for (int dx = 0; dx < w; dx++) {
                    float n = 1.0f + (noise(x + dx, y + dy) / 255.0f - 0.5f) * grain;
                    px(x + dx, y + dy, shade(rgb, f * n));
                }
            }
        }

        private void hd(int u, int v, int w, int h, int rgb, float top, float bottom, float grain) {
            rect(u * S, v * S, w * S, h * S, rgb, top, bottom, grain);
        }

        private void body(int u, int v, int w, int h, int d, boolean head) {
            hd(u + d, v, w, d, skin, 1.08f, 1.0f, 0.05f);
            hd(u + d + w, v, w, d, skin, 0.72f, 0.66f, 0.05f);
            hd(u, v + d, d, h, skin, 0.92f, 0.8f, 0.05f);
            hd(u + d, v + d, w, h, skin, 1.04f, 0.9f, 0.05f);
            hd(u + d + w, v + d, d, h, skin, 0.92f, 0.8f, 0.05f);
            hd(u + d + w + d, v + d, w, h, skin, 0.86f, 0.76f, 0.05f);
            if (!head) {
                edgeShade((u + d) * S, (v + d) * S, w * S, h * S, 0.9f);
            }
        }

        private void edgeShade(int x, int y, int w, int h, float f) {
            for (int dy = 0; dy < h; dy++) {
                darken(x, y + dy, f);
                darken(x + w - 1, y + dy, f);
            }
        }

        private void darken(int x, int y, float f) {
            if (x < 0 || y < 0 || x >= 128 || y >= 128) {
                return;
            }
            int c = img.getPixel(x, y);
            if ((c >>> 24) == 0) {
                return;
            }
            px(x, y, shade(c & 0xFFFFFF, f));
        }

        private void torsoAnatomy() {
            int x = 20 * S;
            int y = 20 * S;
            float strength = a.build == 2 ? 0.82f : a.build == 1 ? 0.88f : 0.94f;
            for (int dx = 1; dx < 15; dx++) {
                darken(x + dx, y + 7 + (dx == 7 || dx == 8 ? 1 : 0), strength);
            }
            for (int dy = 8; dy < 21; dy++) {
                darken(x + 8, y + dy, strength);
            }
            if (a.build > 0) {
                for (int row = 11; row < 20; row += 3) {
                    for (int dx = 4; dx < 12; dx++) {
                        darken(x + dx, y + row, strength + 0.06f);
                    }
                }
            }
            for (int dx = 2; dx < 14; dx++) {
                darken(x + dx, y + 1, 1.06f);
            }
        }

        private void limbAnatomy(int u, int v) {
            int x = (u + 4) * S;
            int y = (v + 4) * S;
            for (int dy = 3; dy < 9; dy++) {
                for (int dx = 2; dx < 6; dx++) {
                    darken(x + dx, y + dy, 1.06f);
                }
            }
            for (int dx = 0; dx < 8; dx++) {
                darken(x + dx, y + 10, 0.9f);
            }
        }

        private void scalp() {
            if (bald) {
                hd(8, 0, 8, 8, skin, 1.12f, 1.02f, 0.04f);
                return;
            }
            int hair = hairColor;
            strands(8 * S, 0, 16, 16, hair, 1.0f);
            strands(24 * S, 8 * S, 16, 16, hair, 0.86f);
            strands(0, 8 * S, 16, 6, hair, 0.92f);
            strands(16 * S, 8 * S, 16, 6, hair, 0.92f);
            strands(0, 14 * S - 2, 4, 6, hair, 0.9f);
            strands(16 * S + 12, 14 * S - 2, 4, 6, hair, 0.9f);
            strands(8 * S, 8 * S, 16, 4, hair, 0.95f);
            for (int dx = 0; dx < 16; dx += 3) {
                int len = 2 + (noise(dx, 1) % 3);
                for (int dy = 0; dy < len; dy++) {
                    px(8 * S + dx, 8 * S + 4 + dy, shade(hair, 0.88f));
                }
            }
        }

        private void strands(int x, int y, int w, int h, int hair, float base) {
            for (int dx = 0; dx < w; dx++) {
                float strand = 0.82f + (noise(x + dx, 7) % 40) / 160.0f;
                for (int dy = 0; dy < h; dy++) {
                    float f = base * strand * (dy == 0 ? 1.15f : 1.0f) * (1.0f - dy * 0.012f);
                    if ((dx + dy / 3) % 5 == 0) {
                        f *= 0.85f;
                    }
                    px(x + dx, y + dy, shade(hair, f));
                }
            }
        }

        private void face() {
            int fx = 8 * S;
            int fy = 8 * S;
            int brow = race == Race.FRIEZA_RACE ? 0x3A2440 : race == Race.BIO_ANDROID ? 0x1E2A16 : bald ? shade(skin, 0.6f) : shade(hairColor, 0.9f);
            int browY = fy + 6;
            switch (a.eyebrowStyle) {
                case 1 -> {
                    line(fx + 2, browY - 1, fx + 6, browY + 1, brow);
                    line(fx + 9, browY + 1, fx + 13, browY - 1, brow);
                }
                case 2 -> {
                    line(fx + 2, browY, fx + 6, browY, brow);
                    line(fx + 2, browY + 1, fx + 6, browY + 1, brow);
                    line(fx + 9, browY, fx + 13, browY, brow);
                    line(fx + 9, browY + 1, fx + 13, browY + 1, brow);
                }
                case 3 -> {
                    line(fx + 3, browY, fx + 5, browY, brow);
                    line(fx + 10, browY, fx + 12, browY, brow);
                }
                default -> {
                    line(fx + 2, browY + 1, fx + 6, browY, brow);
                    line(fx + 9, browY, fx + 13, browY + 1, brow);
                }
            }
            eye(fx + 2, fy + 8, false);
            eye(fx + 9, fy + 8, true);
            int nose = shade(skin, 0.82f);
            px(fx + 7, fy + 10, shade(skin, 1.06f));
            px(fx + 8, fy + 10, shade(skin, 1.06f));
            px(fx + 7, fy + 11, nose);
            px(fx + 8, fy + 11, shade(skin, 0.75f));
            int mouth = shade(skin, 0.5f);
            int my = fy + 13;
            switch (a.expression) {
                case 1 -> line(fx + 6, my, fx + 9, my, mouth);
                case 2 -> {
                    for (int dx = 5; dx <= 10; dx++) {
                        px(fx + dx, my - 1, 0xF0F0F0);
                        px(fx + dx, my, 0x3A1414);
                    }
                    line(fx + 5, my + 1, fx + 10, my + 1, mouth);
                }
                case 3 -> {
                    px(fx + 5, my - 1, mouth);
                    line(fx + 6, my, fx + 9, my, mouth);
                    px(fx + 10, my - 1, mouth);
                }
                default -> line(fx + 6, my, fx + 9, my, mouth);
            }
            for (int dx = 2; dx < 14; dx++) {
                darken(fx + dx, fy + 15, 0.9f);
            }
            if (a.presentation == 1) {
                int lash = shade(skin, 0.22f);
                line(fx + 2, fy + 8, fx + 5, fy + 8, lash);
                line(fx + 10, fy + 8, fx + 13, fy + 8, lash);
                px(fx + 1, fy + 7, lash);
                px(fx + 14, fy + 7, lash);
                px(fx + 2, fy + 11, mix(skin, 0xFF7080, 0.35f));
                px(fx + 13, fy + 11, mix(skin, 0xFF7080, 0.35f));
                px(fx + 3, fy + 11, mix(skin, 0xFF7080, 0.2f));
                px(fx + 12, fy + 11, mix(skin, 0xFF7080, 0.2f));
                line(fx + 6, my, fx + 9, my, mix(skin, 0xC8384F, 0.7f));
                line(fx + 6, my + 1, fx + 9, my + 1, mix(skin, 0xC8384F, 0.45f));
            }
        }

        private void feminineBody() {
            for (int y = 52; y < 60; y++) {
                darken(40, y, 0.84f);
                darken(41, y, 0.9f);
                darken(54, y, 0.9f);
                darken(55, y, 0.84f);
            }
            for (int x = 42; x < 54; x++) {
                darken(x, 49, 0.86f);
            }
        }

        private void eye(int x, int y, boolean right) {
            int lid = shade(skin, 0.45f);
            switch (a.eyeStyle) {
                case 1 -> {
                    line(x, y + 1, x + 4, y + 1, lid);
                    px(x + (right ? 1 : 2), y + 2, eyeColor);
                    px(x + (right ? 2 : 3), y + 2, shade(eyeColor, 0.6f));
                    px(x + (right ? 0 : 1), y + 2, white);
                }
                case 2 -> {
                    line(x, y, x + 4, y, lid);
                    for (int dx = 0; dx < 5; dx++) {
                        px(x + dx, y + 1, dx == (right ? 1 : 3) || dx == 2 ? eyeColor : white);
                    }
                    px(x + 2, y + 2, shade(eyeColor, 0.5f));
                }
                case 3 -> {
                    line(x, y - 1, x + 4, y - 1, lid);
                    for (int dy = 0; dy < 3; dy++) {
                        for (int dx = 0; dx < 5; dx++) {
                            px(x + dx, y + dy, white);
                        }
                    }
                    for (int dy = 0; dy < 3; dy++) {
                        px(x + (right ? 1 : 2), y + dy, eyeColor);
                        px(x + (right ? 2 : 3), y + dy, shade(eyeColor, 0.7f));
                    }
                    px(x + (right ? 1 : 2), y, 0xFFFFFF);
                }
                default -> {
                    line(x, y, x + 4, y, lid);
                    for (int dx = 0; dx < 5; dx++) {
                        px(x + dx, y + 1, white);
                        px(x + dx, y + 2, whiteShade);
                    }
                    int ix = x + (right ? 1 : 2);
                    px(ix, y + 1, eyeColor);
                    px(ix + 1, y + 1, shade(eyeColor, 0.55f));
                    px(ix, y + 2, shade(eyeColor, 0.8f));
                    px(ix + 1, y + 2, eyeColor);
                    px(ix, y + 1, mix(eyeColor, 0xFFFFFF, 0.5f));
                }
            }
        }

        private void line(int x0, int y0, int x1, int y1, int rgb) {
            int steps = Math.max(Math.abs(x1 - x0), Math.abs(y1 - y0));
            for (int i = 0; i <= steps; i++) {
                float t = steps == 0 ? 0 : (float) i / steps;
                px(Math.round(x0 + (x1 - x0) * t), Math.round(y0 + (y1 - y0) * t), rgb);
            }
        }

        /** The three steam holes Majin Buu has on each side of his head. */
        private void steamHoles(int u, int v) {
            int[][] holes = {{2, 2}, {4, 1}, {6, 2}};
            for (int[] hole : holes) {
                int x = u * S + hole[0] * S;
                int y = v * S + hole[1] * S;
                px(x, y, shade(skin, 0.32f));
                px(x + 1, y, shade(skin, 0.4f));
                px(x, y + 1, shade(skin, 0.45f));
                px(x + 1, y + 1, shade(skin, 0.55f));
                px(x - 1, y, shade(skin, 1.08f));
                px(x + 2, y + 1, shade(skin, 1.08f));
            }
        }

        private void ears() {
            int ear = shade(skin, 0.85f);
            for (int[] side : new int[][]{{0, 8}, {16, 8}}) {
                int x = side[0] * S + 6;
                int y = side[1] * S + 6;
                for (int dy = 0; dy < 5; dy++) {
                    px(x, y + dy, ear);
                    px(x + 1, y + dy, shade(ear, dy == 0 || dy == 4 ? 0.8f : 1.05f));
                    px(x + 2, y + dy, ear);
                }
                px(x + 1, y + 2, shade(ear, 0.7f));
            }
        }

        private void raceDetails() {
            int feature = a.featureColor;
            switch (race) {
                case NAMEKIAN -> {
                    for (int[] limb : new int[][]{{40, 16}, {32, 48}}) {
                        int x = limb[0] * S;
                        int y = (limb[1] + 4) * S;
                        for (int band = 0; band < 3; band++) {
                            int by = y + 3 + band * 5;
                            rect(x, by, 32, 2, feature, 1.0f, 0.9f, 0.08f);
                        }
                    }
                    for (int dx = 2; dx < 14; dx += 4) {
                        px(8 * S + dx, 8 * S + 3, shade(skin, 0.8f));
                    }
                }
                case FRIEZA_RACE -> {
                    int plate = TenkaichiLayers.plateColor(skin);
                    int dark = 0x3A2440;
                    rect(8 * S, 0, 16, 16, plate, 1.08f, 0.96f, 0.03f);
                    rect(0, 8 * S, 16, 7, plate, 0.95f, 0.88f, 0.03f);
                    rect(16 * S, 8 * S, 16, 7, plate, 0.95f, 0.88f, 0.03f);
                    rect(24 * S, 8 * S, 16, 9, plate, 0.9f, 0.82f, 0.03f);
                    rect(8 * S, 8 * S, 16, 5, plate, 1.06f, 0.98f, 0.03f);
                    rect(8 * S + 6, 2, 4, 6, feature, 1.15f, 0.85f, 0.02f);
                    int fx = 8 * S;
                    int fy = 8 * S;
                    line(fx, fy + 5, fx + 15, fy + 5, shade(plate, 0.7f));
                    line(fx + 2, fy + 10, fx + 1, fy + 12, dark);
                    line(fx + 13, fy + 10, fx + 14, fy + 12, dark);
                    line(fx + 3, fy + 11, fx + 5, fy + 11, shade(skin, 0.75f));
                    line(fx + 10, fy + 11, fx + 12, fy + 11, shade(skin, 0.75f));
                    line(fx + 6, fy + 13, fx + 9, fy + 13, 0x5A2A68);
                    line(fx + 7, fy + 14, fx + 8, fy + 14, shade(skin, 0.8f));
                    px(0 * S + 7, 8 * S + 10, dark);
                    px(16 * S + 8, 8 * S + 10, dark);
                    if (a.outfit == Appearance.OUTFIT_NATURAL) {
                        rect(20 * S, 20 * S, 16, 12, plate, 1.08f, 0.95f, 0.03f);
                        rect(32 * S, 20 * S, 16, 12, plate, 0.85f, 0.78f, 0.03f);
                        line(20 * S + 7, 20 * S + 2, 20 * S + 7, 20 * S + 11, shade(plate, 0.8f));
                        line(20 * S, 20 * S + 12, 20 * S + 15, 20 * S + 12, shade(plate, 0.6f));
                        rect(20 * S + 6, 20 * S + 3, 4, 3, feature, 1.15f, 0.85f, 0.02f);
                        for (int row = 15; row < 23; row += 3) {
                            line(20 * S + 4, 20 * S + row, 20 * S + 11, 20 * S + row, shade(skin, 0.86f));
                        }
                        for (int[] limb : new int[][]{{40, 16}, {32, 48}}) {
                            rect((limb[0] + 4) * S, limb[1] * S, 8, 8, plate, 1.1f, 1.0f, 0.03f);
                            rect(limb[0] * S, (limb[1] + 4) * S, 32, 5, plate, 1.0f, 0.9f, 0.03f);
                            line(limb[0] * S, (limb[1] + 4) * S + 5, limb[0] * S + 31, (limb[1] + 4) * S + 5, shade(plate, 0.65f));
                        }
                        for (int[] leg : new int[][]{{0, 16}, {16, 48}}) {
                            rect((leg[0] + 4) * S, (leg[1] + 4) * S + 11, 8, 10, plate, 1.06f, 0.94f, 0.03f);
                            rect((leg[0] + 4) * S + 2, (leg[1] + 4) * S + 8, 4, 3, feature, 1.15f, 0.85f, 0.02f);
                            line(leg[0] * S, (leg[1] + 4) * S + 21, leg[0] * S + 31, (leg[1] + 4) * S + 21, shade(skin, 0.7f));
                        }
                    }
                }
                case MAJIN -> {
                    for (int dx = 0; dx < 16; dx++) {
                        px(8 * S + dx, 8 * S + 1, shade(skin, 0.85f));
                    }
                    steamHoles(0, 8);
                    steamHoles(16, 8);
                    for (int dy = 12; dy < 22; dy += 3) {
                        for (int dx = 2; dx < 14; dx++) {
                            darken(20 * S + dx, 20 * S + dy, 0.93f);
                        }
                    }
                }
                case BIO_ANDROID -> {
                    int fx = 8 * S;
                    int fy = 8 * S;
                    int mark = 0x7A2E8C;
                    line(fx + 3, fy + 11, fx + 2, fy + 14, mark);
                    line(fx + 4, fy + 11, fx + 3, fy + 14, mark);
                    line(fx + 12, fy + 11, fx + 13, fy + 14, mark);
                    line(fx + 11, fy + 11, fx + 12, fy + 14, mark);
                    line(fx + 6, fy + 13, fx + 9, fy + 13, 0x1A1C16);
                    line(fx + 7, fy + 14, fx + 8, fy + 14, 0x1A1C16);
                    px(fx + 7, fy + 15, 0x2A2C22);
                    int shell = TenkaichiLayers.carapaceColor(feature);
                    rect(8 * S, 0, 16, 16, shell, 1.1f, 0.9f, 0.04f);
                    rect(fx, fy, 16, 4, shell, 1.0f, 0.85f, 0.04f);
                    if (a.outfit == Appearance.OUTFIT_NATURAL) {
                        rect(20 * S, 20 * S, 16, 11, shell, 1.15f, 0.9f, 0.04f);
                        line(20 * S + 7, 20 * S + 1, 20 * S + 7, 20 * S + 10, shade(shell, 1.6f));
                        for (int row = 0; row < 3; row++) {
                            int ry = 20 * S + 12 + row * 4;
                            rect(20 * S + 2 + row, ry, 12 - row * 2, 3, shell, 1.1f, 0.85f, 0.04f);
                        }
                        rect(32 * S, 20 * S, 16, 8, shell, 0.9f, 0.8f, 0.04f);
                    }
                }
                case ANDROID -> {
                    px(0 * S + 7, 8 * S + 7, feature);
                    px(16 * S + 8, 8 * S + 7, feature);
                    line(20 * S, 20 * S + 22, 28 * S - 1, 20 * S + 22, shade(skin, 0.85f));
                }
                case DEMON -> {
                    line(8 * S + 2, 8 * S + 11, 8 * S + 4, 8 * S + 13, feature);
                    line(8 * S + 13, 8 * S + 11, 8 * S + 11, 8 * S + 13, feature);
                }
                case KAIOSHIN -> {
                    px(0 * S + 7, 8 * S + 12, feature);
                    px(16 * S + 8, 8 * S + 12, feature);
                }
                default -> {
                }
            }
        }

        private void overlayClear() {
            for (int y = 0; y < 128; y++) {
                for (int x = 0; x < 128; x++) {
                    boolean overlay = (y >= 64 && y < 96 && x < 112) || (y >= 96 && (x < 32 || x >= 96)) || (y < 32 && x >= 64);
                    if (overlay) {
                        clear(x, y);
                    }
                }
            }
        }

        private void faceRows(int u, int v, int w, int h, int d, int from, int to, int rgb, float grain) {
            for (int row = from * S; row < to * S && row < h * S; row++) {
                float t = (float) row / (h * S);
                int y = (v + d) * S + row;
                rect(u * S, y, d * S, 1, rgb, 0.86f - t * 0.08f, 0.86f, grain);
                rect((u + d) * S, y, w * S, 1, rgb, 1.02f - t * 0.1f, 1.0f, grain);
                rect((u + d + w) * S, y, d * S, 1, rgb, 0.86f - t * 0.08f, 0.86f, grain);
                rect((u + d + w + d) * S, y, w * S, 1, rgb, 0.8f - t * 0.08f, 0.8f, grain);
            }
        }

        private void topBottom(int u, int v, int w, int d, int rgb) {
            hd(u + d, v, w, d, rgb, 1.05f, 1.0f, 0.05f);
            hd(u + d + w, v, w, d, rgb, 0.65f, 0.6f, 0.05f);
        }

        private void folds(int u, int v, int w, int h, int d, int rgb) {
            int x = (u + d) * S;
            int y = (v + d) * S;
            for (int i = 0; i < w * S; i += 5) {
                int fx = x + i + (noise(i, v) % 3);
                for (int dy = 2; dy < h * S - 2; dy += 1) {
                    if (noise(fx, y + dy) > 150) {
                        darken(fx, y + dy, 0.9f);
                    }
                }
            }
        }

        private void gi() {
            overlayClear();
            int main = a.outfitPrimary;
            int accent = a.outfitSecondary;
            faceRows(16, 16, 8, 12, 4, 0, 12, accent, 0.06f);
            faceRows(16, 32, 8, 12, 4, 0, 12, main, 0.08f);
            topBottom(16, 32, 8, 4, main);
            int cx = 20 * S;
            int cy = 36 * S;
            for (int dy = 0; dy < 10; dy++) {
                int half = Math.max(0, 5 - dy / 2);
                for (int dx = 8 - half; dx < 8 + half; dx++) {
                    clear(cx + dx, cy + dy);
                }
            }
            for (int dy = 0; dy < 10; dy++) {
                int half = Math.max(0, 5 - dy / 2);
                darken(cx + 8 - half - 1, cy + dy, 0.75f);
                darken(cx + 8 + half, cy + dy, 0.75f);
            }
            folds(16, 32, 8, 12, 4, main);
            faceRows(16, 32, 8, 12, 4, 9, 11, accent, 0.05f);
            for (int dy = 18; dy < 26; dy++) {
                px(cx + 10, cy + dy, shade(accent, 0.8f));
                px(cx + 11, cy + dy + 1, shade(accent, 0.7f));
            }
            faceRows(0, 32, 4, 12, 4, 0, 8, main, 0.08f);
            faceRows(0, 48, 4, 12, 4, 0, 8, main, 0.08f);
            topBottom(0, 32, 4, 4, main);
            topBottom(0, 48, 4, 4, main);
            folds(0, 32, 4, 12, 4, main);
            folds(0, 48, 4, 12, 4, main);
            faceRows(0, 32, 4, 12, 4, 8, 12, accent, 0.06f);
            faceRows(0, 48, 4, 12, 4, 8, 12, accent, 0.06f);
            hd(8, 32, 4, 4, shade(accent, 0.55f), 1.0f, 0.9f, 0.05f);
            hd(8, 48, 4, 4, shade(accent, 0.55f), 1.0f, 0.9f, 0.05f);
            faceRows(0, 16, 4, 12, 4, 0, 12, main, 0.08f);
            faceRows(16, 48, 4, 12, 4, 0, 12, main, 0.08f);
            faceRows(0, 16, 4, 12, 4, 8, 12, accent, 0.06f);
            faceRows(16, 48, 4, 12, 4, 8, 12, accent, 0.06f);
            faceRows(40, 32, 4, 12, 4, 7, 10, accent, 0.05f);
            faceRows(48, 48, 4, 12, 4, 7, 10, accent, 0.05f);
            faceRows(40, 16, 4, 12, 4, 7, 10, accent, 0.05f);
            faceRows(32, 48, 4, 12, 4, 7, 10, accent, 0.05f);
            faceRows(40, 16, 4, 12, 4, 0, 4, main, 0.08f);
            faceRows(32, 48, 4, 12, 4, 0, 4, main, 0.08f);
            faceRows(40, 32, 4, 12, 4, 0, 4, main, 0.08f);
            faceRows(48, 48, 4, 12, 4, 0, 4, main, 0.08f);
            faceRows(40, 32, 4, 12, 4, 3, 4, shade(main, 0.75f), 0.04f);
            faceRows(48, 48, 4, 12, 4, 3, 4, shade(main, 0.75f), 0.04f);
            coverTops(main, main);
        }

        private void coverTops(int shirt, int pants) {
            hd(44, 16, 4, 4, shirt, 1.05f, 0.95f, 0.05f);
            hd(36, 48, 4, 4, shirt, 1.05f, 0.95f, 0.05f);
            hd(44, 32, 4, 4, shirt, 1.08f, 1.0f, 0.05f);
            hd(52, 48, 4, 4, shirt, 1.08f, 1.0f, 0.05f);
            hd(4, 16, 4, 4, pants, 0.8f, 0.75f, 0.05f);
            hd(20, 48, 4, 4, pants, 0.8f, 0.75f, 0.05f);
            hd(4, 32, 4, 4, pants, 0.85f, 0.8f, 0.05f);
            hd(4, 48, 4, 4, pants, 0.85f, 0.8f, 0.05f);
            hd(28, 16, 8, 4, pants, 0.7f, 0.65f, 0.05f);
            hd(28, 32, 8, 4, pants, 0.72f, 0.66f, 0.05f);
        }

        private void armor() {
            overlayClear();
            int suit = a.outfitPrimary;
            int plate = 0xEDE7D6;
            int trim = a.outfitSecondary;
            faceRows(16, 16, 8, 12, 4, 0, 12, suit, 0.04f);
            faceRows(40, 16, 4, 12, 4, 0, 12, suit, 0.04f);
            faceRows(32, 48, 4, 12, 4, 0, 12, suit, 0.04f);
            faceRows(0, 16, 4, 12, 4, 0, 12, suit, 0.04f);
            faceRows(16, 48, 4, 12, 4, 0, 12, suit, 0.04f);
            for (int[] seam : new int[][]{{40, 16}, {32, 48}, {0, 16}, {16, 48}}) {
                for (int dy = 0; dy < 24; dy++) {
                    darken((seam[0] + 4) * S + 3, (seam[1] + 4) * S + dy, 0.8f);
                }
            }
            faceRows(16, 32, 8, 12, 4, 0, 8, plate, 0.03f);
            topBottom(16, 32, 8, 4, plate);
            faceRows(16, 32, 8, 12, 4, 7, 9, trim, 0.03f);
            int cx = 20 * S;
            int cy = 36 * S;
            for (int dx = 0; dx < 16; dx++) {
                px(cx + dx, cy, shade(plate, 1.1f));
                darken(cx + dx, cy + 13, 0.8f);
            }
            for (int dy = 1; dy < 13; dy++) {
                px(cx + 7, cy + dy, shade(plate, 0.92f));
                px(cx + 8, cy + dy, shade(plate, 1.08f));
            }
            rect(cx + 1, cy + 1, 3, 5, trim, 1.0f, 0.9f, 0.03f);
            rect(cx + 12, cy + 1, 3, 5, trim, 1.0f, 0.9f, 0.03f);
            faceRows(40, 32, 4, 12, 4, 0, 2, trim, 0.03f);
            faceRows(48, 48, 4, 12, 4, 0, 2, trim, 0.03f);
            topBottom(40, 32, 4, 4, plate);
            topBottom(48, 48, 4, 4, plate);
            faceRows(40, 32, 4, 12, 4, 8, 12, plate, 0.03f);
            faceRows(48, 48, 4, 12, 4, 8, 12, plate, 0.03f);
            faceRows(0, 32, 4, 12, 4, 6, 12, plate, 0.03f);
            faceRows(0, 48, 4, 12, 4, 6, 12, plate, 0.03f);
            faceRows(0, 32, 4, 12, 4, 6, 7, trim, 0.03f);
            faceRows(0, 48, 4, 12, 4, 6, 7, trim, 0.03f);
            hd(8, 32, 4, 4, shade(plate, 0.6f), 1.0f, 0.9f, 0.03f);
            hd(8, 48, 4, 4, shade(plate, 0.6f), 1.0f, 0.9f, 0.03f);
            coverTops(suit, suit);
            hd(44, 32, 4, 4, plate, 1.1f, 1.0f, 0.03f);
            hd(52, 48, 4, 4, plate, 1.1f, 1.0f, 0.03f);
        }

        private void bioBase() {
            int feature = a.featureColor;
            java.util.Random rng = new java.util.Random(race.ordinal() * 31L + skin);
            int[][] regions = {{0, 16, 64, 32}, {32, 48, 32, 16}, {16, 48, 32, 16}, {0, 0, 64, 16}, {64, 0, 64, 16}};
            for (int[] region : regions) {
                int count = region[2] * region[3] / 26;
                for (int i = 0; i < count; i++) {
                    int cx = region[0] * S + rng.nextInt(region[2] * S);
                    int cy = region[1] * S + rng.nextInt(region[3] * S);
                    int r = 2 + rng.nextInt(3);
                    for (int dy = -r; dy <= r; dy++) {
                        for (int dx = -r; dx <= r; dx++) {
                            if (dx * dx + dy * dy <= r * r + 1) {
                                int x = cx + dx;
                                int y = cy + dy;
                                if (x >= 0 && y >= 0 && x < 128 && y < 128 && (img.getPixel(x, y) >>> 24) != 0) {
                                    px(x, y, shade(feature, 0.9f + rng.nextFloat() * 0.3f));
                                }
                            }
                        }
                    }
                }
            }
            int face = mix(skin, 0xE8DCD4, 0.78f);
            rect(8 * S, 8 * S + 4, 16, 12, face, 1.05f, 0.92f, 0.03f);
            rect(0 * S + 10, 8 * S + 8, 6, 6, face, 0.9f, 0.85f, 0.03f);
            rect(16 * S, 8 * S + 8, 6, 6, face, 0.9f, 0.85f, 0.03f);
        }

        private void natural() {
            overlayClear();
            if (race == Race.FRIEZA_RACE || race == Race.BIO_ANDROID) {
                return;
            }
            int shorts = a.outfitPrimary;
            int belt = a.outfitSecondary;
            faceRows(16, 16, 8, 12, 4, 9, 12, shorts, 0.06f);
            faceRows(16, 16, 8, 12, 4, 9, 10, belt, 0.04f);
            faceRows(0, 16, 4, 12, 4, 0, 5, shorts, 0.06f);
            faceRows(16, 48, 4, 12, 4, 0, 5, shorts, 0.06f);
            hd(4, 16, 4, 4, shorts, 0.8f, 0.75f, 0.05f);
            hd(20, 48, 4, 4, shorts, 0.8f, 0.75f, 0.05f);
            hd(28, 16, 8, 4, shorts, 0.7f, 0.65f, 0.05f);
        }

        private static final int[][] GLYPHS = {
                {0b101, 0b110, 0b100, 0b110, 0b101},
                {0b010, 0b101, 0b111, 0b101, 0b101},
                {0b111, 0b100, 0b110, 0b100, 0b111},
                {0b101, 0b101, 0b111, 0b101, 0b101},
                {0b110, 0b101, 0b110, 0b101, 0b110},
                {0b101, 0b111, 0b111, 0b111, 0b101},
                {0b110, 0b101, 0b110, 0b101, 0b101}
        };

        private void glyph(int x, int y, int index) {
            int[] rows = GLYPHS[index % GLYPHS.length];
            for (int r = 0; r < 5; r++) {
                for (int c = 0; c < 3; c++) {
                    if ((rows[r] >> (2 - c) & 1) != 0) {
                        px(x + c, y + r, 0x141414);
                    }
                }
            }
        }

        private void triangle(int x, int y, boolean down) {
            int purple = 0x7A4FC8;
            for (int r = 0; r < 3; r++) {
                int row = down ? 2 - r : r;
                for (int c = 0; c <= row * 2; c++) {
                    px(x - row + c, y + r, c == 0 ? shade(purple, 0.8f) : purple);
                }
            }
        }

        private void roshi() {
            overlayClear();
            int shirt = 0xF2A030;
            int shorts = 0xF2F0EA;
            int strap = 0x3E9E3A;
            int sole = 0x7A4A22;
            faceRows(16, 16, 8, 12, 4, 0, 12, shirt, 0.05f);
            faceRows(40, 16, 4, 12, 4, 0, 5, shirt, 0.05f);
            faceRows(32, 48, 4, 12, 4, 0, 5, shirt, 0.05f);
            faceRows(40, 16, 4, 12, 4, 4, 5, shade(shirt, 0.82f), 0.03f);
            faceRows(32, 48, 4, 12, 4, 4, 5, shade(shirt, 0.82f), 0.03f);
            int fx = 20 * S;
            int fy = 20 * S;
            for (int dy = 0; dy < 8; dy++) {
                int half = (8 - dy) / 2;
                for (int dx = 8 - half; dx < 8 + half; dx++) {
                    clear(fx + dx, fy + dy);
                }
                darken(fx + 7 - half, fy + dy, 0.72f);
                darken(fx + 8 + half, fy + dy, 0.72f);
            }
            for (int dy = 8; dy < 24; dy++) {
                darken(fx + 8, fy + dy, 0.8f);
            }
            for (int dx = 0; dx < 16; dx++) {
                darken(fx + dx, fy + 23, 0.82f);
                darken(fx + 16 + 8 + dx, fy + 23, 0.82f);
            }
            glyph(fx + 2, fy + 8, 0);
            glyph(fx + 11, fy + 10, 1);
            glyph(fx + 3, fy + 16, 3);
            glyph(fx + 11, fy + 17, 4);
            triangle(fx + 5, fy + 3, false);
            triangle(fx + 13, fy + 2, true);
            triangle(fx + 7, fy + 20, false);
            triangle(fx + 2, fy + 21, true);
            int bx = 32 * S;
            glyph(bx + 2, fy + 3, 2);
            glyph(bx + 7, fy + 6, 5);
            glyph(bx + 11, fy + 3, 6);
            glyph(bx + 4, fy + 13, 3);
            glyph(bx + 10, fy + 15, 1);
            triangle(bx + 8, fy + 11, true);
            triangle(bx + 3, fy + 20, false);
            triangle(bx + 13, fy + 21, true);
            triangle(16 * S + 4, fy + 6, false);
            triangle(28 * S + 4, fy + 14, true);
            triangle(44 * S + 3, 20 * S + 2, false);
            triangle(36 * S + 4, 52 * S + 3, true);
            faceRows(0, 16, 4, 12, 4, 0, 6, shorts, 0.1f);
            faceRows(16, 48, 4, 12, 4, 0, 6, shorts, 0.1f);
            faceRows(0, 16, 4, 12, 4, 5, 6, shade(shorts, 0.86f), 0.04f);
            faceRows(16, 48, 4, 12, 4, 5, 6, shade(shorts, 0.86f), 0.04f);
            folds(0, 16, 4, 6, 4, shorts);
            folds(16, 48, 4, 6, 4, shorts);
            for (int[] leg : new int[][]{{0, 16}, {16, 48}}) {
                int ly = (leg[1] + 4) * S;
                for (int face = 0; face < 4; face++) {
                    int lx = (leg[0] + face * 4) * S;
                    for (int dx = 0; dx < 8; dx++) {
                        px(lx + dx, ly + 23, sole);
                    }
                    if (face == 1) {
                        for (int dx = 1; dx < 7; dx++) {
                            px(lx + dx, ly + 21, strap);
                        }
                        px(lx + 3, ly + 20, strap);
                        px(lx + 4, ly + 20, strap);
                        px(lx + 3, ly + 22, shade(strap, 0.8f));
                        px(lx + 4, ly + 22, shade(strap, 0.8f));
                    } else if (face != 3) {
                        for (int dx = 0; dx < 8; dx++) {
                            if (dx < 3) {
                                px(lx + (face == 0 ? 8 - 1 - dx : dx), ly + 22, strap);
                            }
                        }
                    }
                }
                hd(leg[0] + 8, leg[1], 4, 4, sole, 0.9f, 0.8f, 0.05f);
            }
            coverTops(shirt, shorts);
            hd(4, 32, 4, 4, shorts, 0.85f, 0.8f, 0.05f);
            hd(4, 48, 4, 4, shorts, 0.85f, 0.8f, 0.05f);
        }

        private void casual() {
            overlayClear();
            int shirt = a.outfitPrimary;
            int pants = a.outfitSecondary;
            faceRows(16, 16, 8, 12, 4, 0, 12, shirt, 0.05f);
            faceRows(40, 16, 4, 12, 4, 0, 4, shirt, 0.05f);
            faceRows(32, 48, 4, 12, 4, 0, 4, shirt, 0.05f);
            faceRows(0, 16, 4, 12, 4, 0, 11, pants, 0.12f);
            faceRows(16, 48, 4, 12, 4, 0, 11, pants, 0.12f);
            faceRows(0, 16, 4, 12, 4, 11, 12, 0x2A2A2A, 0.03f);
            faceRows(16, 48, 4, 12, 4, 11, 12, 0x2A2A2A, 0.03f);
            hd(8, 16, 4, 4, 0x2A2A2A, 1.0f, 0.9f, 0.03f);
            hd(8, 48, 4, 4, 0x2A2A2A, 1.0f, 0.9f, 0.03f);
            int jacket = shade(pants, 0.7f);
            faceRows(16, 32, 8, 12, 4, 0, 12, jacket, 0.06f);
            topBottom(16, 32, 8, 4, jacket);
            int cx = 20 * S;
            int cy = 36 * S;
            for (int dy = 0; dy < 24; dy++) {
                for (int dx = 5; dx < 11; dx++) {
                    clear(cx + dx, cy + dy);
                }
                darken(cx + 4, cy + dy, 0.75f);
                darken(cx + 11, cy + dy, 0.75f);
            }
            faceRows(40, 32, 4, 12, 4, 0, 6, jacket, 0.06f);
            faceRows(48, 48, 4, 12, 4, 0, 6, jacket, 0.06f);
            folds(0, 16, 4, 12, 4, pants);
            folds(16, 48, 4, 12, 4, pants);
            coverTops(shirt, pants);
            hd(44, 32, 4, 4, jacket, 1.05f, 0.95f, 0.05f);
            hd(52, 48, 4, 4, jacket, 1.05f, 0.95f, 0.05f);
        }
    }
}
