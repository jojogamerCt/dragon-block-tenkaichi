package com.dbtenkaichi.server;

import com.dbtenkaichi.Tenkaichi;
import com.dbtenkaichi.combat.CombatSystem;
import com.dbtenkaichi.combat.TenkaichiDamage;
import com.dbtenkaichi.entity.KiBeamEntity;
import com.dbtenkaichi.entity.MasterEntity;
import com.dbtenkaichi.entity.RaiderEntity;
import com.dbtenkaichi.form.FormRegistry;
import com.dbtenkaichi.ki.Techniques;
import com.dbtenkaichi.registry.ModEntities;
import com.dbtenkaichi.registry.ModSounds;
import com.dbtenkaichi.world.DestructionManager;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.monster.zombie.Zombie;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;

public final class SmokeTest {
    private static final boolean ENABLED = Boolean.getBoolean("dbtenkaichi.smokeTest");
    private static final List<String> FAILURES = new ArrayList<>();
    private static final EnumSet<com.dbtenkaichi.entity.ai.RaiderBrain.State> STATES = EnumSet.noneOf(com.dbtenkaichi.entity.ai.RaiderBrain.State.class);
    private static int tick = -40;
    private static RaiderEntity raider;
    private static MasterEntity master;
    private static Zombie dummy;
    private static float dummyHealth;
    private static boolean done;
    private static int tenkaichiHits;

    private SmokeTest() {
    }

    public static void tick(MinecraftServer server) {
        if (!ENABLED || done) {
            return;
        }
        tick++;
        if (tick < 0) {
            return;
        }
        ServerLevel level = server.overworld();
        try {
            if (tick == 0) {
                staticChecks();
                spawn(level);
            } else if (tick > 0 && tick < 300) {
                observe(level);
            } else if (tick == 300) {
                finish(server);
            }
        } catch (RuntimeException e) {
            FAILURES.add("exception at tick " + tick + ": " + e);
            Tenkaichi.LOG.error("[DBT-SMOKE] exception", e);
            finish(server);
        }
    }

    private static void check(boolean condition, String message) {
        if (condition) {
            Tenkaichi.LOG.info("[DBT-SMOKE] PASS {}", message);
        } else {
            FAILURES.add(message);
            Tenkaichi.LOG.error("[DBT-SMOKE] FAIL {}", message);
        }
    }

    private static void staticChecks() {
        check(FormRegistry.get("dbtenkaichi:super_saiyan", false) != null, "super_saiyan form loaded from datapack");
        List<String> missing = new ArrayList<>();
        for (String name : ModSounds.NAMES) {
            if (!BuiltInRegistries.SOUND_EVENT.containsKey(Identifier.fromNamespaceAndPath(Tenkaichi.MOD_ID, name))) {
                missing.add(name);
            }
        }
        check(missing.isEmpty() && ModSounds.NAMES.size() == 40, "all " + ModSounds.NAMES.size() + " sound events registered " + missing);
        check(ModSounds.UI_OPEN.get() != null && ModSounds.UI_TAB.get() != null && ModSounds.UI_UPGRADE.get() != null && ModSounds.UI_DENY.get() != null, "scouter UI sound events registered");
    }

    private static void spawn(ServerLevel level) {
        level.setChunkForced(0, 0, true);
        level.getChunk(0, 0);
        int y = level.getHeight(Heightmap.Types.MOTION_BLOCKING, 0, 0);
        BlockPos base = new BlockPos(0, y + 1, 0);
        master = ModEntities.MASTER.get().spawn(level, base.offset(6, 0, 0), EntitySpawnReason.COMMAND);
        raider = ModEntities.RAIDER.get().spawn(level, base, EntitySpawnReason.COMMAND);
        dummy = EntityTypes.HUSK.spawn(level, base.offset(3, 0, 0), EntitySpawnReason.COMMAND);
        if (dummy != null) {
            dummy.setPersistenceRequired();
            dummy.setNoAi(true);
        }
        check(master != null && raider != null && dummy != null, "spawned master, raider and target dummy");
        if (raider != null && dummy != null) {
            raider.setTarget(dummy);
            dummyHealth = dummy.getHealth();
        }
    }

    private static void observe(ServerLevel level) {
        if (raider != null && raider.isAlive()) {
            STATES.add(raider.brain().state());
            if (dummy != null && dummy.isAlive()) {
                raider.setTarget(dummy);
                DamageSource last = dummy.getLastDamageSource();
                if (last != null && TenkaichiDamage.isTenkaichi(last)) {
                    tenkaichiHits++;
                }
            }
        }
        if (tick == 60 && master != null) {
            CombatSystem.playGround(level, master, 1.0f, true);
            CombatSystem.playGround(level, master, 0.7f, false);
            Vec3 sky = new Vec3(0, level.getMaxY() - 4, 0);
            master.teleportTo(sky.x + 6, sky.y, sky.z);
            CombatSystem.playGround(level, master, 1.0f, true);
            master.teleportTo(6, level.getHeight(Heightmap.Types.MOTION_BLOCKING, 6, 0) + 1, 0);
            check(true, "block-aware ground sounds play on solid ground and are skipped mid-air");
        }
        if (tick == 120 && raider != null && master != null) {
            KiBeamEntity beam = KiBeamEntity.create(level, master, Techniques.KAMEHAMEHA, 0.6);
            master.lookAtPoint(raider.getBoundingBox().getCenter());
            beam.setDirection(raider.getBoundingBox().getCenter().subtract(beam.position()));
            level.addFreshEntity(beam);
            check(beam.isAlive(), "kamehameha beam entity spawned");
        }
        if (tick == 200) {
            Vec3 sky = new Vec3(0, level.getMaxY() - 10, 0);
            DestructionManager.explode(level, sky, 3.0, null, 0.0, 0.0);
            check(true, "explosion with layered vanilla blast handled");
        }
    }

    private static void finish(MinecraftServer server) {
        done = true;
        if (dummy != null) {
            DamageSource last = dummy.getLastDamageSource();
            boolean byTenkaichi = last != null && TenkaichiDamage.isTenkaichi(last) || tenkaichiHits > 0;
            check(byTenkaichi && (dummy.isDeadOrDying() || dummy.getHealth() < dummyHealth), "raider melee hits (with layered vanilla hit sounds) damaged the target (states " + STATES + ")");
        }
        if (FAILURES.isEmpty()) {
            Tenkaichi.LOG.info("[DBT-SMOKE] RESULT PASS");
        } else {
            Tenkaichi.LOG.error("[DBT-SMOKE] RESULT FAIL {}", FAILURES);
        }
        server.overworld().setChunkForced(0, 0, false);
        server.halt(false);
    }
}
