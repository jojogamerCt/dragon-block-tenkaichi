package com.dbtenkaichi.client;

import com.dbtenkaichi.Tenkaichi;
import com.dbtenkaichi.client.screen.CharacterScreen;
import com.dbtenkaichi.form.TransformationSystem;
import com.dbtenkaichi.network.ActionType;
import com.dbtenkaichi.network.Packets;
import com.dbtenkaichi.platform.Services;
import com.dbtenkaichi.player.Appearance;
import com.dbtenkaichi.player.HairStyle;
import com.dbtenkaichi.player.PlayerData;
import com.dbtenkaichi.race.Race;
import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.client.server.IntegratedServer;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public final class ClientSmokeTest {
    private static final boolean ENABLED = Boolean.getBoolean("dbtenkaichi.clientSmoke");
    private static final List<String> LOG = new ArrayList<>();
    private static final String SSJ = "dbtenkaichi:super_saiyan";
    private static final String SSJ2 = "dbtenkaichi:super_saiyan_2";
    private static int tick = -1;
    private static boolean finished;
    private static Vec3 base;

    private ClientSmokeTest() {
    }

    public static boolean enabled() {
        return ENABLED;
    }

    public static void tick(Minecraft mc) {
        if (!ENABLED || finished || mc.player == null || mc.level == null) {
            return;
        }
        tick++;
        try {
            step(mc);
        } catch (RuntimeException e) {
            Tenkaichi.LOG.error("[DBT-CLIENT] step {} failed", tick, e);
            LOG.add("FAIL step " + tick + ": " + e);
        }
    }

    private static void action(ActionType type, int arg) {
        Services.NETWORK.sendToServer(new Packets.Action(type.ordinal(), arg));
    }

    private static void command(Minecraft mc, String cmd) {
        IntegratedServer server = mc.getSingleplayerServer();
        if (server != null) {
            server.execute(() -> server.getCommands().performPrefixedCommand(server.createCommandSourceStack(), cmd));
        }
    }

    private static void shot(Minecraft mc, String name) {
        Screenshot.grab(mc.gameDirectory, "dbt_" + name + ".png", mc.gameRenderer.mainRenderTarget(), 1, message -> Tenkaichi.LOG.info("[DBT-CLIENT] screenshot {}: {}", name, message.getString()));
        LOG.add("shot " + name);
    }

    private static void place(Minecraft mc, String name, double dx, double dy, double dz, float yaw, float pitch) {
        command(mc, String.format(Locale.ROOT, "tp %s %.2f %.2f %.2f %.1f %.1f", name, base.x + dx, base.y + dy, base.z + dz, yaw, pitch));
    }

    private static void forms(PlayerData data, String label) {
        LOG.add(String.format(Locale.ROOT, "%s: form=%s ssj equipped=%s ssj2 equipped=%s", label, data.form(), data.isFormEquipped(SSJ), data.isFormEquipped(SSJ2)));
    }

    private static void step(Minecraft mc) {
        String name = mc.player.getName().getString();
        PlayerData data = Services.PLATFORM.playerData(mc.player);
        switch (tick) {
            case 40 -> {
                Appearance a = Appearance.defaultsFor(Race.SAIYAN);
                a.hairStyle = HairStyle.GOKU.ordinal();
                Services.NETWORK.sendToServer(new Packets.CreateCharacter(Race.SAIYAN.ordinal(), a.presentation, 1.0f, a.build, a.skinColor, a.eyeStyle, a.eyeColor,
                        a.eyebrowStyle, a.hairStyle, a.hairColor, a.expression, a.auraColor, a.outfit, a.outfitPrimary, a.outfitSecondary, a.featureColor, true, false));
                mc.gui.setScreen(null);
            }
            case 60 -> {
                command(mc, "tenkaichi setstat " + name + " all 60");
                command(mc, "tenkaichi unlockform " + name + " dbtenkaichi:super_saiyan");
                command(mc, "tenkaichi restore " + name);
                mc.options.setCameraType(net.minecraft.client.CameraType.FIRST_PERSON);
            }
            case 70 -> shot(mc, "01_first_person_before");
            case 72 -> action(ActionType.TRANSFORM_UP, com.dbtenkaichi.form.TransformationSystem.CINEMATIC_AUTO);
            case 90 -> {
                float before = mc.player.getYRot();
                boolean turned = false;
                try {
                    var dx = net.minecraft.client.MouseHandler.class.getDeclaredField("accumulatedDX");
                    dx.setAccessible(true);
                    dx.setDouble(mc.mouseHandler, 400.0);
                    var turn = net.minecraft.client.MouseHandler.class.getDeclaredMethod("turnPlayer", double.class);
                    turn.setAccessible(true);
                    turn.invoke(mc.mouseHandler, 0.05);
                    turned = true;
                } catch (ReflectiveOperationException e) {
                    LOG.add("reflection failed: " + e);
                }
                LOG.add("cinematic active: " + com.dbtenkaichi.client.cinematic.CinematicDirector.localActive() + " mouse turn attempted: " + turned + " yaw before " + before + " after " + mc.player.getYRot());
            }
            case 96 -> shot(mc, "02_cutscene_first_person");
            case 130 -> shot(mc, "03_cutscene_later");
            case 400 -> {
                float before = mc.player.getYRot();
                try {
                    var dx = net.minecraft.client.MouseHandler.class.getDeclaredField("accumulatedDX");
                    dx.setAccessible(true);
                    dx.setDouble(mc.mouseHandler, 400.0);
                    var turn = net.minecraft.client.MouseHandler.class.getDeclaredMethod("turnPlayer", double.class);
                    turn.setAccessible(true);
                    turn.invoke(mc.mouseHandler, 0.05);
                } catch (ReflectiveOperationException e) {
                    LOG.add("reflection failed: " + e);
                }
                LOG.add("after cutscene active: " + com.dbtenkaichi.client.cinematic.CinematicDirector.localActive() + " form " + data.form() + " yaw before " + before + " after " + mc.player.getYRot());
                shot(mc, "04_after_cutscene_hand");
            }
            case 410 -> {
                finished = true;
                Tenkaichi.LOG.info("[DBT-CLIENT] RESULT {}", LOG);
                mc.stop();
            }
            default -> {
            }
        }
    }
}
