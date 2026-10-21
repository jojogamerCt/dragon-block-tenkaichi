package com.dbtenkaichi.form;

import com.dbtenkaichi.anim.AnimClip;
import com.dbtenkaichi.combat.CombatFxKind;
import com.dbtenkaichi.combat.CombatSystem;
import com.dbtenkaichi.combat.Fighter;
import com.dbtenkaichi.config.ConfigManager;
import com.dbtenkaichi.network.Packets;
import com.dbtenkaichi.platform.Services;
import com.dbtenkaichi.player.PlayerData;
import com.dbtenkaichi.player.RuntimeState;
import com.dbtenkaichi.registry.ModSounds;
import com.dbtenkaichi.server.PlayerManager;
import com.dbtenkaichi.stats.BattlePower;
import com.dbtenkaichi.stats.StatType;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

import java.util.Map;

public final class TransformationSystem {
    public static final int CINEMATIC_AUTO = 0;
    public static final int CINEMATIC_SKIP = 1;
    public static final int CINEMATIC_SHORT = 2;

    private TransformationSystem() {
    }

    public static @Nullable String missingRequirement(ServerPlayer player, PlayerData data, FormDefinition form) {
        if (!form.allows(data.race())) {
            return "race";
        }
        FormDefinition.Requirements req = form.requirements;
        long baseBp = BattlePower.base(data.stats(), ConfigManager.balance());
        if (baseBp < req.battlePower) {
            return "battle_power";
        }
        for (Map.Entry<String, Integer> entry : req.stats.entrySet()) {
            StatType type = StatType.byId(entry.getKey());
            if (type != null && data.stat(type) < entry.getValue()) {
                return "stat:" + type.id();
            }
        }
        if (req.requiredForm != null && !req.requiredForm.isEmpty() && data.mastery(req.requiredForm) < req.requiredMastery) {
            return "mastery";
        }
        return null;
    }

    public static void transformUp(ServerPlayer player, PlayerData data, int cinematicMode, long now) {
        RuntimeState rt = data.runtime();
        if (rt.busy(now) || rt.activeBeam >= 0 || rt.clashId >= 0) {
            return;
        }
        FormDefinition next = nextEquipped(data, data.form());
        if (next == null) {
            player.sendSystemMessage(Component.translatable("message.dbtenkaichi.no_next_form"));
            return;
        }
        String missing = missingRequirement(player, data, next);
        if (missing != null) {
            player.sendSystemMessage(Component.translatable("message.dbtenkaichi.form_locked", Component.translatable(next.name), Component.translatable("requirement.dbtenkaichi." + missing.replace(':', '.'))));
            return;
        }
        if (!data.hasUnlockedForm(next.id)) {
            String unlock = next.requirements.unlock == null ? "none" : next.requirements.unlock;
            if ("trial".equals(unlock)) {
                if (rt.trialPromptUntil >= now) {
                    rt.trialPromptUntil = 0L;
                    com.dbtenkaichi.training.TrialManager.startForm(player, data, next);
                } else {
                    rt.trialPromptUntil = now + 100L;
                    player.sendSystemMessage(Component.translatable("message.dbtenkaichi.form_trial_ready", Component.translatable(next.name)));
                }
                return;
            }
            if (!"none".equals(unlock)) {
                player.sendSystemMessage(Component.translatable("message.dbtenkaichi.form_hint." + unlock, Component.translatable(next.name)));
                return;
            }
        }
        if (!PlayerManager.spendKi(player, data, next.kiActivationCost)) {
            player.sendSystemMessage(Component.translatable("message.dbtenkaichi.no_ki"));
            return;
        }
        begin(player, data, next, cinematicMode, false, now);
    }

    public static void begin(ServerPlayer player, PlayerData data, FormDefinition form, int cinematicMode, boolean forced, long now) {
        RuntimeState rt = data.runtime();
        boolean firstTime = !data.hasSeenCinematic(form.id);
        int ticks;
        boolean full;
        if (cinematicMode == CINEMATIC_SKIP && !forced) {
            ticks = form.quickTicks;
            full = false;
        } else if (firstTime || forced) {
            ticks = cinematicMode == CINEMATIC_SHORT ? Math.min(form.cinematicTicks, 30) : Math.min(form.cinematicTicks, ConfigManager.gameplay().cinematicTicks);
            full = true;
        } else {
            ticks = cinematicMode == CINEMATIC_SHORT ? Math.min(form.quickTicks + 10, 24) : form.quickTicks;
            full = false;
        }
        ticks = Math.max(1, ticks);
        rt.charging = false;
        rt.guarding = false;
        rt.skillSlot = -1;
        rt.pendingForm = form.id;
        rt.transformingUntil = now + ticks;
        if (full) {
            rt.invulnerableUntil = Math.max(rt.invulnerableUntil, rt.transformingUntil);
        }
        rt.stateDirty = true;
        Services.NETWORK.sendToTrackingAndSelf(player, new Packets.Cinematic(player.getId(), form.id, ticks, full));
        CombatSystem.anim(player, full ? AnimClip.TRANSFORM : AnimClip.POWER_UP, ticks, 0);
        ServerLevel level = player.level();
        if (!full) {
            CombatSystem.playSound(level, player, ModSounds.TRANSFORM.get(), 1.4f, 1.0f);
            CombatSystem.fx(player, CombatFxKind.AURA_BURST, player, player, player.position(), 1.0f);
        }
    }

    public static void tickTransforming(ServerPlayer player, PlayerData data, long now) {
        RuntimeState rt = data.runtime();
        if (rt.pendingForm == null || now < rt.transformingUntil) {
            return;
        }
        FormDefinition form = FormRegistry.get(rt.pendingForm, false);
        rt.pendingForm = null;
        if (form == null) {
            return;
        }
        data.setForm(form.id);
        data.unlockForm(form.id);
        data.markCinematicSeen(form.id);
        if (rt.kaioken > 0 && !form.kaiokenCompatible) {
            rt.kaioken = 0;
        }
        rt.stateDirty = true;
        rt.dataDirty = true;
        PlayerManager.refreshCaches(player, data);
        PlayerManager.applyAttributes(player, data);
        ServerLevel level = player.level();
        CombatSystem.fx(player, CombatFxKind.AURA_BURST, player, player, player.position(), 2.0f);
        CombatSystem.fx(player, CombatFxKind.SHOCKWAVE, player, player, player.position(), 2.0f);
        CombatSystem.playSound(level, player, ModSounds.AURA_BURST.get(), 1.6f, 0.9f);
        for (LivingEntity other : level.getEntitiesOfClass(LivingEntity.class, player.getBoundingBox().inflate(4.0), e -> e != player && e.isAlive())) {
            Vec3 push = other.position().subtract(player.position()).multiply(1, 0, 1);
            if (push.lengthSqr() > 1.0e-4) {
                CombatSystem.knockback(other, push.normalize().scale(0.9).add(0, 0.3, 0));
            }
        }
    }

    public static void transformDown(ServerPlayer player, PlayerData data, long now) {
        RuntimeState rt = data.runtime();
        if (rt.kaioken > 0 && rt.transformingUntil <= now) {
            KaiokenSystem.off(player, data, now, null);
            return;
        }
        if (!data.isTransformed() || rt.transformingUntil > now) {
            return;
        }
        FormDefinition current = FormRegistry.get(data.form(), false);
        String target = equippedAncestor(data, current == null ? PlayerData.BASE_FORM : current.parent);
        data.setForm(target);
        rt.stateDirty = true;
        rt.dataDirty = true;
        PlayerManager.refreshCaches(player, data);
        PlayerManager.applyAttributes(player, data);
        CombatSystem.playSound(player.level(), player, ModSounds.REVERT.get(), 1.0f, 1.0f);
        CombatSystem.anim(player, AnimClip.POWER_UP, 8, 1);
    }

    public static @Nullable FormDefinition nextEquipped(PlayerData data, String from) {
        FormDefinition next = FormRegistry.next(data.race(), from, false);
        int guard = 0;
        while (next != null && data.hasUnlockedForm(next.id) && !data.isFormEquipped(next.id) && guard++ < 32) {
            next = FormRegistry.next(data.race(), next.id, false);
        }
        return next;
    }

    public static String equippedAncestor(PlayerData data, String formId) {
        String target = formId;
        int guard = 0;
        while (!PlayerData.BASE_FORM.equals(target) && !data.isFormEquipped(target) && guard++ < 32) {
            FormDefinition form = FormRegistry.get(target, false);
            target = form == null ? PlayerData.BASE_FORM : form.parent;
        }
        return target;
    }

    public static void toggleEquipped(ServerPlayer player, PlayerData data, int index, long now) {
        java.util.List<FormDefinition> chain = new java.util.ArrayList<>(FormRegistry.chainFor(data.race(), false));
        if (!data.isCreated() || index < 0 || index >= chain.size()) {
            return;
        }
        FormDefinition form = chain.get(index);
        if (!data.hasUnlockedForm(form.id)) {
            return;
        }
        boolean equip = !data.isFormEquipped(form.id);
        data.setFormEquipped(form.id, equip);
        RuntimeState rt = data.runtime();
        rt.dataDirty = true;
        if (!equip && data.isTransformed() && rt.transformingUntil <= now && !data.isFormEquipped(data.form())) {
            data.setForm(equippedAncestor(data, data.form()));
            if (rt.kaioken > 0) {
                FormDefinition current = FormRegistry.get(data.form(), false);
                if (current != null && !current.kaiokenCompatible) {
                    rt.kaioken = 0;
                }
            }
            rt.stateDirty = true;
            PlayerManager.refreshCaches(player, data);
            PlayerManager.applyAttributes(player, data);
            CombatSystem.playSound(player.level(), player, ModSounds.REVERT.get(), 1.0f, 1.0f);
        }
        CombatSystem.playSound(player.level(), player, equip ? ModSounds.UI_CONFIRM.get() : ModSounds.UI_TAB.get(), 0.8f, equip ? 1.1f : 0.9f);
        player.sendSystemMessage(Component.translatable(equip ? "message.dbtenkaichi.form_equipped" : "message.dbtenkaichi.form_unequipped", Component.translatable(form.name)));
    }

    public static void forceRevert(ServerPlayer player, PlayerData data, @Nullable Component reason) {
        data.runtime().kaioken = 0;
        if (!data.isTransformed()) {
            return;
        }
        data.setForm(PlayerData.BASE_FORM);
        data.runtime().pendingForm = null;
        data.runtime().stateDirty = true;
        data.runtime().dataDirty = true;
        PlayerManager.refreshCaches(player, data);
        PlayerManager.applyAttributes(player, data);
        CombatSystem.playSound(player.level(), player, ModSounds.REVERT.get(), 1.0f, 0.8f);
        if (reason != null) {
            player.sendSystemMessage(reason);
        }
    }

    public static boolean tryRageUnlock(ServerPlayer player, PlayerData data, @Nullable LivingEntity attacker, long now) {
        RuntimeState rt = data.runtime();
        if (attacker == null || rt.busy(now) || data.isTransformed()) {
            return false;
        }
        if (player.getHealth() / Math.max(1.0f, player.getMaxHealth()) > 0.3f) {
            return false;
        }
        Fighter self = Fighter.of(player);
        Fighter enemy = Fighter.of(attacker);
        if (enemy.battlePower() < self.battlePower() * 0.8) {
            return false;
        }
        for (FormDefinition form : FormRegistry.chainFor(data.race(), false)) {
            if (!"rage".equals(form.requirements.unlock) || data.hasUnlockedForm(form.id) || !PlayerData.BASE_FORM.equals(form.parent)) {
                continue;
            }
            if (missingRequirement(player, data, form) != null) {
                continue;
            }
            data.unlockForm(form.id);
            data.setKi(Math.max(data.ki(), data.runtime().cachedMaxKi * 0.6));
            player.setHealth(Math.max(player.getHealth(), player.getMaxHealth() * 0.35f));
            player.sendSystemMessage(Component.translatable("message.dbtenkaichi.rage_unlock", Component.translatable(form.name)));
            begin(player, data, form, CINEMATIC_AUTO, true, now);
            return true;
        }
        return false;
    }
}
