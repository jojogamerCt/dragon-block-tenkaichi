package com.dbtenkaichi.client.fx;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.SingleQuadParticle;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.core.particles.ColorParticleOption;
import net.minecraft.util.LightCoordsUtil;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;

public class TenkaichiParticle extends SingleQuadParticle {
    public enum Style {
        KI_SPARK(0.07f, 0.13f, 8, 16, 0.0f, 0.86f, 0.0f, 1.0f, true, true),
        AURA_FLAME(0.18f, 0.3f, 12, 20, -0.004f, 0.9f, 0.5f, 0.6f, true, true),
        ENERGY_ORB(0.12f, 0.24f, 16, 28, 0.0f, 0.92f, 1.2f, 0.85f, true, true),
        IMPACT_STAR(0.7f, 1.0f, 5, 7, 0.0f, 0.5f, 1.8f, 1.0f, true, false),
        SHOCK_RING(0.4f, 0.6f, 9, 12, 0.0f, 0.5f, 6.0f, 0.9f, true, false),
        DUST_PUFF(0.2f, 0.34f, 18, 30, -0.002f, 0.9f, 1.6f, 0.75f, false, true),
        LIGHTNING(0.35f, 0.6f, 5, 9, 0.0f, 0.5f, 1.0f, 1.0f, true, false),
        SPEED_LINE(0.25f, 0.4f, 5, 8, 0.0f, 0.95f, 0.6f, 0.8f, true, false),
        ROCK(0.09f, 0.16f, 50, 80, -0.12f, 0.97f, 0.0f, 1.0f, false, false);

        final float minSize;
        final float maxSize;
        final int minLife;
        final int maxLife;
        final float gravity;
        final float friction;
        final float growth;
        final float startAlpha;
        final boolean glow;
        final boolean animated;

        Style(float minSize, float maxSize, int minLife, int maxLife, float gravity, float friction, float growth, float startAlpha, boolean glow, boolean animated) {
            this.minSize = minSize;
            this.maxSize = maxSize;
            this.minLife = minLife;
            this.maxLife = maxLife;
            this.gravity = gravity;
            this.friction = friction;
            this.growth = growth;
            this.startAlpha = startAlpha;
            this.glow = glow;
            this.animated = animated;
        }
    }

    private final SpriteSet sprites;
    private final Style style;
    private float baseSize;
    private final float spin;

    protected TenkaichiParticle(ClientLevel level, double x, double y, double z, double dx, double dy, double dz, ColorParticleOption options, SpriteSet sprites, Style style) {
        super(level, x, y, z, sprites.first());
        this.sprites = sprites;
        this.style = style;
        this.xd = dx;
        this.yd = dy;
        this.zd = dz;
        this.hasPhysics = style == Style.DUST_PUFF;
        this.friction = style.friction;
        this.gravity = style.gravity;
        this.lifetime = style.minLife + random.nextInt(Math.max(1, style.maxLife - style.minLife + 1));
        this.baseSize = style.minSize + random.nextFloat() * (style.maxSize - style.minSize);
        this.quadSize = baseSize;
        this.rCol = options.getRed();
        this.gCol = options.getGreen();
        this.bCol = options.getBlue();
        this.alpha = style.startAlpha;
        this.roll = 0.0f;
        this.oRoll = roll;
        this.spin = 0.0f;
        if (style == Style.SPEED_LINE) {
            this.roll = (float) Math.atan2(dy, Math.sqrt(dx * dx + dz * dz)) + Mth.HALF_PI;
            this.oRoll = roll;
        }
        if (style.animated) {
            setSpriteFromAge(sprites);
        } else {
            setSprite(sprites.get(random));
        }
    }

    @Override
    public void tick() {
        super.tick();
        if (removed) {
            return;
        }
        float t = (float) age / Math.max(1, lifetime);
        oRoll = roll;
        roll += spin;
        quadSize = baseSize * (1.0f + style.growth * easeOut(t));
        if (style == Style.KI_SPARK || style == Style.LIGHTNING) {
            quadSize = baseSize * (1.0f - t * 0.8f);
        }
        float fade = style == Style.ROCK ? Mth.clamp((1.0f - t) * 5.0f, 0.0f, 1.0f) : style == Style.AURA_FLAME || style == Style.ENERGY_ORB ? Mth.clamp((1.0f - t) * 1.6f, 0.0f, 1.0f) : 1.0f - t * t;
        alpha = style.startAlpha * fade;
        if (style == Style.LIGHTNING && random.nextInt(2) == 0) {
            setSprite(sprites.get(random));
        }
        if (style.animated) {
            setSpriteFromAge(sprites);
        }
    }

    @Override
    public Particle scale(float factor) {
        baseSize *= factor;
        quadSize = baseSize;
        return this;
    }

    private static float easeOut(float t) {
        return 1.0f - (1.0f - t) * (1.0f - t);
    }

    @Override
    protected SingleQuadParticle.Layer getLayer() {
        return style == Style.ROCK ? SingleQuadParticle.Layer.OPAQUE : SingleQuadParticle.Layer.TRANSLUCENT;
    }

    @Override
    protected int getLightCoords(float partial) {
        return style.glow ? LightCoordsUtil.FULL_BRIGHT : super.getLightCoords(partial);
    }

    public static ParticleProvider<ColorParticleOption> provider(SpriteSet sprites, Style style) {
        return (options, level, x, y, z, dx, dy, dz, random) -> create(options, level, x, y, z, dx, dy, dz, random, sprites, style);
    }

    private static Particle create(ColorParticleOption options, ClientLevel level, double x, double y, double z, double dx, double dy, double dz, RandomSource random,
                                   SpriteSet sprites, Style style) {
        return new TenkaichiParticle(level, x, y, z, dx, dy, dz, options, sprites, style);
    }
}
