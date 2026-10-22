package com.dbtenkaichi.client.render;

import com.dbtenkaichi.Tenkaichi;
import com.dbtenkaichi.client.EntityVisual;
import com.dbtenkaichi.player.Appearance;
import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.player.PlayerModel;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.Identifier;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Whole-body shapes of Majin Buu: the fat base body and the Evil, Super and Kid forms. Each body is a set of boxes
 * attached to the player model parts (the plain limbs are hidden while a body is shown) and painted with a generated
 * texture that follows the skin color and the outfit colors.
 */
public enum MajinBody {
    FAT(1.0f),
    EVIL(1.0f),
    SUPER(1.0f),
    KID(1.16f);

    private static final int TEX = 128;
    private static final int S = 2;
    private static final int MAX_CACHED = 24;
    private static final Map<Long, Identifier> TEXTURES = new LinkedHashMap<>(16, 0.75f, true);
    private static int counter;

    public final float headScale;
    public final ModelLayerLocation layer;
    private @Nullable List<Piece> pieces;

    MajinBody(float headScale) {
        this.headScale = headScale;
        this.layer = new ModelLayerLocation(Tenkaichi.id("majin_body_" + id()), "main");
    }

    public String id() {
        return name().toLowerCase(Locale.ROOT);
    }

    public static @Nullable MajinBody byId(@Nullable String id) {
        if (id == null) {
            return null;
        }
        for (MajinBody body : values()) {
            if (body.id().equals(id)) {
                return body;
            }
        }
        return null;
    }

    enum Mat {
        SKIN, SKIN_CHEST, SKIN_ABS, VEST, VEST_OPEN, PANTS, BELT, BUCKLE, GLOVE, BOOT, BOOT_BAND, SHOE, CAPE, ARMBAND
    }

    record Piece(String part, String name, float x, float y, float z, float w, float h, float d, Mat mat,
                 float ox, float oy, float oz, float rx, float ry, float rz, int u, int v) {
        Piece at(int u, int v) {
            return new Piece(part, name, x, y, z, w, h, d, mat, ox, oy, oz, rx, ry, rz, u, v);
        }
    }

    private static final class Spec {
        final List<Piece> list = new ArrayList<>();

        Spec body(String name, float x, float y, float z, float w, float h, float d, Mat mat) {
            return posed("body", name, x, y, z, w, h, d, mat, 0, 0, 0, 0, 0, 0);
        }

        Spec posed(String part, String name, float x, float y, float z, float w, float h, float d, Mat mat,
                   float ox, float oy, float oz, float rx, float ry, float rz) {
            list.add(new Piece(part, name, x, y, z, w, h, d, mat, ox, oy, oz, rx, ry, rz, 0, 0));
            return this;
        }

        /** Adds a piece to the right limb and its mirror image to the left one. */
        Spec limb(String limb, String name, float x, float y, float z, float w, float h, float d, Mat mat, float ox) {
            posed("right_" + limb, name, x, y, z, w, h, d, mat, ox, 0, 0, 0, 0, 0);
            posed("left_" + limb, name, -(x + w), y, z, w, h, d, mat, -ox, 0, 0, 0, 0, 0);
            return this;
        }
    }

    List<Piece> pieces() {
        if (pieces == null) {
            pieces = pack(spec().list);
        }
        return pieces;
    }

    private Spec spec() {
        Spec s = new Spec();
        switch (this) {
            case FAT -> {
                s.body("chest", -5.4f, -0.4f, -3.4f, 10.8f, 4.2f, 6.6f, Mat.VEST_OPEN)
                        .body("belly", -6.9f, 3.4f, -4.6f, 13.8f, 7.0f, 8.4f, Mat.VEST_OPEN)
                        .body("belly_front", -5.4f, 1.6f, -5.4f, 10.8f, 9.4f, 1.0f, Mat.SKIN)
                        .body("belly_peak", -4.2f, 3.4f, -6.2f, 8.4f, 6.4f, 0.9f, Mat.SKIN_ABS)
                        .body("belt", -6.5f, 9.9f, -5.0f, 13.0f, 1.6f, 8.9f, Mat.BELT)
                        .body("buckle", -1.5f, 10.0f, -5.7f, 3.0f, 1.8f, 0.8f, Mat.BUCKLE)
                        .body("seat", -6.2f, 10.6f, -4.3f, 12.4f, 4.6f, 7.6f, Mat.PANTS)
                        .body("collar", -5.8f, -0.9f, -2.0f, 11.6f, 1.2f, 6.0f, Mat.CAPE)
                        .posed("body", "cape", -5.0f, -0.2f, 0.0f, 10.0f, 17.5f, 0.5f, Mat.CAPE, 0, 0, 4.0f, 0.08f, 0, 0);
                s.limb("arm", "shoulder", -3.6f, -2.6f, -2.9f, 5.4f, 4.2f, 5.8f, Mat.VEST, -1.6f)
                        .limb("arm", "upper", -3.3f, 1.4f, -2.6f, 4.8f, 5.0f, 5.2f, Mat.SKIN, -1.6f)
                        .limb("arm", "forearm", -3.0f, 6.2f, -2.4f, 4.2f, 2.4f, 4.8f, Mat.SKIN, -1.6f)
                        .limb("arm", "cuff", -3.4f, 7.6f, -2.7f, 5.0f, 1.4f, 5.4f, Mat.GLOVE, -1.6f)
                        .limb("arm", "glove", -3.1f, 8.8f, -2.4f, 4.4f, 3.2f, 4.8f, Mat.GLOVE, -1.6f);
                s.limb("leg", "thigh", -3.4f, 1.0f, -3.5f, 5.3f, 6.0f, 7.0f, Mat.PANTS, 0)
                        .limb("leg", "calf", -2.9f, 6.5f, -3.0f, 4.8f, 2.2f, 6.0f, Mat.PANTS, 0)
                        .limb("leg", "boot_top", -2.7f, 8.2f, -2.8f, 4.6f, 1.3f, 5.4f, Mat.BOOT, 0)
                        .limb("leg", "boot", -2.3f, 9.3f, -2.6f, 3.8f, 2.7f, 4.6f, Mat.BOOT, 0)
                        .limb("leg", "toe", -2.3f, 10.6f, -3.6f, 3.8f, 1.4f, 1.2f, Mat.BOOT, 0);
            }
            case EVIL -> {
                s.body("chest", -3.4f, -0.3f, -1.9f, 6.8f, 5.6f, 3.8f, Mat.VEST_OPEN)
                        .body("waist", -3.0f, 5.1f, -1.7f, 6.0f, 4.4f, 3.4f, Mat.VEST_OPEN)
                        .body("belt", -3.3f, 9.2f, -1.9f, 6.6f, 1.4f, 3.8f, Mat.BELT)
                        .body("buckle", -1.2f, 9.1f, -2.4f, 2.4f, 1.6f, 0.6f, Mat.BUCKLE)
                        .body("seat", -3.6f, 10.4f, -2.3f, 7.2f, 3.6f, 4.6f, Mat.PANTS)
                        .body("collar", -4.3f, -0.7f, -1.6f, 8.6f, 1.0f, 4.2f, Mat.CAPE)
                        .posed("body", "cape", -3.6f, -0.2f, 0.0f, 7.2f, 18.5f, 0.4f, Mat.CAPE, 0, 0, 2.1f, 0.1f, 0, 0);
                s.limb("arm", "shoulder", -2.4f, -2.3f, -1.6f, 3.2f, 2.6f, 3.2f, Mat.VEST, 1.2f)
                        .limb("arm", "upper", -2.1f, 0.3f, -1.3f, 2.6f, 5.4f, 2.6f, Mat.SKIN, 1.2f)
                        .limb("arm", "forearm", -2.0f, 5.5f, -1.2f, 2.4f, 2.5f, 2.4f, Mat.SKIN, 1.2f)
                        .limb("arm", "cuff", -2.6f, 7.6f, -1.8f, 3.6f, 1.2f, 3.6f, Mat.GLOVE, 1.2f)
                        .limb("arm", "glove", -2.3f, 8.8f, -1.5f, 3.0f, 2.8f, 3.0f, Mat.GLOVE, 1.2f);
                s.limb("leg", "thigh", -2.6f, 0.5f, -2.4f, 4.4f, 6.5f, 4.8f, Mat.PANTS, 0)
                        .limb("leg", "calf", -2.2f, 6.8f, -2.1f, 3.8f, 2.2f, 4.2f, Mat.PANTS, 0)
                        .limb("leg", "boot_top", -1.9f, 8.6f, -1.9f, 3.4f, 1.2f, 3.6f, Mat.BOOT, 0)
                        .limb("leg", "boot", -1.6f, 9.6f, -1.7f, 2.8f, 2.4f, 3.2f, Mat.BOOT, 0)
                        .limb("leg", "toe", -1.6f, 10.8f, -2.6f, 2.8f, 1.2f, 1.0f, Mat.BOOT, 0);
            }
            case SUPER -> {
                s.body("chest", -5.0f, -0.6f, -2.9f, 10.0f, 5.4f, 5.6f, Mat.SKIN_CHEST)
                        .body("traps", -3.2f, -1.3f, -1.6f, 6.4f, 1.2f, 3.6f, Mat.SKIN)
                        .body("abs", -3.9f, 4.6f, -2.4f, 7.8f, 4.9f, 4.6f, Mat.SKIN_ABS)
                        .body("belt", -4.2f, 9.2f, -2.6f, 8.4f, 1.5f, 5.2f, Mat.BELT)
                        .body("buckle", -1.3f, 9.1f, -3.1f, 2.6f, 1.7f, 0.6f, Mat.BUCKLE)
                        .body("seat", -4.6f, 10.5f, -2.9f, 9.2f, 4.0f, 5.8f, Mat.PANTS);
                s.limb("arm", "delt", -3.6f, -2.6f, -2.4f, 4.8f, 3.8f, 4.8f, Mat.SKIN, -0.6f)
                        .limb("arm", "bicep", -3.2f, 1.0f, -2.0f, 4.2f, 4.4f, 4.0f, Mat.SKIN, -0.6f)
                        .limb("arm", "armband", -3.4f, 5.2f, -2.2f, 4.6f, 3.8f, 4.4f, Mat.ARMBAND, -0.6f)
                        .limb("arm", "hand", -2.9f, 9.0f, -1.7f, 3.6f, 2.8f, 3.4f, Mat.SKIN, -0.6f);
                s.limb("leg", "thigh", -3.0f, 0.4f, -3.0f, 5.0f, 6.6f, 6.0f, Mat.PANTS, 0)
                        .limb("leg", "calf", -2.6f, 6.8f, -2.6f, 4.4f, 1.8f, 5.2f, Mat.PANTS, 0)
                        .limb("leg", "boot", -2.2f, 8.3f, -2.2f, 3.8f, 2.6f, 4.4f, Mat.BOOT_BAND, 0)
                        .limb("leg", "shoe", -2.3f, 10.8f, -3.1f, 4.0f, 1.2f, 5.4f, Mat.SHOE, 0);
            }
            case KID -> {
                s.body("chest", -3.3f, -0.2f, -1.9f, 6.6f, 5.0f, 3.8f, Mat.SKIN_CHEST)
                        .body("tummy", -3.0f, 4.6f, -1.8f, 6.0f, 4.6f, 3.6f, Mat.SKIN_ABS)
                        .body("belt", -3.3f, 8.9f, -2.0f, 6.6f, 1.4f, 4.0f, Mat.BELT)
                        .body("buckle", -1.1f, 8.8f, -2.5f, 2.2f, 1.6f, 0.6f, Mat.BUCKLE)
                        .body("seat", -3.7f, 10.1f, -2.4f, 7.4f, 3.8f, 4.8f, Mat.PANTS);
                s.limb("arm", "shoulder", -2.3f, -2.1f, -1.5f, 3.0f, 3.0f, 3.0f, Mat.SKIN, 1.0f)
                        .limb("arm", "upper", -2.0f, 0.8f, -1.2f, 2.4f, 4.0f, 2.4f, Mat.SKIN, 1.0f)
                        .limb("arm", "armband", -2.4f, 4.6f, -1.6f, 3.2f, 3.6f, 3.2f, Mat.ARMBAND, 1.0f)
                        .limb("arm", "hand", -2.1f, 8.1f, -1.3f, 2.6f, 2.6f, 2.6f, Mat.SKIN, 1.0f);
                s.limb("leg", "thigh", -2.6f, 0.4f, -2.5f, 4.4f, 6.8f, 5.0f, Mat.PANTS, 0)
                        .limb("leg", "calf", -2.3f, 7.0f, -2.2f, 3.8f, 1.6f, 4.4f, Mat.PANTS, 0)
                        .limb("leg", "boot", -1.8f, 8.4f, -1.8f, 3.2f, 2.5f, 3.6f, Mat.BOOT_BAND, 0)
                        .limb("leg", "shoe", -1.9f, 10.8f, -2.6f, 3.4f, 1.2f, 4.6f, Mat.SHOE, 0);
            }
        }
        return s;
    }

    /** Shelf-packs every box's UV net into the texture. */
    private static List<Piece> pack(List<Piece> raw) {
        List<Piece> out = new ArrayList<>();
        int x = 0;
        int y = 0;
        int row = 0;
        for (Piece p : raw) {
            int fw = (int) Math.ceil(2 * (p.d() + p.w())) + 1;
            int fh = (int) Math.ceil(p.d() + p.h()) + 1;
            if (x + fw > TEX) {
                x = 0;
                y += row;
                row = 0;
            }
            if (y + fh > TEX) {
                throw new IllegalStateException("Majin body texture is full");
            }
            out.add(p.at(x, y));
            x += fw;
            row = Math.max(row, fh);
        }
        return out;
    }

    public LayerDefinition createLayer() {
        MeshDefinition mesh = PlayerModel.createMesh(CubeDeformation.NONE, false);
        mesh.getRoot().clearRecursively();
        int index = 0;
        for (Piece p : pieces()) {
            mesh.getRoot().getChild(p.part()).addOrReplaceChild("majin_" + p.name() + "_" + index++,
                    CubeListBuilder.create().texOffs(p.u(), p.v()).addBox(p.x(), p.y(), p.z(), p.w(), p.h(), p.d()),
                    PartPose.offsetAndRotation(p.ox(), p.oy(), p.oz(), p.rx(), p.ry(), p.rz()));
        }
        return LayerDefinition.create(mesh, TEX, TEX);
    }

    public static void applyScale(PoseStack poseStack, EntityVisual visual) {
        float grow = com.dbtenkaichi.client.cinematic.CinematicDirector.grow(visual, com.dbtenkaichi.client.cinematic.CinematicDirector.partial());
        float h = visual.modelHeight() * grow;
        float w = visual.modelWidth() * grow;
        if (h != 1.0f || w != 1.0f) {
            poseStack.scale(w, h, w);
        }
    }

    public Identifier texture(EntityVisual visual) {
        Palette palette = Palette.of(this, visual);
        long key = ordinal() * 31L + palette.hash();
        Identifier existing = TEXTURES.get(key);
        if (existing != null) {
            return existing;
        }
        NativeImage image = new Painter(this, palette).paint();
        Identifier id = Tenkaichi.id("dynamic/majin_" + id() + "_" + (counter++));
        Minecraft.getInstance().getTextureManager().register(id, new DynamicTexture(id::toString, image));
        TEXTURES.put(key, id);
        if (TEXTURES.size() > MAX_CACHED) {
            Map.Entry<Long, Identifier> eldest = TEXTURES.entrySet().iterator().next();
            TEXTURES.remove(eldest.getKey());
            Minecraft.getInstance().getTextureManager().release(eldest.getValue());
        }
        return id;
    }

    public static void clear() {
        for (Identifier id : TEXTURES.values()) {
            Minecraft.getInstance().getTextureManager().release(id);
        }
        TEXTURES.clear();
    }

    record Palette(int skin, int vest, int pants, int cape, int boots, boolean natural) {
        static final int TRIM = 0xE2B33A;
        static final int YELLOW = 0xF0C43A;
        static final int BELT = 0x1C1A20;
        static final int SHOE = 0x857A6E;

        static Palette of(MajinBody body, EntityVisual visual) {
            Appearance a = visual.appearance;
            boolean natural = body == FAT && a.outfit == Appearance.OUTFIT_NATURAL;
            int cape = body == EVIL ? 0x24306E : 0x7B3FA6;
            int boots = body == SUPER || body == KID ? BELT : YELLOW;
            return new Palette(visual.skinColor(), a.outfitSecondary, a.outfitPrimary, cape, boots, natural);
        }

        long hash() {
            long h = 7;
            for (int v : new int[]{skin, vest, pants, cape, boots, natural ? 1 : 0}) {
                h = h * 1_000_003L + v;
            }
            return h;
        }

        int color(Mat mat) {
            if (natural && mat != Mat.SKIN_ABS && mat != Mat.SKIN_CHEST) {
                return skin;
            }
            return switch (mat) {
                case SKIN, SKIN_CHEST, SKIN_ABS -> skin;
                case VEST, VEST_OPEN -> vest;
                case PANTS -> pants;
                case BELT, ARMBAND -> BELT;
                case BUCKLE -> TRIM;
                case GLOVE -> YELLOW;
                case BOOT, BOOT_BAND -> boots;
                case SHOE -> SHOE;
                case CAPE -> cape;
            };
        }
    }

    private static final class Painter {
        private final MajinBody body;
        private final Palette palette;
        private final NativeImage img = new NativeImage(TEX * S, TEX * S, true);

        Painter(MajinBody body, Palette palette) {
            this.body = body;
            this.palette = palette;
        }

        NativeImage paint() {
            for (Piece p : body.pieces()) {
                piece(p);
            }
            return img;
        }

        private enum Face {
            TOP(1.1f), BOTTOM(0.66f), SIDE_A(0.88f), FRONT(1.0f), SIDE_B(0.88f), BACK(0.8f);

            final float shade;

            Face(float shade) {
                this.shade = shade;
            }
        }

        private void piece(Piece p) {
            float u = p.u();
            float v = p.v();
            float w = p.w();
            float h = p.h();
            float d = p.d();
            face(p, Face.TOP, u + d, v, w, d);
            face(p, Face.BOTTOM, u + d + w, v, w, d);
            face(p, Face.SIDE_A, u, v + d, d, h);
            face(p, Face.FRONT, u + d, v + d, w, h);
            face(p, Face.SIDE_B, u + d + w, v + d, d, h);
            face(p, Face.BACK, u + d + w + d, v + d, w, h);
        }

        private void face(Piece p, Face face, float fu, float fv, float fw, float fh) {
            int x0 = (int) Math.floor(fu * S);
            int y0 = (int) Math.floor(fv * S);
            int x1 = Math.max(x0 + 1, (int) Math.ceil((fu + fw) * S));
            int y1 = Math.max(y0 + 1, (int) Math.ceil((fv + fh) * S));
            int pw = x1 - x0;
            int ph = y1 - y0;
            boolean side = face == Face.SIDE_A || face == Face.SIDE_B || face == Face.FRONT || face == Face.BACK;
            Mat mat = p.mat();
            int base = palette.color(mat);
            for (int py = 0; py < ph; py++) {
                float ty = ph <= 1 ? 0.0f : (float) py / (ph - 1);
                for (int px = 0; px < pw; px++) {
                    float tx = pw <= 1 ? 0.5f : (float) px / (pw - 1);
                    int color = base;
                    float shade = face.shade * (side ? 1.04f - 0.12f * ty : 1.0f);
                    if (!palette.natural()) {
                        int detail = detail(mat, face, px, py, pw, ph, tx, ty);
                        if (detail != -1) {
                            color = detail;
                        }
                    }
                    shade *= tone(mat, face, px, py, pw, ph, tx, ty);
                    boolean edge = px == 0 || py == 0 || px == pw - 1 || py == ph - 1;
                    if (edge) {
                        shade *= mat == Mat.SKIN || mat == Mat.SKIN_ABS || mat == Mat.SKIN_CHEST ? 0.96f : 0.86f;
                    }
                    shade *= 1.0f + (noise(x0 + px, y0 + py) / 255.0f - 0.5f) * 0.05f;
                    set(x0 + px, y0 + py, CustomSkins.shade(color, shade));
                }
            }
        }

        /** Replaces the base color with trim, opening, buckle and stripe details; -1 keeps the base color. */
        private int detail(Mat mat, Face face, int px, int py, int pw, int ph, float tx, float ty) {
            switch (mat) {
                case VEST_OPEN -> {
                    if (face == Face.FRONT) {
                        float opening = 0.56f;
                        float dist = Math.abs(tx - 0.5f) * 2.0f;
                        if (dist < opening - 0.08f) {
                            return palette.skin();
                        }
                        if (dist < opening + 0.04f) {
                            return Palette.TRIM;
                        }
                    }
                    if (face != Face.TOP && face != Face.BOTTOM && py >= ph - 2) {
                        return Palette.TRIM;
                    }
                }
                case VEST -> {
                    if (face != Face.TOP && face != Face.BOTTOM && py >= ph - 2) {
                        return Palette.TRIM;
                    }
                }
                case BUCKLE -> {
                    if (face == Face.FRONT && glyphM(tx, ty)) {
                        return 0x1A1414;
                    }
                }
                case ARMBAND -> {
                    if (face != Face.TOP && face != Face.BOTTOM && (py <= 1 || py >= ph - 2)) {
                        return Palette.YELLOW;
                    }
                }
                case BOOT_BAND -> {
                    if (face != Face.TOP && face != Face.BOTTOM) {
                        float stripe = Math.abs(tx - 0.5f);
                        if (py <= 1 || ty > 0.25f && ty < 0.38f || face == Face.FRONT && stripe < 0.12f && ty > 0.3f) {
                            return Palette.YELLOW;
                        }
                    }
                }
                case BOOT -> {
                    if (body == FAT || body == EVIL) {
                        if (face != Face.TOP && face != Face.BOTTOM && py <= 0) {
                            return CustomSkins.shade(Palette.YELLOW, 1.12f);
                        }
                    }
                }
                default -> {
                }
            }
            return -1;
        }

        /** Shading marks drawn over the base color: folds, muscles and seams. */
        private float tone(Mat mat, Face face, int px, int py, int pw, int ph, float tx, float ty) {
            switch (mat) {
                case PANTS -> {
                    if (face != Face.TOP && face != Face.BOTTOM) {
                        float f = 1.0f;
                        if ((px + (py / 5)) % 4 == 0) {
                            f *= 0.9f;
                        }
                        if (py >= ph - 2) {
                            f *= 0.86f;
                        }
                        return f;
                    }
                }
                case SKIN_CHEST -> {
                    if (face == Face.FRONT) {
                        boolean centre = Math.abs(px - (pw - 1) / 2.0f) < 0.6f;
                        boolean pec = Math.abs(ty - 0.62f - Math.abs(tx - 0.5f) * 0.25f) < 0.07f && Math.abs(tx - 0.5f) > 0.06f;
                        return centre && ty > 0.2f ? 0.84f : pec ? 0.82f : ty < 0.55f ? 1.05f : 1.0f;
                    }
                }
                case SKIN_ABS -> {
                    if (face == Face.FRONT) {
                        if (body == FAT) {
                            float dx = tx - 0.5f;
                            float dy = ty - 0.62f;
                            boolean navel = dx * dx * 4.0f + dy * dy * 9.0f < 0.012f;
                            return navel ? 0.7f : 1.08f - (dx * dx + (ty - 0.45f) * (ty - 0.45f)) * 0.5f;
                        }
                        boolean centre = Math.abs(px - (pw - 1) / 2.0f) < 0.6f;
                        boolean row = Math.abs(ty - 0.33f) < 0.05f || Math.abs(ty - 0.66f) < 0.05f;
                        return centre || row && Math.abs(tx - 0.5f) < 0.36f ? 0.84f : 1.0f;
                    }
                }
                case CAPE -> {
                    if (face == Face.FRONT) {
                        return 0.72f;
                    }
                    if (face == Face.BACK && (px + py / 3) % 6 == 0) {
                        return 0.9f;
                    }
                }
                case BELT -> {
                    if (face != Face.TOP && face != Face.BOTTOM && py == 0) {
                        return 1.6f;
                    }
                }
                default -> {
                }
            }
            return 1.0f;
        }

        /** The Majin "M" emblem on the belt buckle. */
        private static boolean glyphM(float tx, float ty) {
            if (ty < 0.2f || ty > 0.8f || tx < 0.15f || tx > 0.85f) {
                return false;
            }
            float x = (tx - 0.15f) / 0.7f;
            float y = (ty - 0.2f) / 0.6f;
            boolean legs = x < 0.2f || x > 0.8f;
            boolean v = Math.abs(Math.abs(x - 0.5f) - (0.3f - y * 0.3f)) < 0.11f && y < 0.7f;
            return legs || v;
        }

        private void set(int x, int y, int rgb) {
            if (x >= 0 && y >= 0 && x < TEX * S && y < TEX * S) {
                img.setPixel(x, y, 0xFF000000 | (rgb & 0xFFFFFF));
            }
        }

        private static int noise(int x, int y) {
            int h = x * 374761393 + y * 668265263;
            h = (h ^ (h >>> 13)) * 1274126177;
            return (h ^ (h >>> 16)) & 255;
        }
    }
}
