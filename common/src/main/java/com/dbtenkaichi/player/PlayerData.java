package com.dbtenkaichi.player;

import com.dbtenkaichi.race.Race;
import com.dbtenkaichi.stats.StatBlock;
import com.dbtenkaichi.stats.StatType;
import com.mojang.serialization.Codec;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class PlayerData {
    public static final int SCHEMA = 2;
    public static final int SKILL_SLOTS = 4;
    public static final String BASE_FORM = "dbtenkaichi:base";

    public static final Codec<PlayerData> CODEC = CompoundTag.CODEC.xmap(PlayerData::fromTag, PlayerData::save);

    private boolean created;
    private boolean outfitGiven;
    private Race race = Race.HUMAN;
    private Appearance appearance = Appearance.defaultsFor(Race.HUMAN);
    private StatBlock stats = new StatBlock();
    private long trainingPoints;
    private long lifetimeTrainingPoints;
    private double ki = -1.0;
    private double stamina = -1.0;
    private double fatigue;
    private String form = BASE_FORM;
    private final Map<String, Float> formMastery = new HashMap<>();
    private final Set<String> unlockedForms = new LinkedHashSet<>();
    private final Set<String> unequippedForms = new LinkedHashSet<>();
    private final Set<String> skills = new LinkedHashSet<>();
    private final Map<String, Float> skillMastery = new HashMap<>();
    private final List<String> skillSlots = new ArrayList<>(List.of("", "", "", ""));
    private net.minecraft.nbt.CompoundTag returnPoint;
    private final Map<String, Integer> dailyTraining = new HashMap<>();
    private long trainingDay = -1L;
    private long zenkaiReadyAt;
    private float suppression = 1.0f;
    private int zenkaiCount;
    private final Set<String> seenCinematics = new LinkedHashSet<>();
    private final RuntimeState runtime = new RuntimeState();
    private final float[] practice = new float[StatType.COUNT];
    private int trainingSessions;

    public RuntimeState runtime() {
        return runtime;
    }

    public boolean isCreated() {
        return created;
    }

    public void setCreated(boolean created) {
        this.created = created;
    }

    public Race race() {
        return race;
    }

    public void setRace(Race race) {
        this.race = race;
    }

    public Appearance appearance() {
        return appearance;
    }

    public void setAppearance(Appearance appearance) {
        this.appearance = appearance;
    }

    public StatBlock stats() {
        return stats;
    }

    public void setStats(StatBlock stats) {
        this.stats = stats;
    }

    public int stat(StatType type) {
        return stats.get(type);
    }

    public long trainingPoints() {
        return trainingPoints;
    }

    public void setTrainingPoints(long trainingPoints) {
        this.trainingPoints = Math.max(0L, trainingPoints);
    }

    public void addTrainingPoints(long amount) {
        if (amount <= 0) {
            return;
        }
        this.trainingPoints += amount;
        this.lifetimeTrainingPoints += amount;
    }

    public long lifetimeTrainingPoints() {
        return lifetimeTrainingPoints;
    }

    public double ki() {
        return ki;
    }

    public void setKi(double ki) {
        this.ki = ki;
    }

    public double stamina() {
        return stamina;
    }

    public void setStamina(double stamina) {
        this.stamina = stamina;
    }

    public double fatigue() {
        return fatigue;
    }

    public void setFatigue(double fatigue) {
        this.fatigue = Math.max(0.0, Math.min(1.0, fatigue));
    }

    public String form() {
        return form;
    }

    public void setForm(String form) {
        this.form = form == null || form.isEmpty() ? BASE_FORM : form;
    }

    public boolean isTransformed() {
        return !BASE_FORM.equals(form);
    }

    public float mastery(String formId) {
        return formMastery.getOrDefault(formId, 0.0f);
    }

    public void setMastery(String formId, float value) {
        formMastery.put(formId, Math.max(0.0f, Math.min(1.0f, value)));
    }

    public Map<String, Float> masteryView() {
        return Map.copyOf(formMastery);
    }

    public boolean hasUnlockedForm(String formId) {
        return unlockedForms.contains(formId);
    }

    public void unlockForm(String formId) {
        unlockedForms.add(formId);
    }

    public Set<String> unlockedForms() {
        return Set.copyOf(unlockedForms);
    }

    public boolean isFormEquipped(String formId) {
        return !unequippedForms.contains(formId);
    }

    public void setFormEquipped(String formId, boolean equipped) {
        if (equipped) {
            unequippedForms.remove(formId);
        } else {
            unequippedForms.add(formId);
        }
    }

    public boolean hasSkill(String skill) {
        return skills.contains(skill);
    }

    public net.minecraft.nbt.CompoundTag returnPoint() {
        return returnPoint;
    }

    public void setReturnPoint(net.minecraft.nbt.CompoundTag tag) {
        returnPoint = tag == null ? null : tag.copy();
    }

    public boolean learnSkill(String skill) {
        boolean added = skills.add(skill);
        if (added && com.dbtenkaichi.ki.Techniques.get(skill) != null) {
            for (int i = 0; i < SKILL_SLOTS; i++) {
                if (skillSlots.get(i).isEmpty()) {
                    skillSlots.set(i, skill);
                    break;
                }
            }
        }
        return added;
    }

    public Set<String> skills() {
        return Set.copyOf(skills);
    }

    public float skillMastery(String skill) {
        return skillMastery.getOrDefault(skill, 0.0f);
    }

    public void setSkillMastery(String skill, float value) {
        skillMastery.put(skill, Math.max(0.0f, Math.min(1.0f, value)));
    }

    public String skillInSlot(int slot) {
        return slot >= 0 && slot < SKILL_SLOTS ? skillSlots.get(slot) : "";
    }

    public void setSkillSlot(int slot, String skill) {
        if (slot >= 0 && slot < SKILL_SLOTS) {
            skillSlots.set(slot, skill == null ? "" : skill);
        }
    }

    public int trainingRepetitions(String activity, long day) {
        rollDay(day);
        return dailyTraining.getOrDefault(activity, 0);
    }

    public void recordTraining(String activity, long day) {
        rollDay(day);
        dailyTraining.merge(activity, 1, Integer::sum);
    }

    private void rollDay(long day) {
        if (day != trainingDay) {
            trainingDay = day;
            dailyTraining.clear();
        }
    }

    public long zenkaiReadyAt() {
        return zenkaiReadyAt;
    }

    public void setZenkaiReadyAt(long tick) {
        this.zenkaiReadyAt = tick;
    }

    public int zenkaiCount() {
        return zenkaiCount;
    }

    public void incrementZenkai() {
        zenkaiCount++;
    }

    public float suppression() {
        return suppression;
    }

    public void setSuppression(float suppression) {
        this.suppression = Math.max(0.05f, Math.min(1.0f, suppression));
    }

    public float practice(StatType type) {
        return practice[type.ordinal()];
    }

    public void setPractice(StatType type, float value) {
        practice[type.ordinal()] = Math.max(0.0f, value);
    }

    public int trainingSessions() {
        return trainingSessions;
    }

    public void incrementTrainingSessions() {
        trainingSessions++;
    }

    public boolean hasSeenCinematic(String id) {
        return seenCinematics.contains(id);
    }

    public void markCinematicSeen(String id) {
        seenCinematics.add(id);
    }

    public boolean outfitGiven() {
        return outfitGiven;
    }

    public void setOutfitGiven(boolean given) {
        outfitGiven = given;
    }

    public void resetCharacter() {
        PlayerData fresh = new PlayerData();
        load(fresh.save());
    }

    public void copyFrom(PlayerData other) {
        load(other.save());
    }

    public CompoundTag save() {
        CompoundTag tag = new CompoundTag();
        tag.putInt("schema", SCHEMA);
        tag.putBoolean("created", created);
        tag.putBoolean("outfit_given", outfitGiven);
        tag.putString("race", race.id());
        tag.put("appearance", appearance.save());
        tag.putIntArray("stats", stats.toArray());
        tag.putLong("tp", trainingPoints);
        tag.putLong("tp_total", lifetimeTrainingPoints);
        tag.putDouble("ki", ki);
        tag.putDouble("stamina", stamina);
        tag.putDouble("fatigue", fatigue);
        tag.putString("form", form);
        tag.put("form_mastery", floatMap(formMastery));
        tag.put("forms", stringList(unlockedForms));
        tag.put("unequipped_forms", stringList(unequippedForms));
        tag.put("skills", stringList(skills));
        tag.put("skill_mastery", floatMap(skillMastery));
        tag.put("slots", stringList(skillSlots));
        CompoundTag daily = new CompoundTag();
        dailyTraining.forEach(daily::putInt);
        tag.put("daily", daily);
        tag.putLong("training_day", trainingDay);
        tag.putLong("zenkai_ready", zenkaiReadyAt);
        tag.putInt("zenkai_count", zenkaiCount);
        tag.putFloat("suppression", suppression);
        tag.put("cinematics", stringList(seenCinematics));
        CompoundTag practiceTag = new CompoundTag();
        for (StatType type : StatType.all()) {
            if (practice[type.ordinal()] > 0) {
                practiceTag.putFloat(type.id(), practice[type.ordinal()]);
            }
        }
        tag.put("practice", practiceTag);
        tag.putInt("sessions", trainingSessions);
        if (returnPoint != null) {
            tag.put("return_point", returnPoint.copy());
        }
        return tag;
    }

    public void load(CompoundTag raw) {
        CompoundTag tag = PlayerDataMigrations.migrate(raw);
        created = tag.getBooleanOr("created", false);
        outfitGiven = tag.getBooleanOr("outfit_given", false);
        Race loadedRace = Race.byId(tag.getStringOr("race", Race.HUMAN.id()));
        race = loadedRace == null ? Race.HUMAN : loadedRace;
        appearance = Appearance.defaultsFor(race);
        appearance.load(tag.getCompoundOrEmpty("appearance"));
        stats = new StatBlock(tag.getIntArray("stats").orElse(new int[0]));
        trainingPoints = Math.max(0L, tag.getLongOr("tp", 0L));
        lifetimeTrainingPoints = Math.max(trainingPoints, tag.getLongOr("tp_total", 0L));
        ki = tag.getDoubleOr("ki", -1.0);
        stamina = tag.getDoubleOr("stamina", -1.0);
        setFatigue(tag.getDoubleOr("fatigue", 0.0));
        setForm(tag.getStringOr("form", BASE_FORM));
        formMastery.clear();
        readFloatMap(tag.getCompoundOrEmpty("form_mastery"), formMastery);
        unlockedForms.clear();
        unlockedForms.addAll(readStringList(tag.getListOrEmpty("forms")));
        unequippedForms.clear();
        unequippedForms.addAll(readStringList(tag.getListOrEmpty("unequipped_forms")));
        skills.clear();
        skills.addAll(readStringList(tag.getListOrEmpty("skills")));
        skillMastery.clear();
        readFloatMap(tag.getCompoundOrEmpty("skill_mastery"), skillMastery);
        returnPoint = tag.getCompound("return_point").map(net.minecraft.nbt.CompoundTag::copy).orElse(null);
        List<String> slots = readStringList(tag.getListOrEmpty("slots"));
        for (int i = 0; i < SKILL_SLOTS; i++) {
            skillSlots.set(i, i < slots.size() ? slots.get(i) : "");
        }
        dailyTraining.clear();
        CompoundTag daily = tag.getCompoundOrEmpty("daily");
        for (String key : daily.keySet()) {
            dailyTraining.put(key, daily.getIntOr(key, 0));
        }
        trainingDay = tag.getLongOr("training_day", -1L);
        zenkaiReadyAt = tag.getLongOr("zenkai_ready", 0L);
        zenkaiCount = tag.getIntOr("zenkai_count", 0);
        setSuppression(tag.getFloatOr("suppression", 1.0f));
        seenCinematics.clear();
        seenCinematics.addAll(readStringList(tag.getListOrEmpty("cinematics")));
        CompoundTag practiceTag = tag.getCompoundOrEmpty("practice");
        for (StatType type : StatType.all()) {
            practice[type.ordinal()] = Math.max(0.0f, practiceTag.getFloatOr(type.id(), 0.0f));
        }
        trainingSessions = tag.getIntOr("sessions", 0);
    }

    public static PlayerData fromTag(CompoundTag tag) {
        PlayerData data = new PlayerData();
        data.load(tag);
        return data;
    }

    private static CompoundTag floatMap(Map<String, Float> map) {
        CompoundTag tag = new CompoundTag();
        map.forEach(tag::putFloat);
        return tag;
    }

    private static void readFloatMap(CompoundTag tag, Map<String, Float> out) {
        for (String key : tag.keySet()) {
            out.put(key, tag.getFloatOr(key, 0.0f));
        }
    }

    private static ListTag stringList(Iterable<String> values) {
        ListTag list = new ListTag();
        for (String value : values) {
            list.add(StringTag.valueOf(value));
        }
        return list;
    }

    private static List<String> readStringList(ListTag list) {
        List<String> out = new ArrayList<>();
        for (Tag element : list) {
            element.asString().ifPresent(out::add);
        }
        return out;
    }
}
