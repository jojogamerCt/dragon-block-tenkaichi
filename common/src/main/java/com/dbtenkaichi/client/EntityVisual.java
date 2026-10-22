package com.dbtenkaichi.client;

import com.dbtenkaichi.client.anim.AnimPlayer;
import com.dbtenkaichi.form.FormDefinition;
import com.dbtenkaichi.form.FormRegistry;
import com.dbtenkaichi.player.Appearance;
import com.dbtenkaichi.player.HairStyle;
import com.dbtenkaichi.player.PlayerData;
import com.dbtenkaichi.player.RuntimeState;
import com.dbtenkaichi.race.Race;
import org.jspecify.annotations.Nullable;

public final class EntityVisual {
    public Race race = Race.HUMAN;
    public Appearance appearance = Appearance.defaultsFor(Race.HUMAN);
    public boolean created;
    public boolean beard;
    public boolean roshi;
    public boolean cat;
    public boolean previewing;
    public String form = PlayerData.BASE_FORM;
    public int flags;
    public float kiRatio = 1.0f;
    public float mastery;
    public float aura;
    public float auraO;
    public float flight;
    public float flightO;
    public long cinematicStart;
    public int cinematicTicks;
    public boolean cinematicFull;
    public String cinematicForm = PlayerData.BASE_FORM;
    public long lastSeen;
    public final AnimPlayer anim = new AnimPlayer();
    public com.dbtenkaichi.ki.SkillStyle style = com.dbtenkaichi.ki.SkillStyle.KAMEHAMEHA;

    public boolean has(int flag) {
        return (flags & flag) != 0;
    }

    public @Nullable FormDefinition formDefinition() {
        return PlayerData.BASE_FORM.equals(form) ? null : FormRegistry.get(form, true);
    }

    public @Nullable FormDefinition visualForm() {
        if (com.dbtenkaichi.client.cinematic.CinematicDirector.active(this)) {
            return com.dbtenkaichi.client.cinematic.CinematicDirector.showsTargetForm(this) ? com.dbtenkaichi.client.cinematic.CinematicDirector.targetForm(this) : formDefinition();
        }
        return formDefinition();
    }

    public HairStyle hairStyle() {
        FormDefinition def = visualForm();
        HairStyle base = HairStyle.byOrdinal(appearance.hairStyle);
        HairStyle formStyle = def == null ? null : def.hairStyle();
        if (formStyle == null || !race.hasHair()) {
            return base;
        }
        return HairStyle.transformed(base, formStyle);
    }

    public int hairColor() {
        FormDefinition def = visualForm();
        return def == null ? appearance.hairColor : FormDefinition.parseColor(def.visual.hairColor, appearance.hairColor);
    }

    public boolean hairGlow() {
        FormDefinition def = visualForm();
        return def != null && def.visual.hairGlow;
    }

    public int eyeColor() {
        FormDefinition def = visualForm();
        return def == null ? appearance.eyeColor : FormDefinition.parseColor(def.visual.eyeColor, appearance.eyeColor);
    }

    public com.dbtenkaichi.client.render.@Nullable MajinBody majinBody() {
        if (appearance.useOwnSkin) {
            return null;
        }
        FormDefinition def = visualForm();
        com.dbtenkaichi.client.render.MajinBody body = def == null ? null : com.dbtenkaichi.client.render.MajinBody.byId(def.visual.body);
        if (body != null) {
            return body;
        }
        return race == Race.MAJIN && appearance.build == Appearance.BUILD_FAT ? com.dbtenkaichi.client.render.MajinBody.FAT : null;
    }

    public int skinColor() {
        FormDefinition def = visualForm();
        return def == null ? appearance.skinColor : FormDefinition.parseColor(def.visual.skinColor, appearance.skinColor);
    }

    /** The color of the whites of the eyes, or -1 for the normal white. */
    public int scleraColor() {
        FormDefinition def = visualForm();
        return def == null ? -1 : FormDefinition.parseColor(def.visual.scleraColor, -1);
    }

    public float modelHeight() {
        FormDefinition def = visualForm();
        return def == null ? 1.0f : def.visual.modelHeight;
    }

    public float modelWidth() {
        FormDefinition def = visualForm();
        return def == null ? 1.0f : def.visual.modelWidth;
    }

    public net.minecraft.resources.Identifier skinTexture() {
        return com.dbtenkaichi.client.render.CustomSkins.textureFor(race, appearance, eyeColor(), hairColor(), skinColor(), scleraColor());
    }

    public int kaioken() {
        return com.dbtenkaichi.form.KaiokenSystem.fromFlags(flags);
    }

    public int auraColor() {
        FormDefinition def = visualForm();
        int base = def == null ? appearance.auraColor : FormDefinition.parseColor(def.visual.auraColor, appearance.auraColor);
        return kaioken() > 0 ? mix(base, 0xFF2A18, 0.72f) : base;
    }

    public int auraSecondary() {
        FormDefinition def = visualForm();
        int fallback = mix(auraColor(), 0xFFFFFF, 0.55f);
        int base = def == null ? fallback : FormDefinition.parseColor(def.visual.auraSecondary, fallback);
        return kaioken() > 0 ? mix(base, 0xFF9080, 0.7f) : base;
    }

    public float auraScale() {
        FormDefinition def = visualForm();
        return (def == null ? 1.0f : def.visual.auraScale) * (1.0f + 0.06f * kaioken());
    }

    public boolean auraLightning() {
        FormDefinition def = visualForm();
        return def != null && def.visual.lightning;
    }

    public float auraTarget(long now) {
        float scripted = com.dbtenkaichi.client.cinematic.CinematicDirector.auraLevel(this);
        if (!Float.isNaN(scripted)) {
            return scripted;
        }
        float target = 0.0f;
        if (has(RuntimeState.FLAG_CHARGING)) {
            target = 1.0f;
        } else if (has(RuntimeState.FLAG_TRANSFORMING)) {
            target = 1.25f;
        } else if (has(RuntimeState.FLAG_AURA)) {
            target = 0.75f;
        } else if (kaioken() > 0) {
            target = 0.5f + 0.05f * kaioken();
        } else if (!PlayerData.BASE_FORM.equals(form)) {
            target = 0.32f;
        }
        if (has(RuntimeState.FLAG_EXHAUSTED)) {
            target *= 0.4f;
        }
        return target;
    }

    public static int mix(int a, int b, float t) {
        int ar = a >> 16 & 255, ag = a >> 8 & 255, ab = a & 255;
        int br = b >> 16 & 255, bg = b >> 8 & 255, bb = b & 255;
        int r = (int) (ar + (br - ar) * t);
        int g = (int) (ag + (bg - ag) * t);
        int bl = (int) (ab + (bb - ab) * t);
        return r << 16 | g << 8 | bl;
    }
}
