package com.dbtenkaichi.form;

import com.dbtenkaichi.config.BalanceConfig;
import com.dbtenkaichi.player.HairStyle;
import com.dbtenkaichi.race.Race;
import com.dbtenkaichi.stats.BattlePower;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class FormDefinition {
    public String id;
    public String name;
    public List<String> races = new ArrayList<>();
    public String parent = FormRegistry.BASE;
    public int order;
    public Requirements requirements = new Requirements();
    public double bpMultiplier = 1.0;
    public double attackMultiplier;
    public double defenseMultiplier;
    public double kiMultiplier;
    public double speedMultiplier = 1.0;
    public double kiDrainPerSecond;
    public double staminaDrainPerSecond;
    public double healthDrainPerSecond;
    public double masteryPerSecond = 0.001;
    public double masteryDrainReduction = 0.6;
    public double lowMasteryControlPenalty = 0.15;
    public double kiActivationCost = 15.0;
    public int cinematicTicks = 70;
    public String cinematic = "power_up";
    public int quickTicks = 8;
    public boolean kaiokenCompatible;
    public Visual visual = new Visual();

    public boolean allows(Race race) {
        return races == null || races.isEmpty() || races.contains(race.id());
    }

    public double attack(BalanceConfig cfg) {
        return attackMultiplier > 0 ? attackMultiplier : BattlePower.combatMultiplierFromForm(bpMultiplier, cfg);
    }

    public double defense(BalanceConfig cfg) {
        return defenseMultiplier > 0 ? defenseMultiplier : BattlePower.combatMultiplierFromForm(bpMultiplier, cfg) * 0.9;
    }

    public double ki(BalanceConfig cfg) {
        return kiMultiplier > 0 ? kiMultiplier : BattlePower.combatMultiplierFromForm(bpMultiplier, cfg);
    }

    public double drainAt(float mastery) {
        return kiDrainPerSecond * (1.0 - masteryDrainReduction * Math.max(0.0f, Math.min(1.0f, mastery)));
    }

    public double controlFactor(float mastery) {
        return 1.0 - lowMasteryControlPenalty * (1.0 - Math.max(0.0f, Math.min(1.0f, mastery)));
    }

    public HairStyle hairStyle() {
        return visual == null || visual.hairStyle == null ? null : HairStyle.byId(visual.hairStyle);
    }

    public void validate() {
        if (races == null) {
            races = new ArrayList<>();
        }
        if (requirements == null) {
            requirements = new Requirements();
        }
        if (requirements.stats == null) {
            requirements.stats = new LinkedHashMap<>();
        }
        if (visual == null) {
            visual = new Visual();
        }
        if (parent == null || parent.isEmpty()) {
            parent = FormRegistry.BASE;
        }
        bpMultiplier = Math.max(0.01, Math.min(1.0e6, bpMultiplier));
        speedMultiplier = Math.max(0.1, Math.min(10.0, speedMultiplier));
        kiDrainPerSecond = Math.max(0.0, kiDrainPerSecond);
        staminaDrainPerSecond = Math.max(0.0, staminaDrainPerSecond);
        healthDrainPerSecond = Math.max(0.0, Math.min(20.0, healthDrainPerSecond));
        masteryPerSecond = Math.max(0.0, Math.min(1.0, masteryPerSecond));
        masteryDrainReduction = Math.max(0.0, Math.min(1.0, masteryDrainReduction));
        lowMasteryControlPenalty = Math.max(0.0, Math.min(0.9, lowMasteryControlPenalty));
        visual.modelHeight = Math.max(0.3f, Math.min(3.0f, visual.modelHeight));
        visual.modelWidth = Math.max(0.3f, Math.min(3.0f, visual.modelWidth));
        cinematicTicks = Math.max(0, Math.min(400, cinematicTicks));
        quickTicks = Math.max(0, Math.min(60, quickTicks));
    }

    public static final class Requirements {
        public long battlePower;
        public Map<String, Integer> stats = new LinkedHashMap<>();
        public String requiredForm;
        public float requiredMastery;
        public String unlock = "none";
    }

    public static final class Visual {
        public String hairStyle;
        public String hairColor;
        public boolean hairGlow;
        public String eyeColor;
        public String auraColor;
        public String auraSecondary;
        public String auraStyle = "flame";
        public boolean lightning;
        public float auraScale = 1.0f;
        public float auraIntensity = 1.0f;
        public String body;
        public String skinColor;
        public String scleraColor;
        public float modelHeight = 1.0f;
        public float modelWidth = 1.0f;
    }

    public static int parseColor(String value, int fallback) {
        if (value == null || value.isEmpty()) {
            return fallback;
        }
        try {
            String hex = value.startsWith("#") ? value.substring(1) : value;
            return Integer.parseUnsignedInt(hex, 16) & 0xFFFFFF;
        } catch (NumberFormatException e) {
            return fallback;
        }
    }
}
