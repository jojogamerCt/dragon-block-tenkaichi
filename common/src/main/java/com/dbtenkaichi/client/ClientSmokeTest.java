package com.dbtenkaichi.client;

import com.dbtenkaichi.Tenkaichi;
import com.dbtenkaichi.client.render.MajinBody;
import com.dbtenkaichi.form.FormRegistry;
import com.dbtenkaichi.form.TransformationSystem;
import com.dbtenkaichi.network.Packets;
import com.dbtenkaichi.platform.Services;
import com.dbtenkaichi.player.Appearance;
import com.dbtenkaichi.race.Race;
import com.dbtenkaichi.world.Landmarks;
import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.client.server.IntegratedServer;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.levelgen.Heightmap;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;
import java.util.function.Consumer;

public final class ClientSmokeTest {
    private static final boolean ENABLED = Boolean.getBoolean("dbtenkaichi.clientSmoke");
    private static final List<String> LOG = new ArrayList<>();
    private static final Map<Integer, Consumer<Minecraft>> STEPS = new TreeMap<>();
    private static int tick = -1;
    private static int end;
    private static boolean finished;
    private static String name = "";

    private ClientSmokeTest() {
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
        try {
            Consumer<Minecraft> step = STEPS.get(tick);
            if (step != null) {
                step.accept(mc);
            }
        } catch (RuntimeException e) {
            Tenkaichi.LOG.error("[DBT-CLIENT] step {} failed", tick, e);
            LOG.add("FAIL step " + tick + ": " + e);
        }
        if (tick >= end) {
            finished = true;
            Tenkaichi.LOG.info("[DBT-CLIENT] RESULT {}", LOG);
            mc.stop();
        }
    }

    private static int at(int t, Consumer<Minecraft> step) {
        STEPS.merge(t, step, (a, b) -> a.andThen(b));
        return t;
    }

    private static void build() {
        at(20, mc -> {
            command(mc, "difficulty peaceful");
            command(mc, "time set 6000");
            command(mc, "weather clear");
            command(mc, "gamerule advance_time false");
        });
        at(30, mc -> command(mc, "tenkaichi reset " + name));
        at(40, mc -> {
            Appearance a = Appearance.defaultsFor(Race.MAJIN);
            Services.NETWORK.sendToServer(new Packets.CreateCharacter(Race.MAJIN.ordinal(), a.presentation, 1.0f, a.build, a.skinColor, a.eyeStyle, a.eyeColor,
                    a.eyebrowStyle, a.hairStyle, a.hairColor, a.expression, a.auraColor, a.outfit, a.outfitPrimary, a.outfitSecondary, a.featureColor, false, false));
            mc.gui.setScreen(null);
        });
        at(70, mc -> {
            command(mc, "tenkaichi setstat " + name + " all 40");
            command(mc, "tenkaichi restore " + name);
            stage(mc, Landmarks.KORIN_TOWER, -36, 30, 135.0f);
        });
        at(78, mc -> command(mc, "clear " + name));
        at(80, mc -> command(mc, "execute at " + name + " run fill ~-14 ~ ~-14 ~14 ~8 ~14 air replace #minecraft:replaceable_by_trees"));
        at(100, mc -> {
            mc.options.setCameraType(CameraType.THIRD_PERSON_FRONT);
            body(mc, "fat base");
        });
        preview(104, "01_fat");
        at(126, mc -> {
            mc.player.getInventory().setSelectedSlot(8);
            mc.options.setCameraType(CameraType.FIRST_PERSON);
        });
        at(134, mc -> shot(mc, "03_fat_hands"));
        at(136, mc -> mc.options.setCameraType(CameraType.THIRD_PERSON_FRONT));
        int t = cutscene(150, "dbtenkaichi:evil_buu", "evil", 170, new float[]{0.07f, 0.2f, 0.35f, 0.5f, 0.64f, 0.8f, 0.95f});
        t = cutscene(t, "dbtenkaichi:super_buu", "super", 160, new float[]{0.14f, 0.36f, 0.5f, 0.66f, 0.81f, 0.95f});
        t = cutscene(t, "dbtenkaichi:kid_buu", "kid", 180, new float[]{0.06f, 0.2f, 0.4f, 0.5f, 0.66f, 0.8f, 0.95f});
        at(t, mc -> {
            mc.player.getInventory().setSelectedSlot(8);
            mc.options.setCameraType(CameraType.FIRST_PERSON);
        });
        at(t + 8, mc -> shot(mc, "kid_hands"));
        end = t + 12;
    }

    private static int cutscene(int t, String form, String label, int ticks, float[] beats) {
        at(t, mc -> {
            command(mc, "tenkaichi restore " + name);
            forceForm(mc, form);
            LOG.add("begin " + label + " cutscene");
        });
        for (int i = 0; i < beats.length; i++) {
            String shot = String.format(Locale.ROOT, "%s_%d_%02d", label, i + 1, Math.round(beats[i] * 100));
            at(t + 2 + Math.round(beats[i] * ticks), mc -> shot(mc, shot));
        }
        int after = t + ticks + 14;
        at(after, mc -> {
            mc.options.setCameraType(CameraType.THIRD_PERSON_FRONT);
            body(mc, label);
        });
        preview(after + 4, label);
        return after + 30;
    }

    /** Shows the player large on an empty screen from the front, the side and the back. */
    private static void preview(int t, String label) {
        float[] yaws = {25.0f, 90.0f, 200.0f};
        String[] names = {"front", "side", "back"};
        for (int i = 0; i < yaws.length; i++) {
            float yaw = yaws[i];
            String shot = label + "_" + names[i];
            at(t + i * 6, mc -> mc.gui.setScreen(new Preview(yaw)));
            at(t + i * 6 + 4, mc -> shot(mc, shot));
        }
        at(t + yaws.length * 6, mc -> mc.gui.setScreen(null));
    }

    private static final class Preview extends net.minecraft.client.gui.screens.Screen {
        private final float yaw;

        Preview(float yaw) {
            super(net.minecraft.network.chat.Component.empty());
            this.yaw = yaw;
        }

        @Override
        public void extractRenderState(net.minecraft.client.gui.GuiGraphicsExtractor g, int mouseX, int mouseY, float partial) {
            g.fill(0, 0, this.width, this.height, 0xFF8FB8D8);
            Minecraft mc = Minecraft.getInstance();
            if (mc.player != null) {
                com.dbtenkaichi.client.screen.EntityPreview.draw(g, 0, 0, this.width, this.height, (int) (this.height * 0.32f), yaw, -5.0f, 1.0f, mc.player);
            }
        }

        @Override
        public boolean isPauseScreen() {
            return false;
        }
    }

    private static void body(Minecraft mc, String label) {
        EntityVisual visual = ClientState.forEntity(mc.player);
        MajinBody body = visual.majinBody();
        LOG.add(String.format(Locale.ROOT, "%s: race=%s build=%d form=%s body=%s skin=%06X sclera=%d height=%.2f width=%.2f", label, visual.race, visual.appearance.build,
                visual.form, body, visual.skinColor(), visual.scleraColor(), visual.modelHeight(), visual.modelWidth()));
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
            var form = FormRegistry.get(formId, false);
            if (form != null) {
                TransformationSystem.begin(sp, pd, form, TransformationSystem.CINEMATIC_AUTO, true, sp.level().getGameTime());
            } else {
                LOG.add("FAIL missing form " + formId);
            }
        });
    }

    private static void command(Minecraft mc, String cmd) {
        IntegratedServer server = mc.getSingleplayerServer();
        if (server != null) {
            server.execute(() -> server.getCommands().performPrefixedCommand(server.createCommandSourceStack(), cmd));
        }
    }

    private static void shot(Minecraft mc, String label) {
        Screenshot.grab(mc.gameDirectory, "dbt_" + label + ".png", mc.gameRenderer.mainRenderTarget(), 1, message -> Tenkaichi.LOG.info("[DBT-CLIENT] screenshot {}: {}", label, message.getString()));
        LOG.add("shot " + label);
    }
}
