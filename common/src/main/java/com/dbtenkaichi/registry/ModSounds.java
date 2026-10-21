package com.dbtenkaichi.registry;

import com.dbtenkaichi.Tenkaichi;
import com.dbtenkaichi.platform.Services;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

public final class ModSounds {
    public static final List<String> NAMES = new ArrayList<>();

    public static final Supplier<SoundEvent> PUNCH_LIGHT = register("combat.punch_light");
    public static final Supplier<SoundEvent> PUNCH_HEAVY = register("combat.punch_heavy");
    public static final Supplier<SoundEvent> KICK = register("combat.kick");
    public static final Supplier<SoundEvent> SWING = register("combat.swing");
    public static final Supplier<SoundEvent> GUARD = register("combat.guard");
    public static final Supplier<SoundEvent> PERFECT_GUARD = register("combat.perfect_guard");
    public static final Supplier<SoundEvent> GUARD_BREAK = register("combat.guard_break");
    public static final Supplier<SoundEvent> MELEE_CLASH = register("combat.clash");
    public static final Supplier<SoundEvent> DODGE = register("combat.dodge");
    public static final Supplier<SoundEvent> DASH = register("combat.dash");
    public static final Supplier<SoundEvent> VANISH = register("combat.vanish");
    public static final Supplier<SoundEvent> KI_CHARGE_START = register("ki.charge_start");
    public static final Supplier<SoundEvent> KI_CHARGE_LOOP = register("ki.charge_loop");
    public static final Supplier<SoundEvent> KI_BLAST = register("ki.blast");
    public static final Supplier<SoundEvent> KI_IMPACT = register("ki.impact");
    public static final Supplier<SoundEvent> BEAM_CHARGE = register("ki.beam_charge");
    public static final Supplier<SoundEvent> BEAM_FIRE = register("ki.beam_fire");
    public static final Supplier<SoundEvent> BEAM_LOOP = register("ki.beam_loop");
    public static final Supplier<SoundEvent> EXPLOSION = register("ki.explosion");
    public static final Supplier<SoundEvent> CLASH_LOOP = register("ki.clash_loop");
    public static final Supplier<SoundEvent> FLIGHT_TAKEOFF = register("flight.takeoff");
    public static final Supplier<SoundEvent> FLIGHT_LOOP = register("flight.loop");
    public static final Supplier<SoundEvent> SONIC_BOOM = register("flight.sonic");
    public static final Supplier<SoundEvent> LAND = register("flight.land");
    public static final Supplier<SoundEvent> AURA_BURST = register("aura.burst");
    public static final Supplier<SoundEvent> AURA_LOOP = register("aura.loop");
    public static final Supplier<SoundEvent> TRANSFORM = register("transform.power_up");
    public static final Supplier<SoundEvent> TRANSFORM_SCREAM = register("transform.scream");
    public static final Supplier<SoundEvent> REVERT = register("transform.revert");
    public static final Supplier<SoundEvent> TRANSFORM_THUNDER = register("transform.thunder");
    public static final Supplier<SoundEvent> TRANSFORM_RUMBLE = register("transform.rumble");
    public static final Supplier<SoundEvent> UI_CLICK = register("ui.click");
    public static final Supplier<SoundEvent> UI_CONFIRM = register("ui.confirm");
    public static final Supplier<SoundEvent> UI_OPEN = register("ui.open");
    public static final Supplier<SoundEvent> UI_TAB = register("ui.tab");
    public static final Supplier<SoundEvent> UI_UPGRADE = register("ui.upgrade");
    public static final Supplier<SoundEvent> UI_DENY = register("ui.deny");
    public static final Supplier<SoundEvent> TRAINING_BEAT = register("training.beat");
    public static final Supplier<SoundEvent> TRAINING_SUCCESS = register("training.success");
    public static final Supplier<SoundEvent> VOICE_GRUNT = register("voice.grunt");

    private ModSounds() {
    }

    private static Supplier<SoundEvent> register(String name) {
        NAMES.add(name);
        Identifier id = Identifier.fromNamespaceAndPath(Tenkaichi.MOD_ID, name);
        return Services.REGISTRY.register(BuiltInRegistries.SOUND_EVENT, name, () -> SoundEvent.createVariableRangeEvent(id));
    }

    public static void init() {
    }
}
