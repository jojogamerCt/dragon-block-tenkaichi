package com.dbtenkaichi.client.ui;

import com.dbtenkaichi.Tenkaichi;
import com.dbtenkaichi.client.ClientState;
import com.dbtenkaichi.registry.ModSounds;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvent;

public final class TenkaichiUi {
    public static final Identifier PANEL = Tenkaichi.id("panel");
    public static final Identifier CARD = Tenkaichi.id("card");
    public static final Identifier BUTTON = Tenkaichi.id("button");
    public static final Identifier BUTTON_HOVER = Tenkaichi.id("button_hover");
    public static final Identifier BUTTON_DISABLED = Tenkaichi.id("button_disabled");
    public static final Identifier BUTTON_SELECTED = Tenkaichi.id("button_selected");
    public static final Identifier SLOT = Tenkaichi.id("slot");
    public static final Identifier SLOT_SELECTED = Tenkaichi.id("slot_selected");
    public static final Identifier TAB = Tenkaichi.id("tab");
    public static final Identifier TAB_SELECTED = Tenkaichi.id("tab_selected");
    public static final Identifier CHIP = Tenkaichi.id("chip");
    public static final Identifier PORTRAIT_FRAME = Tenkaichi.id("portrait_frame");
    public static final Identifier EMBLEM = Tenkaichi.id("emblem");
    public static final Identifier HEART = Identifier.withDefaultNamespace("hud/heart/full");
    public static final Identifier FOOD = Identifier.withDefaultNamespace("hud/food_full");
    public static final Identifier ARMOR = Identifier.withDefaultNamespace("hud/armor_full");

    public static final int INK = 0xFF180C06;
    public static final int GOLD = 0xFFFFB12B;
    public static final int GOLD_LIGHT = 0xFFFFECA0;
    public static final int ORANGE = 0xFFF5962C;
    public static final int BLUE = 0xFF3E68E4;
    public static final int NAVY = 0xFF141C40;
    public static final int TEXT = 0xFFF4F1EA;
    public static final int MUTED = 0xFFA8B2D8;
    public static final int GOOD = 0xFF7CFF9A;
    public static final int DANGER = 0xFFFF6A5A;
    public static final int STAR = 0xFFD6221A;

    private static final int[][] CLOUDS = {
            {0, 0, 5, 1}, {1, -1, 3, 1}, {-1, 1, 7, 1}, {2, -2, 1, 1},
            {0, 0, 4, 1}, {3, 0, 3, 1}, {1, 1, 6, 1}, {4, -1, 2, 1},
            {0, 0, 6, 1}, {1, -1, 2, 1}, {4, -1, 1, 1}, {-2, 1, 9, 1}, {0, 2, 5, 1}
    };
    private static final int[] CLOUD_START = {0, 4, 8, 13};

    private TenkaichiUi() {
    }

    public static Identifier ball(int stars) {
        return Tenkaichi.id("ball/" + Math.max(1, Math.min(7, stars)));
    }

    public static Identifier icon(String name) {
        return Tenkaichi.id("icon/" + name);
    }

    public static void panel(GuiGraphicsExtractor g, int x, int y, int w, int h) {
        g.blitSprite(RenderPipelines.GUI_TEXTURED, PANEL, x, y, w, h);
    }

    public static void card(GuiGraphicsExtractor g, int x, int y, int w, int h) {
        g.blitSprite(RenderPipelines.GUI_TEXTURED, CARD, x, y, w, h);
    }

    public static void slot(GuiGraphicsExtractor g, int x, int y, int size, boolean selected) {
        g.blitSprite(RenderPipelines.GUI_TEXTURED, selected ? SLOT_SELECTED : SLOT, x, y, size, size);
    }

    public static void sprite(GuiGraphicsExtractor g, Identifier sprite, int x, int y, int w, int h) {
        g.blitSprite(RenderPipelines.GUI_TEXTURED, sprite, x, y, w, h);
    }

    public static void sprite(GuiGraphicsExtractor g, Identifier sprite, int x, int y, int w, int h, int argb) {
        g.blitSprite(RenderPipelines.GUI_TEXTURED, sprite, x, y, w, h, argb);
    }

    public static Identifier skillIcon(String techniqueId) {
        int colon = techniqueId.indexOf(':');
        String path = colon >= 0 ? techniqueId.substring(colon + 1) : techniqueId;
        return Tenkaichi.id("skill/" + path);
    }

    public static void playUi(SoundEvent sound, float pitch, float volume) {
        Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(sound, pitch, volume));
    }

    public static void blip() {
        playUi(ModSounds.UI_TAB.get(), 1.0f, 0.55f);
    }

    public static void background(GuiGraphicsExtractor g, int width, int height) {
        float time = ClientState.clientTicks + com.dbtenkaichi.client.anim.PoseApplier.partial();
        int horizon = height * 3 / 5;
        g.fillGradient(0, 0, width, horizon, 0xFF0A1238, 0xFF294496);
        g.fillGradient(0, horizon, width, height, 0xFF335AB0, 0xFFE08A3A);
        int band = Math.max(2, height / 90);
        for (int i = 0; i < 4; i++) {
            int y = horizon + (height - horizon) * (i + 1) / 5;
            g.fill(0, y, width, y + band, 0x14FFE0A0);
        }
        stars(g, width, horizon, time);
        int size = Math.min(width, height) * 3 / 4;
        g.pose().pushMatrix();
        g.pose().translate(width * 0.5f, height * 0.46f);
        g.pose().rotate(time * 0.0025f);
        sprite(g, EMBLEM, -size / 2, -size / 2, size, size, 0x1CFFFFFF);
        g.pose().popMatrix();
        clouds(g, width, height, time);
        g.fillGradient(0, 0, width, 28, 0x70000000, 0x00000000);
        g.fillGradient(0, height - 28, width, height, 0x00000000, 0x50000000);
    }

    private static void stars(GuiGraphicsExtractor g, int width, int height, float time) {
        for (int i = 0; i < 46; i++) {
            int x = (i * 7919) % Math.max(1, width);
            int y = (i * 3571) % Math.max(1, height * 2 / 3);
            float twinkle = 0.5f + 0.5f * (float) Math.sin(time * 0.07f + i * 1.7f);
            int a = Math.round(40 + 120 * twinkle);
            g.fill(x, y, x + 1, y + 1, a << 24 | 0xFFF6D0);
            if (i % 7 == 0 && twinkle > 0.8f) {
                g.fill(x - 1, y, x + 2, y + 1, (a / 2) << 24 | 0xFFF6D0);
                g.fill(x, y - 1, x + 1, y + 2, (a / 2) << 24 | 0xFFF6D0);
            }
        }
    }

    private static void clouds(GuiGraphicsExtractor g, int width, int height, float time) {
        int cell = Math.max(4, height / 40);
        for (int i = 0; i < 9; i++) {
            int shape = i % 4;
            int span = (CLOUDS.length - CLOUD_START[shape]) * cell + cell * 10;
            float speed = 0.06f + (i % 3) * 0.035f;
            int x = (int) ((i * 173 + time * speed) % (width + span)) - span / 2;
            int y = height * 58 / 100 + (i * 37) % Math.max(1, height / 3);
            int alpha = 0x48 + (i % 3) * 0x16;
            int end = shape == 3 ? CLOUDS.length : CLOUD_START[shape + 1];
            for (int k = CLOUD_START[shape]; k < end; k++) {
                int[] r = CLOUDS[k];
                int x0 = x + r[0] * cell * 2;
                int y0 = y + r[1] * cell;
                g.fill(x0, y0, x0 + r[2] * cell * 2, y0 + r[3] * cell, alpha << 24 | 0xF4F6FF);
                g.fill(x0, y0 + r[3] * cell - 1, x0 + r[2] * cell * 2, y0 + r[3] * cell, (alpha / 2) << 24 | 0x8AA0D8);
            }
        }
    }

    public static void header(GuiGraphicsExtractor g, Font font, Component title, int cx, int y) {
        header(g, font, title, cx, y, Integer.MAX_VALUE, 2.0f);
    }

    public static void header(GuiGraphicsExtractor g, Font font, Component title, int cx, int y, int maxWidth, float maxScale) {
        float scale = Math.max(1.0f, Math.min(maxScale, (maxWidth - 40) / (float) Math.max(1, font.width(title))));
        int w = Math.round(font.width(title) * scale) + 40;
        int x0 = cx - w / 2;
        int top = y - 5;
        int bottom = y + Math.round(8 * scale) + 5;
        g.fill(x0 - 1, top - 1, x0 + w + 1, bottom + 1, INK);
        g.fillGradient(x0, top, x0 + w, bottom, 0xFF2A48C0, 0xFF16286E);
        g.fill(x0, top, x0 + w, top + 1, 0xFF8AB0FF);
        g.fill(x0, bottom - 2, x0 + w, bottom, 0xFF0C1640);
        g.fill(x0 + 2, top + 2, x0 + w - 2, top + 3, GOLD);
        g.fill(x0 + 2, bottom - 4, x0 + w - 2, bottom - 3, GOLD);
        int mid = (top + bottom) / 2;
        star(g, x0 + 8, mid);
        star(g, x0 + w - 9, mid);
        logo(g, font, title, cx, y, scale, true);
    }

    public static void star(GuiGraphicsExtractor g, int cx, int cy) {
        g.fill(cx - 2, cy - 2, cx + 3, cy + 3, INK);
        g.fill(cx - 1, cy - 2, cx + 2, cy + 3, STAR);
        g.fill(cx - 2, cy - 1, cx + 3, cy + 2, STAR);
        g.fill(cx, cy - 3, cx + 1, cy + 4, STAR);
        g.fill(cx - 3, cy, cx + 4, cy + 1, STAR);
        g.fill(cx, cy, cx + 1, cy + 1, 0xFFFF8A70);
    }

    public static void logo(GuiGraphicsExtractor g, Font font, Component text, int x, int y, float scale, boolean centered) {
        int w = font.width(text);
        g.pose().pushMatrix();
        g.pose().translate(x, y);
        g.pose().scale(scale, scale);
        int ox = centered ? -w / 2 : 0;
        for (int dx = -1; dx <= 1; dx++) {
            for (int dy = -1; dy <= 2; dy++) {
                if (dx != 0 || dy != 0) {
                    g.text(font, text.getString(), ox + dx, dy, 0xFF5A0E06, false);
                }
            }
        }
        g.text(font, text.getString(), ox, 0, 0xFFFF7A1A, false);
        g.enableScissor(ox - 2, -1, ox + w + 2, 4);
        g.text(font, text.getString(), ox, 0, 0xFFFFE34A, false);
        g.disableScissor();
        g.enableScissor(ox - 2, -1, ox + w + 2, 2);
        g.text(font, text.getString(), ox, 0, 0xFFFFFAD0, false);
        g.disableScissor();
        g.pose().popMatrix();
    }

    public static void divider(GuiGraphicsExtractor g, int x, int y, int w) {
        g.fill(x, y, x + w, y + 1, GOLD);
        g.fill(x, y + 1, x + w, y + 2, 0xFF5A3208);
        g.fill(x, y - 1, x + 3, y + 3, STAR);
    }

    public static void section(GuiGraphicsExtractor g, Font font, Component title, int x, int y, int w) {
        star(g, x + 3, y + 3);
        g.text(font, title, x + 9, y, GOLD_LIGHT, true);
        int lx = x + 13 + font.width(title);
        if (lx < x + w) {
            g.fill(lx, y + 4, x + w, y + 5, 0x90FFB12B);
            g.fill(lx, y + 5, x + w, y + 6, 0x605A3208);
        }
    }

    public static void chip(GuiGraphicsExtractor g, Font font, String label, String value, int x, int y, int color) {
        int w = font.width(label) + font.width(value) + 14;
        sprite(g, CHIP, x, y, w, 13);
        g.text(font, label, x + 5, y + 3, color, true);
        g.text(font, value, x + 9 + font.width(label), y + 3, TEXT, true);
    }

    public static int chipWidth(Font font, String label, String value) {
        return font.width(label) + font.width(value) + 14;
    }

    public static void pedestal(GuiGraphicsExtractor g, int cx, int cy, int w, int color, float time) {
        int h = Math.max(6, w / 5);
        float pulse = 0.5f + 0.5f * (float) Math.sin(time * 0.12f);
        for (int ring = 3; ring >= 1; ring--) {
            int rw = w / 2 + ring * 6 + Math.round(pulse * 3);
            int rh = h / 2 + ring * 2;
            int a = Math.round((0x16 + 0x10 * (3 - ring)) * (0.7f + pulse * 0.3f));
            ellipse(g, cx, cy, rw, rh, a << 24 | (color & 0xFFFFFF));
        }
        ellipse(g, cx, cy + 3, w / 2, h / 2, 0xFF0A0F26);
        ellipse(g, cx, cy, w / 2, h / 2, 0xFF2E3C70);
        ellipse(g, cx, cy - 1, w / 2 - 3, h / 2 - 2, 0xFF46589A);
        ellipseOutline(g, cx, cy, w / 2, h / 2, INK);
        ellipseOutline(g, cx, cy - 1, w / 2 - 4, h / 2 - 2, 0xFF000000 | (color & 0xFFFFFF));
    }

    public static void ellipse(GuiGraphicsExtractor g, int cx, int cy, int rx, int ry, int color) {
        if (rx <= 0 || ry <= 0) {
            return;
        }
        for (int dy = -ry; dy <= ry; dy++) {
            double t = (double) dy / ry;
            int half = (int) Math.round(rx * Math.sqrt(Math.max(0.0, 1.0 - t * t)));
            g.fill(cx - half, cy + dy, cx + half, cy + dy + 1, color);
        }
    }

    public static void ellipseOutline(GuiGraphicsExtractor g, int cx, int cy, int rx, int ry, int color) {
        if (rx <= 0 || ry <= 0) {
            return;
        }
        int prev = 0;
        for (int dy = -ry; dy <= ry; dy++) {
            double t = (double) dy / ry;
            int half = (int) Math.round(rx * Math.sqrt(Math.max(0.0, 1.0 - t * t)));
            int from = dy <= 0 ? prev : half;
            int lo = Math.min(from, half);
            int hi = Math.max(from, half);
            if (dy == -ry || dy == ry) {
                g.fill(cx - half, cy + dy, cx + half, cy + dy + 1, color);
            } else {
                g.fill(cx - hi - 1, cy + dy, cx - lo, cy + dy + 1, color);
                g.fill(cx + lo, cy + dy, cx + hi + 1, cy + dy + 1, color);
            }
            prev = half;
        }
    }

    public static void glow(GuiGraphicsExtractor g, int cx, int cy, int radius, int color, float time) {
        float pulse = 0.85f + 0.15f * (float) Math.sin(time * 0.1f);
        for (int i = 5; i >= 1; i--) {
            int r = Math.round(radius * i / 5.0f * pulse);
            ellipse(g, cx, cy, r, Math.round(r * 1.25f), (0x0C + (5 - i) * 3) << 24 | (color & 0xFFFFFF));
        }
    }

    public static int mix(int a, int b, float t) {
        int aa = a >>> 24;
        int ar = a >> 16 & 255;
        int ag = a >> 8 & 255;
        int ab = a & 255;
        int ba = b >>> 24;
        int br = b >> 16 & 255;
        int bg = b >> 8 & 255;
        int bb = b & 255;
        return Math.round(aa + (ba - aa) * t) << 24 | Math.round(ar + (br - ar) * t) << 16 | Math.round(ag + (bg - ag) * t) << 8 | Math.round(ab + (bb - ab) * t);
    }

    public static int darker(int argb, float f) {
        int r = Math.round((argb >> 16 & 255) * f);
        int gr = Math.round((argb >> 8 & 255) * f);
        int b = Math.round((argb & 255) * f);
        return argb & 0xFF000000 | r << 16 | gr << 8 | b;
    }

    public static void gauge(GuiGraphicsExtractor g, int x, int y, int w, int h, float ratio, float trail, int color) {
        float filled = clamp(ratio);
        float ghost = clamp(trail);
        int slant = h / 2;
        int light = mix(color, 0xFFFFFFFF, 0.55f);
        int face = color;
        int shade = darker(color, 0.62f);
        g.fill(x + slant - 1, y - 1, x + w + 1, y, INK);
        g.fill(x - 1, y + h, x + w - slant + 1, y + h + 1, INK);
        int segments = Math.max(1, (w - slant) / 10);
        for (int row = 0; row < h; row++) {
            int off = (h - 1 - row) * slant / Math.max(1, h - 1);
            int x0 = x + off;
            int x1 = x0 + w - slant;
            int span = x1 - x0;
            g.fill(x0 - 1, y + row, x0, y + row + 1, INK);
            g.fill(x1, y + row, x1 + 1, y + row + 1, INK);
            g.fill(x0, y + row, x1, y + row + 1, row == h - 1 ? 0xE00A0E1E : 0xD0161C34);
            int ghostEnd = x0 + Math.round(span * ghost);
            if (ghostEnd > x0) {
                g.fill(x0, y + row, ghostEnd, y + row + 1, 0xB0FFF0D0);
            }
            int end = x0 + Math.round(span * filled);
            if (end > x0) {
                int c = h <= 2 ? face : row == 0 ? light : row >= h - 1 - h / 4 ? shade : face;
                g.fill(x0, y + row, end, y + row + 1, c);
                if (end - x0 > 2 && filled < 1.0f) {
                    g.fill(end - 1, y + row, end, y + row + 1, 0xFFFFFFFF);
                }
            }
            if (h >= 4) {
                for (int s = 1; s < segments; s++) {
                    int tx = x0 + span * s / segments;
                    g.fill(tx, y + row, tx + 1, y + row + 1, 0x50000000);
                }
            }
        }
    }

    private static float clamp(float v) {
        return Float.isNaN(v) ? 0.0f : Math.max(0.0f, Math.min(1.0f, v));
    }

    public static void label(GuiGraphicsExtractor g, Font font, Component text, int x, int y, int color) {
        g.text(font, text, x, y, color, true);
    }
}
