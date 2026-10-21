package com.dbtenkaichi.world;

import com.dbtenkaichi.Tenkaichi;
import com.dbtenkaichi.combat.CombatFxKind;
import com.dbtenkaichi.combat.CombatSystem;
import com.dbtenkaichi.combat.Fighter;
import com.dbtenkaichi.combat.TenkaichiDamage;
import com.dbtenkaichi.config.BalanceConfig;
import com.dbtenkaichi.config.ConfigManager;
import com.dbtenkaichi.config.GameplayConfig;
import com.dbtenkaichi.config.TenkaichiConfig;
import com.dbtenkaichi.combat.DamageMath;
import com.dbtenkaichi.network.Packets;
import com.dbtenkaichi.platform.Services;
import com.dbtenkaichi.registry.ModSounds;
import com.dbtenkaichi.stats.StatType;
import net.minecraft.core.BlockPos;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gamerules.GameRules;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public final class DestructionManager {
    private static final int MAX_JOBS = 64;
    private static final double REFERENCE_BATTLE_POWER = 5000.0;
    private static final Deque<Restore> RESTORE = new ArrayDeque<>();
    private static final Deque<Job> JOBS = new ArrayDeque<>();
    private static int budgetLeft = 1500;

    private DestructionManager() {
    }

    public static void tick(MinecraftServer server) {
        budgetLeft = ConfigManager.gameplay().maxDestructionBlocksPerTick;
        long now = System.currentTimeMillis();
        int restored = 0;
        while (!RESTORE.isEmpty() && RESTORE.peekFirst().at <= now && restored < 200) {
            Restore r = RESTORE.pollFirst();
            restore(server, r);
            restored++;
        }
        while (budgetLeft > 0 && !JOBS.isEmpty()) {
            Job job = JOBS.peekFirst();
            ServerLevel level = server.getLevel(job.dimension);
            if (level == null || job.run(level, budgetLeft)) {
                JOBS.pollFirst();
            }
        }
    }

    public static int pendingJobs() {
        return JOBS.size();
    }

    public static void restoreAll(MinecraftServer server) {
        JOBS.clear();
        int count = 0;
        while (!RESTORE.isEmpty()) {
            restore(server, RESTORE.pollFirst());
            count++;
        }
        if (count > 0) {
            Tenkaichi.LOG.info("Restored {} blocks from temporary battle damage", count);
        }
    }

    private static void restore(MinecraftServer server, Restore r) {
        ServerLevel level = server.getLevel(r.dimension);
        if (level != null && level.isLoaded(r.pos) && level.getBlockState(r.pos) == r.expected) {
            level.setBlock(r.pos, r.state, 3);
        }
    }

    public static double battlePowerScale(@Nullable LivingEntity source) {
        if (source == null || !ConfigManager.gameplay().scaleExplosionsWithBattlePower) {
            return 1.0;
        }
        double ratio = Math.max(0.1, Math.min(1.0e6, Fighter.of(source).battlePower() / REFERENCE_BATTLE_POWER));
        return Math.max(0.7, Math.min(2.8, 1.0 + 0.3 * Math.log10(ratio)));
    }

    public static double scaledRadius(@Nullable LivingEntity source, double radius) {
        double max = ConfigManager.gameplay().maxExplosionRadius;
        return Math.max(0.5, Math.min(max, radius * battlePowerScale(source)));
    }

    public static void explode(ServerLevel level, Vec3 center, double radius, @Nullable LivingEntity source, double baseDamage, double chargeRatio) {
        impact(level, center, radius, source, baseDamage, chargeRatio, false);
    }

    public static void crater(ServerLevel level, Vec3 center, double radius, @Nullable LivingEntity source, double baseDamage, double chargeRatio) {
        impact(level, center, radius, source, baseDamage, chargeRatio, true);
    }

    private static void impact(ServerLevel level, Vec3 center, double radius, @Nullable LivingEntity source, double baseDamage, double chargeRatio, boolean crater) {
        TenkaichiConfig cfg = ConfigManager.get();
        GameplayConfig gp = cfg.gameplay;
        BalanceConfig balance = cfg.balance;
        double r = scaledRadius(source, radius);
        damageArea(level, center, Math.min(r * 1.25, 20.0), source, baseDamage, chargeRatio, balance);
        sendNear(level, center, 128 + r * 4, new Packets.CombatFx(CombatFxKind.EXPLOSION.ordinal(), source == null ? -1 : source.getId(), -1, center.x, center.y, center.z, (float) r));
        level.playSound(null, center.x, center.y, center.z, ModSounds.EXPLOSION.get(), SoundSource.PLAYERS, (float) Math.min(6.0, 1.0 + r * 0.5), (crater ? 0.6f : 0.8f) + level.getRandom().nextFloat() * 0.2f);
        level.playSound(null, center.x, center.y, center.z, net.minecraft.sounds.SoundEvents.GENERIC_EXPLODE.value(), SoundSource.PLAYERS, (float) Math.min(3.0, 0.5 + r * 0.2), 0.65f + level.getRandom().nextFloat() * 0.15f);
        TenkaichiConfig.DestructionMode mode = cfg.destruction();
        if (mode == TenkaichiConfig.DestructionMode.OFF || mode == TenkaichiConfig.DestructionMode.VISUAL || com.dbtenkaichi.training.TrialManager.isRealm(level)) {
            return;
        }
        if (gp.destructionDimensionBlacklist.contains(level.dimension().identifier().toString())) {
            return;
        }
        if (!(source instanceof Player) && !level.getGameRules().get(GameRules.MOB_GRIEFING)) {
            return;
        }
        if (JOBS.size() >= MAX_JOBS) {
            return;
        }
        Job job = crater ? craterJob(level, center, r, source, mode == TenkaichiConfig.DestructionMode.REGENERATING, gp)
                : sphereJob(level, center, r, source, mode == TenkaichiConfig.DestructionMode.REGENERATING, gp);
        if (job.run(level, budgetLeft)) {
            return;
        }
        JOBS.addLast(job);
    }

    private static void damageArea(ServerLevel level, Vec3 center, double radius, @Nullable LivingEntity source, double baseDamage, double chargeRatio, BalanceConfig balance) {
        if (baseDamage <= 0) {
            return;
        }
        Fighter attacker = source == null ? null : Fighter.of(source);
        AABB box = new AABB(center, center).inflate(radius);
        List<LivingEntity> victims = level.getEntitiesOfClass(LivingEntity.class, box, e -> e.isAlive() && !e.isSpectator());
        for (LivingEntity victim : victims) {
            if (source != null && (victim == source || !CombatSystem.canHarm(source, victim))) {
                continue;
            }
            double dist = victim.getBoundingBox().getCenter().distanceTo(center);
            if (dist > radius) {
                continue;
            }
            double falloff = 1.0 - (dist / radius) * 0.6;
            Fighter defender = Fighter.of(victim);
            double raw = attacker == null ? baseDamage : DamageMath.ki(baseDamage, attacker.stat(StatType.KI_OUTPUT), chargeRatio, attacker.kiMultiplier(balance), balance);
            raw *= falloff;
            double guardFactor = 1.0;
            var rt = CombatSystem.runtime(victim);
            DamageMath.GuardState guard = rt != null && rt.guarding ? DamageMath.GuardState.GUARD : DamageMath.GuardState.NONE;
            if (rt != null && rt.invulnerableUntil > level.getGameTime()) {
                continue;
            }
            double damage = DamageMath.finalDamage(raw * guardFactor, attacker == null ? defender.battlePower() : attacker.battlePower(), defender.battlePower(),
                    defender.stat(StatType.DEFENSE), defender.defenseMultiplier(balance), guard, defender.fatigue(), balance);
            damage *= 1.0 - Math.min(0.5, defender.stat(StatType.SPIRIT) * 0.002);
            TenkaichiDamage.hurt(victim, level, TenkaichiDamage.source(level, TenkaichiDamage.KI_EXPLOSION, source, source), (float) damage);
            Vec3 push = victim.position().subtract(center);
            if (push.lengthSqr() > 1.0e-4) {
                CombatSystem.knockback(victim, push.normalize().scale(0.4 + falloff * 0.8).add(0, 0.3 * falloff, 0));
            }
            if (rt != null) {
                rt.lastCombat = level.getGameTime();
                rt.charging = false;
                rt.stunnedUntil = Math.max(rt.stunnedUntil, level.getGameTime() + 8);
            }
        }
    }

    private static Job sphereJob(ServerLevel level, Vec3 center, double radius, @Nullable LivingEntity source, boolean regenerate, GameplayConfig gp) {
        RandomSource random = level.getRandom();
        BlockPos origin = BlockPos.containing(center);
        int ri = (int) Math.ceil(radius);
        List<BlockPos> targets = new ArrayList<>();
        for (int dy = -ri; dy <= ri; dy++) {
            for (int dx = -ri; dx <= ri; dx++) {
                for (int dz = -ri; dz <= ri; dz++) {
                    double noise = 0.75 + random.nextDouble() * 0.35;
                    double d2 = dx * dx + dy * dy * 1.3 + dz * dz;
                    if (d2 > radius * radius * noise) {
                        continue;
                    }
                    targets.add(origin.offset(dx, dy, dz));
                }
            }
        }
        targets.sort((a, b) -> Double.compare(a.distSqr(origin), b.distSqr(origin)));
        return new Job(level.dimension(), targets, List.of(), source, regenerate, gp);
    }

    private static Job craterJob(ServerLevel level, Vec3 center, double radius, @Nullable LivingEntity source, boolean regenerate, GameplayConfig gp) {
        RandomSource random = level.getRandom();
        BlockPos origin = BlockPos.containing(center);
        int ri = (int) Math.ceil(radius);
        double depth = Math.max(1.5, radius * 0.55);
        int di = (int) Math.ceil(depth);
        List<BlockPos> targets = new ArrayList<>();
        Map<Long, Integer> floor = new HashMap<>();
        for (int dx = -ri; dx <= ri; dx++) {
            for (int dz = -ri; dz <= ri; dz++) {
                double h2 = dx * dx + dz * dz;
                if (h2 > radius * radius) {
                    continue;
                }
                double edge = 0.9 + random.nextDouble() * 0.2;
                for (int dy = -di; dy <= ri; dy++) {
                    double v = dy < 0 ? dy / depth * radius : dy * 0.85;
                    if (h2 + v * v > radius * radius * edge) {
                        continue;
                    }
                    targets.add(origin.offset(dx, dy, dz));
                    long key = BlockPos.asLong(dx, 0, dz);
                    floor.merge(key, dy, Math::min);
                }
            }
        }
        targets.sort((a, b) -> Double.compare(a.distSqr(origin), b.distSqr(origin)));
        List<Scorch> scorch = new ArrayList<>();
        for (Map.Entry<Long, Integer> entry : floor.entrySet()) {
            BlockPos offset = BlockPos.of(entry.getKey());
            BlockPos pos = origin.offset(offset.getX(), entry.getValue() - 1, offset.getZ());
            double dist = Math.sqrt(offset.getX() * offset.getX() + offset.getZ() * offset.getZ()) / Math.max(1.0, radius);
            BlockState state;
            if (dist < 0.45) {
                state = random.nextFloat() < 0.6f ? Blocks.BLACKSTONE.defaultBlockState() : Blocks.BASALT.defaultBlockState();
            } else if (dist < 0.8) {
                state = random.nextFloat() < 0.55f ? Blocks.COARSE_DIRT.defaultBlockState() : Blocks.TUFF.defaultBlockState();
            } else {
                if (random.nextFloat() < 0.5f) {
                    continue;
                }
                state = Blocks.COARSE_DIRT.defaultBlockState();
            }
            scorch.add(new Scorch(pos, state));
        }
        return new Job(level.dimension(), targets, scorch, source, regenerate, gp);
    }

    private record Scorch(BlockPos pos, BlockState state) {
    }

    private static final class Job {
        final ResourceKey<Level> dimension;
        final List<BlockPos> targets;
        final List<Scorch> scorch;
        final @Nullable UUID player;
        final boolean regenerate;
        final long expire;
        final int limit;
        final float hardness;
        final boolean protectSpawn;
        int index;
        int scorchIndex;
        int removed;

        Job(ResourceKey<Level> dimension, List<BlockPos> targets, List<Scorch> scorch, @Nullable LivingEntity source, boolean regenerate, GameplayConfig gp) {
            this.dimension = dimension;
            this.targets = targets;
            this.scorch = scorch;
            this.player = source instanceof ServerPlayer sp ? sp.getUUID() : null;
            this.regenerate = regenerate;
            this.expire = System.currentTimeMillis() + Math.max(1, gp.terrainRegenSeconds) * 1000L;
            this.limit = gp.maxBlocksPerExplosion;
            this.hardness = gp.maxBlockHardness;
            this.protectSpawn = gp.protectSpawnArea;
        }

        boolean run(ServerLevel level, int budget) {
            ServerPlayer owner = player == null ? null : level.getServer().getPlayerList().getPlayer(player);
            int spent = 0;
            while (index < targets.size() && removed < limit && spent < budget) {
                BlockPos pos = targets.get(index++);
                spent++;
                if (!breakable(level, pos, owner)) {
                    continue;
                }
                BlockState state = level.getBlockState(pos);
                if (regenerate) {
                    RESTORE.addLast(new Restore(level.dimension(), pos.immutable(), state, Blocks.AIR.defaultBlockState(), expire + removed * 15L));
                }
                level.setBlock(pos, Blocks.AIR.defaultBlockState(), 3);
                removed++;
            }
            if (index < targets.size() && removed < limit) {
                budgetLeft -= spent;
                return false;
            }
            while (scorchIndex < scorch.size() && spent < budget) {
                Scorch s = scorch.get(scorchIndex++);
                spent++;
                if (!level.getBlockState(s.pos.above()).isAir() || !breakable(level, s.pos, owner)) {
                    continue;
                }
                BlockState state = level.getBlockState(s.pos);
                if (!state.isCollisionShapeFullBlock(level, s.pos)) {
                    continue;
                }
                if (regenerate) {
                    RESTORE.addLast(new Restore(level.dimension(), s.pos.immutable(), state, s.state, expire + removed * 15L));
                }
                level.setBlock(s.pos, s.state, 3);
            }
            budgetLeft -= spent;
            return scorchIndex >= scorch.size();
        }

        private boolean breakable(ServerLevel level, BlockPos pos, @Nullable ServerPlayer owner) {
            if (!level.isLoaded(pos) || level.isOutsideBuildHeight(pos)) {
                return false;
            }
            BlockState state = level.getBlockState(pos);
            if (state.isAir() || !state.getFluidState().isEmpty()) {
                return false;
            }
            float h = state.getDestroySpeed(level, pos);
            if (h < 0 || h > hardness || state.hasBlockEntity()) {
                return false;
            }
            if (SafeZones.isProtected(level, pos)) {
                return false;
            }
            if (owner != null && !Services.PLATFORM.canBreakBlock(owner, pos, state)) {
                return false;
            }
            return !(protectSpawn && owner != null && level.getServer().isUnderSpawnProtection(level, pos, owner));
        }
    }

    public static void sendNear(ServerLevel level, Vec3 pos, double range, CustomPacketPayload payload) {
        double r2 = range * range;
        for (ServerPlayer player : level.players()) {
            if (player.distanceToSqr(pos) <= r2) {
                Services.NETWORK.sendToPlayer(player, payload);
            }
        }
    }

    private record Restore(ResourceKey<Level> dimension, BlockPos pos, BlockState state, BlockState expected, long at) {
    }
}
