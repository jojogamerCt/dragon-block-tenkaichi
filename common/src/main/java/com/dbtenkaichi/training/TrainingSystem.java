package com.dbtenkaichi.training;

import com.dbtenkaichi.combat.CombatFxKind;
import com.dbtenkaichi.combat.CombatSystem;
import com.dbtenkaichi.config.BalanceConfig;
import com.dbtenkaichi.config.ConfigManager;
import com.dbtenkaichi.entity.MasterEntity;
import com.dbtenkaichi.ki.Techniques;
import com.dbtenkaichi.network.Packets;
import com.dbtenkaichi.platform.Services;
import com.dbtenkaichi.player.PlayerData;
import com.dbtenkaichi.player.RuntimeState;
import com.dbtenkaichi.progression.ProgressionMath;
import com.dbtenkaichi.registry.ModSounds;
import com.dbtenkaichi.server.PlayerManager;
import com.dbtenkaichi.stats.StatType;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

public final class TrainingSystem {
    public static final String MEDITATION = "meditation";
    public static final String KI_CONTROL = "ki_control";
    public static final String SPARRING = "sparring";
    public static final String DODGE_DRILL = "dodge_drill";

    public static final int PHASE_START = 0;
    public static final int PHASE_PRESS = 1;
    public static final int PHASE_STOP = 2;
    public static final int PHASE_LEARN = 3;
    public static final int PHASE_TRIAL = 4;

    public static final int MEDITATION_PERIOD = 30;
    public static final int SESSION_TICKS = 600;
    public static final int SPARRING_TICKS = 1200;
    public static final int DODGE_TICKS = 900;
    private static final double SESSION_RANGE_SQR = 40.0 * 40.0;

    private TrainingSystem() {
    }

    public static long day(ServerPlayer player) {
        return player.level().getGameTime() / 24000L;
    }

    public static double passiveGain(ServerPlayer player, PlayerData data, String activity, StatType stat, double base) {
        if (!data.isCreated()) {
            return 0.0;
        }
        BalanceConfig cfg = ConfigManager.balance();
        long day = day(player);
        int reps = data.trainingRepetitions(activity, day);
        double gain = ProgressionMath.trainingGain(base, data.fatigue(), reps, cfg) * data.race().tpGainMultiplier();
        data.recordTraining(activity, day);
        PlayerManager.grantTp(player, data, gain);
        addPractice(player, data, stat, gain * 0.035);
        data.setFatigue(data.fatigue() + base * 0.0005 * com.dbtenkaichi.race.RaceMechanics.fatigueFactor(data.race()));
        return gain;
    }

    public static void addPractice(ServerPlayer player, PlayerData data, StatType stat, double amount) {
        float value = data.practice(stat) + (float) (amount * com.dbtenkaichi.race.RaceMechanics.practiceFactor(data.race()));
        int cap = ProgressionMath.statCap(data.stat(StatType.POTENTIAL), ConfigManager.balance());
        if (value >= 1.0f && data.stat(stat) < cap) {
            value -= 1.0f;
            data.stats().add(stat, 1);
            PlayerManager.refreshCaches(player, data);
            PlayerManager.applyAttributes(player, data);
            data.runtime().dataDirty = true;
            player.sendOverlayMessage(Component.translatable("message.dbtenkaichi.practice_level", Component.translatable(stat.translationKey()), data.stat(stat)));
            CombatSystem.fx(player, CombatFxKind.LEVEL_UP, player, player, player.position(), 1.0f);
        } else if (value >= 1.0f) {
            value = 1.0f;
        }
        data.setPractice(stat, value);
    }

    public static void handleInput(ServerPlayer player, PlayerData data, String activity, int phase, int value, long now) {
        RuntimeState rt = data.runtime();
        switch (phase) {
            case PHASE_START -> start(player, data, activity, value, now);
            case PHASE_PRESS -> press(player, data, activity, now);
            case PHASE_STOP -> {
                if (activity.equals(rt.trainingActivity)) {
                    finish(player, data, now, false);
                }
            }
            case PHASE_LEARN -> lesson(player, data, activity, value, false);
            case PHASE_TRIAL -> lesson(player, data, activity, value, true);
            default -> {
            }
        }
    }

    private static MasterEntity nearbyMaster(ServerPlayer player, int entityId) {
        if (player.level().getEntity(entityId) instanceof MasterEntity master && master.isAlive() && master.distanceToSqr(player) < 64.0) {
            return master;
        }
        return null;
    }

    private static MasterEntity sessionMaster(ServerPlayer player, int entityId) {
        if (player.level().getEntity(entityId) instanceof MasterEntity master && master.isAlive() && master.distanceToSqr(player) < SESSION_RANGE_SQR) {
            return master;
        }
        return null;
    }

    public static int sessionTicks(String activity) {
        if (SPARRING.equals(activity)) {
            return SPARRING_TICKS;
        }
        if (DODGE_DRILL.equals(activity)) {
            return DODGE_TICKS;
        }
        return SESSION_TICKS;
    }

    private static void start(ServerPlayer player, PlayerData data, String activity, int masterId, long now) {
        RuntimeState rt = data.runtime();
        MasterEntity master = nearbyMaster(player, masterId);
        if (master == null || rt.trainingActivity != null || now - rt.lastCombat < 100 || rt.busy(now)) {
            return;
        }
        if (data.fatigue() > 0.95) {
            player.sendSystemMessage(Component.translatable("message.dbtenkaichi.too_tired"));
            return;
        }
        if (!MEDITATION.equals(activity) && !KI_CONTROL.equals(activity) && !SPARRING.equals(activity) && !DODGE_DRILL.equals(activity)) {
            return;
        }
        rt.trainingActivity = activity;
        rt.trainingStart = now;
        rt.trainingStreak = 0;
        rt.trainingHits = 0;
        rt.trainingScore = 0;
        rt.trainingMaster = master.getId();
        rt.stateDirty = true;
        CompoundTag tag = new CompoundTag();
        tag.putString("activity", activity);
        tag.putString("mode", "run");
        tag.putInt("period", MEDITATION_PERIOD);
        tag.putInt("duration", sessionTicks(activity));
        if (KI_CONTROL.equals(activity)) {
            data.setKi(rt.cachedMaxKi * 0.2);
            rerollBand(player, rt);
            tag.putFloat("band_min", rt.bandMin);
            tag.putFloat("band_max", rt.bandMax);
        }
        if (SPARRING.equals(activity)) {
            master.startSparring(player);
        }
        if (DODGE_DRILL.equals(activity)) {
            master.startDrill(player);
        }
        Services.NETWORK.sendToPlayer(player, new Packets.OpenScreen(PlayerManager.SCREEN_TRAINING, tag));
    }

    private static void rerollBand(ServerPlayer player, RuntimeState rt) {
        float width = 0.12f;
        float min = 0.35f + player.getRandom().nextFloat() * 0.45f;
        rt.bandMin = min;
        rt.bandMax = Math.min(1.0f, min + width);
    }

    private static void press(ServerPlayer player, PlayerData data, String activity, long now) {
        RuntimeState rt = data.runtime();
        if (!MEDITATION.equals(activity) || !MEDITATION.equals(rt.trainingActivity)) {
            return;
        }
        if (now - rt.lastTrainingPress < 8) {
            rt.trainingStreak = 0;
            return;
        }
        rt.lastTrainingPress = now;
        long phase = (now - rt.trainingStart) % MEDITATION_PERIOD;
        int tolerance = 3 + Math.min(4, player.connection.latency() / 50);
        boolean good = phase <= tolerance || phase >= MEDITATION_PERIOD - tolerance;
        if (good) {
            rt.trainingStreak++;
            rt.trainingScore++;
            double base = 0.8 * (1.0 + Math.min(10, rt.trainingStreak) * 0.12);
            passiveGain(player, data, "meditation_beat", StatType.SPIRIT, base);
            addPractice(player, data, StatType.KI_CONTROL, base * 0.02);
            CombatSystem.playSound(player.level(), player, ModSounds.TRAINING_SUCCESS.get(), 0.6f, 1.0f + Math.min(10, rt.trainingStreak) * 0.05f);
            CombatSystem.playSound(player.level(), player, net.minecraft.sounds.SoundEvents.EXPERIENCE_ORB_PICKUP, 0.25f, 1.0f + Math.min(10, rt.trainingStreak) * 0.05f);
        } else {
            rt.trainingStreak = 0;
            CombatSystem.playSound(player.level(), player, ModSounds.UI_CLICK.get(), 0.6f, 0.6f);
        }
        rt.vitalsDirty = true;
    }

    public static void onChargeEnd(ServerPlayer player, PlayerData data) {
        RuntimeState rt = data.runtime();
        if (!KI_CONTROL.equals(rt.trainingActivity)) {
            return;
        }
        float ratio = (float) (data.ki() / Math.max(1.0, rt.cachedMaxKi));
        if (ratio >= rt.bandMin && ratio <= rt.bandMax) {
            rt.trainingScore++;
            passiveGain(player, data, "ki_control_band", StatType.KI_CONTROL, 1.6);
            CombatSystem.playSound(player.level(), player, ModSounds.TRAINING_SUCCESS.get(), 0.7f, 1.2f);
            data.setKi(rt.cachedMaxKi * 0.2);
        } else {
            CombatSystem.playSound(player.level(), player, ModSounds.UI_CLICK.get(), 0.6f, 0.6f);
            data.setKi(rt.cachedMaxKi * 0.2);
        }
        rerollBand(player, rt);
        CompoundTag tag = new CompoundTag();
        tag.putString("activity", KI_CONTROL);
        tag.putString("mode", "band");
        tag.putFloat("band_min", rt.bandMin);
        tag.putFloat("band_max", rt.bandMax);
        tag.putInt("score", rt.trainingScore);
        Services.NETWORK.sendToPlayer(player, new Packets.OpenScreen(PlayerManager.SCREEN_TRAINING, tag));
        rt.vitalsDirty = true;
    }

    public static void tick(ServerPlayer player, PlayerData data, long now) {
        RuntimeState rt = data.runtime();
        if (rt.trainingActivity == null) {
            return;
        }
        MasterEntity master = sessionMaster(player, rt.trainingMaster);
        if (master == null || now - rt.trainingStart > sessionTicks(rt.trainingActivity)) {
            finish(player, data, now, master == null);
            return;
        }
        if (SPARRING.equals(rt.trainingActivity) && player.getHealth() < player.getMaxHealth() * 0.25f) {
            finish(player, data, now, false);
        }
    }

    public static void finish(ServerPlayer player, PlayerData data, long now, boolean aborted) {
        RuntimeState rt = data.runtime();
        String activity = rt.trainingActivity;
        if (activity == null) {
            return;
        }
        MasterEntity master = sessionMaster(player, rt.trainingMaster);
        if (master != null) {
            master.stopSession(player);
        }
        double reward = 0.0;
        if (!aborted) {
            switch (activity) {
                case SPARRING -> {
                    reward = passiveGain(player, data, "sparring_session", StatType.MARTIAL_ARTS, 4.0 + rt.trainingScore * 0.6);
                    addPractice(player, data, StatType.STRENGTH, 0.15 + rt.trainingScore * 0.02);
                }
                case DODGE_DRILL -> {
                    int dodged = Math.max(0, rt.trainingScore - rt.trainingHits);
                    reward = passiveGain(player, data, "dodge_session", StatType.AGILITY, 2.0 + dodged * 0.8);
                    addPractice(player, data, StatType.SPEED, dodged * 0.03);
                }
                case MEDITATION -> reward = passiveGain(player, data, "meditation_session", StatType.SPIRIT, 1.5 + rt.trainingScore * 0.3);
                case KI_CONTROL -> reward = passiveGain(player, data, "ki_control_session", StatType.KI_CONTROL, 1.5 + rt.trainingScore * 0.8);
                default -> {
                }
            }
            if (rt.trainingScore > 0) {
                data.incrementTrainingSessions();
            }
        }
        CompoundTag tag = new CompoundTag();
        tag.putString("activity", activity);
        tag.putString("mode", "summary");
        tag.putInt("score", rt.trainingScore);
        tag.putInt("hits", rt.trainingHits);
        tag.putFloat("reward", (float) reward);
        tag.putBoolean("aborted", aborted);
        rt.trainingActivity = null;
        rt.trainingMaster = -1;
        rt.stateDirty = true;
        rt.dataDirty = true;
        Services.NETWORK.sendToPlayer(player, new Packets.OpenScreen(PlayerManager.SCREEN_TRAINING, tag));
    }

    private static void lesson(ServerPlayer player, PlayerData data, String id, int masterId, boolean trial) {
        MasterEntity master = nearbyMaster(player, masterId);
        Lessons.Lesson lesson = Lessons.get(id);
        if (master == null || lesson == null || !lesson.mentors().contains(master.kind())) {
            return;
        }
        if (trial) {
            TrialManager.start(player, data, lesson, master.kind());
        } else {
            Lessons.buy(player, data, master, lesson);
        }
    }
}
