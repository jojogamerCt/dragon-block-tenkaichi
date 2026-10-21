package com.dbtenkaichi.movement;

import com.dbtenkaichi.anim.AnimClip;
import com.dbtenkaichi.combat.CombatFxKind;
import com.dbtenkaichi.combat.CombatSystem;
import com.dbtenkaichi.combat.Fighter;
import com.dbtenkaichi.config.ConfigManager;
import com.dbtenkaichi.config.GameplayConfig;
import com.dbtenkaichi.player.PlayerData;
import com.dbtenkaichi.player.RuntimeState;
import com.dbtenkaichi.registry.ModSounds;
import com.dbtenkaichi.server.PlayerManager;
import com.dbtenkaichi.stats.DerivedStats;
import com.dbtenkaichi.stats.StatType;
import com.dbtenkaichi.training.TrainingSystem;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

public final class FlightSystem {
    private FlightSystem() {
    }

    public static boolean isKiFlying(Player player) {
        return PlayerManager.data(player).runtime().flying;
    }

    public static void toggle(ServerPlayer player, PlayerData data, long now) {
        if (data.runtime().flying) {
            stop(player, data);
        } else {
            start(player, data, now);
        }
    }

    public static boolean start(ServerPlayer player, PlayerData data, long now) {
        GameplayConfig gp = ConfigManager.gameplay();
        RuntimeState rt = data.runtime();
        if (!gp.flightEnabled || player.isSpectator() || player.isPassenger() || player.isSleeping() || rt.busy(now)) {
            return false;
        }
        if (data.ki() < 5.0) {
            player.sendSystemMessage(Component.translatable("message.dbtenkaichi.no_ki"));
            return false;
        }
        rt.flying = true;
        rt.fastFlight = false;
        rt.lastPos = player.position();
        rt.flightViolations = 0;
        player.setNoGravity(true);
        player.resetFallDistance();
        rt.stateDirty = true;
        ServerLevel level = player.level();
        if (player.onGround()) {
            CombatSystem.fx(player, CombatFxKind.SHOCKWAVE, player, player, player.position(), 0.6f);
        }
        CombatSystem.playSound(level, player, ModSounds.FLIGHT_TAKEOFF.get(), 0.8f, 1.0f);
        return true;
    }

    public static void stop(ServerPlayer player, PlayerData data) {
        RuntimeState rt = data.runtime();
        if (!rt.flying) {
            return;
        }
        rt.flying = false;
        rt.fastFlight = false;
        player.setNoGravity(false);
        player.resetFallDistance();
        rt.stateDirty = true;
    }

    public static void setFast(ServerPlayer player, PlayerData data, boolean fast) {
        RuntimeState rt = data.runtime();
        if (rt.flying && rt.fastFlight != fast) {
            rt.fastFlight = fast;
            rt.stateDirty = true;
            if (fast) {
                CombatSystem.playSound(player.level(), player, ModSounds.FLIGHT_TAKEOFF.get(), 0.7f, 1.3f);
            }
        }
    }

    public static double kiCostPerTick(ServerPlayer player, PlayerData data) {
        GameplayConfig gp = ConfigManager.gameplay();
        RuntimeState rt = data.runtime();
        Vec3 motion = player.getDeltaMovement();
        double perSecond;
        if (rt.fastFlight) {
            perSecond = gp.fastFlightKiPerSecond;
        } else if (motion.lengthSqr() < 0.01) {
            perSecond = gp.hoverKiPerSecond;
        } else {
            perSecond = gp.flightKiPerSecond;
        }
        return perSecond * data.race().flightCostMultiplier() * DerivedStats.kiCostMultiplier(data.stats(), data.race(), ConfigManager.balance()) / 20.0;
    }

    public static double maxSpeed(Player player, PlayerData data) {
        GameplayConfig gp = ConfigManager.gameplay();
        Fighter fighter = Fighter.of(player);
        return DerivedStats.flightSpeed(data.stats(), data.runtime().fastFlight, fighter.speedMultiplier(), gp.baseFlightSpeed, gp.fastFlightSpeed, gp.flightSpeedPerLevel, gp.maxFlightSpeed);
    }

    public static void tick(ServerPlayer player, PlayerData data, long now) {
        RuntimeState rt = data.runtime();
        if (!rt.flying) {
            if (player.isNoGravity() && !player.isSpectator() && rt.wasFlightControlled) {
                player.setNoGravity(false);
            }
            rt.wasFlightControlled = false;
            return;
        }
        rt.wasFlightControlled = true;
        if (player.isPassenger() || player.isSleeping() || player.isFallFlying()) {
            stop(player, data);
            return;
        }
        if (!player.isNoGravity()) {
            player.setNoGravity(true);
        }
        player.resetFallDistance();
        Vec3 pos = player.position();
        if (rt.lastPos != null) {
            double moved = pos.distanceTo(rt.lastPos);
            double allowed = maxSpeed(player, data) * 1.6 + 1.2;
            if (moved > allowed && rt.dashingUntil < now && rt.invulnerableUntil < now) {
                rt.flightViolations++;
                if (rt.flightViolations > 60) {
                    stop(player, data);
                    rt.flightViolations = 0;
                }
            } else if (rt.flightViolations > 0) {
                rt.flightViolations--;
            }
            if (rt.fastFlight && moved > 1.8 && now - rt.lastSonicBoom > 60) {
                rt.lastSonicBoom = now;
                CombatSystem.fx(player, CombatFxKind.SONIC_BOOM, player, player, pos.add(0, player.getBbHeight() * 0.5, 0), 1.0f);
                CombatSystem.playSound(player.level(), player, ModSounds.SONIC_BOOM.get(), 1.0f, 1.0f);
                CombatSystem.playSound(player.level(), player, net.minecraft.sounds.SoundEvents.WIND_CHARGE_BURST.value(), 0.7f, 0.7f);
            }
            if (now % 40 == 0 && moved > 0.2) {
                TrainingSystem.passiveGain(player, data, "flight", StatType.SPEED, rt.fastFlight ? 0.25 : 0.1);
            }
        }
        rt.lastPos = pos;
    }

    public static void landed(ServerPlayer player, PlayerData data, int speedHundredths, long now) {
        RuntimeState rt = data.runtime();
        if (now - rt.lastLanding < 10) {
            return;
        }
        rt.lastLanding = now;
        double speed = Math.min(6.0, Math.max(0, speedHundredths) / 100.0);
        if (rt.flying) {
            stop(player, data);
        }
        if (speed < 0.8 || !player.onGround()) {
            return;
        }
        ServerLevel level = player.level();
        float strength = (float) Math.min(2.5, speed);
        CombatSystem.anim(player, AnimClip.LAND, 8, 0);
        CombatSystem.fx(player, CombatFxKind.LAND, player, player, player.position(), strength);
        CombatSystem.playSound(level, player, ModSounds.LAND.get(), Math.min(1.0f, 0.4f + strength * 0.3f), 1.0f);
        CombatSystem.playGround(level, player, Math.min(1.2f, 0.6f + strength * 0.3f), true);
        double radius = 1.5 + strength;
        for (LivingEntity other : level.getEntitiesOfClass(LivingEntity.class, player.getBoundingBox().inflate(radius, 1.0, radius), e -> e != player && e.isAlive())) {
            if (!CombatSystem.canHarm(player, other)) {
                continue;
            }
            Vec3 push = other.position().subtract(player.position()).multiply(1, 0, 1);
            if (push.lengthSqr() < 1.0e-4) {
                continue;
            }
            CombatSystem.knockback(other, push.normalize().scale(0.3 + strength * 0.25).add(0, 0.25, 0));
        }
    }
}
