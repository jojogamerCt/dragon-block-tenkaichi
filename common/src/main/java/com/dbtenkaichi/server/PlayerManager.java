package com.dbtenkaichi.server;

import com.dbtenkaichi.Tenkaichi;
import com.dbtenkaichi.combat.Fighter;
import com.dbtenkaichi.config.BalanceConfig;
import com.dbtenkaichi.config.ConfigManager;
import com.dbtenkaichi.config.GameplayConfig;
import com.dbtenkaichi.form.FormDefinition;
import com.dbtenkaichi.form.FormRegistry;
import com.dbtenkaichi.form.TransformationSystem;
import com.dbtenkaichi.movement.FlightSystem;
import com.dbtenkaichi.network.Packets;
import com.dbtenkaichi.platform.Services;
import com.dbtenkaichi.player.PlayerData;
import com.dbtenkaichi.player.RuntimeState;
import com.dbtenkaichi.progression.ProgressionMath;
import com.dbtenkaichi.race.RacePassive;
import com.dbtenkaichi.stats.DerivedStats;
import com.dbtenkaichi.stats.StatType;
import com.dbtenkaichi.training.TrainingSystem;
import net.minecraft.core.Holder;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;

public final class PlayerManager {
    public static final int SCREEN_CREATOR = 0;
    public static final int SCREEN_CHARACTER = 1;
    public static final int SCREEN_TRAINING = 2;

    private static final Identifier HEALTH_ID = id("constitution_health");
    private static final Identifier SPEED_ID = id("speed_bonus");
    private static final Identifier SCALE_ID = id("character_height");
    private static final Identifier FALL_ID = id("trained_body_fall");
    private static final Identifier FORM_SPEED_ID = id("form_speed");
    private static final Identifier CHARGE_SLOW_ID = id("charge_slow");

    private PlayerManager() {
    }

    private static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath(Tenkaichi.MOD_ID, path);
    }

    public static PlayerData data(Player player) {
        return Services.PLATFORM.playerData(player);
    }

    public static void onJoin(ServerPlayer player) {
        PlayerData data = data(player);
        data.runtime().resetCombat();
        if (data.isTransformed() && FormRegistry.get(data.form(), false) == null) {
            data.setForm(PlayerData.BASE_FORM);
        }
        refreshCaches(player, data);
        if (data.ki() < 0) {
            data.setKi(data.runtime().cachedMaxKi);
        }
        if (data.stamina() < 0) {
            data.setStamina(data.runtime().cachedMaxStamina);
        }
        applyAttributes(player, data);
        if (data.isCreated() && !data.outfitGiven()) {
            com.dbtenkaichi.item.Outfits.equip(player, data.appearance());
            data.setOutfitGiven(true);
            data.runtime().dataDirty = true;
        }
        Services.NETWORK.sendToPlayer(player, new Packets.ServerConfig(ConfigManager.serializeCommon()));
        Services.NETWORK.sendToPlayer(player, new Packets.Forms(FormRegistry.serializeServer()));
        markAllDirty(data);
        flush(player, data, player.level().getGameTime());
        if (!data.isCreated()) {
            openCreator(player);
        }
    }

    public static void onRespawn(ServerPlayer player, boolean died) {
        PlayerData data = data(player);
        data.runtime().resetCombat();
        player.setNoGravity(false);
        if (died) {
            if (data.isTransformed()) {
                data.setForm(PlayerData.BASE_FORM);
            }
            refreshCaches(player, data);
            data.setKi(data.runtime().cachedMaxKi * 0.5);
            data.setStamina(data.runtime().cachedMaxStamina);
        }
        refreshCaches(player, data);
        applyAttributes(player, data);
        if (died) {
            player.setHealth(player.getMaxHealth());
        }
        markAllDirty(data);
        flush(player, data, player.level().getGameTime());
    }

    public static void onLeave(ServerPlayer player) {
        PlayerData data = data(player);
        com.dbtenkaichi.ki.BeamClashManager.onLeave(player);
        com.dbtenkaichi.training.TrialManager.onLeave(player);
        if (data.runtime().flying) {
            player.setNoGravity(false);
        }
        data.runtime().resetCombat();
    }

    public static void onStartTracking(ServerPlayer tracker, Entity target) {
        if (target instanceof ServerPlayer other) {
            PlayerData data = data(other);
            Services.NETWORK.sendToPlayer(tracker, appearancePacket(other, data));
            Services.NETWORK.sendToPlayer(tracker, statePacket(other, data, other.level().getGameTime()));
        }
    }

    public static void openCreator(ServerPlayer player) {
        CompoundTag extra = new CompoundTag();
        StringBuilder races = new StringBuilder();
        for (String race : ConfigManager.gameplay().enabledRaces) {
            if (!races.isEmpty()) {
                races.append(',');
            }
            races.append(race);
        }
        extra.putString("races", races.toString());
        extra.putFloat("min_height", ConfigManager.gameplay().minHeight);
        extra.putFloat("max_height", ConfigManager.gameplay().maxHeight);
        Services.NETWORK.sendToPlayer(player, new Packets.OpenScreen(SCREEN_CREATOR, extra));
    }

    public static void tickServer(MinecraftServer server) {
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            try {
                tickPlayer(player);
            } catch (RuntimeException e) {
                Tenkaichi.LOG.error("Error ticking Tenkaichi data for {}", player.getPlainTextName(), e);
            }
        }
    }

    public static void buyTechnique(ServerPlayer player, PlayerData data, int index) {
        if (index == com.dbtenkaichi.form.KaiokenSystem.SHOP_INDEX) {
            buyKaioken(player, data);
            return;
        }
        if (!data.isCreated() || index < 0 || index >= com.dbtenkaichi.ki.Techniques.UNLOCKS.size()) {
            return;
        }
        com.dbtenkaichi.ki.Techniques.Unlock unlock = com.dbtenkaichi.ki.Techniques.UNLOCKS.get(index);
        String id = unlock.technique().id();
        if (data.hasSkill(id) || !unlock.allows(data.race())) {
            return;
        }
        if (com.dbtenkaichi.stats.BattlePower.base(data.stats(), ConfigManager.balance()) < unlock.battlePower()) {
            player.sendSystemMessage(Component.translatable("message.dbtenkaichi.lesson_locked", unlock.battlePower()));
            return;
        }
        if (data.trainingPoints() < unlock.price()) {
            player.sendSystemMessage(Component.translatable("message.dbtenkaichi.not_enough_tp", unlock.price()));
            return;
        }
        data.setTrainingPoints(data.trainingPoints() - unlock.price());
        data.learnSkill(id);
        data.runtime().dataDirty = true;
        player.sendSystemMessage(Component.translatable("message.dbtenkaichi.learned", Component.translatable(unlock.technique().translationKey())));
        com.dbtenkaichi.combat.CombatSystem.playSound(player.level(), player, com.dbtenkaichi.registry.ModSounds.UI_CONFIRM.get(), 1.0f, 1.0f);
        com.dbtenkaichi.combat.CombatSystem.playSound(player.level(), player, net.minecraft.sounds.SoundEvents.PLAYER_LEVELUP, 0.35f, 1.2f);
    }

    public static void formTrial(ServerPlayer player, PlayerData data, int index) {
        java.util.List<FormDefinition> chain = new java.util.ArrayList<>(FormRegistry.chainFor(data.race(), false));
        if (!data.isCreated() || index < 0 || index >= chain.size()) {
            return;
        }
        FormDefinition form = chain.get(index);
        if (data.hasUnlockedForm(form.id)) {
            return;
        }
        if (!PlayerData.BASE_FORM.equals(form.parent) && !data.hasUnlockedForm(form.parent)) {
            FormDefinition parent = FormRegistry.get(form.parent, false);
            player.sendSystemMessage(Component.translatable("message.dbtenkaichi.form_trial_parent", Component.translatable(parent == null ? form.parent : parent.name)));
            return;
        }
        String missing = com.dbtenkaichi.form.TransformationSystem.missingRequirement(player, data, form);
        if (missing != null) {
            player.sendSystemMessage(Component.translatable("message.dbtenkaichi.form_locked", Component.translatable(form.name), Component.translatable("requirement.dbtenkaichi." + missing.replace(':', '.'))));
            return;
        }
        com.dbtenkaichi.training.TrialManager.startForm(player, data, form);
    }

    private static void buyKaioken(ServerPlayer player, PlayerData data) {
        if (!data.isCreated() || data.hasSkill(com.dbtenkaichi.form.KaiokenSystem.ABILITY)) {
            return;
        }
        if (com.dbtenkaichi.stats.BattlePower.base(data.stats(), ConfigManager.balance()) < com.dbtenkaichi.form.KaiokenSystem.BATTLE_POWER) {
            player.sendSystemMessage(Component.translatable("message.dbtenkaichi.lesson_locked", com.dbtenkaichi.form.KaiokenSystem.BATTLE_POWER));
            return;
        }
        if (data.trainingPoints() < com.dbtenkaichi.form.KaiokenSystem.PRICE) {
            player.sendSystemMessage(Component.translatable("message.dbtenkaichi.not_enough_tp", com.dbtenkaichi.form.KaiokenSystem.PRICE));
            return;
        }
        data.setTrainingPoints(data.trainingPoints() - com.dbtenkaichi.form.KaiokenSystem.PRICE);
        data.learnSkill(com.dbtenkaichi.form.KaiokenSystem.ABILITY);
        data.runtime().dataDirty = true;
        player.sendSystemMessage(Component.translatable("message.dbtenkaichi.learned", Component.translatable("technique.dbtenkaichi.kaioken")));
        com.dbtenkaichi.combat.CombatSystem.playSound(player.level(), player, com.dbtenkaichi.registry.ModSounds.UI_CONFIRM.get(), 1.0f, 1.0f);
        com.dbtenkaichi.combat.CombatSystem.playSound(player.level(), player, net.minecraft.sounds.SoundEvents.PLAYER_LEVELUP, 0.35f, 1.2f);
    }

    private static void tickPlayer(ServerPlayer player) {
        PlayerData data = data(player);
        RuntimeState rt = data.runtime();
        long now = player.level().getGameTime();
        if (!data.isCreated()) {
            return;
        }
        BalanceConfig cfg = ConfigManager.balance();
        GameplayConfig gp = ConfigManager.gameplay();
        if (now % 10 == 0) {
            refreshCaches(player, data);
        }
        double maxKi = rt.cachedMaxKi;
        double maxStamina = rt.cachedMaxStamina;
        double ki = data.ki() < 0 ? maxKi : data.ki();
        double stamina = data.stamina() < 0 ? maxStamina : data.stamina();

        if (player.isSpectator() || player.isDeadOrDying()) {
            if (rt.flying) {
                FlightSystem.stop(player, data);
            }
            return;
        }

        boolean exhausted = rt.exhaustedUntil > now;
        if (rt.charging && !exhausted && !rt.busy(now)) {
            ki += DerivedStats.kiChargePerSecond(data.stats(), data.race(), cfg) / 20.0;
            stamina -= 0.5 / 20.0;
            if (now % 20 == 0) {
                TrainingSystem.passiveGain(player, data, "ki_charge", StatType.KI_CONTROL, 0.6);
            }
        } else if (!exhausted) {
            double regen = DerivedStats.kiRegenPerSecond(data.stats(), data.race(), cfg);
            if (data.race().has(RacePassive.ENERGY_RESERVE)) {
                regen *= 2.0;
            }
            if (now - rt.lastCombat < 100) {
                regen *= 0.5;
            }
            ki += regen / 20.0;
        }

        if (data.isTransformed()) {
            FormDefinition form = FormRegistry.get(data.form(), false);
            if (form == null) {
                data.setForm(PlayerData.BASE_FORM);
                rt.stateDirty = true;
                rt.dataDirty = true;
            } else {
                double drain = form.drainAt(data.mastery(form.id)) * DerivedStats.kiCostMultiplier(data.stats(), data.race(), cfg) / 20.0;
                ki -= drain;
                stamina -= form.staminaDrainPerSecond / 20.0;
                if (form.healthDrainPerSecond > 0.0 && player.getHealth() > 2.0f) {
                    float drained = (float) (form.healthDrainPerSecond * (1.0 - 0.5 * data.mastery(form.id)) / 20.0);
                    player.setHealth(Math.max(2.0f, player.getHealth() - drained));
                }
                float gain = (float) (form.masteryPerSecond / 20.0 * (now - rt.lastCombat < 100 ? 2.0 : 1.0) * cfg.trainingSpeedMultiplier);
                float mastery = data.mastery(form.id);
                if (mastery < 1.0f && gain > 0) {
                    data.setMastery(form.id, mastery + gain);
                    if (now % 100 == 0) {
                        rt.dataDirty = true;
                    }
                }
                if (ki <= 0) {
                    ki = 0;
                    TransformationSystem.forceRevert(player, data, Component.translatable("message.dbtenkaichi.form_exhausted"));
                    rt.exhaustedUntil = now + 100;
                    data.setFatigue(data.fatigue() + 0.1);
                }
            }
        }

        if (rt.kaioken > 0) {
            ki -= com.dbtenkaichi.form.KaiokenSystem.tick(player, data, cfg, now) * DerivedStats.kiCostMultiplier(data.stats(), data.race(), cfg);
            if (ki <= 0) {
                ki = 0;
                com.dbtenkaichi.form.KaiokenSystem.off(player, data, now, "message.dbtenkaichi.kaioken_exhausted");
            }
        }

        if (rt.flying) {
            ki -= FlightSystem.kiCostPerTick(player, data);
            if (ki <= 0) {
                ki = 0;
                FlightSystem.stop(player, data);
                player.sendSystemMessage(Component.translatable("message.dbtenkaichi.flight_exhausted"));
            }
        }

        if (!rt.guarding && now - rt.lastCombat > 20) {
            stamina += DerivedStats.staminaRegenPerSecond(data.stats(), cfg) / 20.0;
        } else if (!rt.guarding) {
            stamina += DerivedStats.staminaRegenPerSecond(data.stats(), cfg) / 60.0;
        }

        double fatigueRecovery = player.isSleeping() ? 0.004 : 0.0003;
        if (now - rt.lastCombat > 200 && data.fatigue() > 0) {
            data.setFatigue(data.fatigue() - fatigueRecovery);
        }

        ki = com.dbtenkaichi.race.RaceMechanics.tick(player, data, ki, now);
        data.setKi(clamp(ki, 0, maxKi));
        data.setStamina(clamp(stamina, 0, maxStamina));

        ZenkaiSystem.tick(player, data, now);
        TransformationSystem.tickTransforming(player, data, now);
        FlightSystem.tick(player, data, now);
        TrainingSystem.tick(player, data, now);

        flush(player, data, now);
    }

    public static void refreshCaches(ServerPlayer player, PlayerData data) {
        BalanceConfig cfg = ConfigManager.balance();
        RuntimeState rt = data.runtime();
        rt.cachedMaxKi = DerivedStats.maxKi(data.stats(), data.race(), cfg);
        rt.cachedMaxStamina = DerivedStats.maxStamina(data.stats(), cfg);
        rt.cachedBp = Fighter.of(player).displayedBattlePower();
    }

    public static void applyAttributes(ServerPlayer player, PlayerData data) {
        BalanceConfig cfg = ConfigManager.balance();
        float healthRatio = player.getMaxHealth() > 0 ? player.getHealth() / player.getMaxHealth() : 1.0f;
        setModifier(player, Attributes.MAX_HEALTH, HEALTH_ID, data.isCreated() ? DerivedStats.bonusHealth(data.stats(), cfg) : 0.0, AttributeModifier.Operation.ADD_VALUE);
        setModifier(player, Attributes.MOVEMENT_SPEED, SPEED_ID, data.isCreated() ? DerivedStats.movementBonus(data.stats()) : 0.0, AttributeModifier.Operation.ADD_MULTIPLIED_BASE);
        setModifier(player, Attributes.SCALE, SCALE_ID, data.isCreated() ? data.appearance().height - 1.0 : 0.0, AttributeModifier.Operation.ADD_MULTIPLIED_BASE);
        setModifier(player, Attributes.SAFE_FALL_DISTANCE, FALL_ID, data.isCreated() ? Math.min(40.0, 3.0 + data.stat(StatType.CONSTITUTION) * 0.25 + data.stat(StatType.AGILITY) * 0.15) : 0.0, AttributeModifier.Operation.ADD_VALUE);
        FormDefinition form = data.isTransformed() ? FormRegistry.get(data.form(), false) : null;
        setModifier(player, Attributes.MOVEMENT_SPEED, FORM_SPEED_ID, (form == null ? 1.0 : form.speedMultiplier * form.controlFactor(data.mastery(form.id))) * com.dbtenkaichi.form.KaiokenSystem.speed(data.runtime().kaioken) - 1.0, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL);
        setModifier(player, Attributes.MOVEMENT_SPEED, CHARGE_SLOW_ID, data.runtime().charging ? -0.6 : 0.0, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL);
        com.dbtenkaichi.race.RaceMechanics.applyAttributes(player, data);
        float newMax = player.getMaxHealth();
        if (player.getHealth() > newMax) {
            player.setHealth(newMax);
        } else if (healthRatio > 0 && healthRatio <= 1.0f && Math.abs(player.getHealth() / newMax - healthRatio) > 0.01f) {
            player.setHealth(Math.max(1.0f, newMax * healthRatio));
        }
    }

    private static void setModifier(ServerPlayer player, Holder<Attribute> attribute, Identifier id, double amount, AttributeModifier.Operation op) {
        AttributeInstance instance = player.getAttribute(attribute);
        if (instance == null) {
            return;
        }
        if (Math.abs(amount) < 1.0e-6) {
            instance.removeModifier(id);
        } else {
            instance.addOrUpdateTransientModifier(new AttributeModifier(id, amount, op));
        }
    }

    public static void markAllDirty(PlayerData data) {
        RuntimeState rt = data.runtime();
        rt.dataDirty = true;
        rt.vitalsDirty = true;
        rt.stateDirty = true;
        rt.appearanceDirty = true;
    }

    public static void flush(ServerPlayer player, PlayerData data, long now) {
        RuntimeState rt = data.runtime();
        if (rt.dataDirty) {
            rt.dataDirty = false;
            Services.NETWORK.sendToPlayer(player, new Packets.SyncData(data.save()));
            rt.vitalsDirty = true;
        }
        if (rt.appearanceDirty) {
            rt.appearanceDirty = false;
            Services.NETWORK.sendToTrackingAndSelf(player, appearancePacket(player, data));
        }
        int flags = rt.flags(now);
        if (rt.stateDirty || flags != rt.lastSentFlags) {
            rt.stateDirty = false;
            rt.lastSentFlags = flags;
            Services.NETWORK.sendToTrackingAndSelf(player, statePacket(player, data, now));
        }
        float ki = (float) data.ki();
        float stamina = (float) data.stamina();
        boolean changed = rt.vitalsDirty
                || Math.abs(ki - rt.lastSentKi) > Math.max(0.5f, (float) rt.cachedMaxKi * 0.004f)
                || Math.abs(stamina - rt.lastSentStamina) > 0.75f
                || rt.cachedBp != rt.lastSentBp;
        if (changed && (rt.vitalsDirty || now % 2 == 0)) {
            rt.vitalsDirty = false;
            rt.lastSentKi = ki;
            rt.lastSentStamina = stamina;
            rt.lastSentBp = rt.cachedBp;
            int chargeTicks = rt.skillSlot >= 0 ? (int) (now - rt.skillStart) : 0;
            Services.NETWORK.sendToPlayer(player, new Packets.Vitals(ki, (float) rt.cachedMaxKi, stamina, (float) rt.cachedMaxStamina,
                    (float) data.fatigue(), rt.cachedBp, data.trainingPoints(), flags, chargeTicks));
        }
    }

    public static Packets.Appearance appearancePacket(ServerPlayer player, PlayerData data) {
        CompoundTag tag = data.appearance().save();
        tag.putBoolean("created", data.isCreated());
        return new Packets.Appearance(player.getId(), data.race().ordinal(), tag);
    }

    public static Packets.EntityState statePacket(ServerPlayer player, PlayerData data, long now) {
        double maxKi = Math.max(1.0, data.runtime().cachedMaxKi);
        return new Packets.EntityState(player.getId(), data.form(), data.runtime().flags(now), (float) (Math.max(0, data.ki()) / maxKi), data.mastery(data.form()));
    }

    public static boolean spendKi(ServerPlayer player, PlayerData data, double amount) {
        double cost = amount * DerivedStats.kiCostMultiplier(data.stats(), data.race(), ConfigManager.balance());
        if (data.ki() < cost) {
            return false;
        }
        data.setKi(data.ki() - cost);
        data.runtime().vitalsDirty = true;
        return true;
    }

    public static boolean spendStamina(PlayerData data, double amount) {
        if (data.stamina() < amount) {
            return false;
        }
        data.setStamina(data.stamina() - amount);
        data.runtime().vitalsDirty = true;
        return true;
    }

    public static void grantTp(ServerPlayer player, PlayerData data, double amount) {
        long whole = (long) Math.floor(amount);
        double frac = amount - whole;
        if (player.getRandom().nextDouble() < frac) {
            whole++;
        }
        if (whole > 0) {
            data.addTrainingPoints(whole);
            data.runtime().vitalsDirty = true;
        }
    }

    public static boolean upgradeStat(ServerPlayer player, PlayerData data, StatType type) {
        BalanceConfig cfg = ConfigManager.balance();
        int level = data.stat(type);
        int cap = type == StatType.POTENTIAL ? cfg.statHardCap : ProgressionMath.statCap(data.stat(StatType.POTENTIAL), cfg);
        if (level >= cap) {
            player.sendSystemMessage(Component.translatable("message.dbtenkaichi.stat_capped", Component.translatable(type.translationKey()), cap));
            return false;
        }
        double affinity = data.race().affinity(type);
        if (type == StatType.POTENTIAL) {
            affinity *= 0.5;
        }
        long cost = ProgressionMath.statUpgradeCost(level, affinity, cfg);
        if (data.trainingPoints() < cost) {
            com.dbtenkaichi.combat.CombatSystem.playSound(player.level(), player, com.dbtenkaichi.registry.ModSounds.UI_DENY.get(), 0.6f, 1.0f);
            return false;
        }
        data.setTrainingPoints(data.trainingPoints() - cost);
        data.stats().add(type, 1);
        com.dbtenkaichi.combat.CombatSystem.playSound(player.level(), player, com.dbtenkaichi.registry.ModSounds.UI_UPGRADE.get(), 0.7f, 1.0f);
        com.dbtenkaichi.combat.CombatSystem.playSound(player.level(), player, net.minecraft.sounds.SoundEvents.EXPERIENCE_ORB_PICKUP, 0.3f, 0.9f + Math.min(0.6f, level * 0.004f));
        refreshCaches(player, data);
        applyAttributes(player, data);
        data.runtime().dataDirty = true;
        return true;
    }

    private static double clamp(double v, double min, double max) {
        return v < min ? min : Math.min(v, max);
    }
}
