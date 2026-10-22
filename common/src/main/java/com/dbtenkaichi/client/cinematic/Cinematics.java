package com.dbtenkaichi.client.cinematic;

import com.dbtenkaichi.client.cinematic.CinematicScript.Camera;
import com.dbtenkaichi.client.cinematic.CinematicScript.EventKind;

import java.util.HashMap;
import java.util.Map;

public final class Cinematics {
    public static final String DEFAULT = "power_up";
    private static final Map<String, CinematicScript> SCRIPTS = new HashMap<>();

    static {
        register(superSaiyanAwakening());
        register(superSaiyan2Awakening());
        register(superSaiyan3Awakening());
        register(darkRage());
        register(majinFission());
        register(majinAbsorption());
        register(majinRegression());
        register(powerUp());
    }

    private Cinematics() {
    }

    public static void register(CinematicScript script) {
        SCRIPTS.put(script.id, script);
    }

    public static CinematicScript get(String id) {
        CinematicScript script = id == null ? null : SCRIPTS.get(id);
        return script != null ? script : SCRIPTS.get(DEFAULT);
    }

    private static Camera cam(float yaw, float pitch, float distance, float height) {
        return new Camera(yaw, pitch, distance, height);
    }

    private static CinematicScript superSaiyanAwakening() {
        return CinematicScript.create("super_saiyan_awakening")
                .colors(0xFFFFFF, 0x0E1A10, 0xD8F0FF)
                .cut(0.00f, 0.14f, cam(0, 2, 1.25f, 0.9f), cam(0, 2, 1.0f, 0.92f))
                .cut(0.14f, 0.30f, cam(38, -10, 10.0f, 0.45f), cam(30, -8, 8.5f, 0.45f))
                .cut(0.30f, 0.46f, cam(0, 4, 4.4f, 0.55f), cam(0, 2, 3.6f, 0.55f))
                .cut(0.46f, 0.60f, cam(-28, 12, 1.5f, 0.93f), cam(-20, 8, 1.3f, 0.93f))
                .shot(0.60f, 0.68f, cam(0, 4, 1.1f, 0.92f), cam(0, 6, 3.8f, 0.8f))
                .shot(0.68f, 0.86f, cam(180, -6, 5.0f, 0.5f), cam(175, -4, 6.5f, 0.5f))
                .shot(0.86f, 1.00f, cam(18, 3, 2.3f, 0.9f), cam(6, 2, 1.7f, 0.92f))
                .darken(0.00f, 0.10f, 0.0f, 0.6f).darken(0.10f, 0.68f, 0.6f, 0.78f).darken(0.68f, 0.80f, 0.45f, 0.0f)
                .shake(0.00f, 0.30f, 0.15f, 0.3f).shake(0.30f, 0.60f, 0.35f, 0.7f).shake(0.60f, 0.70f, 1.2f, 1.6f).shake(0.70f, 0.86f, 0.6f, 0.2f)
                .aura(0.00f, 0.30f, 0.0f, 0.25f).aura(0.30f, 0.60f, 0.3f, 0.6f).aura(0.60f, 0.68f, 0.7f, 1.0f).aura(0.68f, 0.86f, 1.6f, 1.3f).aura(0.86f, 1.0f, 0.9f, 0.7f)
                .rocks(0.30f, 0.68f, 0.6f, 1.0f).rocks(0.68f, 0.86f, 0.6f, 0.2f)
                .flash(0.17f, 0.19f, 0.45f, 0.0f).flash(0.22f, 0.24f, 0.55f, 0.0f).flash(0.27f, 0.29f, 0.4f, 0.0f)
                .flash(0.52f, 0.54f, 0.35f, 0.0f).flash(0.62f, 0.68f, 0.0f, 1.0f).flash(0.68f, 0.76f, 1.0f, 0.0f)
                .flicker(0.46f, 0.66f)
                .formFrom(0.68f)
                .revealAt(0.86f)
                .event(0.00f, EventKind.SOUND_RUMBLE, 1.0f)
                .event(0.17f, EventKind.LIGHTNING, 1.0f).event(0.17f, EventKind.SOUND_THUNDER, 1.0f)
                .event(0.22f, EventKind.LIGHTNING, 1.2f).event(0.22f, EventKind.SOUND_THUNDER, 0.9f)
                .event(0.27f, EventKind.LIGHTNING, 0.9f).event(0.27f, EventKind.SOUND_THUNDER, 1.1f)
                .event(0.31f, EventKind.GROUND_BREAK, 1.0f).event(0.31f, EventKind.SOUND_RUMBLE, 1.2f)
                .event(0.52f, EventKind.LIGHTNING, 0.7f).event(0.52f, EventKind.SOUND_THUNDER, 1.2f)
                .event(0.60f, EventKind.SOUND_SCREAM, 1.0f)
                .event(0.68f, EventKind.BURST, 2.0f).event(0.68f, EventKind.SOUND_BURST, 1.0f).event(0.68f, EventKind.GROUND_BREAK, 2.0f)
                .event(0.86f, EventKind.SOUND_POWER, 0.8f);
    }

    private static CinematicScript superSaiyan2Awakening() {
        return CinematicScript.create("super_saiyan_2_awakening")
                .colors(0xFFF8C8, 0x080406, 0xBFE4FF)
                .cut(0.00f, 0.14f, cam(0, 2, 1.15f, 0.92f), cam(0, 2, 1.0f, 0.93f))
                .cut(0.14f, 0.40f, cam(0, 2, 1.1f, 0.92f), cam(0, 2, 1.1f, 0.92f))
                .cut(0.40f, 0.56f, cam(0, 0, 3.0f, 0.55f), cam(0, 0, 3.0f, 0.55f))
                .cut(0.56f, 0.68f, cam(20, -14, 5.0f, 0.35f), cam(14, -18, 4.6f, 0.35f))
                .shot(0.68f, 0.86f, cam(35, -10, 6.5f, 0.5f), cam(25, -6, 8.0f, 0.5f))
                .shot(0.86f, 1.00f, cam(8, 2, 1.6f, 0.92f), cam(0, 2, 1.25f, 0.93f))
                .darken(0.00f, 0.14f, 0.3f, 0.55f).darken(0.68f, 0.80f, 0.35f, 0.0f)
                .streak(0.24f, 0.40f)
                .backdrop(0.14f, 0.40f, 0x000000, 0.95f, 1.0f, 0.0f)
                .backdrop(0.40f, 0.56f, 0xB8C25E, 0.42f, 0.56f, 0.0f)
                .shake(0.00f, 0.14f, 0.1f, 0.25f).shake(0.56f, 0.68f, 0.6f, 1.2f).shake(0.68f, 0.86f, 1.6f, 0.4f)
                .aura(0.00f, 0.40f, 0.1f, 0.3f).aura(0.40f, 0.56f, 0.6f, 1.0f).aura(0.56f, 0.68f, 1.0f, 1.3f).aura(0.68f, 0.86f, 1.7f, 1.2f).aura(0.86f, 1.0f, 1.0f, 0.8f)
                .rocks(0.56f, 0.86f, 0.8f, 0.3f)
                .flash(0.58f, 0.66f, 0.0f, 1.0f).flash(0.66f, 0.74f, 1.0f, 0.0f)
                .flicker(0.42f, 0.56f)
                .formFrom(0.66f)
                .revealAt(0.86f)
                .event(0.00f, EventKind.SOUND_RUMBLE, 0.8f)
                .event(0.06f, EventKind.SOUND_THUNDER, 0.6f)
                .event(0.24f, EventKind.SOUND_THUNDER, 1.1f)
                .event(0.40f, EventKind.SOUND_SCREAM, 1.2f)
                .event(0.56f, EventKind.SOUND_RUMBLE, 1.3f)
                .event(0.66f, EventKind.BURST, 2.4f).event(0.66f, EventKind.SOUND_BURST, 1.2f).event(0.66f, EventKind.GROUND_BREAK, 2.4f)
                .event(0.70f, EventKind.LIGHTNING, 1.2f).event(0.70f, EventKind.SOUND_THUNDER, 1.0f)
                .event(0.76f, EventKind.LIGHTNING, 1.0f).event(0.76f, EventKind.SOUND_THUNDER, 0.9f)
                .event(0.90f, EventKind.LIGHTNING, 0.6f).event(0.90f, EventKind.SOUND_THUNDER, 0.8f)
                .event(0.86f, EventKind.SOUND_POWER, 0.9f);
    }

    private static CinematicScript superSaiyan3Awakening() {
        return CinematicScript.create("super_saiyan_3_awakening")
                .colors(0xFFFFFF, 0x10140A, 0xD8F0FF)
                .shot(0.00f, 0.18f, cam(30, 0, 4.5f, 0.55f), cam(15, -4, 3.6f, 0.55f))
                .shot(0.18f, 0.40f, cam(0, -62, 2.4f, 2.0f), cam(70, -66, 2.4f, 2.0f))
                .cut(0.40f, 0.62f, cam(-22, 8, 1.4f, 0.93f), cam(-14, 6, 1.2f, 0.93f))
                .shot(0.62f, 0.76f, cam(150, -8, 4.5f, 0.55f), cam(200, -6, 5.0f, 0.55f))
                .shot(0.76f, 0.86f, cam(0, 4, 3.0f, 0.6f), cam(0, 2, 4.0f, 0.6f))
                .shot(0.86f, 1.00f, cam(25, 2, 3.0f, 0.8f), cam(5, 2, 2.3f, 0.85f))
                .cloudRush(0.04f, 0.18f, 0.2f, 0.7f).cloudRush(0.18f, 0.40f, 1.0f, 1.0f).cloudRush(0.40f, 0.76f, 0.8f, 1.0f).cloudRush(0.76f, 0.86f, 1.0f, 0.0f)
                .darken(0.40f, 0.62f, 0.2f, 0.4f).darken(0.62f, 0.76f, 0.2f, 0.0f)
                .shake(0.00f, 0.40f, 0.2f, 0.6f).shake(0.40f, 0.76f, 0.7f, 1.4f).shake(0.76f, 0.86f, 1.6f, 0.3f)
                .aura(0.00f, 0.40f, 0.6f, 0.9f).aura(0.40f, 0.76f, 1.0f, 1.4f).aura(0.76f, 0.86f, 1.8f, 1.0f).aura(0.86f, 1.0f, 0.55f, 0.4f)
                .rocks(0.18f, 0.80f, 0.6f, 1.0f)
                .flash(0.70f, 0.78f, 0.0f, 1.0f).flash(0.78f, 0.86f, 1.0f, 0.0f)
                .flicker(0.42f, 0.74f)
                .formFrom(0.78f)
                .revealAt(0.86f)
                .event(0.00f, EventKind.SOUND_POWER, 0.9f).event(0.04f, EventKind.SOUND_RUMBLE, 1.0f)
                .event(0.20f, EventKind.LIGHTNING, 1.0f).event(0.20f, EventKind.SOUND_THUNDER, 1.0f).event(0.20f, EventKind.GROUND_BREAK, 1.2f)
                .event(0.30f, EventKind.LIGHTNING, 1.2f).event(0.30f, EventKind.SOUND_THUNDER, 1.1f).event(0.30f, EventKind.SOUND_RUMBLE, 1.3f)
                .event(0.40f, EventKind.SOUND_SCREAM, 1.3f)
                .event(0.52f, EventKind.LIGHTNING, 0.9f).event(0.52f, EventKind.SOUND_THUNDER, 1.0f)
                .event(0.64f, EventKind.LIGHTNING, 1.3f).event(0.64f, EventKind.SOUND_THUNDER, 1.2f).event(0.64f, EventKind.GROUND_BREAK, 1.8f)
                .event(0.70f, EventKind.SOUND_SCREAM, 1.0f)
                .event(0.78f, EventKind.BURST, 2.6f).event(0.78f, EventKind.SOUND_BURST, 1.3f).event(0.78f, EventKind.GROUND_BREAK, 2.6f)
                .event(0.86f, EventKind.SOUND_POWER, 1.0f);
    }

    private static CinematicScript darkRage() {
        return CinematicScript.create("dark_rage")
                .colors(0xFF6A6A, 0x1A0610, 0xC890FF)
                .shot(0.00f, 0.35f, cam(-60, -12, 6.0f, 0.5f), cam(60, -12, 6.0f, 0.5f))
                .cut(0.35f, 0.55f, cam(0, 18, 1.4f, 0.9f), cam(0, 10, 1.2f, 0.92f))
                .shot(0.55f, 1.00f, cam(200, -15, 4.0f, 0.4f), cam(150, -6, 7.0f, 0.5f))
                .darken(0.0f, 0.2f, 0.0f, 0.6f).darken(0.2f, 0.85f, 0.6f, 0.6f).darken(0.85f, 1.0f, 0.6f, 0.0f)
                .shake(0.0f, 0.55f, 0.3f, 0.8f).shake(0.55f, 1.0f, 1.0f, 0.2f)
                .aura(0.0f, 0.55f, 0.4f, 1.0f).aura(0.55f, 1.0f, 1.5f, 1.0f)
                .rocks(0.2f, 0.8f, 0.5f, 0.8f)
                .flash(0.55f, 0.62f, 0.8f, 0.0f)
                .formFrom(0.55f)
                .revealAt(0.7f)
                .event(0.0f, EventKind.SOUND_RUMBLE, 1.0f)
                .event(0.25f, EventKind.LIGHTNING, 1.0f).event(0.25f, EventKind.SOUND_THUNDER, 0.8f)
                .event(0.40f, EventKind.SOUND_SCREAM, 0.8f)
                .event(0.55f, EventKind.BURST, 1.6f).event(0.55f, EventKind.SOUND_BURST, 0.9f).event(0.55f, EventKind.GROUND_BREAK, 1.5f);
    }

    /**
     * Evil Buu: Buu's rage boils over, steam blasts out of the holes in his head and rises into a cloud that congeals in
     * the sky, drops back down and leaves the thin, grey Evil Buu standing in the smoke.
     */
    private static CinematicScript majinFission() {
        return CinematicScript.create("majin_fission")
                .colors(0xFFFFFF, 0x1A0E1C, 0xE0C8F0)
                .cut(0.00f, 0.13f, cam(0, 2, 1.25f, 0.9f), cam(0, 2, 1.05f, 0.91f))
                .cut(0.13f, 0.28f, cam(80, 0, 2.6f, 0.82f), cam(70, -2, 2.2f, 0.84f))
                .shot(0.28f, 0.42f, cam(25, -24, 5.0f, 0.5f), cam(10, -38, 6.0f, 0.6f))
                .shot(0.42f, 0.58f, cam(0, -8, 11.0f, 2.2f), cam(20, -10, 12.0f, 2.6f))
                .cut(0.58f, 0.72f, cam(-30, -6, 6.5f, 0.6f), cam(-15, -4, 5.5f, 0.6f))
                .shot(0.72f, 0.86f, cam(0, -12, 5.0f, 0.5f), cam(0, -6, 3.4f, 0.6f))
                .shot(0.86f, 1.00f, cam(16, 3, 1.8f, 0.9f), cam(6, 2, 1.5f, 0.92f))
                .darken(0.00f, 0.12f, 0.0f, 0.5f).darken(0.12f, 0.72f, 0.5f, 0.62f).darken(0.72f, 0.86f, 0.62f, 0.0f)
                .shake(0.00f, 0.13f, 0.1f, 0.3f).shake(0.13f, 0.28f, 0.4f, 0.8f).shake(0.28f, 0.58f, 0.5f, 0.6f).shake(0.58f, 0.66f, 1.2f, 0.6f)
                .steam(0.03f, 0.13f, 0.15f, 0.5f).steam(0.13f, 0.28f, 0.8f, 1.6f).steam(0.28f, 0.50f, 1.6f, 0.6f)
                .smoke(0.26f, 0.42f, 0xE6E2EA, 1.6f, 1.2f, 0.3f, 0.9f)
                .smoke(0.40f, 0.60f, 0xC8C2D2, 4.2f, 3.0f, 1.2f, 1.8f)
                .smoke(0.58f, 0.74f, 0xD0C8DA, 0.55f, 1.3f, 1.6f, 1.0f)
                .smoke(0.74f, 0.80f, 0xD0C8DA, 0.5f, 2.2f, 0.5f, 0.0f)
                .aura(0.00f, 0.58f, 0.0f, 0.2f).aura(0.74f, 0.86f, 0.4f, 1.1f).aura(0.86f, 1.0f, 0.25f, 0.2f)
                .rocks(0.13f, 0.30f, 0.3f, 0.6f)
                .flash(0.58f, 0.62f, 0.0f, 0.6f).flash(0.62f, 0.68f, 0.6f, 0.0f)
                .formFrom(0.66f)
                .revealAt(0.86f)
                .event(0.00f, EventKind.SOUND_RUMBLE, 0.7f)
                .event(0.03f, EventKind.SOUND_STEAM, 0.6f)
                .event(0.13f, EventKind.SOUND_STEAM, 1.2f).event(0.13f, EventKind.SOUND_SCREAM, 0.7f)
                .event(0.20f, EventKind.SOUND_STEAM, 1.3f)
                .event(0.28f, EventKind.SOUND_RUMBLE, 1.1f).event(0.28f, EventKind.SOUND_STEAM, 1.0f)
                .event(0.44f, EventKind.SOUND_THUNDER, 0.6f)
                .event(0.58f, EventKind.SMOKE_BURST, 1.2f).event(0.58f, EventKind.SOUND_BURST, 0.9f).event(0.58f, EventKind.GROUND_BREAK, 1.2f)
                .event(0.74f, EventKind.SOUND_POWER, 0.7f)
                .event(0.74f, EventKind.BURST, 1.2f)
                .event(0.86f, EventKind.SOUND_LAUGH, 1.0f);
    }

    /**
     * Super Buu: Evil Buu blows the candy beam back, the chocolate flies into his mouth, he laughs and pink smoke pours
     * out of his body; inside the cloud the body swells, then the smoke is blown away to reveal the towering Super Buu.
     */
    private static CinematicScript majinAbsorption() {
        return CinematicScript.create("majin_absorption")
                .colors(0xFFE8F4, 0x1A0A14, 0xFFC0E0)
                .cut(0.00f, 0.12f, cam(0, 2, 1.2f, 0.9f), cam(0, 2, 1.05f, 0.91f))
                .cut(0.12f, 0.30f, cam(75, 2, 3.0f, 0.75f), cam(65, 0, 2.6f, 0.78f))
                .cut(0.30f, 0.42f, cam(-14, 6, 1.15f, 0.92f), cam(-8, 4, 1.0f, 0.93f))
                .shot(0.42f, 0.56f, cam(0, -4, 2.6f, 0.6f), cam(0, -8, 6.5f, 0.55f))
                .shot(0.56f, 0.76f, cam(-40, -6, 7.0f, 0.6f), cam(40, -10, 7.5f, 0.7f))
                .cut(0.76f, 0.86f, cam(0, -18, 5.0f, 0.4f), cam(0, -16, 4.2f, 0.45f))
                .shot(0.86f, 1.00f, cam(22, -8, 4.0f, 0.6f), cam(10, -5, 3.3f, 0.66f))
                .darken(0.00f, 0.10f, 0.0f, 0.4f).darken(0.10f, 0.76f, 0.4f, 0.55f).darken(0.76f, 0.86f, 0.55f, 0.0f)
                .shake(0.30f, 0.42f, 0.3f, 0.6f).shake(0.42f, 0.56f, 0.9f, 0.7f).shake(0.56f, 0.76f, 0.5f, 0.9f).shake(0.76f, 0.86f, 1.5f, 0.3f)
                .smoke(0.42f, 0.56f, 0xFF8AC8, 0.5f, 1.4f, 0.6f, 1.6f)
                .smoke(0.56f, 0.76f, 0xFF7ABC, 0.55f, 1.9f, 1.8f, 1.8f)
                .smoke(0.76f, 0.81f, 0xFF9ACD, 0.55f, 2.8f, 1.0f, 0.0f)
                .grow(0.58f, 0.72f, 0.8f, 1.0f)
                .aura(0.00f, 0.42f, 0.0f, 0.15f).aura(0.76f, 0.86f, 1.1f, 0.5f).aura(0.86f, 1.0f, 0.3f, 0.25f)
                .rocks(0.56f, 0.86f, 0.4f, 0.8f)
                .flash(0.42f, 0.45f, 0.5f, 0.0f).flash(0.76f, 0.80f, 0.0f, 0.9f).flash(0.80f, 0.86f, 0.9f, 0.0f)
                .formFrom(0.58f)
                .revealAt(0.86f)
                .event(0.00f, EventKind.SOUND_RUMBLE, 0.6f)
                .event(0.04f, EventKind.SOUND_STEAM, 0.5f)
                .event(0.10f, EventKind.CHOCOLATE, 1.0f).event(0.16f, EventKind.CHOCOLATE, 0.8f)
                .event(0.26f, EventKind.SOUND_GULP, 1.0f)
                .event(0.31f, EventKind.SOUND_LAUGH, 1.0f)
                .event(0.42f, EventKind.SMOKE_BURST, 1.0f).event(0.42f, EventKind.SOUND_STEAM, 1.4f).event(0.42f, EventKind.SOUND_RUMBLE, 1.0f)
                .event(0.56f, EventKind.SOUND_STRETCH, 1.0f).event(0.66f, EventKind.SOUND_STRETCH, 0.9f)
                .event(0.76f, EventKind.BURST, 1.6f).event(0.76f, EventKind.SOUND_BURST, 1.2f).event(0.76f, EventKind.GROUND_BREAK, 2.0f)
                .event(0.76f, EventKind.SMOKE_BURST, 1.0f)
                .event(0.86f, EventKind.SOUND_POWER, 0.9f)
                .event(0.92f, EventKind.SOUND_LAUGH, 0.8f);
    }

    /**
     * Kid Buu: with Good Buu pulled out, Super Buu clutches his head while steam spews from it, balloons into a hulking
     * giant, then shrinks fast, flickering through his old forms, until a steam blast leaves the small, manic Kid Buu.
     */
    private static CinematicScript majinRegression() {
        return CinematicScript.create("majin_regression")
                .colors(0xFFF0FA, 0x14060E, 0xFFB0E0)
                .cut(0.00f, 0.12f, cam(0, 2, 1.3f, 0.9f), cam(0, 2, 1.1f, 0.91f))
                .cut(0.12f, 0.28f, cam(-35, -4, 4.4f, 0.6f), cam(-25, -4, 3.8f, 0.62f))
                .shot(0.28f, 0.44f, cam(0, -8, 4.2f, 0.55f), cam(0, -14, 8.0f, 0.65f))
                .cut(0.44f, 0.62f, cam(30, -4, 3.4f, 0.5f), cam(20, -6, 3.0f, 0.5f))
                .cut(0.62f, 0.72f, cam(0, -10, 6.5f, 0.5f), cam(0, -8, 7.5f, 0.5f))
                .shot(0.72f, 0.86f, cam(0, -6, 4.6f, 0.45f), cam(0, -2, 3.2f, 0.5f))
                .shot(0.86f, 1.00f, cam(14, 4, 2.1f, 0.9f), cam(0, 2, 1.8f, 0.92f))
                .darken(0.00f, 0.12f, 0.2f, 0.55f).darken(0.12f, 0.62f, 0.55f, 0.7f).darken(0.62f, 0.74f, 0.5f, 0.0f)
                .shake(0.00f, 0.28f, 0.3f, 0.9f).shake(0.28f, 0.44f, 0.9f, 1.4f).shake(0.44f, 0.62f, 0.7f, 1.0f).shake(0.62f, 0.72f, 1.8f, 0.5f)
                .steam(0.00f, 0.12f, 0.6f, 1.0f).steam(0.12f, 0.28f, 1.2f, 1.8f).steam(0.28f, 0.44f, 1.6f, 1.2f).steam(0.44f, 0.62f, 1.0f, 0.6f)
                .grow(0.28f, 0.44f, 1.0f, 1.45f).grow(0.44f, 0.62f, 1.45f, 1.0f)
                .flicker(0.46f, 0.60f)
                .smoke(0.44f, 0.62f, 0xF6D8EA, 0.5f, 1.4f, 0.2f, 0.8f)
                .smoke(0.62f, 0.74f, 0xF2E8F0, 0.5f, 2.6f, 1.8f, 0.6f)
                .smoke(0.74f, 0.80f, 0xF2E8F0, 0.4f, 3.0f, 0.5f, 0.0f)
                .aura(0.00f, 0.44f, 0.2f, 0.6f).aura(0.62f, 0.72f, 1.3f, 0.6f).aura(0.72f, 0.86f, 0.5f, 0.2f).aura(0.86f, 1.0f, 0.15f, 0.15f)
                .rocks(0.12f, 0.62f, 0.4f, 1.0f)
                .flash(0.46f, 0.48f, 0.35f, 0.0f).flash(0.53f, 0.55f, 0.35f, 0.0f).flash(0.60f, 0.64f, 0.0f, 1.0f).flash(0.64f, 0.72f, 1.0f, 0.0f)
                .formFrom(0.60f)
                .revealAt(0.86f)
                .event(0.00f, EventKind.SOUND_STEAM, 1.0f).event(0.00f, EventKind.SOUND_RUMBLE, 0.8f)
                .event(0.06f, EventKind.SOUND_SCREAM, 0.8f)
                .event(0.12f, EventKind.SOUND_STEAM, 1.4f).event(0.22f, EventKind.SOUND_STEAM, 1.4f)
                .event(0.28f, EventKind.SOUND_STRETCH, 1.3f).event(0.28f, EventKind.SOUND_RUMBLE, 1.2f)
                .event(0.36f, EventKind.SOUND_STRETCH, 1.1f).event(0.44f, EventKind.SOUND_STRETCH, 1.2f)
                .event(0.50f, EventKind.SOUND_STEAM, 1.0f)
                .event(0.62f, EventKind.BURST, 2.4f).event(0.62f, EventKind.SOUND_BURST, 1.3f).event(0.62f, EventKind.GROUND_BREAK, 2.4f)
                .event(0.62f, EventKind.SMOKE_BURST, 1.6f)
                .event(0.86f, EventKind.SOUND_CACKLE, 1.0f)
                .event(0.94f, EventKind.SOUND_POWER, 0.7f);
    }

    private static CinematicScript powerUp() {
        return CinematicScript.create(DEFAULT)
                .shot(0.0f, 0.6f, cam(40, 5, 4.0f, 0.6f), cam(-20, 5, 2.8f, 0.7f))
                .shot(0.6f, 1.0f, cam(0, -5, 3.0f, 0.55f), cam(0, 0, 4.2f, 0.55f))
                .shake(0.0f, 0.6f, 0.2f, 0.6f).shake(0.6f, 1.0f, 0.8f, 0.1f)
                .aura(0.0f, 0.6f, 0.5f, 1.0f).aura(0.6f, 1.0f, 1.4f, 0.8f)
                .rocks(0.2f, 0.7f, 0.3f, 0.6f)
                .flash(0.6f, 0.66f, 0.7f, 0.0f)
                .formFrom(0.6f)
                .revealAt(0.65f)
                .event(0.0f, EventKind.SOUND_POWER, 1.0f)
                .event(0.6f, EventKind.BURST, 1.4f).event(0.6f, EventKind.SOUND_BURST, 0.8f).event(0.6f, EventKind.GROUND_BREAK, 1.0f);
    }
}
