package com.dbtenkaichi.client;

import com.dbtenkaichi.Tenkaichi;
import com.dbtenkaichi.client.screen.CharacterCreatorScreen;
import com.dbtenkaichi.client.screen.CharacterScreen;
import com.dbtenkaichi.network.ActionType;
import com.dbtenkaichi.network.Packets;
import com.dbtenkaichi.platform.Services;
import com.dbtenkaichi.player.Appearance;
import com.dbtenkaichi.player.HairStyle;
import com.dbtenkaichi.player.PlayerData;
import com.dbtenkaichi.race.Race;
import com.dbtenkaichi.registry.ModSounds;
import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.server.IntegratedServer;
import net.minecraft.resources.Identifier;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public final class ClientSmokeTest {
    private static final boolean ENABLED = Boolean.getBoolean("dbtenkaichi.clientSmoke");
    private static final List<String> LOG = new ArrayList<>();
    private static int tick = -1;
    private static boolean finished;

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

    private static void screenInfo(Minecraft mc, String label) {
        if (mc.gui.screen() == null) {
            LOG.add(label + ": no screen");
            return;
        }
        int visible = 0;
        int total = 0;
        for (var child : mc.gui.screen().children()) {
            if (child instanceof AbstractWidget widget) {
                total++;
                if (widget.visible) {
                    visible++;
                }
            }
        }
        LOG.add(String.format(Locale.ROOT, "%s: %s %dx%d widgets %d visible %d", label, mc.gui.screen().getClass().getSimpleName(), mc.gui.screen().width, mc.gui.screen().height, total, visible));
    }

    private static void sounds(Minecraft mc) {
        List<String> missing = new ArrayList<>();
        for (String name : ModSounds.NAMES) {
            if (mc.getSoundManager().getSoundEvent(Identifier.fromNamespaceAndPath(Tenkaichi.MOD_ID, name)) == null) {
                missing.add(name);
            }
        }
        LOG.add("sound events " + ModSounds.NAMES.size() + " missing " + missing);
    }

    private static void summonRaider(Minecraft mc, double distance, double side) {
        Vec3 look = mc.player.getLookAngle().multiply(1, 0, 1).normalize();
        Vec3 right = new Vec3(-look.z, 0, look.x);
        Vec3 at = mc.player.position().add(look.scale(distance)).add(right.scale(side));
        command(mc, String.format(Locale.ROOT, "summon dbtenkaichi:saiyan_raider %.2f %.2f %.2f {NoAI:1b,NoGravity:1b}", at.x, mc.player.getY(), at.z));
    }

    private static void step(Minecraft mc) {
        String name = mc.player.getName().getString();
        PlayerData data = Services.PLATFORM.playerData(mc.player);
        switch (tick) {
            case 2 -> {
                command(mc, "kill @e[type=dbtenkaichi:saiyan_raider]");
                command(mc, "difficulty peaceful");
                command(mc, "time set day");
                command(mc, "weather clear");
                command(mc, "gamerule advance_time false");
            }
            case 40 -> {
                LOG.add("creator opened on first join: " + (mc.gui.screen() instanceof CharacterCreatorScreen));
                screenInfo(mc, "creator");
                shot(mc, "01_creator");
                sounds(mc);
            }
            case 60 -> {
                Appearance a = Appearance.defaultsFor(Race.SAIYAN);
                a.hairStyle = HairStyle.GOKU.ordinal();
                a.auraColor = 0xE8F4FF;
                a.outfit = 0;
                Services.NETWORK.sendToServer(new Packets.CreateCharacter(Race.SAIYAN.ordinal(), a.presentation, 1.0f, a.build, a.skinColor, a.eyeStyle, a.eyeColor,
                        a.eyebrowStyle, a.hairStyle, a.hairColor, a.expression, a.voice, a.auraColor, a.outfit, a.outfitPrimary, a.outfitSecondary, a.featureColor, true, false));
                mc.gui.setScreen(null);
            }
            case 80 -> {
                for (String stat : new String[]{"strength", "defense", "constitution", "ki_control", "ki_capacity", "ki_output", "speed", "agility", "stamina", "martial_arts", "spirit", "potential"}) {
                    command(mc, "tenkaichi setstat " + name + " " + stat + " 30");
                }
                command(mc, "tenkaichi learn " + name + " dbtenkaichi:kamehameha");
                command(mc, "tenkaichi unlockform " + name + " dbtenkaichi:super_saiyan");
                command(mc, "tenkaichi restore " + name);
                command(mc, "give " + name + " dbtenkaichi:senzu_bean 8");
                command(mc, "damage " + name + " 6");
            }
            case 90 -> {
                Services.NETWORK.sendToServer(new Packets.AssignSlot(0, "dbtenkaichi:ki_blast"));
                Services.NETWORK.sendToServer(new Packets.AssignSlot(1, "dbtenkaichi:kamehameha"));
                mc.options.setCameraType(CameraType.THIRD_PERSON_FRONT);
                action(ActionType.FLIGHT_TOGGLE, 0);
            }
            case 94 -> command(mc, "execute as " + name + " at @s run tp @s ~ ~24 ~ 0 0");
            case 110 -> {
                LOG.add("character created: " + data.isCreated() + " race=" + data.race().id() + " skills=" + data.skills());
                shot(mc, "02_hud");
            }
            case 120 -> action(ActionType.CHARGE_START, 0);
            case 150 -> shot(mc, "03_aura_particles");
            case 160 -> action(ActionType.CHARGE_END, 0);
            case 170 -> mc.gui.setScreen(new CharacterScreen(0));
            case 180 -> {
                screenInfo(mc, "status tab");
                shot(mc, "04a_screen_status");
            }
            case 182 -> {
                if (mc.gui.screen() != null) {
                    mc.gui.screen().mouseScrolled(mc.gui.screen().width - 40, mc.gui.screen().height / 2.0, 0, -20);
                }
            }
            case 188 -> {
                screenInfo(mc, "status tab scrolled");
                shot(mc, "04b_screen_status_scrolled");
            }
            case 190 -> mc.gui.setScreen(new CharacterScreen(1));
            case 200 -> {
                screenInfo(mc, "techniques tab");
                shot(mc, "04c_screen_techniques");
            }
            case 202 -> mc.gui.setScreen(new CharacterScreen(2));
            case 212 -> {
                screenInfo(mc, "forms tab");
                shot(mc, "04d_screen_forms");
            }
            case 214 -> mc.gui.setScreen(new CharacterScreen(3));
            case 224 -> {
                screenInfo(mc, "settings tab");
                shot(mc, "04e_screen_settings");
            }
            case 226 -> {
                net.minecraft.nbt.CompoundTag menu = new net.minecraft.nbt.CompoundTag();
                menu.putString("mode", "menu");
                menu.putInt("master", -1);
                menu.putInt("kind", com.dbtenkaichi.entity.MasterKind.ROSHI.ordinal());
                menu.putLong("tp", data.trainingPoints());
                net.minecraft.nbt.ListTag lessons = new net.minecraft.nbt.ListTag();
                for (com.dbtenkaichi.training.Lessons.Lesson lesson : com.dbtenkaichi.training.Lessons.forMaster(com.dbtenkaichi.entity.MasterKind.ROSHI)) {
                    net.minecraft.nbt.CompoundTag entry = new net.minecraft.nbt.CompoundTag();
                    entry.putString("id", lesson.id());
                    entry.putInt("state", com.dbtenkaichi.training.Lessons.AVAILABLE);
                    entry.putInt("price", lesson.price());
                    entry.putBoolean("trial", lesson.trial());
                    entry.putLong("bp", lesson.battlePower());
                    lessons.add(entry);
                }
                menu.put("lessons", lessons);
                mc.gui.setScreen(new com.dbtenkaichi.client.screen.TrainingScreen(menu));
            }
            case 234 -> {
                screenInfo(mc, "training");
                shot(mc, "05_training");
            }
            case 236 -> {
                mc.gui.setScreen(null);
                mc.options.setCameraType(CameraType.THIRD_PERSON_BACK);
                mc.player.setXRot(5.0f);
                command(mc, "tenkaichi restore " + name);
            }
            case 240 -> action(ActionType.SKILL_PRESS, 0);
            case 242 -> action(ActionType.SKILL_RELEASE, 0);
            case 245 -> shot(mc, "06_ki_blast_cube_core");
            case 260 -> summonRaider(mc, 26, 0);
            case 266 -> action(ActionType.SKILL_PRESS, 1);
            case 300 -> action(ActionType.SKILL_RELEASE, 1);
            case 306 -> {
                mc.player.setYRot(mc.player.getYRot() + 90.0f);
                mc.player.setXRot(30.0f);
            }
            case 308 -> shot(mc, "07_beam_prism_core");
            case 330 -> {
                mc.player.setYRot(mc.player.getYRot() - 90.0f);
                mc.player.setXRot(10.0f);
                command(mc, "kill @e[type=dbtenkaichi:saiyan_raider]");
                command(mc, "tenkaichi restore " + name);
            }
            case 334 -> summonRaider(mc, 2.0, 1.2);
            case 336 -> {
                com.dbtenkaichi.client.input.LockOn.toggle();
                LOG.add("locked target: " + com.dbtenkaichi.client.input.LockOn.target());
            }
            case 338, 344, 350, 356 -> action(ActionType.LIGHT_ATTACK, 0);
            case 352 -> shot(mc, "08_hit_sparks");
            case 362 -> action(ActionType.HEAVY_ATTACK, 0);
            case 366 -> shot(mc, "09_heavy_hit");
            case 376 -> action(ActionType.DASH, 0);
            case 390 -> {
                LOG.add("health after test " + mc.player.getHealth() + " ki " + ClientState.ki);
                finished = true;
                Tenkaichi.LOG.info("[DBT-CLIENT] RESULT {}", LOG);
                mc.stop();
            }
            default -> {
            }
        }
    }
}
