package com.dbtenkaichi.client;

import com.dbtenkaichi.Tenkaichi;
import com.dbtenkaichi.client.input.LockOn;
import com.dbtenkaichi.form.TransformationSystem;
import com.dbtenkaichi.network.ActionType;
import com.dbtenkaichi.network.Packets;
import com.dbtenkaichi.platform.Services;
import com.dbtenkaichi.player.Appearance;
import com.dbtenkaichi.player.HairStyle;
import com.dbtenkaichi.race.Race;
import com.dbtenkaichi.world.Landmarks;
import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.client.server.IntegratedServer;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.Vec3;

import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;
import java.util.function.Consumer;

public final class MarketingCapture {
    private static final boolean ENABLED = Boolean.getBoolean("dbtenkaichi.marketing");
    private static final Map<Integer, Consumer<Minecraft>> STEPS = new TreeMap<>();
    private static int tick = -1;
    private static int end;
    private static boolean finished;
    private static String name = "";

    private MarketingCapture() {
    }

    public static boolean enabled() {
        return ENABLED;
    }

    public static void tick(Minecraft mc) {
        if (!ENABLED || finished || mc.player == null || mc.level == null) {
            return;
        }
        if (tick < 0) {
            name = mc.player.getName().getString();
            build();
        }
        tick++;
        Consumer<Minecraft> step = STEPS.get(tick);
        if (step != null) {
            try {
                step.accept(mc);
            } catch (RuntimeException e) {
                Tenkaichi.LOG.error("[DBT-MARKETING] step {} failed", tick, e);
            }
        }
        if (tick >= end) {
            finished = true;
            Tenkaichi.LOG.info("[DBT-MARKETING] DONE");
            mc.stop();
        }
    }

    private static int at(int t, Consumer<Minecraft> step) {
        STEPS.merge(t, step, (a, b) -> a.andThen(b));
        return t;
    }

    private static void build() {
        int t = 2;
        at(t, mc -> {
            command(mc, "difficulty peaceful");
            command(mc, "time set 6000");
            command(mc, "weather clear");
            command(mc, "gamerule advance_time false");
            command(mc, "gamerule advance_weather false");
            command(mc, "kill @e[type=dbtenkaichi:saiyan_raider]");
        });
        at(t + 10, mc -> {
            Appearance a = Appearance.defaultsFor(Race.SAIYAN);
            a.hairStyle = HairStyle.GOKU.ordinal();
            a.outfit = 0;
            Services.NETWORK.sendToServer(new Packets.CreateCharacter(Race.SAIYAN.ordinal(), a.presentation, 1.0f, a.build, a.skinColor, a.eyeStyle, a.eyeColor,
                    a.eyebrowStyle, a.hairStyle, a.hairColor, a.expression, a.auraColor, a.outfit, a.outfitPrimary, a.outfitSecondary, a.featureColor, true, false));
            mc.gui.setScreen(null);
        });
        at(t + 20, mc -> {
            for (String stat : new String[]{"strength", "defense", "constitution", "ki_control", "ki_capacity", "ki_output", "speed", "agility", "stamina", "martial_arts", "spirit", "potential"}) {
                command(mc, "tenkaichi setstat " + name + " " + stat + " 60");
            }
            for (String skill : new String[]{"kamehameha", "final_flash", "big_bang_attack", "kaioken"}) {
                command(mc, "tenkaichi learn " + name + " dbtenkaichi:" + skill);
            }
            command(mc, "tenkaichi restore " + name);
            hideHud(mc, true);
        });
        t = 40;
        at(t, mc -> stage(mc, Landmarks.KORIN_TOWER, -18, 14, 135.0f));
        at(t + 30, mc -> mc.options.setCameraType(CameraType.THIRD_PERSON_FRONT));
        t = transformation(80, "dbtenkaichi:super_saiyan", "ssj", new int[]{7, 51, 141}, new String[]{"face", "rocks", "reveal"});
        t = transformation(t, "dbtenkaichi:super_saiyan_2", "ssj2", new int[]{33, 50, 141}, new String[]{"backdrop", "streak", "reveal"});
        t = transformation(t, "dbtenkaichi:super_saiyan_3", "ssj3", new int[]{48, 86, 159}, new String[]{"clouds", "hair", "reveal"});
        at(t, mc -> {
            command(mc, "tenkaichi restore " + name);
            action(ActionType.KAIOKEN, 0);
        });
        at(t + 10, mc -> action(ActionType.KAIOKEN, 0));
        at(t + 14, mc -> action(ActionType.CHARGE_START, 0));
        at(t + 36, mc -> mc.gui.setScreen(new Preview(20.0f)));
        at(t + 40, mc -> {
            shot(mc, "kaioken");
            mc.gui.setScreen(null);
        });
        at(t + 42, mc -> {
            action(ActionType.CHARGE_END, 0);
            action(ActionType.TRANSFORM_DOWN, 0);
        });
        t += 60;
        at(t, mc -> {
            forceForm(mc, "dbtenkaichi:super_saiyan");
            mc.options.setCameraType(CameraType.THIRD_PERSON_BACK);
            Vec3 look = mc.player.getLookAngle().multiply(1, 0, 1).normalize();
            Vec3 foe = mc.player.position().add(look.scale(28));
            command(mc, String.format(Locale.ROOT, "summon dbtenkaichi:saiyan_raider %.1f %.1f %.1f {NoAI:1b,NoGravity:1b}", foe.x, mc.player.getY(), foe.z));
            assign(mc, "dbtenkaichi:kamehameha");
        });
        at(t + 30, mc -> LockOn.toggle());
        at(t + 34, mc -> action(ActionType.SKILL_PRESS, 0));
        at(t + 50, mc -> mc.options.setCameraType(CameraType.THIRD_PERSON_FRONT));
        at(t + 80, mc -> shot(mc, "kamehameha_charge"));
        at(t + 82, mc -> {
            action(ActionType.SKILL_RELEASE, 0);
            LockOn.release();
            mc.player.setYRot(mc.player.getYRot() + 70.0f);
            mc.player.setXRot(8.0f);
            mc.options.setCameraType(CameraType.THIRD_PERSON_BACK);
        });
        at(t + 84, mc -> counterBeam(mc));
        at(t + 98, mc -> shot(mc, "beam_clash"));
        at(t + 104, mc -> shot(mc, "beam_clash_late"));
        at(t + 120, mc -> {
            action(ActionType.SKILL_RELEASE, 0);
            command(mc, "kill @e[type=dbtenkaichi:saiyan_raider]");
        });
        t += 140;
        t = technique(t, "dbtenkaichi:final_flash", "final_flash", true);
        t = technique(t, "dbtenkaichi:big_bang_attack", "big_bang", false);
        at(t, mc -> {
            forceForm(mc, null);
            command(mc, "gamemode spectator " + name);
            tour(mc, Landmarks.KAME_HOUSE, 9, 6, 16, 1, 5, -2);
        });
        at(t + 40, mc -> shot(mc, "kame_house"));
        at(t + 42, mc -> tour(mc, Landmarks.KORIN_TOWER, 26, -60, 26, 0, -20, -6));
        at(t + 82, mc -> shot(mc, "korin_tower"));
        at(t + 84, mc -> tour(mc, Landmarks.KAMI_LOOKOUT, 0, 10, 48, 0, 0, -8));
        at(t + 124, mc -> shot(mc, "kami_lookout"));
        at(t + 126, mc -> {
            BlockPos site = Landmarks.sites().get(Landmarks.KAME_HOUSE);
            if (site != null) {
                command(mc, String.format(Locale.ROOT, "summon dbtenkaichi:master %d.5 %d %d.5 {NoAI:1b,Rotation:[-20f,0f],master_kind:1,Tags:[\"mkt_roshi\"]}", site.getX() + 1, site.getY(), site.getZ() + 4));
            }
            tour(mc, Landmarks.KAME_HOUSE, 0, 1, 7, 1, 1, 4);
        });
        at(t + 150, mc -> {
            com.dbtenkaichi.entity.MasterEntity roshi = null;
            for (com.dbtenkaichi.entity.MasterEntity master : mc.level.getEntitiesOfClass(com.dbtenkaichi.entity.MasterEntity.class, mc.player.getBoundingBox().inflate(48))) {
                if (master.kind() == com.dbtenkaichi.entity.MasterKind.ROSHI && (roshi == null || master.distanceTo(mc.player) < roshi.distanceTo(mc.player))) {
                    roshi = master;
                }
            }
            if (roshi != null) {
                mc.gui.setScreen(new Preview(25.0f, roshi));
            }
        });
        at(t + 160, mc -> shot(mc, "roshi"));
        at(t + 161, mc -> mc.gui.setScreen(null));
        at(t + 162, mc -> command(mc, "kill @e[tag=mkt_roshi]"));
        at(t + 168, mc -> command(mc, "gamemode survival " + name));
        t += 180;
        t = trial(t, "dbtenkaichi:super_saiyan_2", "trial_cell");
        t = trial(t, "dbtenkaichi:super_saiyan", "trial_namek");
        Race[] races = Race.values();
        at(t, mc -> {
            stage(mc, Landmarks.KORIN_TOWER, -18, 14, 135.0f);
            mc.options.setCameraType(CameraType.THIRD_PERSON_BACK);
        });
        t += 40;
        for (int i = 0; i < races.length; i++) {
            Race race = races[i];
            for (int presentation = 0; presentation < 2; presentation++) {
                int p = presentation;
                at(t, mc -> {
                    Appearance a = Appearance.defaultsFor(race);
                    a.presentation = p;
                    a.hairStyle = Appearance.defaultHair(race, p);
                    ClientState.preview = a;
                    ClientState.previewRace = race;
                    mc.gui.setScreen(new Preview(20.0f));
                });
                at(t + 3, mc -> shot(mc, "race_" + race.id() + (p == 1 ? "_f" : "")));
                t += 5;
            }
        }
        at(t, mc -> {
            ClientState.preview = null;
            ClientState.previewRace = null;
            mc.gui.setScreen(new com.dbtenkaichi.client.screen.CharacterScreen(2));
        });
        at(t + 6, mc -> shot(mc, "menu_forms"));
        at(t + 7, mc -> mc.gui.setScreen(new com.dbtenkaichi.client.screen.CharacterScreen(0)));
        at(t + 12, mc -> shot(mc, "menu_status"));
        at(t + 13, mc -> mc.gui.setScreen(new com.dbtenkaichi.client.screen.CharacterScreen(1)));
        at(t + 18, mc -> shot(mc, "menu_techniques"));
        t += 12;
        at(t + 8, mc -> {
            net.minecraft.nbt.CompoundTag tag = new net.minecraft.nbt.CompoundTag();
            StringBuilder ids = new StringBuilder();
            for (Race race : races) {
                ids.append(ids.isEmpty() ? "" : ",").append(race.id());
            }
            tag.putString("races", ids.toString());
            mc.gui.setScreen(new com.dbtenkaichi.client.screen.CharacterCreatorScreen(tag));
        });
        at(t + 16, mc -> shot(mc, "creator"));
        end = majin(t + 20);
    }

    /** Recreates the character as a Majin and films the fat body and the three Buu transformations in a row. */
    private static int majin(int t) {
        at(t, mc -> {
            mc.gui.setScreen(null);
            command(mc, "tenkaichi reset " + name);
        });
        at(t + 10, mc -> {
            Appearance a = Appearance.defaultsFor(Race.MAJIN);
            Services.NETWORK.sendToServer(new Packets.CreateCharacter(Race.MAJIN.ordinal(), a.presentation, 1.0f, a.build, a.skinColor, a.eyeStyle, a.eyeColor,
                    a.eyebrowStyle, a.hairStyle, a.hairColor, a.expression, a.auraColor, a.outfit, a.outfitPrimary, a.outfitSecondary, a.featureColor, false, false));
            mc.gui.setScreen(null);
        });
        at(t + 20, mc -> {
            command(mc, "tenkaichi setstat " + name + " all 60");
            command(mc, "tenkaichi restore " + name);
            command(mc, "clear " + name);
            stage(mc, Landmarks.KORIN_TOWER, -36, 30, 135.0f);
            hideHud(mc, true);
        });
        at(t + 30, mc -> command(mc, "execute at " + name + " run fill ~-14 ~ ~-14 ~14 ~8 ~14 air replace #minecraft:replaceable_by_trees"));
        at(t + 44, mc -> {
            mc.options.setCameraType(CameraType.THIRD_PERSON_FRONT);
            mc.gui.setScreen(new Preview(20.0f));
        });
        at(t + 48, mc -> shot(mc, "majin_fat"));
        at(t + 50, mc -> mc.gui.setScreen(null));
        t = transformation(t + 60, "dbtenkaichi:evil_buu", "evil_buu", new int[]{34, 85, 160}, new String[]{"steam", "cloud", "reveal"}, false);
        t = transformation(t, "dbtenkaichi:super_buu", "super_buu", new int[]{22, 106, 150}, new String[]{"chocolate", "smoke", "reveal"}, false);
        t = transformation(t, "dbtenkaichi:kid_buu", "kid_buu", new int[]{36, 76, 169}, new String[]{"steam", "swell", "reveal"}, false);
        return t;
    }

    private static int transformation(int t, String form, String label, int[] offsets, String[] names) {
        return transformation(t, form, label, offsets, names, true);
    }

    private static int transformation(int t, String form, String label, int[] offsets, String[] names, boolean revert) {
        at(t, mc -> {
            command(mc, "tenkaichi restore " + name);
            if (revert) {
                forceForm(mc, null);
            }
        });
        at(t + 6, mc -> {
            hideHud(mc, false);
            forceForm(mc, form);
        });
        for (int i = 0; i < offsets.length; i++) {
            String shot = label + "_" + names[i];
            at(t + 6 + offsets[i], mc -> shot(mc, shot));
        }
        int after = t + 6 + offsets[offsets.length - 1] + 12;
        at(after, mc -> {
            hideHud(mc, true);
            mc.options.setCameraType(CameraType.THIRD_PERSON_FRONT);
            mc.gui.setScreen(new Preview(20.0f));
        });
        at(after + 4, mc -> shot(mc, label + "_full"));
        at(after + 6, mc -> mc.gui.setScreen(null));
        return after + 14;
    }

    private static int technique(int t, String id, String label, boolean beam) {
        at(t, mc -> {
            command(mc, "tenkaichi restore " + name);
            assign(mc, id);
            mc.player.setYRot(mc.player.getYRot() + 40.0f);
            mc.player.setXRot(beam ? -4.0f : 12.0f);
            mc.options.setCameraType(CameraType.THIRD_PERSON_BACK);
        });
        at(t + 4, mc -> action(ActionType.SKILL_PRESS, 0));
        if (beam) {
            at(t + 40, mc -> shot(mc, label + "_charge"));
            at(t + 50, mc -> {
                action(ActionType.SKILL_RELEASE, 0);
                mc.options.setCameraType(CameraType.THIRD_PERSON_BACK);
            });
            at(t + 62, mc -> shot(mc, label + "_fire"));
            return t + 110;
        }
        at(t + 10, mc -> shot(mc, label + "_fire"));
        at(t + 11, mc -> {
            action(ActionType.SKILL_RELEASE, 0);
            mc.options.setCameraType(CameraType.THIRD_PERSON_BACK);
        });
        at(t + 20, mc -> shot(mc, label + "_impact"));
        at(t + 50, mc -> {
            command(mc, "gamemode spectator " + name);
            command(mc, "execute as " + name + " at @s run tp @s ^ ^14 ^-10 facing ^ ^ ^14");
        });
        at(t + 80, mc -> shot(mc, label + "_crater"));
        at(t + 82, mc -> command(mc, "gamemode survival " + name));
        return t + 100;
    }

    private static int trial(int t, String form, String label) {
        at(t, mc -> {
            command(mc, "tenkaichi restore " + name);
            forceForm(mc, null);
            command(mc, "tenkaichi trial form " + name + " " + form);
        });
        at(t + 40, mc -> {
            mc.options.setCameraType(CameraType.THIRD_PERSON_BACK);
            mc.player.setYRot(mc.player.getYRot() + 25.0f);
            shot(mc, label + "_arena");
        });
        at(t + 112, mc -> shot(mc, label + "_fight"));
        at(t + 116, mc -> command(mc, "tenkaichi trial end " + name + " win"));
        return t + 150;
    }

    private static void counterBeam(Minecraft mc) {
        IntegratedServer server = mc.getSingleplayerServer();
        if (server == null) {
            return;
        }
        server.execute(() -> {
            ServerPlayer sp = server.getPlayerList().getPlayerByName(name);
            if (sp == null) {
                return;
            }
            var raiders = sp.level().getEntitiesOfClass(com.dbtenkaichi.entity.RaiderEntity.class, sp.getBoundingBox().inflate(40));
            if (!raiders.isEmpty()) {
                var raider = raiders.getFirst();
                raider.lookAtPoint(sp.getEyePosition());
                sp.level().addFreshEntity(com.dbtenkaichi.entity.KiBeamEntity.create(sp.level(), raider, com.dbtenkaichi.ki.Techniques.RAIDER_CANNON, 0.8));
            }
        });
    }

    private static void stage(Minecraft mc, String site, int dx, int dz, float yaw) {
        IntegratedServer server = mc.getSingleplayerServer();
        BlockPos pos = Landmarks.sites().get(site);
        if (server == null || pos == null) {
            return;
        }
        server.execute(() -> {
            ServerPlayer sp = server.getPlayerList().getPlayerByName(name);
            if (sp == null) {
                return;
            }
            int x = pos.getX() + dx;
            int z = pos.getZ() + dz - 6;
            sp.level().getChunk(x >> 4, z >> 4);
            int y = sp.level().getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z);
            sp.teleportTo(x + 0.5, y, z + 0.5);
            sp.setYRot(yaw);
            sp.setYHeadRot(yaw);
        });
    }

    private static void tour(Minecraft mc, String site, int dx, int dy, int dz, int lx, int ly, int lz) {
        BlockPos pos = Landmarks.sites().get(site);
        if (pos != null) {
            command(mc, String.format(Locale.ROOT, "tp %s %d %d %d facing %d %d %d", name, pos.getX() + dx, pos.getY() + dy, pos.getZ() + dz, pos.getX() + lx, pos.getY() + ly, pos.getZ() + lz));
        }
    }

    private static void forceForm(Minecraft mc, String formId) {
        IntegratedServer server = mc.getSingleplayerServer();
        if (server == null) {
            return;
        }
        server.execute(() -> {
            ServerPlayer sp = server.getPlayerList().getPlayerByName(name);
            if (sp == null) {
                return;
            }
            var pd = com.dbtenkaichi.server.PlayerManager.data(sp);
            if (formId == null) {
                TransformationSystem.forceRevert(sp, pd, null);
                return;
            }
            var form = com.dbtenkaichi.form.FormRegistry.get(formId, false);
            if (form != null) {
                TransformationSystem.begin(sp, pd, form, TransformationSystem.CINEMATIC_AUTO, true, sp.level().getGameTime());
            }
        });
    }

    private static void assign(Minecraft mc, String id) {
        Services.NETWORK.sendToServer(new Packets.AssignSlot(0, id));
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

    private static void hideHud(Minecraft mc, boolean hidden) {
        try {
            java.lang.reflect.Field field = mc.gui.hud.getClass().getDeclaredField("isHidden");
            field.setAccessible(true);
            field.setBoolean(mc.gui.hud, hidden);
        } catch (ReflectiveOperationException | RuntimeException e) {
            Tenkaichi.LOG.warn("[DBT-MARKETING] could not hide the HUD", e);
        }
    }

    private static void shot(Minecraft mc, String label) {
        Screenshot.grab(mc.gameDirectory, "mkt_" + label + ".png", mc.gameRenderer.mainRenderTarget(), 1, message -> Tenkaichi.LOG.info("[DBT-MARKETING] {}: {}", label, message.getString()));
    }

    private static final class Preview extends net.minecraft.client.gui.screens.Screen {
        private final float yaw;
        private final net.minecraft.world.entity.LivingEntity subject;

        Preview(float yaw) {
            this(yaw, null);
        }

        Preview(float yaw, net.minecraft.world.entity.LivingEntity subject) {
            super(net.minecraft.network.chat.Component.empty());
            this.yaw = yaw;
            this.subject = subject;
        }

        @Override
        public void extractRenderState(net.minecraft.client.gui.GuiGraphicsExtractor g, int mouseX, int mouseY, float partial) {
            Minecraft mc = Minecraft.getInstance();
            net.minecraft.world.entity.LivingEntity target = subject != null ? subject : mc.player;
            if (target != null) {
                com.dbtenkaichi.client.screen.EntityPreview.draw(g, 0, 0, this.width, this.height, (int) (this.height * 0.3f), yaw, -5.0f, 1.0f, target);
            }
        }

        @Override
        public boolean isPauseScreen() {
            return false;
        }
    }
}
