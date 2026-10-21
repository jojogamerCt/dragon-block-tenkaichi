package com.dbtenkaichi.player;

import com.dbtenkaichi.race.Race;
import net.minecraft.nbt.CompoundTag;

public final class Appearance {
    public static final int PRESENTATIONS = 2;
    public static final int EYE_STYLES = 4;
    public static final int EYEBROW_STYLES = 4;
    public static final int HAIR_STYLES = HairStyle.PLAYER_SELECTABLE;
    public static final int EXPRESSIONS = 4;
    public static final int OUTFITS = 4;
    public static final int OUTFIT_NATURAL = 3;
    public static final int BODY_BUILDS = 3;

    public int presentation;
    public float height = 1.0f;
    public int build = 1;
    public int skinColor = 0xF1C27D;
    public int eyeStyle;
    public int eyeColor = 0x1B1B1B;
    public int eyebrowStyle;
    public int hairStyle;
    public int hairColor = 0x111111;
    public int expression;
    public int auraColor = 0xE8F4FF;
    public int outfit;
    public int outfitPrimary = 0xF07A1A;
    public int outfitSecondary = 0x1F3FAE;
    public int featureColor = 0xD86A8C;
    public boolean tail = true;
    public boolean useOwnSkin;

    public Appearance copy() {
        Appearance a = new Appearance();
        a.load(save());
        return a;
    }

    public void sanitize(Race race, float minHeight, float maxHeight) {
        presentation = clamp(presentation, 0, PRESENTATIONS - 1);
        height = Math.max(minHeight, Math.min(maxHeight, Float.isFinite(height) ? height : 1.0f));
        build = clamp(build, 0, BODY_BUILDS - 1);
        eyeStyle = clamp(eyeStyle, 0, EYE_STYLES - 1);
        eyebrowStyle = clamp(eyebrowStyle, 0, EYEBROW_STYLES - 1);
        hairStyle = clamp(hairStyle, 0, HAIR_STYLES - 1);
        expression = clamp(expression, 0, EXPRESSIONS - 1);
        outfit = clamp(outfit, 0, OUTFITS - 1);
        skinColor &= 0xFFFFFF;
        eyeColor &= 0xFFFFFF;
        hairColor &= 0xFFFFFF;
        auraColor &= 0xFFFFFF;
        outfitPrimary &= 0xFFFFFF;
        outfitSecondary &= 0xFFFFFF;
        featureColor &= 0xFFFFFF;
        if (!race.has(com.dbtenkaichi.race.RacePassive.TAIL)) {
            tail = false;
        }
        if (!race.hasHair()) {
            hairStyle = HairStyle.BALD.ordinal();
        }
    }

    public CompoundTag save() {
        CompoundTag tag = new CompoundTag();
        tag.putInt("presentation", presentation);
        tag.putFloat("height", height);
        tag.putInt("build", build);
        tag.putInt("skin", skinColor);
        tag.putInt("eye_style", eyeStyle);
        tag.putInt("eye_color", eyeColor);
        tag.putInt("brow", eyebrowStyle);
        tag.putInt("hair_style", hairStyle);
        tag.putInt("hair_color", hairColor);
        tag.putInt("expression", expression);
        tag.putInt("aura", auraColor);
        tag.putInt("outfit", outfit);
        tag.putInt("outfit_primary", outfitPrimary);
        tag.putInt("outfit_secondary", outfitSecondary);
        tag.putInt("feature", featureColor);
        tag.putBoolean("tail", tail);
        tag.putBoolean("own_skin", useOwnSkin);
        return tag;
    }

    public void load(CompoundTag tag) {
        presentation = tag.getIntOr("presentation", presentation);
        height = tag.getFloatOr("height", height);
        build = tag.getIntOr("build", build);
        skinColor = tag.getIntOr("skin", skinColor);
        eyeStyle = tag.getIntOr("eye_style", eyeStyle);
        eyeColor = tag.getIntOr("eye_color", eyeColor);
        eyebrowStyle = tag.getIntOr("brow", eyebrowStyle);
        hairStyle = tag.getIntOr("hair_style", hairStyle);
        hairColor = tag.getIntOr("hair_color", hairColor);
        expression = tag.getIntOr("expression", expression);
        auraColor = tag.getIntOr("aura", auraColor);
        outfit = tag.getIntOr("outfit", outfit);
        outfitPrimary = tag.getIntOr("outfit_primary", outfitPrimary);
        outfitSecondary = tag.getIntOr("outfit_secondary", outfitSecondary);
        featureColor = tag.getIntOr("feature", featureColor);
        tail = tag.getBooleanOr("tail", tail);
        useOwnSkin = tag.getBooleanOr("own_skin", useOwnSkin);
    }

    public static int defaultHair(Race race, int presentation) {
        if (presentation != 1 || !race.hasHair()) {
            return defaultsFor(race).hairStyle;
        }
        return switch (race) {
            case HUMAN -> HairStyle.CHI_CHI.ordinal();
            case ANDROID, KAIOSHIN, DEMON -> HairStyle.ANDROID_18.ordinal();
            default -> HairStyle.VIDEL.ordinal();
        };
    }

    public static Appearance defaultsFor(Race race) {
        Appearance a = new Appearance();
        a.tail = race.tailByDefault();
        switch (race) {
            case SAIYAN -> {
                a.hairStyle = HairStyle.GOKU.ordinal();
                a.hairColor = 0x111111;
                a.auraColor = 0xF2F6FF;
            }
            case HALF_SAIYAN -> {
                a.hairStyle = HairStyle.GOHAN.ordinal();
                a.hairColor = 0x1A1410;
                a.auraColor = 0xFFF2C0;
                a.outfitPrimary = 0x6A2FA0;
                a.outfitSecondary = 0xE0B030;
            }
            case HUMAN -> {
                a.hairStyle = HairStyle.YAMCHA.ordinal();
                a.hairColor = 0x3B2416;
                a.auraColor = 0x9FD8FF;
            }
            case NAMEKIAN -> {
                a.skinColor = 0x6DBF4A;
                a.featureColor = 0xD8789A;
                a.auraColor = 0xF4FFF0;
                a.outfitPrimary = 0x5C3B8E;
                a.outfitSecondary = 0x2A6FB0;
            }
            case FRIEZA_RACE -> {
                a.skinColor = 0xE6CCDA;
                a.featureColor = 0x9A3CC8;
                a.eyeColor = 0xB0141E;
                a.auraColor = 0xC890FF;
                a.eyebrowStyle = 1;
                a.outfit = OUTFIT_NATURAL;
                a.outfitPrimary = 0x24204A;
                a.outfitSecondary = 0x7A3010;
            }
            case MAJIN -> {
                a.skinColor = 0xF4A0C0;
                a.featureColor = 0xE07AA4;
                a.eyeColor = 0x0E0E0E;
                a.auraColor = 0xFF9ACD;
                a.outfitPrimary = 0xF2F2F2;
                a.outfitSecondary = 0x2A2A2A;
            }
            case ANDROID -> {
                a.skinColor = 0xF0CBA4;
                a.featureColor = 0x9AA7B4;
                a.eyeColor = 0x2A6FD0;
                a.hairStyle = HairStyle.TRUNKS.ordinal();
                a.hairColor = 0xE8D27A;
                a.auraColor = 0xBFE8FF;
                a.outfit = 2;
                a.outfitPrimary = 0x2E2E36;
                a.outfitSecondary = 0x1F3F7A;
            }
            case BIO_ANDROID -> {
                a.skinColor = 0x86BE5E;
                a.featureColor = 0x23361A;
                a.eyeColor = 0xC01A6A;
                a.auraColor = 0xB8F070;
                a.eyebrowStyle = 1;
                a.outfit = OUTFIT_NATURAL;
                a.outfitPrimary = 0x1E3A16;
                a.outfitSecondary = 0x9C6FB8;
            }
            case KAIOSHIN -> {
                a.skinColor = 0xC8A0E8;
                a.featureColor = 0xF0D040;
                a.hairStyle = HairStyle.VEGETA.ordinal();
                a.hairColor = 0xF4F4F4;
                a.auraColor = 0xFFFFFF;
                a.outfitPrimary = 0x3A2A8A;
                a.outfitSecondary = 0xE8E0D0;
            }
            case DEMON -> {
                a.skinColor = 0xC8484A;
                a.featureColor = 0x2A1A30;
                a.eyeColor = 0xF0D020;
                a.hairStyle = HairStyle.RADITZ.ordinal();
                a.hairColor = 0x1A0A24;
                a.auraColor = 0x9A2AE0;
                a.outfit = 1;
                a.outfitPrimary = 0x2A1A30;
                a.outfitSecondary = 0x8A1A1A;
            }
        }
        if (!race.hasHair()) {
            a.hairStyle = HairStyle.BALD.ordinal();
        }
        return a;
    }

    private static int clamp(int v, int min, int max) {
        return v < min ? min : Math.min(v, max);
    }
}
