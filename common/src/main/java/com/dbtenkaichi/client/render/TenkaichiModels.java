package com.dbtenkaichi.client.render;

import com.dbtenkaichi.Tenkaichi;
import com.dbtenkaichi.player.HairStyle;
import com.dbtenkaichi.race.Race;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.client.model.player.PlayerModel;

import java.util.EnumMap;
import java.util.Locale;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.Supplier;

public final class TenkaichiModels {
    public static final Map<HairStyle, ModelLayerLocation> HAIR = new EnumMap<>(HairStyle.class);
    public static final ModelLayerLocation TAIL = new ModelLayerLocation(Tenkaichi.id("tail"), "main");
    public static final ModelLayerLocation AURA_SHELL = new ModelLayerLocation(Tenkaichi.id("aura_shell"), "main");
    public static final ModelLayerLocation FIGHTER = new ModelLayerLocation(Tenkaichi.id("fighter"), "main");
    public static final ModelLayerLocation THICK_TAIL = new ModelLayerLocation(Tenkaichi.id("thick_tail"), "main");
    public static final ModelLayerLocation STINGER_TAIL = new ModelLayerLocation(Tenkaichi.id("stinger_tail"), "main");
    public static final ModelLayerLocation SHOULDER_PADS = new ModelLayerLocation(Tenkaichi.id("shoulder_pads"), "main");
    public static final ModelLayerLocation BEARD = new ModelLayerLocation(Tenkaichi.id("beard"), "main");
    public static final ModelLayerLocation ROSHI_GEAR = new ModelLayerLocation(Tenkaichi.id("roshi_gear"), "main");
    public static final ModelLayerLocation ROSHI_BEARD = new ModelLayerLocation(Tenkaichi.id("roshi_beard"), "main");
    public static final ModelLayerLocation OUTFIT = new ModelLayerLocation(Tenkaichi.id("outfit"), "main");
    public static final ModelLayerLocation OUTFIT_FEMININE = new ModelLayerLocation(Tenkaichi.id("outfit_feminine"), "main");
    public static final ModelLayerLocation BUST = new ModelLayerLocation(Tenkaichi.id("bust"), "main");
    public static final ModelLayerLocation CAT_FEATURES = new ModelLayerLocation(Tenkaichi.id("cat_features"), "main");
    public static final ModelLayerLocation CAT_NOSE = new ModelLayerLocation(Tenkaichi.id("cat_nose"), "main");
    public static final Map<Race, Map<FeatureTint, ModelLayerLocation>> FEATURES = new EnumMap<>(Race.class);
    public static final int TAIL_SEGMENTS = 8;
    public static final int THICK_TAIL_SEGMENTS = 9;
    private static final float PI = (float) Math.PI;

    static {
        for (HairStyle style : HairStyle.values()) {
            if (style != HairStyle.BALD) {
                HAIR.put(style, new ModelLayerLocation(Tenkaichi.id("hair_" + style.id()), "main"));
            }
        }
        for (Race race : Race.values()) {
            for (FeatureTint tint : FeatureTint.values()) {
                if (feature(race, tint) != null) {
                    FEATURES.computeIfAbsent(race, r -> new EnumMap<>(FeatureTint.class))
                            .put(tint, new ModelLayerLocation(Tenkaichi.id("feature_" + race.id() + "_" + tint.name().toLowerCase(Locale.ROOT)), "main"));
                }
            }
        }
    }

    public enum FeatureTint {
        SKIN,
        ACCENT,
        HORN,
        PLATE,
        GEM,
        BODY_PLATE,
        BODY_GEM;

        public boolean bodyOnly() {
            return this == BODY_PLATE || this == BODY_GEM;
        }

        public boolean gem() {
            return this == GEM || this == BODY_GEM;
        }
    }

    private TenkaichiModels() {
    }

    public static void registerLayers(BiConsumer<ModelLayerLocation, Supplier<LayerDefinition>> registrar) {
        for (Map.Entry<HairStyle, ModelLayerLocation> entry : HAIR.entrySet()) {
            HairStyle style = entry.getKey();
            registrar.accept(entry.getValue(), () -> hair(style));
        }
        registrar.accept(TAIL, TenkaichiModels::tail);
        registrar.accept(AURA_SHELL, () -> LayerDefinition.create(PlayerModel.createMesh(new CubeDeformation(0.45f), false), 64, 64));
        registrar.accept(FIGHTER, () -> LayerDefinition.create(PlayerModel.createMesh(CubeDeformation.NONE, false), 64, 64));
        registrar.accept(THICK_TAIL, () -> thickTail(false));
        registrar.accept(STINGER_TAIL, () -> thickTail(true));
        registrar.accept(SHOULDER_PADS, TenkaichiModels::shoulderPads);
        registrar.accept(BEARD, TenkaichiModels::beard);
        registrar.accept(ROSHI_GEAR, TenkaichiModels::roshiGear);
        registrar.accept(ROSHI_BEARD, TenkaichiModels::roshiBeard);
        registrar.accept(CAT_FEATURES, TenkaichiModels::catFeatures);
        registrar.accept(OUTFIT_FEMININE, () -> bust(net.minecraft.client.model.player.PlayerModel.createMesh(new net.minecraft.client.model.geom.builders.CubeDeformation(0.07f), false), 0.07f));
        registrar.accept(BUST, () -> bust(emptyPlayerMesh(), 0.0f));
        registrar.accept(OUTFIT, () -> LayerDefinition.create(net.minecraft.client.model.player.PlayerModel.createMesh(new net.minecraft.client.model.geom.builders.CubeDeformation(0.07f), false), 64, 64));
        registrar.accept(CAT_NOSE, TenkaichiModels::catNose);
        FEATURES.forEach((race, byTint) -> byTint.forEach((tint, location) ->
                registrar.accept(location, () -> LayerDefinition.create(feature(race, tint), 64, 64))));
    }

    private static MeshDefinition emptyPlayerMesh() {
        MeshDefinition mesh = PlayerModel.createMesh(CubeDeformation.NONE, false);
        mesh.getRoot().clearRecursively();
        return mesh;
    }

    public static LayerDefinition hair(HairStyle style) {
        MeshDefinition mesh = emptyPlayerMesh();
        PartDefinition head = mesh.getRoot().getChild("head");
        Builder b = new Builder(head);
        switch (style) {
            case GOKU -> {
                b.cap(true);
                b.spike(-2.5f, -7.5f, -4.0f, 2.6f, 0.0f, 0.4f, 4.6f, 2.6f);
                b.spike(0.2f, -7.8f, -4.1f, 2.78f, 0.0f, -0.05f, 5.6f, 2.8f);
                b.spike(2.6f, -7.4f, -3.9f, 2.55f, 0.0f, -0.45f, 4.2f, 2.4f);
                b.spike(-0.6f, -8.4f, -2.6f, 0.4f, 0.0f, 0.1f, 5.5f, 3.0f);
                for (int i = -2; i <= 2; i++) {
                    float x = i * 1.75f;
                    b.spike(x, -8.3f, 0.4f, -0.9f - Math.abs(i) * 0.1f, 0.0f, i * 0.34f, 9.0f - Math.abs(i) * 0.9f, 3.6f);
                }
                b.spike(-4.3f, -6.9f, -0.8f, 0.05f, 0.0f, -1.3f, 6.5f, 3.2f);
                b.spike(-4.3f, -4.9f, 1.2f, -0.2f, 0.0f, -1.55f, 5.0f, 2.8f);
                b.spike(4.3f, -6.9f, -0.8f, 0.05f, 0.0f, 1.3f, 6.5f, 3.2f);
                b.spike(4.3f, -4.9f, 1.2f, -0.2f, 0.0f, 1.55f, 5.0f, 2.8f);
                b.spike(0.0f, -6.6f, 4.0f, -1.55f, 0.0f, 0.0f, 6.5f, 3.2f);
                b.spike(-2.4f, -4.6f, 4.0f, -2.2f, 0.0f, -0.35f, 5.0f, 2.8f);
                b.spike(2.4f, -4.6f, 4.0f, -2.2f, 0.0f, 0.35f, 5.0f, 2.8f);
            }
            case SUPER_SPIKES -> {
                b.cap(true);
                b.spike(-1.2f, -7.8f, -3.9f, 2.55f, 0.1f, -0.15f, 5.0f, 2.4f);
                b.spike(-3.2f, -8.0f, 0.0f, -0.4f, 0.0f, -0.68f, 9.0f, 3.4f);
                b.spike(-1.2f, -8.3f, 0.6f, -0.52f, 0.0f, -0.26f, 10.0f, 3.6f);
                b.spike(1.2f, -8.3f, 0.6f, -0.58f, 0.0f, 0.28f, 9.6f, 3.6f);
                b.spike(3.2f, -8.0f, 0.0f, -0.42f, 0.0f, 0.7f, 8.6f, 3.4f);
                b.spike(0.0f, -7.5f, 2.8f, -0.9f, 0.0f, 0.0f, 8.4f, 3.4f);
                b.spike(-2.4f, -6.0f, 3.6f, -0.95f, 0.0f, -0.35f, 6.8f, 3.0f);
                b.spike(2.4f, -6.0f, 3.6f, -0.95f, 0.0f, 0.35f, 6.8f, 3.0f);
                b.spike(-4.2f, -6.8f, -0.5f, 0.0f, 0.0f, -0.75f, 5.0f, 2.6f);
                b.spike(4.2f, -6.8f, -0.5f, 0.0f, 0.0f, 0.75f, 5.0f, 2.6f);
                b.spike(-1.8f, -7.6f, -2.0f, 0.08f, 0.0f, -0.5f, 7.0f, 3.0f);
                b.spike(1.8f, -7.6f, -2.0f, 0.08f, 0.0f, 0.5f, 7.0f, 3.0f);
            }
            case ASCENDED_SPIKES -> {
                b.cap(true);
                b.spike(-0.6f, -7.9f, -3.9f, 2.7f, 0.0f, 0.05f, 6.0f, 2.0f);
                for (int i = 0; i < 6; i++) {
                    float x = -3.4f + i * 1.36f;
                    b.spike(x, -8.2f, 0.4f, -0.22f - (i % 2) * 0.1f, 0.0f, -x * 0.11f, 10.0f + (i % 2) * 1.2f, 2.8f);
                }
                b.spike(0.0f, -7.0f, 3.2f, -0.7f, 0.0f, 0.0f, 9.0f, 3.0f);
                b.spike(-2.6f, -5.6f, 3.6f, -1.05f, 0.0f, 0.4f, 7.5f, 2.6f);
                b.spike(2.6f, -5.6f, 3.6f, -1.05f, 0.0f, -0.4f, 7.5f, 2.6f);
                b.spike(-4.2f, -6.8f, -0.8f, 0.0f, 0.0f, 0.7f, 5.5f, 2.2f);
                b.spike(4.2f, -6.8f, -0.8f, 0.0f, 0.0f, -0.7f, 5.5f, 2.2f);
            }
            case SSJ2_SPIKES -> {
                b.cap(true);
                b.spike(-0.9f, -8.0f, -4.05f, 2.95f, 0.0f, 0.18f, 7.0f, 1.3f);
                b.spike(-2.4f, -8.0f, -3.2f, -0.35f, 0.0f, -0.45f, 9.0f, 2.8f);
                b.spike(0.4f, -8.3f, -3.0f, -0.42f, 0.0f, 0.05f, 10.5f, 3.0f);
                b.spike(2.8f, -8.0f, -3.0f, -0.36f, 0.0f, 0.5f, 9.0f, 2.8f);
                float[][] crown = {{-3.4f, -0.62f, 11.0f}, {-1.7f, -0.25f, 12.5f}, {0.0f, 0.0f, 13.5f}, {1.7f, 0.27f, 12.5f}, {3.4f, 0.64f, 11.0f}};
                for (float[] c : crown) {
                    b.spike(c[0], -8.2f, -0.6f, -0.62f, 0.0f, c[1], c[2], 3.4f);
                }
                b.spike(-2.2f, -7.2f, 2.6f, -1.05f, 0.0f, -0.35f, 10.0f, 3.2f);
                b.spike(0.0f, -7.4f, 2.8f, -0.95f, 0.0f, 0.0f, 11.0f, 3.4f);
                b.spike(2.2f, -7.2f, 2.6f, -1.05f, 0.0f, 0.35f, 10.0f, 3.2f);
                b.spike(-2.0f, -4.6f, 4.0f, -1.75f, 0.0f, -0.3f, 6.5f, 2.8f);
                b.spike(2.0f, -4.6f, 4.0f, -1.75f, 0.0f, 0.3f, 6.5f, 2.8f);
                b.spike(-4.3f, -7.0f, -1.2f, -0.2f, 0.0f, -1.05f, 7.5f, 2.8f);
                b.spike(-4.3f, -5.2f, 1.2f, -0.45f, 0.0f, -1.4f, 6.0f, 2.6f);
                b.spike(4.3f, -7.0f, -1.2f, -0.2f, 0.0f, 1.05f, 7.5f, 2.8f);
                b.spike(4.3f, -5.2f, 1.2f, -0.45f, 0.0f, 1.4f, 6.0f, 2.6f);
            }
            case VEGETA, SSJ_VEGETA -> {
                float k = style == HairStyle.SSJ_VEGETA ? 1.3f : 1.0f;
                b.cap(true);
                b.spike(0.0f, -8.2f, -3.8f, -0.15f, 0.0f, 0.0f, 3.5f * k, 2.2f);
                for (int i = 0; i < 7; i++) {
                    float x = -3.3f + i * 1.1f;
                    b.spike(x, -8.3f, -1.2f + (i % 2) * 1.4f, -0.18f - (i % 3) * 0.06f, 0.0f, -x * 0.1f * k, (8.0f + (i % 2) * 1.5f) * k, 3.0f);
                }
                b.spike(-2.0f, -7.8f, 2.4f, -0.55f, 0.0f, 0.2f, 7.5f * k, 3.0f);
                b.spike(2.0f, -7.8f, 2.4f, -0.55f, 0.0f, -0.2f, 7.5f * k, 3.0f);
                b.spike(-4.2f, -7.0f, -1.5f, -0.1f, 0.0f, 0.45f, 4.0f * k, 2.2f);
                b.spike(4.2f, -7.0f, -1.5f, -0.1f, 0.0f, -0.45f, 4.0f * k, 2.2f);
            }
            case RADITZ, LEGENDARY_MANE -> {
                boolean legend = style == HairStyle.LEGENDARY_MANE;
                b.cap(true);
                b.group("mane_root", 0.0f, -8.0f, 3.0f);
                if (legend) {
                    b.slab("mane", -4.8f, -0.4f, -0.4f, 9.6f, 10.0f, 3.2f, 0.1f);
                    float[][] outer = {{-3.9f, 25.0f, 0.2f}, {-2.0f, 29.0f, 0.09f}, {0.0f, 31.0f, 0.0f}, {2.0f, 29.0f, -0.09f}, {3.9f, 25.0f, -0.2f}};
                    for (float[] lock : outer) {
                        b.spike(lock[0], 1.5f, 0.9f, PI - 0.14f, 0.0f, lock[2], lock[1], 4.2f);
                    }
                    float[][] inner = {{-2.9f, 21.0f, 0.14f}, {0.0f, 24.0f, 0.0f}, {2.9f, 21.0f, -0.14f}};
                    for (float[] lock : inner) {
                        b.spike(lock[0], 3.0f, 2.0f, PI - 0.3f, 0.0f, lock[2], lock[1], 3.6f);
                    }
                } else {
                    float length = 20.0f;
                    b.slab("mane", -4.8f, -0.4f, -0.4f, 9.6f, length, 3.4f, 0.12f);
                    b.slab("mane_inner", -4.2f, 2.0f, -1.2f, 8.4f, length * 0.7f, 1.4f, 0.08f);
                    for (int i = 0; i < 4; i++) {
                        float x = -3.3f + i * 2.2f;
                        b.spike(x, -1.4f + length, 0.6f, PI - 0.25f, 0.0f, (i - 1.5f) * 0.18f, 5.0f, 3.0f);
                    }
                }
                b.endGroup();
                if (legend) {
                    b.spike(-1.6f, -8.0f, -3.4f, -0.2f, 0.0f, 0.25f, 6.0f, 3.0f);
                    b.spike(1.6f, -8.0f, -3.4f, -0.2f, 0.0f, -0.25f, 6.0f, 3.0f);
                    b.spike(-4.4f, -6.4f, 0.0f, 0.1f, 0.0f, 1.0f, 6.0f, 2.8f);
                    b.spike(4.4f, -6.4f, 0.0f, 0.1f, 0.0f, -1.0f, 6.0f, 2.8f);
                } else {
                    b.spike(-2.4f, -7.8f, -3.7f, 2.4f, 0.1f, 0.0f, 3.5f, 2.4f);
                    b.spike(1.0f, -8.0f, -3.8f, 2.3f, 0.0f, 0.0f, 4.0f, 2.6f);
                    b.spike(-4.4f, -6.0f, 0.0f, 0.3f, 0.0f, 1.1f, 4.0f, 2.4f);
                    b.spike(4.4f, -6.0f, 0.0f, 0.3f, 0.0f, -1.1f, 4.0f, 2.4f);
                }
            }
            case GOHAN -> {
                b.cap(true);
                b.spike(-1.8f, -7.6f, -3.8f, 2.55f, 0.0f, 0.25f, 4.2f, 2.2f);
                b.spike(0.6f, -7.8f, -3.9f, 2.65f, 0.0f, -0.1f, 4.8f, 2.4f);
                b.spike(2.6f, -7.4f, -3.6f, 2.45f, 0.0f, -0.35f, 3.6f, 2.0f);
                for (int i = 0; i < 5; i++) {
                    float x = -3.0f + i * 1.5f;
                    b.spike(x, -8.2f, 0.2f + (i % 2) * 0.8f, -0.45f - (i % 2) * 0.12f, 0.0f, -x * 0.12f, 6.0f + (i % 2), 3.0f);
                }
                b.spike(0.0f, -6.8f, 3.6f, -1.3f, 0.0f, 0.0f, 5.0f, 2.8f);
                b.spike(-2.0f, -5.0f, 3.8f, -2.3f, 0.0f, 0.3f, 4.5f, 2.6f);
                b.spike(2.0f, -5.0f, 3.8f, -2.3f, 0.0f, -0.3f, 4.5f, 2.6f);
            }
            case YAMCHA -> {
                b.cap(true);
                b.spike(-2.2f, -7.8f, -3.8f, 2.3f, 0.0f, 0.3f, 3.2f, 2.2f);
                b.spike(0.4f, -7.9f, -3.9f, 2.25f, 0.0f, 0.0f, 3.6f, 2.4f);
                b.spike(2.6f, -7.7f, -3.6f, 2.35f, 0.0f, -0.3f, 3.0f, 2.0f);
                for (int i = 0; i < 6; i++) {
                    float x = -3.2f + i * 1.28f;
                    b.spike(x, -8.1f, 0.5f, -0.75f, 0.0f, -x * 0.1f, 4.0f, 2.8f);
                }
                b.spike(-1.6f, -5.5f, 3.8f, -2.0f, 0.0f, 0.2f, 3.2f, 2.4f);
                b.spike(1.6f, -5.5f, 3.8f, -2.0f, 0.0f, -0.2f, 3.2f, 2.4f);
            }
            case TRUNKS -> {
                b.cap(false);
                b.slab("crown", -4.6f, -9.2f, -4.4f, 9.2f, 1.0f, 8.8f, 0.0f);
                b.slab("fringe_r", -4.7f, -8.6f, -4.75f, 4.4f, 3.6f, 1.0f, 0.0f);
                b.slab("fringe_l", 0.3f, -8.6f, -4.75f, 4.4f, 3.6f, 1.0f, 0.0f);
                b.slab("side_r", -4.85f, -8.4f, -4.4f, 1.0f, 7.0f, 8.6f, 0.0f);
                b.slab("side_l", 3.85f, -8.4f, -4.4f, 1.0f, 7.0f, 8.6f, 0.0f);
                b.slab("back", -4.7f, -8.4f, 3.75f, 9.4f, 8.6f, 1.1f, 0.0f);
            }
            case ANDROID_18 -> {
                b.cap(false);
                b.slab("crown", -4.6f, -9.1f, -4.4f, 9.2f, 0.9f, 8.8f, 0.0f);
                b.slab("fringe", -4.7f, -8.6f, -4.75f, 9.4f, 2.6f, 1.0f, 0.0f);
                b.slab("lock", -4.6f, -6.2f, -4.7f, 2.6f, 2.2f, 0.9f, 0.0f);
                b.slab("side_r", -4.9f, -8.4f, -4.3f, 1.1f, 8.2f, 8.6f, 0.0f);
                b.slab("side_l", 3.8f, -8.4f, -1.0f, 1.1f, 6.8f, 5.3f, 0.0f);
                b.slab("back", -4.8f, -8.4f, 3.7f, 9.6f, 8.6f, 1.2f, 0.0f);
            }
            case VIDEL -> {
                b.cap(false);
                b.slab("crown", -4.6f, -9.0f, -4.4f, 9.2f, 0.8f, 8.8f, 0.0f);
                b.slab("fringe", -4.7f, -8.6f, -4.75f, 9.4f, 2.4f, 1.0f, 0.0f);
                b.slab("tie_r", -5.3f, -6.8f, 1.4f, 1.3f, 1.6f, 1.6f, 0.0f);
                b.slab("tie_l", 4.0f, -6.8f, 1.4f, 1.3f, 1.6f, 1.6f, 0.0f);
                b.curved(-5.0f, -6.0f, 2.2f, PI - 0.15f, 0.0f, 0.25f, 9.0f, 2.8f, 0.0f, -0.08f);
                b.curved(5.0f, -6.0f, 2.2f, PI - 0.15f, 0.0f, -0.25f, 9.0f, 2.8f, 0.0f, -0.08f);
            }
            case CHI_CHI -> {
                b.cap(false);
                b.slab("crown", -4.6f, -9.0f, -4.4f, 9.2f, 0.8f, 8.8f, 0.0f);
                b.slab("fringe", -4.7f, -8.6f, -4.75f, 9.4f, 2.8f, 1.0f, 0.0f);
                b.slab("bun", -2.2f, -8.6f, 4.0f, 4.4f, 4.2f, 2.8f, 0.0f);
                b.slab("side_r", -4.9f, -6.5f, -4.4f, 1.0f, 6.0f, 1.6f, 0.0f);
                b.slab("side_l", 3.9f, -6.5f, -4.4f, 1.0f, 6.0f, 1.6f, 0.0f);
            }
            default -> {
            }
        }
        return LayerDefinition.create(mesh, 64, 64);
    }

    public static LayerDefinition tail() {
        MeshDefinition mesh = emptyPlayerMesh();
        PartDefinition parent = mesh.getRoot().getChild("body");
        float[] pitch = {0.95f, 0.38f, 0.22f, 0.12f, 0.0f, -0.12f, -0.28f, -0.4f};
        for (int i = 0; i < TAIL_SEGMENTS; i++) {
            float size = 2.3f - i * 0.16f;
            float length = 2.8f - i * 0.06f;
            CubeListBuilder cube = CubeListBuilder.create().texOffs(i * 6 % 48, i * 4 % 16).addBox(-size / 2.0f, 0.0f, -size / 2.0f, size, length + 0.3f, size);
            if (i == TAIL_SEGMENTS - 1) {
                cube.texOffs(0, 20).addBox(-size * 0.75f, length - 0.4f, -size * 0.75f, size * 1.5f, 2.2f, size * 1.5f);
            }
            PartPose pose = i == 0 ? PartPose.offsetAndRotation(0.0f, 10.5f, 2.0f, pitch[i], 0.0f, 0.0f)
                    : PartPose.offsetAndRotation(0.0f, 2.8f - (i - 1) * 0.06f, 0.0f, pitch[i], 0.0f, 0.0f);
            parent = parent.addOrReplaceChild("tail_" + i, cube, pose);
        }
        return LayerDefinition.create(mesh, 64, 32);
    }

    public static LayerDefinition thickTail(boolean stinger) {
        MeshDefinition mesh = emptyPlayerMesh();
        PartDefinition parent = mesh.getRoot().getChild("body");
        float previous = 0.0f;
        for (int i = 0; i < THICK_TAIL_SEGMENTS; i++) {
            float t = i / (float) (THICK_TAIL_SEGMENTS - 1);
            float size = 3.6f - t * 2.4f;
            float length = 3.4f - t * 0.8f;
            CubeListBuilder cube = CubeListBuilder.create().texOffs(0, i * 7 % 56).addBox(-size / 2.0f, 0.0f, -size / 2.0f, size, length + 0.4f, size);
            if (i == THICK_TAIL_SEGMENTS - 1) {
                if (stinger) {
                    cube.texOffs(32, 0).addBox(-1.1f, length, -1.1f, 2.2f, 1.6f, 2.2f)
                            .texOffs(32, 8).addBox(-0.7f, length + 1.6f, -0.7f, 1.4f, 1.8f, 1.4f)
                            .texOffs(32, 16).addBox(-0.35f, length + 3.4f, -0.35f, 0.7f, 1.8f, 0.7f);
                } else {
                    cube.texOffs(32, 0).addBox(-0.5f, length, -0.5f, 1.0f, 2.2f, 1.0f);
                }
            }
            PartPose pose = i == 0 ? PartPose.offsetAndRotation(0.0f, 10.8f, 1.6f, 1.05f, 0.0f, 0.0f)
                    : PartPose.offsetAndRotation(0.0f, previous, 0.0f, i < 4 ? 0.12f : -0.16f, 0.0f, 0.0f);
            parent = parent.addOrReplaceChild("tail_" + i, cube, pose);
            previous = length;
        }
        return LayerDefinition.create(mesh, 64, 64);
    }

    public static LayerDefinition shoulderPads() {
        MeshDefinition mesh = emptyPlayerMesh();
        PartDefinition root = mesh.getRoot();
        PartDefinition body = root.getChild("body");
        body.addOrReplaceChild("collar", CubeListBuilder.create().texOffs(0, 32).addBox(-4.6f, -0.6f, -2.6f, 9.2f, 1.6f, 5.2f), PartPose.ZERO);
        body.addOrReplaceChild("chest", CubeListBuilder.create().texOffs(0, 40).addBox(-4.5f, 0.6f, -2.75f, 9.0f, 7.5f, 0.6f), PartPose.ZERO);
        body.addOrReplaceChild("back", CubeListBuilder.create().texOffs(0, 40).addBox(-4.5f, 0.6f, 2.15f, 9.0f, 7.5f, 0.6f), PartPose.ZERO);
        root.getChild("right_arm").addOrReplaceChild("pad", CubeListBuilder.create().texOffs(0, 0)
                .addBox(-4.4f, -3.0f, -3.0f, 6.0f, 1.4f, 6.0f)
                .texOffs(0, 16).addBox(-4.4f, -1.6f, -3.0f, 1.2f, 3.2f, 6.0f), PartPose.rotation(0.0f, 0.0f, 0.22f));
        root.getChild("left_arm").addOrReplaceChild("pad", CubeListBuilder.create().texOffs(0, 0).mirror()
                .addBox(-1.6f, -3.0f, -3.0f, 6.0f, 1.4f, 6.0f)
                .texOffs(0, 16).addBox(3.2f, -1.6f, -3.0f, 1.2f, 3.2f, 6.0f), PartPose.rotation(0.0f, 0.0f, -0.22f));
        return LayerDefinition.create(mesh, 64, 64);
    }

    public static LayerDefinition beard() {
        MeshDefinition mesh = emptyPlayerMesh();
        PartDefinition head = mesh.getRoot().getChild("head");
        head.addOrReplaceChild("moustache_l", CubeListBuilder.create().texOffs(0, 0).addBox(-3.2f, -2.9f, -4.6f, 3.0f, 0.8f, 0.6f), PartPose.rotation(0.0f, 0.0f, -0.2f));
        head.addOrReplaceChild("moustache_r", CubeListBuilder.create().texOffs(0, 0).addBox(0.2f, -2.9f, -4.6f, 3.0f, 0.8f, 0.6f), PartPose.rotation(0.0f, 0.0f, 0.2f));
        head.addOrReplaceChild("beard", CubeListBuilder.create().texOffs(0, 8).addBox(-3.0f, -2.0f, -4.7f, 6.0f, 3.0f, 1.4f)
                .texOffs(0, 16).addBox(-2.2f, 1.0f, -4.6f, 4.4f, 3.0f, 1.2f)
                .texOffs(0, 24).addBox(-1.2f, 4.0f, -4.4f, 2.4f, 2.4f, 1.0f), PartPose.ZERO);
        head.addOrReplaceChild("brow_l", CubeListBuilder.create().texOffs(16, 0).addBox(-3.6f, -5.0f, -4.4f, 2.6f, 0.9f, 0.6f), PartPose.rotation(0.0f, 0.0f, 0.15f));
        head.addOrReplaceChild("brow_r", CubeListBuilder.create().texOffs(16, 0).addBox(1.0f, -5.0f, -4.4f, 2.6f, 0.9f, 0.6f), PartPose.rotation(0.0f, 0.0f, -0.15f));
        return LayerDefinition.create(mesh, 64, 64);
    }

    public static LayerDefinition roshiGear() {
        MeshDefinition mesh = emptyPlayerMesh();
        PartDefinition body = mesh.getRoot().getChild("body");
        body.addOrReplaceChild("shell", CubeListBuilder.create()
                .texOffs(0, 24).addBox(-7.0f, -1.6f, 2.3f, 14.0f, 14.6f, 0.6f)
                .texOffs(0, 0).addBox(-6.4f, -1.1f, 2.9f, 12.8f, 13.6f, 1.6f)
                .texOffs(0, 0).addBox(-5.2f, 0.0f, 4.5f, 10.4f, 11.4f, 1.5f)
                .texOffs(0, 0).addBox(-3.8f, 1.4f, 6.0f, 7.6f, 8.6f, 1.2f)
                .texOffs(0, 0).addBox(-2.2f, 3.0f, 7.2f, 4.4f, 5.4f, 0.7f), PartPose.ZERO);
        body.addOrReplaceChild("rope", CubeListBuilder.create()
                .texOffs(40, 0).addBox(-3.4f, -0.5f, -2.7f, 1.0f, 0.5f, 5.4f)
                .texOffs(40, 0).addBox(2.4f, -0.5f, -2.7f, 1.0f, 0.5f, 5.4f)
                .texOffs(40, 8).addBox(-3.4f, 0.0f, -2.75f, 1.0f, 7.0f, 0.4f)
                .texOffs(40, 8).addBox(2.4f, 0.0f, -2.75f, 1.0f, 7.0f, 0.4f)
                .texOffs(40, 8).addBox(-3.4f, 6.4f, -2.8f, 6.8f, 0.8f, 0.4f), PartPose.ZERO);
        PartDefinition head = mesh.getRoot().getChild("head");
        head.addOrReplaceChild("glasses", CubeListBuilder.create()
                .texOffs(40, 16).addBox(-3.9f, -5.3f, -4.75f, 7.8f, 2.3f, 0.5f)
                .texOffs(40, 24).addBox(-3.5f, -5.0f, -4.95f, 2.9f, 1.7f, 0.2f)
                .texOffs(40, 24).addBox(0.6f, -5.0f, -4.95f, 2.9f, 1.7f, 0.2f)
                .texOffs(40, 16).addBox(-4.3f, -4.7f, -4.6f, 0.4f, 0.5f, 4.4f)
                .texOffs(40, 16).addBox(3.9f, -4.7f, -4.6f, 0.4f, 0.5f, 4.4f), PartPose.ZERO);
        return LayerDefinition.create(mesh, 64, 64);
    }

    public static LayerDefinition roshiBeard() {
        MeshDefinition mesh = emptyPlayerMesh();
        PartDefinition head = mesh.getRoot().getChild("head");
        head.addOrReplaceChild("moustache", CubeListBuilder.create().texOffs(0, 0).addBox(-3.8f, -3.0f, -4.85f, 7.6f, 1.1f, 0.8f)
                .texOffs(0, 0).addBox(-4.2f, -2.2f, -4.75f, 1.4f, 2.6f, 0.7f)
                .texOffs(0, 0).addBox(2.8f, -2.2f, -4.75f, 1.4f, 2.6f, 0.7f), PartPose.ZERO);
        head.addOrReplaceChild("beard", CubeListBuilder.create().texOffs(0, 8).addBox(-3.6f, -2.0f, -4.9f, 7.2f, 3.6f, 1.6f)
                .texOffs(0, 16).addBox(-2.9f, 1.6f, -4.8f, 5.8f, 2.4f, 1.5f)
                .texOffs(0, 24).addBox(-2.0f, 4.0f, -4.6f, 4.0f, 1.8f, 1.3f)
                .texOffs(0, 32).addBox(-1.0f, 5.8f, -4.4f, 2.0f, 1.4f, 1.0f), PartPose.ZERO);
        head.addOrReplaceChild("brow_l", CubeListBuilder.create().texOffs(16, 0).addBox(-3.6f, -6.1f, -4.4f, 2.6f, 0.6f, 0.5f), PartPose.rotation(0.0f, 0.0f, 0.12f));
        head.addOrReplaceChild("brow_r", CubeListBuilder.create().texOffs(16, 0).addBox(1.0f, -6.1f, -4.4f, 2.6f, 0.6f, 0.5f), PartPose.rotation(0.0f, 0.0f, -0.12f));
        head.addOrReplaceChild("tuft_l", CubeListBuilder.create().texOffs(24, 0).addBox(-4.7f, -5.8f, -2.4f, 1.1f, 3.0f, 4.4f), PartPose.ZERO);
        head.addOrReplaceChild("tuft_r", CubeListBuilder.create().texOffs(24, 0).addBox(3.6f, -5.8f, -2.4f, 1.1f, 3.0f, 4.4f), PartPose.ZERO);
        head.addOrReplaceChild("tuft_back", CubeListBuilder.create().texOffs(24, 8).addBox(-3.4f, -4.8f, 3.6f, 6.8f, 2.4f, 1.1f), PartPose.ZERO);
        return LayerDefinition.create(mesh, 64, 64);
    }

    static LayerDefinition bust(MeshDefinition mesh, float inflate) {
        PartDefinition body = mesh.getRoot().getChild("body");
        body.addOrReplaceChild("bust", CubeListBuilder.create().texOffs(19, 19)
                .addBox(-3.2f, 1.2f, -3.9f, 6.4f, 3.6f, 2.0f, new net.minecraft.client.model.geom.builders.CubeDeformation(inflate)), PartPose.rotation(-0.1f, 0.0f, 0.0f));
        return LayerDefinition.create(mesh, 64, 64);
    }

    public static LayerDefinition catFeatures() {
        MeshDefinition mesh = emptyPlayerMesh();
        PartDefinition head = mesh.getRoot().getChild("head");
        head.addOrReplaceChild("ear_r", CubeListBuilder.create().texOffs(0, 0).addBox(-4.3f, -10.4f, -1.2f, 2.8f, 3.2f, 1.4f)
                .texOffs(0, 0).addBox(-3.6f, -11.4f, -1.0f, 1.4f, 1.2f, 1.0f), PartPose.rotation(0.0f, 0.0f, -0.2f));
        head.addOrReplaceChild("ear_l", CubeListBuilder.create().texOffs(0, 0).addBox(1.5f, -10.4f, -1.2f, 2.8f, 3.2f, 1.4f)
                .texOffs(0, 0).addBox(2.2f, -11.4f, -1.0f, 1.4f, 1.2f, 1.0f), PartPose.rotation(0.0f, 0.0f, 0.2f));
        head.addOrReplaceChild("muzzle", CubeListBuilder.create().texOffs(0, 8).addBox(-2.2f, -3.4f, -4.9f, 4.4f, 2.4f, 1.0f), PartPose.ZERO);
        head.addOrReplaceChild("cheek_r", CubeListBuilder.create().texOffs(0, 8).addBox(-4.6f, -3.6f, -3.6f, 0.8f, 2.4f, 2.4f), PartPose.ZERO);
        head.addOrReplaceChild("cheek_l", CubeListBuilder.create().texOffs(0, 8).addBox(3.8f, -3.6f, -3.6f, 0.8f, 2.4f, 2.4f), PartPose.ZERO);
        return LayerDefinition.create(mesh, 64, 64);
    }

    public static LayerDefinition catNose() {
        MeshDefinition mesh = emptyPlayerMesh();
        PartDefinition head = mesh.getRoot().getChild("head");
        head.addOrReplaceChild("nose", CubeListBuilder.create().texOffs(0, 0).addBox(-0.7f, -3.8f, -5.2f, 1.4f, 0.9f, 0.5f), PartPose.ZERO);
        head.addOrReplaceChild("inner_ear_r", CubeListBuilder.create().texOffs(0, 0).addBox(-3.7f, -9.8f, -1.35f, 1.6f, 2.0f, 0.2f), PartPose.rotation(0.0f, 0.0f, -0.2f));
        head.addOrReplaceChild("inner_ear_l", CubeListBuilder.create().texOffs(0, 0).addBox(2.1f, -9.8f, -1.35f, 1.6f, 2.0f, 0.2f), PartPose.rotation(0.0f, 0.0f, 0.2f));
        return LayerDefinition.create(mesh, 64, 64);
    }

    public static MeshDefinition feature(Race race, FeatureTint tint) {
        MeshDefinition mesh = emptyPlayerMesh();
        PartDefinition head = mesh.getRoot().getChild("head");
        Builder b = new Builder(head);
        boolean any = true;
        switch (race) {
            case NAMEKIAN -> {
                if (tint == FeatureTint.SKIN) {
                    b.antenna(-1.6f, -7.8f, -3.0f, 0.55f, -0.35f, 6.0f, 0.75f);
                    b.antenna(1.6f, -7.8f, -3.0f, 0.55f, 0.35f, 6.0f, 0.75f);
                    b.ear(-1, 3.6f);
                    b.ear(1, 3.6f);
                } else {
                    any = false;
                }
            }
            case FRIEZA_RACE -> {
                PartDefinition root = mesh.getRoot();
                switch (tint) {
                    case PLATE -> {
                        head.addOrReplaceChild("dome", CubeListBuilder.create().texOffs(0, 0)
                                .addBox(-4.4f, -8.7f, -4.45f, 8.8f, 3.3f, 8.9f)
                                .texOffs(0, 14).addBox(-3.7f, -9.3f, -3.7f, 7.4f, 0.7f, 7.4f)
                                .texOffs(0, 24).addBox(-4.45f, -5.5f, 1.0f, 8.9f, 3.4f, 3.5f), PartPose.ZERO);
                        head.addOrReplaceChild("brow", CubeListBuilder.create().texOffs(0, 34)
                                .addBox(-4.45f, -5.6f, -4.5f, 8.9f, 0.7f, 0.6f), PartPose.ZERO);
                        head.addOrReplaceChild("cheek_l", CubeListBuilder.create().texOffs(32, 24)
                                .addBox(-4.55f, -5.5f, -3.6f, 0.6f, 2.6f, 4.8f), PartPose.ZERO);
                        head.addOrReplaceChild("cheek_r", CubeListBuilder.create().texOffs(32, 24)
                                .addBox(3.95f, -5.5f, -3.6f, 0.6f, 2.6f, 4.8f), PartPose.ZERO);
                    }
                    case GEM -> {
                        head.addOrReplaceChild("crown_gem", CubeListBuilder.create().texOffs(0, 0)
                                .addBox(-1.7f, -9.75f, -2.6f, 3.4f, 0.6f, 4.4f)
                                .texOffs(0, 8).addBox(-1.1f, -10.05f, -1.9f, 2.2f, 0.4f, 3.0f), PartPose.ZERO);
                        head.addOrReplaceChild("side_gem_l", CubeListBuilder.create().texOffs(16, 0)
                                .addBox(-4.75f, -7.6f, -1.2f, 0.4f, 1.6f, 2.4f), PartPose.ZERO);
                        head.addOrReplaceChild("side_gem_r", CubeListBuilder.create().texOffs(16, 0)
                                .addBox(4.35f, -7.6f, -1.2f, 0.4f, 1.6f, 2.4f), PartPose.ZERO);
                    }
                    case HORN -> {
                        b.curved(-4.2f, -6.8f, 0.4f, 0.0f, 0.0f, -1.35f, 8.0f, 2.3f, 0.42f, 0.0f);
                        b.curved(4.2f, -6.8f, 0.4f, 0.0f, 0.0f, 1.35f, 8.0f, 2.3f, -0.42f, 0.0f);
                    }
                    case BODY_PLATE -> {
                        root.getChild("body").addOrReplaceChild("chest", CubeListBuilder.create().texOffs(0, 0)
                                .addBox(-4.3f, -0.3f, -2.5f, 8.6f, 6.2f, 0.7f)
                                .texOffs(0, 8).addBox(-4.3f, -0.3f, 1.8f, 8.6f, 6.2f, 0.7f)
                                .texOffs(0, 16).addBox(-4.3f, -0.4f, -2.5f, 8.6f, 0.8f, 5.0f), PartPose.ZERO);
                        root.getChild("right_arm").addOrReplaceChild("shoulder", CubeListBuilder.create().texOffs(0, 24)
                                .addBox(-3.6f, -2.6f, -2.5f, 4.2f, 2.4f, 5.0f)
                                .texOffs(0, 32).addBox(-3.75f, -0.4f, -2.4f, 0.6f, 2.2f, 4.8f), PartPose.rotation(0.0f, 0.0f, 0.12f));
                        root.getChild("left_arm").addOrReplaceChild("shoulder", CubeListBuilder.create().texOffs(0, 24).mirror()
                                .addBox(-0.6f, -2.6f, -2.5f, 4.2f, 2.4f, 5.0f)
                                .texOffs(0, 32).addBox(3.15f, -0.4f, -2.4f, 0.6f, 2.2f, 4.8f), PartPose.rotation(0.0f, 0.0f, -0.12f));
                        root.getChild("right_leg").addOrReplaceChild("shin", CubeListBuilder.create().texOffs(0, 40)
                                .addBox(-2.2f, 5.5f, -2.45f, 4.4f, 5.0f, 0.6f), PartPose.ZERO);
                        root.getChild("left_leg").addOrReplaceChild("shin", CubeListBuilder.create().texOffs(0, 40)
                                .addBox(-2.2f, 5.5f, -2.45f, 4.4f, 5.0f, 0.6f), PartPose.ZERO);
                    }
                    case BODY_GEM -> {
                        root.getChild("body").addOrReplaceChild("chest_gem", CubeListBuilder.create().texOffs(0, 0)
                                .addBox(-1.2f, 1.2f, -2.75f, 2.4f, 1.8f, 0.4f), PartPose.ZERO);
                        root.getChild("right_arm").addOrReplaceChild("shoulder_gem", CubeListBuilder.create().texOffs(0, 8)
                                .addBox(-2.7f, -2.95f, -1.4f, 2.4f, 0.5f, 2.8f), PartPose.rotation(0.0f, 0.0f, 0.12f));
                        root.getChild("left_arm").addOrReplaceChild("shoulder_gem", CubeListBuilder.create().texOffs(0, 8)
                                .addBox(0.3f, -2.95f, -1.4f, 2.4f, 0.5f, 2.8f), PartPose.rotation(0.0f, 0.0f, -0.12f));
                        root.getChild("right_leg").addOrReplaceChild("knee_gem", CubeListBuilder.create().texOffs(0, 16)
                                .addBox(-0.9f, 4.0f, -2.6f, 1.8f, 1.4f, 0.4f), PartPose.ZERO);
                        root.getChild("left_leg").addOrReplaceChild("knee_gem", CubeListBuilder.create().texOffs(0, 16)
                                .addBox(-0.9f, 4.0f, -2.6f, 1.8f, 1.4f, 0.4f), PartPose.ZERO);
                    }
                    default -> any = false;
                }
            }
            case MAJIN -> {
                if (tint == FeatureTint.SKIN) {
                    b.curved(0.0f, -7.9f, 0.6f, -0.25f, 0.0f, 0.0f, 9.5f, 2.6f, 0.0f, -0.38f);
                    b.ear(-1, 1.6f);
                    b.ear(1, 1.6f);
                } else {
                    any = false;
                }
            }
            case BIO_ANDROID -> {
                PartDefinition root = mesh.getRoot();
                switch (tint) {
                    case PLATE -> {
                        for (int side = -1; side <= 1; side += 2) {
                            PartDefinition crest = head.addOrReplaceChild("crest_" + (side < 0 ? "l" : "r"), CubeListBuilder.create().texOffs(0, 0)
                                            .addBox(-0.7f, -7.5f, -1.9f, 1.4f, 7.5f, 3.8f)
                                            .texOffs(12, 0).addBox(-0.5f, -6.5f, 1.9f, 1.0f, 5.5f, 1.2f),
                                    PartPose.offsetAndRotation(side * 2.6f, -7.6f, -2.2f, -0.12f, 0.0f, side * 0.22f));
                            PartDefinition mid = crest.addOrReplaceChild("mid", CubeListBuilder.create().texOffs(0, 12)
                                    .addBox(-0.55f, -5.0f, -1.4f, 1.1f, 5.2f, 2.8f), PartPose.offsetAndRotation(0.0f, -7.3f, 0.0f, -0.32f, 0.0f, side * 0.1f));
                            mid.addOrReplaceChild("tip", CubeListBuilder.create().texOffs(0, 22)
                                    .addBox(-0.35f, -3.4f, -0.8f, 0.7f, 3.6f, 1.6f), PartPose.offsetAndRotation(0.0f, -4.9f, 0.0f, -0.45f, 0.0f, 0.0f));
                        }
                        head.addOrReplaceChild("crown", CubeListBuilder.create().texOffs(0, 30)
                                .addBox(-4.45f, -8.6f, -4.5f, 8.9f, 1.4f, 8.9f)
                                .texOffs(0, 42).addBox(-4.5f, -6.6f, -4.6f, 9.0f, 1.0f, 0.7f)
                                .texOffs(0, 46).addBox(-1.0f, -9.2f, -4.0f, 2.0f, 0.8f, 7.0f), PartPose.ZERO);
                        head.addOrReplaceChild("jaw_l", CubeListBuilder.create().texOffs(32, 30)
                                .addBox(-4.6f, -4.6f, -4.0f, 0.6f, 3.6f, 3.0f), PartPose.ZERO);
                        head.addOrReplaceChild("jaw_r", CubeListBuilder.create().texOffs(32, 30)
                                .addBox(4.0f, -4.6f, -4.0f, 0.6f, 3.6f, 3.0f), PartPose.ZERO);
                    }
                    case BODY_PLATE -> {
                        PartDefinition body = root.getChild("body");
                        body.addOrReplaceChild("carapace", CubeListBuilder.create().texOffs(0, 0)
                                .addBox(-4.3f, -0.2f, -2.55f, 8.6f, 5.4f, 0.7f)
                                .texOffs(0, 8).addBox(-3.6f, 5.6f, -2.5f, 7.2f, 1.6f, 0.6f)
                                .addBox(-3.3f, 7.5f, -2.5f, 6.6f, 1.6f, 0.6f)
                                .addBox(-3.0f, 9.4f, -2.5f, 6.0f, 1.6f, 0.6f)
                                .texOffs(0, 16).addBox(-4.3f, -0.3f, 1.85f, 8.6f, 4.0f, 0.7f), PartPose.ZERO);
                        for (int side = -1; side <= 1; side += 2) {
                            body.addOrReplaceChild("wing_" + (side < 0 ? "l" : "r"), CubeListBuilder.create().texOffs(0, 24)
                                            .addBox(side < 0 ? -3.8f : 0.0f, 0.0f, 0.0f, 3.8f, 10.5f, 0.6f)
                                            .texOffs(12, 24).addBox(side < 0 ? -2.6f : 0.6f, 10.5f, 0.0f, 2.0f, 2.2f, 0.6f),
                                    PartPose.offsetAndRotation(side * 0.4f, 0.6f, 2.55f, 0.22f, side * -0.18f, side * -0.16f));
                        }
                        root.getChild("right_arm").addOrReplaceChild("shell", CubeListBuilder.create().texOffs(0, 40)
                                .addBox(-3.6f, -2.6f, -2.5f, 4.4f, 2.0f, 5.0f)
                                .texOffs(0, 48).addBox(-3.55f, 6.0f, -2.45f, 0.6f, 3.6f, 4.9f), PartPose.rotation(0.0f, 0.0f, 0.1f));
                        root.getChild("left_arm").addOrReplaceChild("shell", CubeListBuilder.create().texOffs(0, 40).mirror()
                                .addBox(-0.8f, -2.6f, -2.5f, 4.4f, 2.0f, 5.0f)
                                .texOffs(0, 48).addBox(2.95f, 6.0f, -2.45f, 0.6f, 3.6f, 4.9f), PartPose.rotation(0.0f, 0.0f, -0.1f));
                        root.getChild("right_leg").addOrReplaceChild("shell", CubeListBuilder.create().texOffs(0, 56)
                                .addBox(-2.2f, 4.0f, -2.45f, 4.4f, 3.0f, 0.6f), PartPose.ZERO);
                        root.getChild("left_leg").addOrReplaceChild("shell", CubeListBuilder.create().texOffs(0, 56)
                                .addBox(-2.2f, 4.0f, -2.45f, 4.4f, 3.0f, 0.6f), PartPose.ZERO);
                    }
                    default -> any = false;
                }
            }
            case ANDROID -> {
                if (tint == FeatureTint.ACCENT) {
                    head.addOrReplaceChild("ear_piece_l", CubeListBuilder.create().texOffs(0, 0).addBox(-4.8f, -4.8f, -1.2f, 0.9f, 2.4f, 2.4f)
                            .texOffs(8, 0).addBox(-5.1f, -4.3f, -0.7f, 0.4f, 1.4f, 1.4f), PartPose.ZERO);
                    head.addOrReplaceChild("ear_piece_r", CubeListBuilder.create().texOffs(0, 0).addBox(3.9f, -4.8f, -1.2f, 0.9f, 2.4f, 2.4f)
                            .texOffs(8, 0).addBox(4.7f, -4.3f, -0.7f, 0.4f, 1.4f, 1.4f), PartPose.ZERO);
                } else {
                    any = false;
                }
            }
            case KAIOSHIN -> {
                if (tint == FeatureTint.SKIN) {
                    b.ear(-1, 3.0f);
                    b.ear(1, 3.0f);
                } else if (tint == FeatureTint.ACCENT) {
                    head.addOrReplaceChild("earring_l", CubeListBuilder.create().texOffs(0, 0).addBox(-5.6f, -2.4f, -0.6f, 1.2f, 1.6f, 1.2f), PartPose.ZERO);
                    head.addOrReplaceChild("earring_r", CubeListBuilder.create().texOffs(0, 0).addBox(4.4f, -2.4f, -0.6f, 1.2f, 1.6f, 1.2f), PartPose.ZERO);
                } else {
                    any = false;
                }
            }
            case DEMON -> {
                if (tint == FeatureTint.SKIN) {
                    b.ear(-1, 2.8f);
                    b.ear(1, 2.8f);
                } else if (tint == FeatureTint.HORN) {
                    b.curved(-2.4f, -7.9f, -2.6f, -0.35f, 0.0f, 0.42f, 6.5f, 1.8f, -0.36f, 0.0f);
                    b.curved(2.4f, -7.9f, -2.6f, -0.35f, 0.0f, -0.42f, 6.5f, 1.8f, 0.36f, 0.0f);
                } else {
                    any = false;
                }
            }
            default -> any = false;
        }
        return any ? mesh : null;
    }

    private static final class Builder {
        private final PartDefinition head;
        private PartDefinition target;
        private int counter;

        Builder(PartDefinition head) {
            this.head = head;
            this.target = head;
        }

        void group(String name, float x, float y, float z) {
            target = head.addOrReplaceChild(name, CubeListBuilder.create(), PartPose.offset(x, y, z));
        }

        void endGroup() {
            target = head;
        }

        void cap(boolean volume) {
            float lift = volume ? 0.9f : 0.5f;
            head.addOrReplaceChild("cap", CubeListBuilder.create().texOffs(0, 0)
                    .addBox(-4.55f, -8.0f - lift, -4.55f, 9.1f, 3.0f + lift, 9.1f)
                    .texOffs(0, 12)
                    .addBox(-4.55f, -5.0f, 2.2f, 9.1f, 4.6f, 2.35f)
                    .addBox(-4.6f, -5.2f, -2.8f, 1.1f, 3.4f, 5.4f)
                    .addBox(3.5f, -5.2f, -2.8f, 1.1f, 3.4f, 5.4f)
                    .texOffs(32, 12)
                    .addBox(-4.65f, -6.0f, -4.65f, 1.0f, 2.0f, 1.6f)
                    .addBox(3.65f, -6.0f, -4.65f, 1.0f, 2.0f, 1.6f), PartPose.ZERO);
        }

        void slab(String name, float x, float y, float z, float w, float h, float d, float tilt) {
            target.addOrReplaceChild(name + "_" + counter++, CubeListBuilder.create().texOffs(0, 0).addBox(0.0f, 0.0f, 0.0f, w, h, d),
                    PartPose.offsetAndRotation(x, y, z, tilt, 0.0f, 0.0f));
        }

        void spike(float x, float y, float z, float xRot, float yRot, float zRot, float length, float base) {
            float bend = 0.08f + Math.min(0.1f, length * 0.008f);
            curved(x, y, z, xRot, yRot, zRot, length, base, 0.0f, bend);
        }

        void curved(float x, float y, float z, float xRot, float yRot, float zRot, float length, float base, float bendZ, float bendX) {
            float[] share = {0.34f, 0.28f, 0.22f, 0.16f};
            float[] width = {1.0f, 0.76f, 0.52f, 0.26f};
            PartDefinition parent = target;
            String name = "spike_" + counter++;
            for (int i = 0; i < share.length; i++) {
                float len = length * share[i];
                float w = base * width[i];
                float overlap = i == 0 ? 0.0f : 0.3f;
                CubeListBuilder cube = CubeListBuilder.create().texOffs(i * 14, i * 8).addBox(-w / 2.0f, -len, -w / 2.0f, w, len + overlap, w);
                PartPose pose = i == 0 ? PartPose.offsetAndRotation(x, y, z, xRot, yRot, zRot)
                        : PartPose.offsetAndRotation(0.0f, -length * share[i - 1], 0.0f, bendX, 0.0f, bendZ);
                parent = parent.addOrReplaceChild(name + "_" + i, cube, pose);
            }
        }

        void antenna(float x, float y, float z, float xRot, float zRot, float length, float thickness) {
            PartDefinition parent = head;
            String name = "antenna_" + counter++;
            int segments = 3;
            float len = length / segments;
            for (int i = 0; i < segments; i++) {
                CubeListBuilder cube = CubeListBuilder.create().texOffs(0, i * 4).addBox(-thickness / 2.0f, -len, -thickness / 2.0f, thickness, len + 0.1f, thickness);
                if (i == segments - 1) {
                    cube.texOffs(8, 0).addBox(-thickness, -len - thickness * 1.6f, -thickness, thickness * 2.0f, thickness * 1.8f, thickness * 2.0f);
                }
                PartPose pose = i == 0 ? PartPose.offsetAndRotation(x, y, z, xRot, 0.0f, zRot) : PartPose.offsetAndRotation(0.0f, -len, 0.0f, -0.32f, 0.0f, 0.0f);
                parent = parent.addOrReplaceChild(name + "_" + i, cube, pose);
            }
        }

        void ear(int side, float length) {
            float x = side < 0 ? -4.0f : 4.0f;
            PartDefinition ear = head.addOrReplaceChild("ear_" + counter++, CubeListBuilder.create().texOffs(0, 48)
                    .addBox(side < 0 ? -1.6f : 0.0f, -1.6f, -0.4f, 1.6f, 2.6f, 0.8f), PartPose.offsetAndRotation(x, -4.2f, 0.2f, 0.0f, side * 0.25f, side * -0.35f));
            ear.addOrReplaceChild("tip", CubeListBuilder.create().texOffs(8, 48)
                    .addBox(side < 0 ? -length : 0.0f, -0.9f, -0.3f, length, 1.4f, 0.6f),
                    PartPose.offsetAndRotation(side < 0 ? -1.4f : 1.4f, -0.8f, 0.0f, 0.0f, 0.0f, side * -0.5f));
        }

        void crest(int side, float spread) {
            PartDefinition root = head.addOrReplaceChild("crest_" + counter++, CubeListBuilder.create().texOffs(0, 24)
                    .addBox(-0.6f, -6.5f, -1.4f, 1.2f, 6.5f, 3.2f), PartPose.offsetAndRotation(side * 2.8f, -7.6f, -2.0f, -0.55f, 0.0f, side * spread));
            root.addOrReplaceChild("tip", CubeListBuilder.create().texOffs(10, 24)
                    .addBox(-0.45f, -4.0f, -1.0f, 0.9f, 4.0f, 2.0f), PartPose.offsetAndRotation(0.0f, -6.3f, 0.0f, -0.45f, 0.0f, side * 0.18f));
        }
    }
}
