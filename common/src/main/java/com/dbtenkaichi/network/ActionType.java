package com.dbtenkaichi.network;

public enum ActionType {
    LIGHT_ATTACK,
    HEAVY_ATTACK,
    GUARD_START,
    GUARD_END,
    DODGE,
    DASH,
    CHARGE_START,
    CHARGE_END,
    FLIGHT_TOGGLE,
    FLIGHT_FAST,
    TRANSFORM_UP,
    TRANSFORM_DOWN,
    SKILL_PRESS,
    SKILL_RELEASE,
    LOCK_TARGET,
    CLASH_PULSE,
    UPGRADE_STAT,
    OPEN_MENU,
    COMBAT_STANCE,
    LANDED,
    KAIOKEN,
    BUY_TECHNIQUE,
    FORM_TRIAL,
    FORM_EQUIP;

    private static final ActionType[] VALUES = values();

    public static ActionType byOrdinal(int ordinal) {
        return ordinal >= 0 && ordinal < VALUES.length ? VALUES[ordinal] : null;
    }
}
