package com.dbtenkaichi.entity;

import com.dbtenkaichi.anim.AnimClip;
import com.dbtenkaichi.combat.CombatSystem;
import com.dbtenkaichi.entity.ai.FighterMovement;
import com.dbtenkaichi.ki.Techniques;
import com.dbtenkaichi.network.Packets;
import com.dbtenkaichi.platform.Services;
import com.dbtenkaichi.player.PlayerData;
import com.dbtenkaichi.player.RuntimeState;
import com.dbtenkaichi.race.Race;
import com.dbtenkaichi.registry.ModSounds;
import com.dbtenkaichi.server.PlayerManager;
import com.dbtenkaichi.stats.StatBlock;
import com.dbtenkaichi.stats.StatType;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import com.dbtenkaichi.training.Lessons;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

public class MasterEntity extends FighterNpc {
    private static final EntityDataAccessor<Integer> KIND = SynchedEntityData.defineId(MasterEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> MIRROR = SynchedEntityData.defineId(MasterEntity.class, EntityDataSerializers.INT);
    private @Nullable BlockPos home;
    private net.minecraft.world.phys.@Nullable Vec3 sessionSpot;
    private float sessionYaw;
    private @Nullable String landmark;
    private int landmarkGeneration;
    private boolean trial;
    private int sparringPartner = -1;
    private int drillPartner = -1;
    private long nextDrillShot;
    private int comboHits;

    public MasterEntity(EntityType<? extends MasterEntity> type, Level level) {
        super(type, level);
        StatBlock block = new StatBlock();
        for (StatType type2 : StatType.all()) {
            block.set(type2, 40);
        }
        applyStats(block, Race.HUMAN);
        setPersistenceRequired();
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(KIND, 0);
        builder.define(MIRROR, -1);
    }

    public MasterKind kind() {
        return MasterKind.byOrdinal(entityData.get(KIND));
    }

    public void setKind(MasterKind kind) {
        entityData.set(KIND, kind.ordinal());
        setCustomName(kind == MasterKind.MASTER ? null : Component.translatable(kind.translationKey()));
    }

    public void setHome(@Nullable BlockPos pos) {
        home = pos;
    }

    public void setLandmark(String id, int generation) {
        landmark = id;
        landmarkGeneration = generation;
    }

    public boolean isStaleLandmarkMaster() {
        return !trial && com.dbtenkaichi.world.Landmarks.isStaleMaster(landmark, landmarkGeneration, home);
    }

    public boolean inTrial() {
        return trial;
    }

    public void prepareTrial(ServerPlayer player, PlayerData data) {
        trial = true;
        StatBlock block = new StatBlock();
        for (StatType type : StatType.all()) {
            block.set(type, Math.max(5, (int) Math.round(data.stat(type) * 0.9)));
        }
        applyStats(block, Race.HUMAN);
        lookAtPoint(player.getBoundingBox().getCenter());
    }

    public int mirrorOf() {
        return entityData.get(MIRROR);
    }

    public void prepareMirror(ServerPlayer player, PlayerData data, com.dbtenkaichi.form.FormDefinition form) {
        trial = true;
        entityData.set(MIRROR, player.getId());
        StatBlock block = new StatBlock();
        for (StatType type : StatType.all()) {
            block.set(type, Math.max(5, (int) Math.round(data.stat(type) * 0.6)));
        }
        applyStats(block, data.race());
        setFighterForm(form.id);
        setCustomName(Component.translatable("entity.dbtenkaichi.mirror", player.getName(), Component.translatable(form.name)));
        lookAtPoint(player.getBoundingBox().getCenter());
    }

    public void startTrialFight(ServerPlayer player) {
        trial = true;
        sparringPartner = player.getId();
        comboHits = 0;
        nextDrillShot = level().getGameTime() + 40;
        setTarget(player);
    }

    public void openMenu(ServerPlayer player) {
        PlayerData data = PlayerManager.data(player);
        CompoundTag tag = new CompoundTag();
        tag.putString("mode", "menu");
        tag.putInt("master", getId());
        tag.putInt("kind", kind().ordinal());
        tag.putInt("sessions", data.trainingSessions());
        tag.putFloat("fatigue", (float) data.fatigue());
        tag.putLong("tp", data.trainingPoints());
        ListTag lessons = new ListTag();
        for (Lessons.Lesson lesson : Lessons.forMaster(kind())) {
            CompoundTag entry = new CompoundTag();
            entry.putString("id", lesson.id());
            entry.putInt("state", Lessons.state(player, data, lesson));
            entry.putInt("price", lesson.price());
            entry.putBoolean("trial", lesson.trial());
            entry.putLong("bp", lesson.battlePower());
            lessons.add(entry);
        }
        tag.put("lessons", lessons);
        Services.NETWORK.sendToPlayer(player, new Packets.OpenScreen(PlayerManager.SCREEN_TRAINING, tag));
    }

    @Override
    protected void addAdditionalSaveData(ValueOutput output) {
        super.addAdditionalSaveData(output);
        output.putInt("master_kind", kind().ordinal());
        if (sessionSpot != null) {
            output.putBoolean("session_active", true);
            output.putDouble("session_x", sessionSpot.x);
            output.putDouble("session_y", sessionSpot.y);
            output.putDouble("session_z", sessionSpot.z);
            output.putFloat("session_yaw", sessionYaw);
        }
        if (home != null) {
            output.putIntArray("master_home", new int[]{home.getX(), home.getY(), home.getZ()});
        }
        if (landmark != null) {
            output.putString("master_landmark", landmark);
            output.putInt("master_landmark_generation", landmarkGeneration);
        }
    }

    @Override
    protected void readAdditionalSaveData(ValueInput input) {
        super.readAdditionalSaveData(input);
        MasterKind kind = MasterKind.byOrdinal(input.getIntOr("master_kind", 0));
        entityData.set(KIND, kind.ordinal());
        if (kind != MasterKind.MASTER && getCustomName() == null) {
            setCustomName(Component.translatable(kind.translationKey()));
        }
        sessionSpot = input.getBooleanOr("session_active", false)
                ? new net.minecraft.world.phys.Vec3(input.getDoubleOr("session_x", 0.0), input.getDoubleOr("session_y", 0.0), input.getDoubleOr("session_z", 0.0)) : null;
        sessionYaw = input.getFloatOr("session_yaw", 0.0f);
        home = input.getIntArray("master_home").filter(a -> a.length == 3).map(a -> new BlockPos(a[0], a[1], a[2])).orElse(null);
        landmark = input.getString("master_landmark").orElse(null);
        landmarkGeneration = input.getIntOr("master_landmark_generation", 0);
    }

    @Override
    public boolean shouldBeSaved() {
        return !trial && super.shouldBeSaved();
    }

    public static AttributeSupplier.Builder createAttributes() {
        return createMobAttributes()
                .add(Attributes.MAX_HEALTH, 100.0)
                .add(Attributes.MOVEMENT_SPEED, 0.3)
                .add(Attributes.FOLLOW_RANGE, 24.0)
                .add(Attributes.ATTACK_DAMAGE, 2.0)
                .add(Attributes.KNOCKBACK_RESISTANCE, 0.6);
    }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(0, new FloatGoal(this));
        goalSelector.addGoal(6, new LookAtPlayerGoal(this, Player.class, 10.0f));
        goalSelector.addGoal(7, new RandomLookAroundGoal(this));
    }

    @Override
    protected InteractionResult mobInteract(Player player, InteractionHand hand) {
        if (hand != InteractionHand.MAIN_HAND) {
            return InteractionResult.PASS;
        }
        if (player instanceof ServerPlayer serverPlayer) {
            PlayerData data = PlayerManager.data(serverPlayer);
            if (!data.isCreated()) {
                PlayerManager.openCreator(serverPlayer);
                return InteractionResult.SUCCESS;
            }
            if (trial) {
                return InteractionResult.PASS;
            }
            openMenu(serverPlayer);
        }
        return InteractionResult.SUCCESS;
    }

    private void rememberSpot() {
        if (sessionSpot == null && !trial) {
            sessionSpot = home != null ? net.minecraft.world.phys.Vec3.atBottomCenterOf(home) : position();
            sessionYaw = getYRot();
        }
    }

    public void returnToSpot() {
        net.minecraft.world.phys.Vec3 spot = sessionSpot;
        sessionSpot = null;
        if (spot == null || trial || !(level() instanceof ServerLevel level)) {
            return;
        }
        getNavigation().stop();
        setTarget(null);
        setFighterFlying(false);
        runtime.guarding = false;
        if (spot.distanceToSqr(position()) > 0.25) {
            com.dbtenkaichi.combat.CombatSystem.fx(this, com.dbtenkaichi.combat.CombatFxKind.VANISH, this, this, position().add(0, getBbHeight() * 0.5, 0), 1.0f);
            teleportTo(spot.x, spot.y, spot.z);
            com.dbtenkaichi.combat.CombatSystem.fx(this, com.dbtenkaichi.combat.CombatFxKind.VANISH, this, this, spot.add(0, getBbHeight() * 0.5, 0), 1.0f);
            com.dbtenkaichi.combat.CombatSystem.playSound(level, this, com.dbtenkaichi.registry.ModSounds.VANISH.get(), 0.8f, 1.0f);
        }
        setDeltaMovement(net.minecraft.world.phys.Vec3.ZERO);
        resetFallDistance();
        setYRot(sessionYaw);
        setYHeadRot(sessionYaw);
        setYBodyRot(sessionYaw);
    }

    public void startSparring(ServerPlayer player) {
        rememberSpot();
        sparringPartner = player.getId();
        comboHits = 0;
        setTarget(player);
        player.sendSystemMessage(Component.translatable("message.dbtenkaichi.sparring_start"));
    }

    public void startDrill(ServerPlayer player) {
        rememberSpot();
        drillPartner = player.getId();
        nextDrillShot = level().getGameTime() + 40;
        player.sendSystemMessage(Component.translatable("message.dbtenkaichi.drill_start"));
    }

    public void stopSession(ServerPlayer player) {
        if (sparringPartner == player.getId()) {
            sparringPartner = -1;
            setTarget(null);
        }
        if (drillPartner == player.getId()) {
            drillPartner = -1;
        }
        runtime.guarding = false;
        setFighterFlying(false);
        if (sparringPartner < 0 && drillPartner < 0) {
            returnToSpot();
        }
    }

    private @Nullable ServerPlayer partner(int id) {
        return id >= 0 && level().getEntity(id) instanceof ServerPlayer player && player.isAlive() ? player : null;
    }

    @Override
    public double outgoingDamageScale() {
        return trial ? 0.5 : 0.2;
    }

    @Override
    protected void customServerAiStep(ServerLevel level) {
        super.customServerAiStep(level);
        long now = level.getGameTime();
        if (now % 40 == 0 && isStaleLandmarkMaster()) {
            discard();
            return;
        }
        ServerPlayer sparring = partner(sparringPartner);
        boolean ended = false;
        if (sparringPartner >= 0 && sparring == null) {
            sparringPartner = -1;
            ended = true;
        }
        ServerPlayer drill = partner(drillPartner);
        if (drillPartner >= 0 && drill == null) {
            drillPartner = -1;
            ended = true;
        }
        if ((ended || sessionSpot != null) && sparringPartner < 0 && drillPartner < 0) {
            returnToSpot();
        }
        if (sparring != null) {
            sparTick(level, sparring, now);
        } else if (drill != null) {
            drillTick(level, drill, now);
        } else {
            if (getHealth() < getMaxHealth() && now % 10 == 0) {
                heal(2.0f);
            }
            if (isFighterFlying()) {
                setFighterFlying(false);
            }
            if (home != null && !trial && home.distToCenterSqr(position()) > 36.0 && now % 20 == 0) {
                getNavigation().moveTo(home.getX() + 0.5, home.getY(), home.getZ() + 0.5, 0.8);
            }
        }
    }

    private void sparTick(ServerLevel level, ServerPlayer partner, long now) {
        if (isStunned()) {
            return;
        }
        lookAtPoint(partner.getBoundingBox().getCenter());
        double distance = distanceTo(partner);
        boolean fly = !partner.onGround() || Math.abs(partner.getY() - getY()) > 2.0;
        if (fly != isFighterFlying()) {
            setFighterFlying(fly);
        }
        RuntimeState partnerRt = PlayerManager.data(partner).runtime();
        if (trial && distance > 6.0 && now >= nextDrillShot) {
            nextDrillShot = now + 30 + getRandom().nextInt(25);
            KiBlastEntity blast = KiBlastEntity.create(level, this, Techniques.KI_BLAST, 0xBFE6FF, -1, 0.0);
            level.addFreshEntity(blast);
            CombatSystem.anim(this, getRandom().nextBoolean() ? AnimClip.KI_BLAST_LEFT : AnimClip.KI_BLAST_RIGHT, 6, 0);
            CombatSystem.playSound(level, this, ModSounds.KI_BLAST.get(), 0.8f, 1.1f);
        }
        if (distance > 3.0) {
            if (fly) {
                FighterMovement.flyToward(this, partner.getBoundingBox().getCenter(), 0.6, 0.3);
            } else {
                getNavigation().moveTo(partner, 1.1);
            }
            return;
        }
        getNavigation().stop();
        if (partnerRt.lastAttack >= now - 2 && getRandom().nextFloat() < 0.35f) {
            runtime.guarding = true;
            runtime.guardStart = now;
            return;
        }
        runtime.guarding = false;
        if (now >= runtime.nextAttackAllowed && (trial || partner.getHealth() > partner.getMaxHealth() * 0.35f)) {
            if (comboHits >= 3) {
                comboHits = 0;
                runtime.nextAttackAllowed = now + 30;
                CombatSystem.anim(this, AnimClip.POWER_UP, 10, 0);
                return;
            }
            if (CombatSystem.lightAttack(this, now)) {
                comboHits++;
                runtime.nextAttackAllowed += 6;
            }
        }
    }

    private void drillTick(ServerLevel level, ServerPlayer partner, long now) {
        lookAtPoint(partner.getBoundingBox().getCenter());
        double distance = distanceTo(partner);
        if (distance < 6.0) {
            Vec3 away = position().subtract(partner.position()).multiply(1, 0, 1).normalize();
            getNavigation().moveTo(getX() + away.x * 3, getY(), getZ() + away.z * 3, 0.9);
        } else if (distance > 14.0) {
            getNavigation().moveTo(partner, 1.0);
        } else {
            getNavigation().stop();
        }
        if (now >= nextDrillShot) {
            nextDrillShot = now + 16 + getRandom().nextInt(10);
            KiBlastEntity blast = KiBlastEntity.create(level, this, Techniques.KI_BLAST, 0x7CFFB0, -1, 0.0);
            blast.setDeltaMovement(blast.getDeltaMovement().scale(0.45));
            blast.setTraining(true);
            level.addFreshEntity(blast);
            CombatSystem.anim(this, getRandom().nextBoolean() ? AnimClip.KI_BLAST_LEFT : AnimClip.KI_BLAST_RIGHT, 6, 0);
            CombatSystem.playSound(level, this, ModSounds.KI_BLAST.get(), 0.6f, 1.3f);
            PlayerManager.data(partner).runtime().trainingScore++;
        }
    }

    @Override
    public void onFighterHit(LivingEntity attacker, double damage) {
        super.onFighterHit(attacker, damage);
        if (attacker.getId() == sparringPartner && attacker instanceof ServerPlayer player) {
            PlayerManager.data(player).runtime().trainingScore++;
        }
    }

    @Override
    public boolean hurtServer(ServerLevel level, DamageSource source, float damage) {
        if (source.getEntity() == null || source.getEntity().getId() != sparringPartner) {
            return false;
        }
        float capped = Math.min(damage, Math.max(0.0f, getHealth() - 1.0f));
        if (capped <= 0.0f) {
            return false;
        }
        return super.hurtServer(level, source, capped);
    }

    @Override
    public boolean removeWhenFarAway(double distSqr) {
        return false;
    }

    @Override
    public boolean isPushable() {
        return sparringPartner >= 0;
    }
}
