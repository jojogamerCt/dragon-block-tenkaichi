package com.dbtenkaichi.combat;

import com.dbtenkaichi.anim.AnimClip;
import com.dbtenkaichi.config.BalanceConfig;
import com.dbtenkaichi.config.ConfigManager;
import com.dbtenkaichi.config.GameplayConfig;
import com.dbtenkaichi.network.Packets;
import com.dbtenkaichi.platform.Services;
import com.dbtenkaichi.player.PlayerData;
import com.dbtenkaichi.player.RuntimeState;
import com.dbtenkaichi.registry.ModSounds;
import com.dbtenkaichi.server.PlayerManager;
import com.dbtenkaichi.stats.DerivedStats;
import com.dbtenkaichi.stats.StatType;
import com.dbtenkaichi.training.TrainingSystem;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Relative;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

import java.util.List;
import java.util.Set;

public final class CombatSystem {
    public static final int DIR_NEUTRAL = 0;
    public static final int DIR_FORWARD = 1;
    public static final int DIR_BACK = 2;
    public static final int DIR_LEFT = 3;
    public static final int DIR_RIGHT = 4;
    public static final int DIR_UP = 5;
    public static final int DIR_DOWN = 6;

    private CombatSystem() {
    }

    public static @Nullable RuntimeState runtime(LivingEntity entity) {
        if (entity instanceof Player player) {
            return PlayerManager.data(player).runtime();
        }
        if (entity instanceof TenkaichiFighter fighter) {
            return fighter.fighterRuntime();
        }
        return null;
    }

    public static boolean lightAttack(LivingEntity attacker, long now) {
        RuntimeState rt = runtime(attacker);
        if (rt == null || rt.busy(now) || rt.guarding || rt.skillSlot >= 0 || now < rt.nextAttackAllowed) {
            return false;
        }
        if (now - rt.lastAttack > 20) {
            rt.comboIndex = 0;
        }
        MeleeMove move;
        if (rt.counterUntil > now) {
            move = MeleeMove.COUNTER;
            rt.counterUntil = 0;
        } else if (rt.dashingUntil > now && rt.comboIndex == 0) {
            move = MeleeMove.RUSH;
        } else {
            move = MeleeMove.LIGHT_CHAIN[Math.min(rt.comboIndex, MeleeMove.LIGHT_CHAIN.length - 1)];
        }
        Fighter fighter = Fighter.of(attacker);
        int interval = DerivedStats.attackIntervalTicks(fighter.stats());
        rt.comboIndex = move == MeleeMove.ROUNDHOUSE ? 0 : rt.comboIndex + 1;
        rt.lastAttack = now;
        rt.nextAttackAllowed = now + (move.heavy ? interval + 6 : interval);
        rt.charging = false;
        return performMove(attacker, fighter, rt, move, 1.0, now);
    }

    public static boolean heavyAttack(LivingEntity attacker, int chargeTicks, long now) {
        RuntimeState rt = runtime(attacker);
        if (rt == null || rt.busy(now) || rt.guarding || rt.skillSlot >= 0 || now < rt.nextAttackAllowed) {
            return false;
        }
        LivingEntity target = findTarget(attacker, rt, meleeRange(attacker, rt));
        MeleeMove move;
        if (rt.dashingUntil > now) {
            move = MeleeMove.RUSH;
        } else if (!attacker.onGround() && target != null && !target.onGround()) {
            move = MeleeMove.SMASH;
        } else if (rt.comboIndex >= 2 && now - rt.lastAttack <= 20) {
            move = MeleeMove.GUARD_BREAK;
        } else {
            move = MeleeMove.LAUNCHER;
        }
        double charge = 1.0 + Math.min(20, Math.max(0, chargeTicks)) / 20.0 * 0.6;
        if (attacker instanceof ServerPlayer player) {
            PlayerData data = PlayerManager.data(player);
            if (!PlayerManager.spendStamina(data, move.staminaCost * (0.8 + charge * 0.2))) {
                return false;
            }
        }
        Fighter fighter = Fighter.of(attacker);
        rt.comboIndex = 0;
        rt.lastAttack = now;
        rt.nextAttackAllowed = now + DerivedStats.attackIntervalTicks(fighter.stats()) + 8;
        rt.charging = false;
        return performMove(attacker, fighter, rt, move, charge, now);
    }

    private static boolean performMove(LivingEntity attacker, Fighter fighter, RuntimeState rt, MeleeMove move, double mult, long now) {
        ServerLevel level = (ServerLevel) attacker.level();
        rt.lastCombat = now;
        rt.stateDirty = true;
        anim(attacker, move.clip, move.clip.defaultDuration, 0);
        LivingEntity target = findTarget(attacker, rt, meleeRange(attacker, rt));
        if (target == null) {
            playSound(level, attacker, ModSounds.SWING.get(), 0.6f, 1.0f + attacker.getRandom().nextFloat() * 0.2f);
            return true;
        }
        if (move == MeleeMove.RUSH || move == MeleeMove.COUNTER) {
            Vec3 toTarget = target.position().subtract(attacker.position());
            double dist = toTarget.horizontalDistance();
            if (dist > 1.6) {
                Vec3 step = toTarget.normalize().scale(Math.min(dist - 1.2, 2.5));
                attacker.setDeltaMovement(step.scale(0.6));
                attacker.syncVelocity = true;
            }
        }
        applyMeleeHit(attacker, fighter, target, move, mult, now);
        return true;
    }

    public static double meleeRange(LivingEntity attacker, RuntimeState rt) {
        GameplayConfig gp = ConfigManager.gameplay();
        double bonus = attacker instanceof Player player ? com.dbtenkaichi.race.RaceMechanics.meleeRangeBonus(PlayerManager.data(player).race()) : 0.0;
        return (rt.lockedTarget >= 0 ? gp.meleeRangeLocked : gp.meleeRange) + bonus;
    }

    public static @Nullable LivingEntity findTarget(LivingEntity attacker, RuntimeState rt, double range) {
        ServerLevel level = (ServerLevel) attacker.level();
        if (rt.lockedTarget >= 0) {
            Entity locked = level.getEntity(rt.lockedTarget);
            if (locked instanceof LivingEntity living && living.isAlive() && canHarm(attacker, living) && reach(attacker, living) <= range + 0.75 && hasLineOfSight(attacker, living)) {
                return living;
            }
        }
        Vec3 eye = attacker.getEyePosition();
        Vec3 look = attacker.getViewVector(1.0f);
        AABB box = attacker.getBoundingBox().inflate(range + 1.0);
        List<LivingEntity> candidates = level.getEntitiesOfClass(LivingEntity.class, box, e -> e != attacker && e.isAlive() && !e.isSpectator() && canHarm(attacker, e));
        LivingEntity best = null;
        double bestScore = Double.MAX_VALUE;
        for (LivingEntity candidate : candidates) {
            double distance = reach(attacker, candidate);
            if (distance > range + 0.5) {
                continue;
            }
            Vec3 to = candidate.getBoundingBox().getCenter().subtract(eye);
            double len = to.length();
            if (len < 1.0e-4) {
                return candidate;
            }
            double cos = to.scale(1.0 / len).dot(look);
            if (cos < 0.42) {
                continue;
            }
            if (!hasLineOfSight(attacker, candidate)) {
                continue;
            }
            double score = distance * (2.0 - cos);
            if (score < bestScore) {
                bestScore = score;
                best = candidate;
            }
        }
        return best;
    }

    public static double reach(LivingEntity attacker, LivingEntity target) {
        Vec3 eye = attacker.getEyePosition();
        AABB box = target.getBoundingBox();
        double dx = Math.max(box.minX - eye.x, Math.max(0.0, eye.x - box.maxX));
        double dy = Math.max(box.minY - eye.y, Math.max(0.0, eye.y - box.maxY));
        double dz = Math.max(box.minZ - eye.z, Math.max(0.0, eye.z - box.maxZ));
        return Math.sqrt(dx * dx + dy * dy + dz * dz);
    }

    public static boolean hasLineOfSight(Entity from, Entity to) {
        Vec3 start = from.getEyePosition();
        Vec3 end = to.getBoundingBox().getCenter();
        HitResult hit = from.level().clip(new ClipContext(start, end, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, from));
        return hit.getType() == HitResult.Type.MISS;
    }

    public static boolean canHarm(LivingEntity attacker, LivingEntity target) {
        if (attacker == target || !target.isAttackable() || target.isInvulnerable()) {
            return false;
        }
        GameplayConfig gp = ConfigManager.gameplay();
        if (attacker instanceof Player && target instanceof Player) {
            if (!gp.pvpEnabled || !((ServerLevel) attacker.level()).isPvpAllowed()) {
                return false;
            }
            if (!gp.friendlyFire && attacker.isAlliedTo(target)) {
                return false;
            }
        }
        if (target instanceof TamableAnimal pet && attacker instanceof Player player && pet.isOwnedBy(player)) {
            return false;
        }
        return true;
    }

    public static void applyMeleeHit(LivingEntity attacker, Fighter att, LivingEntity target, MeleeMove move, double mult, long now) {
        ServerLevel level = (ServerLevel) attacker.level();
        BalanceConfig cfg = ConfigManager.balance();
        GameplayConfig gp = ConfigManager.gameplay();
        RuntimeState attRt = runtime(attacker);
        RuntimeState defRt = runtime(target);
        Fighter def = Fighter.of(target);
        Vec3 hitPos = target.getBoundingBox().getCenter().add(attacker.getEyePosition().subtract(target.getBoundingBox().getCenter()).normalize().scale(target.getBbWidth() * 0.5));

        if (defRt != null && defRt.invulnerableUntil > now) {
            if (defRt.dodgeStart >= now - 3 && target instanceof ServerPlayer defender && tryVanishCounter(defender, attacker, now)) {
                if (attRt != null) {
                    attRt.stunnedUntil = Math.max(attRt.stunnedUntil, now + 8);
                    attRt.stateDirty = true;
                }
                return;
            }
            fx(target, CombatFxKind.WHIFF, attacker, target, hitPos, 0.5f);
            return;
        }

        if (defRt != null && defRt.clashId < 0 && defRt.lastAttack >= now - 2 && !move.heavy && runtime(target) != null && facing(target, attacker)) {
            Vec3 push = target.position().subtract(attacker.position()).multiply(1, 0, 1).normalize().scale(0.7);
            knockback(target, push.add(0, 0.1, 0));
            knockback(attacker, push.scale(-1).add(0, 0.1, 0));
            fx(target, CombatFxKind.MELEE_CLASH, attacker, target, hitPos, 1.0f);
            playSound(level, target, ModSounds.MELEE_CLASH.get(), 1.0f, 0.9f + level.getRandom().nextFloat() * 0.2f);
            playSound(level, target, SoundEvents.PLAYER_ATTACK_CRIT, 0.6f, 0.8f);
            defRt.lastCombat = now;
            if (attRt != null) {
                attRt.lastCombat = now;
                attRt.comboIndex = 0;
            }
            return;
        }

        DamageMath.GuardState guard = DamageMath.GuardState.NONE;
        if (defRt != null && defRt.guarding && facing(target, attacker)) {
            guard = now - defRt.guardStart <= gp.perfectGuardWindowTicks ? DamageMath.GuardState.PERFECT : DamageMath.GuardState.GUARD;
        }

        if (guard == DamageMath.GuardState.GUARD && (move == MeleeMove.GUARD_BREAK || move == MeleeMove.SMASH)) {
            defRt.guarding = false;
            defRt.stunnedUntil = now + 26;
            defRt.stateDirty = true;
            guard = DamageMath.GuardState.NONE;
            fx(target, CombatFxKind.GUARD_BREAK, attacker, target, hitPos, 1.2f);
            playSound(level, target, ModSounds.GUARD_BREAK.get(), 1.0f, 1.0f);
            playSound(level, target, SoundEvents.SHIELD_BREAK.value(), 0.7f, 0.9f);
        } else if (guard == DamageMath.GuardState.GUARD && target instanceof ServerPlayer defender) {
            PlayerData data = PlayerManager.data(defender);
            double cost = gp.guardStaminaPerHit * (move.heavy ? 2.0 : 1.0) * mult;
            if (!PlayerManager.spendStamina(data, cost)) {
                data.setStamina(0);
                defRt.guarding = false;
                defRt.stunnedUntil = now + 30;
                defRt.stateDirty = true;
                guard = DamageMath.GuardState.NONE;
                fx(target, CombatFxKind.GUARD_BREAK, attacker, target, hitPos, 1.2f);
                playSound(level, target, ModSounds.GUARD_BREAK.get(), 1.0f, 0.9f);
                playSound(level, target, SoundEvents.SHIELD_BREAK.value(), 0.7f, 0.85f);
            }
        }

        if (guard == DamageMath.GuardState.PERFECT) {
            if (attRt != null) {
                attRt.stunnedUntil = now + 16;
                attRt.comboIndex = 0;
                attRt.stateDirty = true;
            }
            defRt.counterUntil = now + 14;
            defRt.lastCombat = now;
            anim(target, AnimClip.GUARD_IMPACT, 6, 1);
            fx(target, CombatFxKind.PERFECT_GUARD, attacker, target, hitPos, 1.0f);
            playSound(level, target, ModSounds.PERFECT_GUARD.get(), 1.0f, 1.2f);
            playSound(level, target, SoundEvents.SHIELD_BLOCK.value(), 0.6f, 1.4f);
            knockback(attacker, attacker.position().subtract(target.position()).multiply(1, 0, 1).normalize().scale(0.45));
            if (target instanceof ServerPlayer defender) {
                TrainingSystem.passiveGain(defender, PlayerManager.data(defender), "perfect_guard", StatType.MARTIAL_ARTS, 1.5);
            }
            return;
        }

        double raw = DamageMath.melee(move.damage * mult, att.stat(StatType.STRENGTH), att.stat(StatType.MARTIAL_ARTS), att.attackMultiplier(cfg),
                DamageMath.comboScaling(attRt == null ? 0 : attRt.comboIndex), cfg);
        if (att.data() != null) {
            raw *= 1.0 - cfg.fatigueCombatPenalty * att.fatigue();
        }
        double damage = DamageMath.finalDamage(raw, att.battlePower(), def.battlePower(), def.stat(StatType.DEFENSE), def.defenseMultiplier(cfg), guard, def.fatigue(), cfg);
        if (attacker instanceof TenkaichiFighter scaled) {
            damage *= scaled.outgoingDamageScale();
        }
        com.dbtenkaichi.ki.BeamClashManager.onAttacked(target, now);
        boolean hurt = TenkaichiDamage.hurt(target, level, TenkaichiDamage.source(level, TenkaichiDamage.MARTIAL_ARTS, attacker, attacker), (float) damage);
        if (!hurt && guard == DamageMath.GuardState.NONE) {
            fx(target, CombatFxKind.WHIFF, attacker, target, hitPos, 0.5f);
            return;
        }

        double resist = target.getAttribute(Attributes.KNOCKBACK_RESISTANCE) != null ? target.getAttributeValue(Attributes.KNOCKBACK_RESISTANCE) : 0.0;
        double strength = move.knockback * (1.0 + att.stat(StatType.STRENGTH) * 0.002) * mult * (1.0 - Math.min(0.9, resist));
        Vec3 dir = target.position().subtract(attacker.position()).multiply(1, 0, 1);
        dir = dir.lengthSqr() < 1.0e-6 ? attacker.getViewVector(1.0f).multiply(1, 0, 1).normalize() : dir.normalize();
        double lift = move.lift * mult;
        if (guard == DamageMath.GuardState.GUARD) {
            strength *= 0.3;
            lift = Math.max(0, lift) * 0.2;
        }
        knockback(target, dir.scale(strength).add(0, lift + (move.lift == 0 && target.onGround() ? 0.08 : 0.0), 0));

        if (defRt != null) {
            int stun = guard == DamageMath.GuardState.GUARD ? Math.max(3, move.hitstun / 3) : move.hitstun;
            if (target instanceof Player) {
                stun = playerHitstun(defRt, attacker, attRt, stun, now);
            }
            defRt.stunnedUntil = Math.max(defRt.stunnedUntil, now + stun);
            defRt.lastCombat = now;
            defRt.charging = false;
            defRt.stateDirty = true;
            if (move == MeleeMove.SMASH && guard == DamageMath.GuardState.NONE) {
                defRt.smashedUntil = now + 40;
            }
        }
        if (attRt != null) {
            attRt.lastCombat = now;
            attRt.damageDealt.merge(target.getId(), damage, Double::sum);
        }
        if (target instanceof TenkaichiFighter npc) {
            npc.onFighterHit(attacker, damage);
        }

        CombatFxKind kind = guard == DamageMath.GuardState.GUARD ? CombatFxKind.GUARD : (move.heavy ? CombatFxKind.HIT_HEAVY : CombatFxKind.HIT_LIGHT);
        float fxStrength = (float) Math.min(3.0, 0.5 + damage / Math.max(1.0, target.getMaxHealth()) * 6.0 + (move.heavy ? 0.6 : 0.0));
        fx(target, kind, attacker, target, hitPos, fxStrength);
        SoundEvent sound = guard == DamageMath.GuardState.GUARD ? ModSounds.GUARD.get()
                : move.heavy ? ModSounds.PUNCH_HEAVY.get()
                : (move == MeleeMove.KNEE || move == MeleeMove.ROUNDHOUSE) ? ModSounds.KICK.get() : ModSounds.PUNCH_LIGHT.get();
        playSound(level, target, sound, move.heavy ? 1.0f : 0.8f, 0.85f + level.getRandom().nextFloat() * 0.3f);
        SoundEvent layer = guard == DamageMath.GuardState.GUARD ? SoundEvents.SHIELD_BLOCK.value()
                : move.heavy ? SoundEvents.PLAYER_ATTACK_STRONG
                : (move == MeleeMove.KNEE || move == MeleeMove.ROUNDHOUSE) ? SoundEvents.PLAYER_ATTACK_KNOCKBACK : SoundEvents.PLAYER_ATTACK_WEAK;
        playSound(level, target, layer, move.heavy ? 0.55f : 0.4f, 0.9f + level.getRandom().nextFloat() * 0.2f);
        if (guard == DamageMath.GuardState.GUARD) {
            anim(target, AnimClip.GUARD_IMPACT, 6, 0);
        } else {
            AnimClip reaction = move == MeleeMove.LAUNCHER ? AnimClip.LAUNCHED : move.heavy ? AnimClip.HIT_HEAVY : AnimClip.HIT_LIGHT;
            anim(target, reaction, reaction.defaultDuration, level.getRandom().nextInt(2));
        }

        if (attacker instanceof ServerPlayer player) {
            PlayerData data = PlayerManager.data(player);
            com.dbtenkaichi.race.RaceMechanics.onMeleeHit(player, data, damage);
            double ratio = (double) def.battlePower() / Math.max(1L, att.battlePower());
            double worth = Math.max(0.05, Math.min(2.0, ratio));
            TrainingSystem.passiveGain(player, data, "melee", StatType.MARTIAL_ARTS, 0.35 * worth * com.dbtenkaichi.race.RaceMechanics.combatGainFactor(data.race(), worth));
            TrainingSystem.passiveGain(player, data, "melee_strength", StatType.STRENGTH, 0.25 * worth);
        }
        if (target instanceof ServerPlayer defender && guard == DamageMath.GuardState.GUARD) {
            TrainingSystem.passiveGain(defender, PlayerManager.data(defender), "guarding", StatType.DEFENSE, 0.3);
        }
    }

    public static final int COMBO_BREAK_HITS = 3;
    public static final int POISE_TICKS = 40;

    private static int playerHitstun(RuntimeState defRt, LivingEntity attacker, @Nullable RuntimeState attRt, int stun, long now) {
        if (now - defRt.lastHitTaken > 30) {
            defRt.hitStreak = 0;
        }
        defRt.lastHitTaken = now;
        if (defRt.poiseUntil > now) {
            return 0;
        }
        int scaled = attacker instanceof Player ? stun : Math.max(2, Math.round(stun * 0.55f));
        if (attacker instanceof Player) {
            return scaled;
        }
        defRt.hitStreak++;
        if (defRt.hitStreak >= COMBO_BREAK_HITS) {
            defRt.hitStreak = 0;
            defRt.poiseUntil = now + POISE_TICKS;
            defRt.stunnedUntil = Math.min(defRt.stunnedUntil, now + scaled);
            if (attRt != null) {
                attRt.comboIndex = 0;
                attRt.nextAttackAllowed = Math.max(attRt.nextAttackAllowed, now + 24);
            }
        }
        return scaled;
    }

    public static boolean facing(LivingEntity defender, Entity attacker) {
        Vec3 look = defender.getViewVector(1.0f).multiply(1, 0, 1);
        Vec3 to = attacker.position().subtract(defender.position()).multiply(1, 0, 1);
        if (look.lengthSqr() < 1.0e-6 || to.lengthSqr() < 1.0e-6) {
            return true;
        }
        return look.normalize().dot(to.normalize()) > 0.0;
    }

    public static void knockback(LivingEntity target, Vec3 velocity) {
        target.setDeltaMovement(velocity);
        target.syncVelocity = true;
        target.needsSync = true;
        if (velocity.y > 0.05) {
            target.resetFallDistance();
        }
    }

    public static void guardStart(LivingEntity entity, long now) {
        RuntimeState rt = runtime(entity);
        if (rt == null || rt.busy(now) || rt.skillSlot >= 0) {
            return;
        }
        rt.guarding = true;
        rt.guardStart = now;
        rt.charging = false;
        rt.stateDirty = true;
    }

    public static void guardEnd(LivingEntity entity) {
        RuntimeState rt = runtime(entity);
        if (rt != null && rt.guarding) {
            rt.guarding = false;
            rt.stateDirty = true;
        }
    }

    public static boolean dodge(ServerPlayer player, int direction, long now) {
        PlayerData data = PlayerManager.data(player);
        RuntimeState rt = data.runtime();
        GameplayConfig gp = ConfigManager.gameplay();
        if (rt.transformingUntil > now || now < rt.nextDodge) {
            return false;
        }
        boolean recovering = rt.stunnedUntil > now;
        if (recovering) {
            if (rt.stunnedUntil - now > 30 || rt.smashedUntil > now && player.onGround()) {
                return false;
            }
            if (!PlayerManager.spendStamina(data, gp.dodgeStaminaCost * 1.5)) {
                return false;
            }
            rt.stunnedUntil = now;
            player.setDeltaMovement(player.getDeltaMovement().scale(0.1));
            player.syncVelocity = true;
        } else if (!PlayerManager.spendStamina(data, gp.dodgeStaminaCost)) {
            return false;
        }
        rt.dodgeStart = now;
        rt.invulnerableUntil = now + gp.dodgeInvulnerabilityTicks;
        rt.nextDodge = now + gp.dodgeCooldownTicks;
        rt.guarding = false;
        rt.charging = false;
        rt.stateDirty = true;
        ServerLevel level = player.level();
        if (direction == DIR_NEUTRAL && !recovering && rt.lockedTarget >= 0 && level.getEntity(rt.lockedTarget) instanceof LivingEntity target
                && reach(player, target) < 6.0 && teleportBehind(player, data, target, now)) {
            return true;
        }
        AnimClip clip = direction == DIR_LEFT ? AnimClip.DODGE_LEFT : direction == DIR_RIGHT ? AnimClip.DODGE_RIGHT : AnimClip.DODGE_BACK;
        anim(player, clip, clip.defaultDuration, 0);
        fx(player, CombatFxKind.DODGE, player, player, player.position().add(0, player.getBbHeight() * 0.5, 0), 1.0f);
        playSound(level, player, ModSounds.DODGE.get(), 0.7f, 1.1f);
        playGround(level, player, 0.6f, false);
        TrainingSystem.passiveGain(player, data, "dodge", StatType.AGILITY, 0.2);
        return true;
    }

    public static boolean teleportBehind(ServerPlayer player, PlayerData data, LivingEntity target, long now) {
        GameplayConfig gp = ConfigManager.gameplay();
        Vec3 look = target.getViewVector(1.0f).multiply(1, 0, 1);
        if (look.lengthSqr() < 1.0e-6) {
            look = new Vec3(0, 0, 1);
        }
        Vec3 behind = target.position().subtract(look.normalize().scale(target.getBbWidth() * 0.5 + 1.3));
        ServerLevel level = player.level();
        AABB box = player.getBoundingBox().move(behind.subtract(player.position()));
        if (!level.noCollision(player, box)) {
            return false;
        }
        if (!PlayerManager.spendKi(player, data, gp.vanishKiCost)) {
            return false;
        }
        Vec3 from = player.position().add(0, player.getBbHeight() * 0.5, 0);
        Vec3 face = target.position().subtract(behind);
        float yaw = (float) (Math.toDegrees(Math.atan2(-face.x, face.z)));
        player.teleportTo(level, behind.x, behind.y, behind.z, Set.<Relative>of(), yaw, player.getXRot(), false);
        data.runtime().invulnerableUntil = now + 6;
        data.runtime().counterUntil = now + 10;
        anim(player, AnimClip.VANISH, 5, 0);
        fx(player, CombatFxKind.VANISH, player, target, from, 1.0f);
        fx(player, CombatFxKind.VANISH, player, target, behind.add(0, player.getBbHeight() * 0.5, 0), 1.0f);
        playSound(level, player, ModSounds.VANISH.get(), 1.0f, 1.0f);
        playSound(level, player, SoundEvents.ENDERMAN_TELEPORT, 0.22f, 1.7f);
        TrainingSystem.passiveGain(player, data, "vanish", StatType.SPEED, 0.4);
        return true;
    }

    private static boolean tryVanishCounter(ServerPlayer defender, LivingEntity attacker, long now) {
        PlayerData data = PlayerManager.data(defender);
        if (data.ki() < ConfigManager.gameplay().vanishKiCost) {
            return false;
        }
        return teleportBehind(defender, data, attacker, now);
    }

    public static boolean dash(ServerPlayer player, int direction, long now) {
        PlayerData data = PlayerManager.data(player);
        RuntimeState rt = data.runtime();
        GameplayConfig gp = ConfigManager.gameplay();
        if (rt.busy(now) || now < rt.nextDash) {
            return false;
        }
        if (!PlayerManager.spendStamina(data, gp.dashStaminaCost)) {
            return false;
        }
        rt.nextDash = now + gp.dashCooldownTicks;
        rt.dashingUntil = now + 10;
        rt.charging = false;
        rt.guarding = false;
        rt.stateDirty = true;
        anim(player, AnimClip.DASH, 8, direction);
        fx(player, CombatFxKind.DASH, player, player, player.position().add(0, player.getBbHeight() * 0.5, 0), 1.0f);
        playSound(player.level(), player, ModSounds.DASH.get(), 0.8f, 1.0f);
        playGround(player.level(), player, 0.7f, false);
        TrainingSystem.passiveGain(player, data, "dash", StatType.SPEED, 0.25);
        return true;
    }

    public static void tickFighter(LivingEntity entity, RuntimeState rt, long now) {
        if (rt.smashedUntil > now && entity.onGround() && rt.smashedUntil - now < 36) {
            rt.smashedUntil = 0;
            rt.stunnedUntil = Math.max(rt.stunnedUntil, now + 24);
            rt.stateDirty = true;
            anim(entity, AnimClip.KNOCKDOWN, 24, 0);
            fx(entity, CombatFxKind.SHOCKWAVE, entity, entity, entity.position(), 1.2f);
            playSound((ServerLevel) entity.level(), entity, ModSounds.LAND.get(), 1.0f, 0.7f);
            playGround((ServerLevel) entity.level(), entity, 1.0f, true);
        }
    }

    public static void fx(Entity around, CombatFxKind kind, Entity attacker, Entity target, Vec3 pos, float strength) {
        Services.NETWORK.sendToTrackingAndSelf(around, new Packets.CombatFx(kind.ordinal(), attacker.getId(), target.getId(), pos.x, pos.y, pos.z, strength));
    }

    public static void anim(Entity entity, AnimClip clip, int duration, int variant) {
        Services.NETWORK.sendToTrackingAndSelf(entity, new Packets.Anim(entity.getId(), clip.ordinal(), duration, variant));
    }

    public static void playSound(ServerLevel level, Entity at, SoundEvent sound, float volume, float pitch) {
        level.playSound(null, at.getX(), at.getY() + at.getBbHeight() * 0.5, at.getZ(), sound, SoundSource.PLAYERS, volume, pitch);
    }

    public static void playGround(ServerLevel level, Entity at, float volume, boolean fall) {
        net.minecraft.core.BlockPos pos = at.blockPosition().below();
        net.minecraft.world.level.block.state.BlockState state = level.getBlockState(pos);
        if (state.isAir()) {
            pos = pos.below();
            state = level.getBlockState(pos);
        }
        if (state.isAir()) {
            return;
        }
        net.minecraft.world.level.block.SoundType type = state.getSoundType();
        SoundEvent sound = fall ? type.getFallSound() : type.getStepSound();
        level.playSound(null, at.getX(), at.getY(), at.getZ(), sound, SoundSource.PLAYERS, type.getVolume() * volume, type.getPitch() * (fall ? 0.75f : 0.9f));
    }
}
