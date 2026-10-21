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
            case 2 -> {
                command(mc, "kill @e[type=dbtenkaichi:saiyan_raider]");
                command(mc, "difficulty peaceful");
                command(mc, "time set noon");
                command(mc, "weather clear");
                command(mc, "gamerule advance_time false");
            }
            case 40 -> {
                Appearance a = Appearance.defaultsFor(Race.SAIYAN);
                a.hairStyle = HairStyle.GOKU.ordinal();
                Services.NETWORK.sendToServer(new Packets.CreateCharacter(Race.SAIYAN.ordinal(), a.presentation, 1.0f, a.build, a.skinColor, a.eyeStyle, a.eyeColor,
                        a.eyebrowStyle, a.hairStyle, a.hairColor, a.expression, a.voice, a.auraColor, a.outfit, a.outfitPrimary, a.outfitSecondary, a.featureColor, true, false));
                mc.gui.setScreen(null);
            }
            case 60 -> {
                command(mc, "tenkaichi setstat " + name + " all 160");
                command(mc, "tenkaichi unlockform " + name + " " + SSJ);
                command(mc, "tenkaichi unlockform " + name + " " + SSJ2);
                command(mc, "tenkaichi restore " + name);
                action(ActionType.FLIGHT_TOGGLE, 0);
            }
            case 64 -> command(mc, "execute as " + name + " at @s run tp @s ~ ~30 ~ 0 0");
            case 74 -> {
                base = mc.player.position();
                command(mc, String.format(Locale.ROOT, "summon dbtenkaichi:master %.2f %.2f %.2f {NoAI:1b,NoGravity:1b,Rotation:[180f,0f],master_kind:1,Tags:[\"dbt_test\"]}", base.x, base.y, base.z + 2.4));
                mc.options.setCameraType(CameraType.FIRST_PERSON);
                if (!mc.gui.hud.isHidden()) {
                    mc.gui.hud.toggle();
                }
            }
            case 84 -> shot(mc, "01_roshi_front");
            case 86 -> place(mc, name, 2.2, 0, 2.4, 90.0f, 0.0f);
            case 94 -> shot(mc, "02_roshi_side");
            case 96 -> place(mc, name, 0, 0, 4.6, 180.0f, 0.0f);
            case 104 -> shot(mc, "03_roshi_back");
            case 106 -> place(mc, name, -1.6, 0.2, 3.6, 240.0f, 10.0f);
            case 114 -> shot(mc, "04_roshi_three_quarter");
            case 116 -> place(mc, name, 0, 0.1, 1.2, 0.0f, 6.0f);
            case 124 -> shot(mc, "05_roshi_face");
            case 126 -> {
                if (mc.gui.hud.isHidden()) {
                    mc.gui.hud.toggle();
                }
                mc.options.setCameraType(CameraType.THIRD_PERSON_BACK);
                place(mc, name, 0, 0, -4, 0.0f, 0.0f);
                command(mc, "kill @e[tag=dbt_test]");
                forms(data, "start");
            }
            case 130 -> action(ActionType.FORM_EQUIP, 0);
            case 140 -> {
                forms(data, "after unequipping Super Saiyan");
                mc.gui.setScreen(new CharacterScreen(2));
            }
            case 150 -> shot(mc, "06_forms_ssj_unequipped");
            case 152 -> {
                mc.gui.setScreen(null);
                command(mc, "tenkaichi restore " + name);
            }
            case 156 -> action(ActionType.TRANSFORM_UP, TransformationSystem.CINEMATIC_SKIP);
            case 186 -> {
                forms(data, "after transform with Super Saiyan unequipped");
                shot(mc, "07_skipped_to_ssj2");
            }
            case 190 -> action(ActionType.TRANSFORM_DOWN, 0);
            case 200 -> forms(data, "after one revert");
            case 204 -> {
                command(mc, "tenkaichi restore " + name);
                action(ActionType.TRANSFORM_UP, TransformationSystem.CINEMATIC_SKIP);
            }
            case 234 -> {
                forms(data, "transformed again");
                action(ActionType.FORM_EQUIP, 1);
            }
            case 244 -> forms(data, "after unequipping the current form");
            case 246 -> {
                action(ActionType.FORM_EQUIP, 0);
                action(ActionType.FORM_EQUIP, 1);
            }
            case 256 -> {
                forms(data, "after equipping both again");
                command(mc, "tenkaichi restore " + name);
                action(ActionType.TRANSFORM_UP, TransformationSystem.CINEMATIC_SKIP);
            }
            case 286 -> {
                forms(data, "after transform with everything equipped");
                action(ActionType.TRANSFORM_DOWN, 0);
            }
            case 296 -> mc.gui.setScreen(new CharacterScreen(2));
            case 306 -> shot(mc, "08_forms_all_equipped");
            case 310 -> {
                mc.gui.setScreen(null);
                finished = true;
                Tenkaichi.LOG.info("[DBT-CLIENT] RESULT {}", LOG);
                mc.stop();
            }
            default -> {
            }
        }
    }
}
