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

    private static com.dbtenkaichi.entity.MasterEntity master;
    private static Vec3 spot;

    private static String fmt(Vec3 v) {
        return v == null ? "none" : String.format(Locale.ROOT, "%.2f %.2f %.2f", v.x, v.y, v.z);
    }

    private static com.dbtenkaichi.entity.MasterEntity nearestMaster(Minecraft mc) {
        com.dbtenkaichi.entity.MasterEntity best = null;
        for (com.dbtenkaichi.entity.MasterEntity m : mc.level.getEntitiesOfClass(com.dbtenkaichi.entity.MasterEntity.class, mc.player.getBoundingBox().inflate(32))) {
            if (best == null || m.distanceTo(mc.player) < best.distanceTo(mc.player)) {
                best = m;
            }
        }
        return best;
    }

    private static void session(Minecraft mc, String name, boolean sparring, boolean start) {
        IntegratedServer server = mc.getSingleplayerServer();
        if (server == null) {
            return;
        }
        server.execute(() -> {
            net.minecraft.server.level.ServerPlayer sp = server.getPlayerList().getPlayerByName(name);
            if (sp == null) {
                return;
            }
            for (com.dbtenkaichi.entity.MasterEntity m : sp.level().getEntitiesOfClass(com.dbtenkaichi.entity.MasterEntity.class, sp.getBoundingBox().inflate(32), e -> e.entityTags().contains("dbt_test"))) {
                if (!start) {
                    m.stopSession(sp);
                } else if (sparring) {
                    m.startSparring(sp);
                } else {
                    m.startDrill(sp);
                }
                LOG.add((start ? "started " : "stopped ") + (sparring ? "sparring" : "drill") + " with master at " + fmt(m.position()));
            }
        });
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
            case 100 -> {
                command(mc, "tenkaichi setstat " + name + " all 40");
                command(mc, "tenkaichi restore " + name);
                Vec3 look = mc.player.getLookAngle().multiply(1, 0, 1).normalize();
                Vec3 at = mc.player.position().add(look.scale(3));
                command(mc, String.format(Locale.ROOT, "summon dbtenkaichi:master %.2f %.2f %.2f {Tags:[\"dbt_test\"]}", at.x, mc.player.getY(), at.z));
            }
            case 140 -> {
                master = nearestMaster(mc);
                spot = master == null ? null : master.position();
                LOG.add("master spot " + fmt(spot));
                session(mc, name, true, true);
            }
            case 160 -> command(mc, "execute as @e[tag=dbt_test] at @s run tp @s ~9 ~ ~6");
            case 170 -> LOG.add("master moved during sparring: " + fmt(nearestMaster(mc) == null ? null : nearestMaster(mc).position()));
            case 172 -> session(mc, name, true, false);
            case 190 -> {
                com.dbtenkaichi.entity.MasterEntity m = nearestMaster(mc);
                LOG.add("after sparring: " + fmt(m == null ? null : m.position()) + " distance from spot " + (m == null || spot == null ? -1 : String.format(Locale.ROOT, "%.2f", m.position().distanceTo(spot))));
                mc.gui.setScreen(null);
            }
            case 240 -> session(mc, name, false, true);
            case 260 -> command(mc, "execute as @e[tag=dbt_test] at @s run tp @s ~-7 ~3 ~5");
            case 270 -> session(mc, name, false, false);
            case 290 -> {
                com.dbtenkaichi.entity.MasterEntity m = nearestMaster(mc);
                LOG.add("after dodge drill: " + fmt(m == null ? null : m.position()) + " distance from spot " + (m == null || spot == null ? -1 : String.format(Locale.ROOT, "%.2f", m.position().distanceTo(spot))));
                mc.gui.setScreen(null);
                mc.options.setCameraType(net.minecraft.client.CameraType.THIRD_PERSON_BACK);
                shot(mc, "01_master_back_home");
            }
            case 300 -> {
                finished = true;
                Tenkaichi.LOG.info("[DBT-CLIENT] RESULT {}", LOG);
                mc.stop();
            }
            default -> {
            }
        }
    }
}
