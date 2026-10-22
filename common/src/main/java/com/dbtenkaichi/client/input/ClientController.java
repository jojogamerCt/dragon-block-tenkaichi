package com.dbtenkaichi.client.input;

import com.dbtenkaichi.client.ClientState;
import com.dbtenkaichi.client.anim.PoseApplier;
import com.dbtenkaichi.client.fx.CameraEffects;
import com.dbtenkaichi.client.fx.CombatFxHandler;
import com.dbtenkaichi.client.screen.CharacterScreen;
import com.dbtenkaichi.combat.CombatSystem;
import com.dbtenkaichi.combat.Fighter;
import com.dbtenkaichi.config.ConfigManager;
import com.dbtenkaichi.config.GameplayConfig;
import com.dbtenkaichi.entity.FighterNpc;
import com.dbtenkaichi.form.TransformationSystem;
import com.dbtenkaichi.network.ActionType;
import com.dbtenkaichi.network.Packets;
import com.dbtenkaichi.platform.Services;
import com.dbtenkaichi.player.PlayerData;
import com.dbtenkaichi.player.RuntimeState;
import com.dbtenkaichi.stats.DerivedStats;
import com.dbtenkaichi.stats.StatType;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

public final class ClientController {
    private static boolean stance;
    private static boolean charging;
    private static boolean guarding;
    private static int attackHold = -1;
    private static final boolean[] SKILL_HELD = new boolean[4];
    private static boolean fastSent;
    private static int dashTicks;
    private static Vec3 dashVelocity = Vec3.ZERO;
    private static double lastVerticalSpeed;
    private static boolean wasOnGround = true;

    private ClientController() {
    }

    public static boolean inStance() {
        return stance;
    }

    public static int attackHoldTicks() {
        return attackHold;
    }

    public static void startTick(Minecraft mc) {
        ClientState.tick();
        LocalPlayer player = mc.player;
        if (player == null || mc.level == null) {
            return;
        }
        for (Entity entity : mc.level.entitiesForRendering()) {
            if (entity instanceof Player || entity instanceof FighterNpc) {
                PoseApplier.tickVisual(entity, ClientState.forEntity(entity));
            }
        }
        CombatFxHandler.tick(mc);
        com.dbtenkaichi.client.cinematic.CinematicDirector.tick(mc);
        PlayerData data = Services.PLATFORM.playerData(player);
        if (!data.isCreated()) {
            while (Keybinds.MENU.consumeClick()) {
                send(ActionType.OPEN_MENU, 0);
            }
            com.dbtenkaichi.client.ClientSmokeTest.tick(mc);
            com.dbtenkaichi.client.MarketingCapture.tick(mc);
            return;
        }
        LockOn.tick(mc);
        if (mc.gui.screen() == null) {
            handleInput(mc, player, data);
        } else {
            releaseAll();
        }
        tickMovement(mc, player, data);
        CameraEffects.tick();
        com.dbtenkaichi.client.ClientSmokeTest.tick(mc);
        com.dbtenkaichi.client.MarketingCapture.tick(mc);
    }

    private static void send(ActionType type, int arg) {
        Services.NETWORK.sendToServer(new Packets.Action(type.ordinal(), arg));
    }

    private static void releaseAll() {
        if (charging) {
            charging = false;
            send(ActionType.CHARGE_END, 0);
        }
        if (guarding) {
            guarding = false;
            send(ActionType.GUARD_END, 0);
        }
        attackHold = -1;
        for (int i = 0; i < SKILL_HELD.length; i++) {
            if (SKILL_HELD[i]) {
                SKILL_HELD[i] = false;
                send(ActionType.SKILL_RELEASE, i);
            }
        }
    }

    private static void handleInput(Minecraft mc, LocalPlayer player, PlayerData data) {
        while (Keybinds.MENU.consumeClick()) {
            mc.gui.setScreen(new CharacterScreen());
            return;
        }
        while (Keybinds.STANCE.consumeClick()) {
            stance = !stance;
            send(ActionType.COMBAT_STANCE, stance ? 1 : 0);
            if (!stance && guarding) {
                guarding = false;
                send(ActionType.GUARD_END, 0);
            }
        }
        while (Keybinds.FLIGHT.consumeClick()) {
            send(ActionType.FLIGHT_TOGGLE, 0);
        }
        while (Keybinds.LOCK_ON.consumeClick()) {
            LockOn.toggle();
        }
        while (Keybinds.NEXT_TARGET.consumeClick()) {
            LockOn.next();
        }
        while (Keybinds.TRANSFORM.consumeClick()) {
            int mode = ConfigManager.client().fullCinematics() ? TransformationSystem.CINEMATIC_AUTO
                    : ConfigManager.client().shortCinematics() ? TransformationSystem.CINEMATIC_SHORT : TransformationSystem.CINEMATIC_SKIP;
            send(ActionType.TRANSFORM_UP, mode);
        }
        while (Keybinds.REVERT.consumeClick()) {
            send(ActionType.TRANSFORM_DOWN, 0);
        }
        while (Keybinds.KAIOKEN.consumeClick()) {
            send(ActionType.KAIOKEN, 0);
        }
        boolean chargeDown = Keybinds.CHARGE.isDown();
        if (ClientState.clashId >= 0) {
            while (Keybinds.CHARGE.consumeClick()) {
                send(ActionType.CLASH_PULSE, 0);
            }
        } else {
            while (Keybinds.CHARGE.consumeClick()) {
            }
            if (chargeDown != charging) {
                charging = chargeDown;
                send(charging ? ActionType.CHARGE_START : ActionType.CHARGE_END, 0);
            }
        }
        for (int i = 0; i < Keybinds.SKILLS.length; i++) {
            KeyMapping key = Keybinds.SKILLS[i];
            while (key.consumeClick()) {
            }
            boolean down = key.isDown();
            if (down != SKILL_HELD[i]) {
                SKILL_HELD[i] = down;
                send(down ? ActionType.SKILL_PRESS : ActionType.SKILL_RELEASE, i);
            }
        }
        while (Keybinds.DODGE.consumeClick()) {
            int dir = direction(mc);
            if (startDodge(mc, player, data, dir)) {
                send(ActionType.DODGE, dir);
            }
        }
        while (Keybinds.DASH.consumeClick()) {
            int dir = direction(mc);
            if (startDash(mc, player, data, dir)) {
                send(ActionType.DASH, dir);
            }
        }
        if (stance) {
            boolean attackDown = mc.options.keyAttack.isDown();
            while (mc.options.keyAttack.consumeClick()) {
                if (attackHold < 0) {
                    attackHold = 0;
                }
            }
            if (attackDown && attackHold >= 0) {
                attackHold++;
            } else if (!attackDown && attackHold >= 0) {
                if (attackHold < 6) {
                    send(ActionType.LIGHT_ATTACK, 0);
                } else {
                    send(ActionType.HEAVY_ATTACK, Math.min(20, attackHold - 6));
                }
                attackHold = -1;
            }
            boolean useDown = mc.options.keyUse.isDown();
            while (mc.options.keyUse.consumeClick()) {
            }
            if (useDown != guarding) {
                guarding = useDown;
                send(guarding ? ActionType.GUARD_START : ActionType.GUARD_END, 0);
            }
        } else {
            attackHold = -1;
        }
    }

    private static int direction(Minecraft mc) {
        if (mc.options.keyLeft.isDown()) {
            return CombatSystem.DIR_LEFT;
        }
        if (mc.options.keyRight.isDown()) {
            return CombatSystem.DIR_RIGHT;
        }
        if (mc.options.keyDown.isDown()) {
            return CombatSystem.DIR_BACK;
        }
        if (mc.options.keyUp.isDown()) {
            return CombatSystem.DIR_FORWARD;
        }
        if (mc.options.keyJump.isDown()) {
            return CombatSystem.DIR_UP;
        }
        return CombatSystem.DIR_NEUTRAL;
    }

    private static Vec3 directionVector(LocalPlayer player, int dir) {
        double yaw = Math.toRadians(player.getYRot());
        Vec3 forward = new Vec3(-Math.sin(yaw), 0, Math.cos(yaw));
        Vec3 left = new Vec3(Math.cos(yaw), 0, Math.sin(yaw));
        return switch (dir) {
            case CombatSystem.DIR_LEFT -> left;
            case CombatSystem.DIR_RIGHT -> left.scale(-1);
            case CombatSystem.DIR_BACK -> forward.scale(-1);
            case CombatSystem.DIR_UP -> new Vec3(0, 1, 0);
            case CombatSystem.DIR_DOWN -> new Vec3(0, -1, 0);
            default -> player.getViewVector(1.0f);
        };
    }

    private static boolean startDodge(Minecraft mc, LocalPlayer player, PlayerData data, int dir) {
        GameplayConfig gp = ConfigManager.gameplay();
        if (ClientState.stamina < gp.dodgeStaminaCost || ClientState.localHas(RuntimeState.FLAG_TRANSFORMING)) {
            return false;
        }
        if (dir == CombatSystem.DIR_NEUTRAL && LockOn.target() >= 0) {
            return true;
        }
        Vec3 v = directionVector(player, dir == CombatSystem.DIR_NEUTRAL ? CombatSystem.DIR_BACK : dir).normalize();
        double speed = 1.1 + data.stat(StatType.AGILITY) * 0.004;
        dashVelocity = v.scale(Math.min(2.2, speed)).add(0, player.onGround() ? 0.15 : 0.0, 0);
        dashTicks = 4;
        return true;
    }

    private static boolean startDash(Minecraft mc, LocalPlayer player, PlayerData data, int dir) {
        GameplayConfig gp = ConfigManager.gameplay();
        if (ClientState.stamina < gp.dashStaminaCost || ClientState.localHas(RuntimeState.FLAG_STUNNED) || ClientState.localHas(RuntimeState.FLAG_TRANSFORMING)) {
            return false;
        }
        Vec3 v;
        LivingEntity target = LockOn.targetEntity();
        if (target != null && (dir == CombatSystem.DIR_NEUTRAL || dir == CombatSystem.DIR_FORWARD)) {
            v = target.getBoundingBox().getCenter().subtract(player.getBoundingBox().getCenter());
            if (!data.runtime().flying) {
                v = v.multiply(1, 0.2, 1);
            }
        } else {
            v = directionVector(player, dir == CombatSystem.DIR_NEUTRAL ? CombatSystem.DIR_FORWARD : dir);
            if (!data.runtime().flying) {
                v = v.multiply(1, 0, 1);
            }
        }
        if (v.lengthSqr() < 1.0e-4) {
            return false;
        }
        double speed = Math.min(ConfigManager.gameplay().maxFlightSpeed, 1.6 + data.stat(StatType.SPEED) * 0.008);
        dashVelocity = v.normalize().scale(speed);
        dashTicks = 7;
        return true;
    }

    private static void tickMovement(Minecraft mc, LocalPlayer player, PlayerData data) {
        RuntimeState rt = data.runtime();
        boolean flying = rt.flying;
        if (dashTicks > 0) {
            dashTicks--;
            Vec3 v = dashTicks == 0 ? dashVelocity.scale(0.35) : dashVelocity;
            if (!flying && !player.onGround()) {
                v = new Vec3(v.x, Math.max(v.y, player.getDeltaMovement().y), v.z);
            }
            player.setDeltaMovement(v);
            if (!flying && dashTicks == 0) {
                player.setDeltaMovement(v.x, player.getDeltaMovement().y, v.z);
            }
            lastVerticalSpeed = player.getDeltaMovement().y;
            wasOnGround = player.onGround();
            return;
        }
        if (!flying) {
            if (fastSent) {
                fastSent = false;
            }
            if (!wasOnGround && player.onGround() && lastVerticalSpeed < -1.0) {
                send(ActionType.LANDED, (int) Math.min(600, -lastVerticalSpeed * 100));
            }
            lastVerticalSpeed = player.getDeltaMovement().y;
            wasOnGround = player.onGround();
            return;
        }
        if (ClientState.localHas(RuntimeState.FLAG_STUNNED) || ClientState.localHas(RuntimeState.FLAG_TRANSFORMING)) {
            player.setDeltaMovement(player.getDeltaMovement().scale(0.7));
            return;
        }
        boolean screenOpen = mc.gui.screen() != null;
        float forward = screenOpen ? 0 : (mc.options.keyUp.isDown() ? 1 : 0) - (mc.options.keyDown.isDown() ? 1 : 0);
        float strafe = screenOpen ? 0 : (mc.options.keyLeft.isDown() ? 1 : 0) - (mc.options.keyRight.isDown() ? 1 : 0);
        float vertical = screenOpen ? 0 : (mc.options.keyJump.isDown() ? 1 : 0) - (mc.options.keyShift.isDown() ? 1 : 0);
        boolean wantsFast = !screenOpen && mc.options.keySprint.isDown() && forward > 0;
        if (wantsFast != fastSent) {
            fastSent = wantsFast;
            send(ActionType.FLIGHT_FAST, wantsFast ? 1 : 0);
        }
        double yaw = Math.toRadians(player.getYRot());
        double pitch = Math.toRadians(player.getXRot());
        Vec3 look = new Vec3(-Math.sin(yaw) * Math.cos(pitch), -Math.sin(pitch), Math.cos(yaw) * Math.cos(pitch));
        Vec3 left = new Vec3(Math.cos(yaw), 0, Math.sin(yaw));
        Vec3 desired = look.scale(forward).add(left.scale(strafe)).add(0, vertical, 0);
        if (desired.lengthSqr() > 1.0) {
            desired = desired.normalize();
        }
        GameplayConfig gp = ConfigManager.gameplay();
        Fighter fighter = Fighter.of(player);
        boolean fast = rt.fastFlight && wantsFast;
        double speed = DerivedStats.flightSpeed(data.stats(), fast, fighter.speedMultiplier(), gp.baseFlightSpeed, gp.fastFlightSpeed, gp.flightSpeedPerLevel, gp.maxFlightSpeed);
        if (ClientState.localHas(RuntimeState.FLAG_CHARGING) || ClientState.localHas(RuntimeState.FLAG_SKILL_CHARGING) || ClientState.localHas(RuntimeState.FLAG_BEAM_FIRING)) {
            speed *= 0.25;
        }
        Vec3 current = player.getDeltaMovement();
        Vec3 target = desired.scale(speed);
        double accel = desired.lengthSqr() > 1.0e-4 ? (fast ? 0.16 : 0.3) : 0.35;
        Vec3 next = current.add(target.subtract(current).scale(accel));
        player.setDeltaMovement(next);
        player.resetFallDistance();
        if (player.onGround() && vertical < 0) {
            send(ActionType.FLIGHT_TOGGLE, 0);
            send(ActionType.LANDED, (int) Math.min(600, Math.abs(current.y) * 100 + current.horizontalDistance() * 50));
        }
        lastVerticalSpeed = next.y;
        wasOnGround = player.onGround();
    }

    public static boolean suppressMovement() {
        return ClientState.localHas(RuntimeState.FLAG_STUNNED) || ClientState.localHas(RuntimeState.FLAG_TRANSFORMING) || dashTicks > 0;
    }

    public static boolean flyingLocally() {
        Minecraft mc = Minecraft.getInstance();
        return mc.player != null && Services.PLATFORM.playerData(mc.player).runtime().flying;
    }

    public static void onDisconnect() {
        stance = false;
        charging = false;
        guarding = false;
        attackHold = -1;
        dashTicks = 0;
        LockOn.reset();
        ClientState.clear();
        com.dbtenkaichi.client.render.CustomSkins.clear();
        com.dbtenkaichi.client.render.MajinBody.clear();
        ConfigManager.clearServerConfig();
        com.dbtenkaichi.form.FormRegistry.clearClient();
    }
}
