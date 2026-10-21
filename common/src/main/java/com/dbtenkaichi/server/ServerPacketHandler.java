package com.dbtenkaichi.server;

import com.dbtenkaichi.Tenkaichi;
import com.dbtenkaichi.combat.CombatSystem;
import com.dbtenkaichi.config.ConfigManager;
import com.dbtenkaichi.config.GameplayConfig;
import com.dbtenkaichi.form.TransformationSystem;
import com.dbtenkaichi.ki.BeamClashManager;
import com.dbtenkaichi.ki.KiSystem;
import com.dbtenkaichi.ki.Techniques;
import com.dbtenkaichi.movement.FlightSystem;
import com.dbtenkaichi.network.ActionType;
import com.dbtenkaichi.network.Packets;
import com.dbtenkaichi.player.Appearance;
import com.dbtenkaichi.player.PlayerData;
import com.dbtenkaichi.player.RuntimeState;
import com.dbtenkaichi.race.Race;
import com.dbtenkaichi.stats.StatBlock;
import com.dbtenkaichi.stats.StatType;
import com.dbtenkaichi.training.TrainingSystem;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;

public final class ServerPacketHandler {
    private ServerPacketHandler() {
    }

    private static boolean allow(ServerPlayer player, PlayerData data) {
        RuntimeState rt = data.runtime();
        long now = player.level().getGameTime();
        if (now - rt.actionWindow >= 20) {
            rt.actionWindow = now;
            rt.actionsThisSecond = 0;
        }
        rt.actionsThisSecond++;
        if (rt.actionsThisSecond > ConfigManager.gameplay().maxActionsPerSecond) {
            if (rt.actionsThisSecond == ConfigManager.gameplay().maxActionsPerSecond + 1) {
                Tenkaichi.LOG.warn("{} exceeded the Tenkaichi action rate limit", player.getPlainTextName());
            }
            return false;
        }
        return player.isAlive() && !player.isSpectator();
    }

    public static void createCharacter(Packets.CreateCharacter packet, ServerPlayer player) {
        PlayerData data = PlayerManager.data(player);
        if (!allow(player, data)) {
            return;
        }
        GameplayConfig gp = ConfigManager.gameplay();
        if (data.isCreated() && !gp.allowCharacterReset) {
            return;
        }
        Race race = Race.byOrdinal(packet.race());
        if (!race.implemented() || !gp.enabledRaces.contains(race.id())) {
            player.sendSystemMessage(Component.translatable("message.dbtenkaichi.race_unavailable"));
            PlayerManager.openCreator(player);
            return;
        }
        Appearance appearance = new Appearance();
        appearance.presentation = packet.presentation();
        appearance.height = packet.height();
        appearance.build = packet.build();
        appearance.skinColor = packet.skinColor();
        appearance.eyeStyle = packet.eyeStyle();
        appearance.eyeColor = packet.eyeColor();
        appearance.eyebrowStyle = packet.eyebrowStyle();
        appearance.hairStyle = packet.hairStyle();
        appearance.hairColor = packet.hairColor();
        appearance.expression = packet.expression();
        appearance.auraColor = packet.auraColor();
        appearance.outfit = packet.outfit();
        appearance.outfitPrimary = packet.outfitPrimary();
        appearance.outfitSecondary = packet.outfitSecondary();
        appearance.featureColor = packet.featureColor();
        appearance.tail = packet.tail();
        appearance.useOwnSkin = packet.useOwnSkin();
        appearance.sanitize(race, gp.minHeight, gp.maxHeight);

        boolean firstCreation = !data.isCreated();
        if (firstCreation) {
            data.resetCharacter();
            StatBlock stats = new StatBlock();
            for (StatType type : StatType.all()) {
                stats.set(type, 5 + race.startBonus(type));
            }
            data.setStats(stats);
            data.setTrainingPoints(gp.startingTrainingPoints);
            data.learnSkill(Techniques.KI_BLAST.id());
        }
        data.setRace(race);
        data.setAppearance(appearance);
        data.setCreated(true);
        com.dbtenkaichi.item.Outfits.equip(player, appearance);
        data.setOutfitGiven(true);
        PlayerManager.refreshCaches(player, data);
        data.setKi(data.runtime().cachedMaxKi);
        data.setStamina(data.runtime().cachedMaxStamina);
        PlayerManager.applyAttributes(player, data);
        if (firstCreation) {
            player.setHealth(player.getMaxHealth());
        }
        PlayerManager.markAllDirty(data);
        PlayerManager.flush(player, data, player.level().getGameTime());
        player.sendSystemMessage(Component.translatable("message.dbtenkaichi.character_created", Component.translatable(race.translationKey())));
    }

    public static void action(Packets.Action packet, ServerPlayer player) {
        PlayerData data = PlayerManager.data(player);
        if (!allow(player, data)) {
            return;
        }
        ActionType type = ActionType.byOrdinal(packet.action());
        if (type == null) {
            return;
        }
        if (type == ActionType.OPEN_MENU) {
            if (!data.isCreated()) {
                PlayerManager.openCreator(player);
            }
            return;
        }
        if (!data.isCreated()) {
            return;
        }
        RuntimeState rt = data.runtime();
        long now = player.level().getGameTime();
        int arg = packet.arg();
        if (rt.trainingActivity != null && type != ActionType.CHARGE_START && type != ActionType.CHARGE_END
                && type != ActionType.LIGHT_ATTACK && type != ActionType.HEAVY_ATTACK && type != ActionType.GUARD_START && type != ActionType.GUARD_END
                && type != ActionType.DODGE && type != ActionType.DASH && type != ActionType.LOCK_TARGET && type != ActionType.FLIGHT_TOGGLE
                && type != ActionType.FLIGHT_FAST && type != ActionType.LANDED && type != ActionType.COMBAT_STANCE) {
            return;
        }
        switch (type) {
            case LIGHT_ATTACK -> CombatSystem.lightAttack(player, now);
            case HEAVY_ATTACK -> CombatSystem.heavyAttack(player, Math.max(0, Math.min(40, arg)), now);
            case GUARD_START -> CombatSystem.guardStart(player, now);
            case GUARD_END -> CombatSystem.guardEnd(player);
            case DODGE -> CombatSystem.dodge(player, Math.max(0, Math.min(6, arg)), now);
            case DASH -> CombatSystem.dash(player, Math.max(0, Math.min(6, arg)), now);
            case CHARGE_START -> KiSystem.chargeStart(player, data, now);
            case CHARGE_END -> {
                KiSystem.chargeEnd(player, data);
                TrainingSystem.onChargeEnd(player, data);
            }
            case FLIGHT_TOGGLE -> FlightSystem.toggle(player, data, now);
            case FLIGHT_FAST -> FlightSystem.setFast(player, data, arg != 0);
            case TRANSFORM_UP -> TransformationSystem.transformUp(player, data, Math.max(0, Math.min(2, arg)), now);
            case TRANSFORM_DOWN -> TransformationSystem.transformDown(player, data, now);
            case SKILL_PRESS -> KiSystem.skillPress(player, data, arg, now);
            case SKILL_RELEASE -> KiSystem.skillRelease(player, data, arg, now);
            case LOCK_TARGET -> lockTarget(player, data, arg);
            case CLASH_PULSE -> BeamClashManager.pulse(player, now);
            case UPGRADE_STAT -> {
                StatType stat = StatType.byOrdinal(arg);
                if (stat != null) {
                    PlayerManager.upgradeStat(player, data, stat);
                }
            }
            case COMBAT_STANCE -> {
                rt.combatStance = arg != 0;
                rt.stateDirty = true;
            }
            case LANDED -> FlightSystem.landed(player, data, arg, now);
            case KAIOKEN -> com.dbtenkaichi.form.KaiokenSystem.cycle(player, data, now);
            case BUY_TECHNIQUE -> PlayerManager.buyTechnique(player, data, arg);
            case FORM_TRIAL -> PlayerManager.formTrial(player, data, arg);
            case FORM_EQUIP -> com.dbtenkaichi.form.TransformationSystem.toggleEquipped(player, data, arg, now);
            default -> {
            }
        }
    }

    private static void lockTarget(ServerPlayer player, PlayerData data, int entityId) {
        RuntimeState rt = data.runtime();
        if (entityId < 0) {
            rt.lockedTarget = -1;
            return;
        }
        Entity entity = player.level().getEntity(entityId);
        int max = ConfigManager.gameplay().maxLockOnDistance;
        if (entity instanceof LivingEntity living && living.isAlive() && entity != player && entity.distanceToSqr(player) <= (double) max * max) {
            rt.lockedTarget = entityId;
        } else {
            rt.lockedTarget = -1;
        }
    }

    public static void assignSlot(Packets.AssignSlot packet, ServerPlayer player) {
        PlayerData data = PlayerManager.data(player);
        if (!allow(player, data) || !data.isCreated()) {
            return;
        }
        if (packet.slot() < 0 || packet.slot() >= PlayerData.SKILL_SLOTS) {
            return;
        }
        String skill = packet.skill();
        if (!skill.isEmpty() && !data.hasSkill(skill)) {
            return;
        }
        for (int i = 0; i < PlayerData.SKILL_SLOTS; i++) {
            if (!skill.isEmpty() && data.skillInSlot(i).equals(skill)) {
                data.setSkillSlot(i, "");
            }
        }
        data.setSkillSlot(packet.slot(), skill);
        data.runtime().dataDirty = true;
    }

    public static void trainingInput(Packets.TrainingInput packet, ServerPlayer player) {
        PlayerData data = PlayerManager.data(player);
        if (!allow(player, data) || !data.isCreated()) {
            return;
        }
        TrainingSystem.handleInput(player, data, packet.activity(), packet.phase(), packet.value(), player.level().getGameTime());
    }
}
