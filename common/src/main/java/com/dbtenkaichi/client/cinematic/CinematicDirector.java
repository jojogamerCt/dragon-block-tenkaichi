package com.dbtenkaichi.client.cinematic;

import com.dbtenkaichi.client.ClientState;
import com.dbtenkaichi.client.EntityVisual;
import com.dbtenkaichi.client.anim.PoseApplier;
import com.dbtenkaichi.client.fx.CameraEffects;
import com.dbtenkaichi.client.fx.CombatFxHandler;
import com.dbtenkaichi.form.FormDefinition;
import com.dbtenkaichi.form.FormRegistry;
import com.dbtenkaichi.registry.ModParticles;
import com.dbtenkaichi.registry.ModSounds;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

public final class CinematicDirector {
    private static final Map<Integer, Float> LAST_PROGRESS = new HashMap<>();

    private CinematicDirector() {
    }

    public record CameraPose(Vec3 position, float yaw, float pitch) {
    }

    public static boolean active(EntityVisual visual) {
        return visual != null && visual.cinematicFull && visual.cinematicTicks > 0 && ClientState.clientTicks - visual.cinematicStart < visual.cinematicTicks;
    }

    public static boolean localActive() {
        net.minecraft.client.Minecraft mc = net.minecraft.client.Minecraft.getInstance();
        return mc.player != null && active(ClientState.existing(mc.player.getId()));
    }

    public static float progress(EntityVisual visual, float partial) {
        return Mth.clamp((ClientState.clientTicks - visual.cinematicStart + partial) / Math.max(1, visual.cinematicTicks), 0.0f, 1.0f);
    }

    public static @Nullable FormDefinition targetForm(EntityVisual visual) {
        return FormRegistry.get(visual.cinematicForm, true);
    }

    public static CinematicScript script(EntityVisual visual) {
        FormDefinition form = targetForm(visual);
        return Cinematics.get(form == null ? null : form.cinematic);
    }

    public static boolean showsTargetForm(EntityVisual visual) {
        if (!active(visual)) {
            return false;
        }
        CinematicScript script = script(visual);
        float t = progress(visual, 0.0f);
        if (t >= script.formFrom) {
            return true;
        }
        if (CinematicScript.sample(script.flicker, t) > 0.0f) {
            long tick = ClientState.clientTicks - visual.cinematicStart;
            return tick % 5 < 2;
        }
        return false;
    }

    public static float auraLevel(EntityVisual visual) {
        if (!active(visual)) {
            return Float.NaN;
        }
        return CinematicScript.sample(script(visual).aura, progress(visual, 0.0f));
    }

    public static float darken(EntityVisual visual, float partial) {
        return active(visual) ? CinematicScript.sample(script(visual).darken, progress(visual, partial)) : 0.0f;
    }

    public static float flash(EntityVisual visual, float partial) {
        return active(visual) ? CinematicScript.sample(script(visual).flash, progress(visual, partial)) : 0.0f;
    }

    public static CinematicScript.@Nullable Backdrop backdrop(EntityVisual visual, float partial) {
        return active(visual) ? script(visual).backdropAt(progress(visual, partial)) : null;
    }

    public static float streak(EntityVisual visual, float partial) {
        return active(visual) ? CinematicScript.progressIn(script(visual).streak, progress(visual, partial)) : -1.0f;
    }

    public static long cloudOffset() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) {
            return 0L;
        }
        EntityVisual visual = ClientState.existing(mc.player.getId());
        if (visual == null || !active(visual)) {
            return 0L;
        }
        float t = progress(visual, partial());
        float rush = CinematicScript.sample(script(visual).cloudRush, t);
        if (rush <= 0.0f) {
            return 0L;
        }
        float time = ClientState.clientTicks + partial();
        double swing = Math.sin(time * 0.42) * 7000.0 + Math.sin(time * 1.13) * 2600.0;
        return (long) (swing * rush);
    }

    public static boolean revealed(EntityVisual visual) {
        return active(visual) && progress(visual, 0.0f) >= script(visual).revealAt;
    }

    public static @Nullable CameraPose camera(Entity entity, float partial) {
        EntityVisual visual = ClientState.existing(entity.getId());
        if (!active(visual)) {
            return null;
        }
        CinematicScript script = script(visual);
        float t = progress(visual, partial);
        CinematicScript.Shot shot = script.shotAt(t);
        if (shot == null) {
            return null;
        }
        float k = shot.to() <= shot.from() ? 0.0f : (t - shot.from()) / (shot.to() - shot.from());
        if (shot.ease()) {
            k = k * k * (3.0f - 2.0f * k);
        }
        CinematicScript.Camera a = shot.start();
        CinematicScript.Camera b = shot.end();
        float yawOffset = Mth.lerp(k, a.yaw(), b.yaw());
        float pitch = Mth.lerp(k, a.pitch(), b.pitch());
        float distance = Mth.lerp(k, a.distance(), b.distance());
        float height = Mth.lerp(k, a.height(), b.height());
        float bodyYaw = entity instanceof LivingEntity living ? Mth.rotLerp(partial, living.yBodyRotO, living.yBodyRot) : entity.getYRot(partial);
        Vec3 base = entity.getPosition(partial);
        Vec3 target = base.add(0, entity.getBbHeight() * height, 0);
        float yaw = bodyYaw + yawOffset;
        double rad = Math.toRadians(yaw);
        double pitchRad = Math.toRadians(pitch);
        Vec3 forward = new Vec3(-Math.sin(rad), 0, Math.cos(rad));
        Vec3 desired = target.add(forward.scale(distance * Math.cos(pitchRad))).add(0, distance * Math.sin(pitchRad), 0);
        HitResult hit = entity.level().clip(new ClipContext(target, desired, ClipContext.Block.VISUAL, ClipContext.Fluid.NONE, entity));
        Vec3 position = desired;
        if (hit.getType() != HitResult.Type.MISS) {
            Vec3 dir = desired.subtract(target);
            double allowed = Math.max(0.6, hit.getLocation().distanceTo(target) - 0.3);
            position = target.add(dir.normalize().scale(allowed));
        }
        return new CameraPose(position, yaw + 180.0f, pitch);
    }

    public static void tick(Minecraft mc) {
        ClientLevel level = mc.level;
        if (level == null || mc.player == null) {
            LAST_PROGRESS.clear();
            return;
        }
        Iterator<Map.Entry<Integer, Float>> it = LAST_PROGRESS.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<Integer, Float> entry = it.next();
            EntityVisual visual = ClientState.existing(entry.getKey());
            if (!active(visual)) {
                it.remove();
            }
        }
        for (Entity entity : level.entitiesForRendering()) {
            EntityVisual visual = ClientState.existing(entity.getId());
            if (!active(visual) || entity.distanceToSqr(mc.player) > 96 * 96) {
                continue;
            }
            CinematicScript script = script(visual);
            float now = progress(visual, 0.0f);
            float before = LAST_PROGRESS.getOrDefault(entity.getId(), -1.0f);
            LAST_PROGRESS.put(entity.getId(), now);
            for (CinematicScript.Event event : script.events) {
                if (event.at() > before && event.at() <= now || before < 0.0f && event.at() <= 0.0f) {
                    fire(level, entity, visual, script, event);
                }
            }
            float auraNow = CinematicScript.sample(script.aura, now);
            if (showsTargetForm(visual) && now >= script.formFrom && auraNow > 1.0f) {
                auraColumn(entity, formColor(visual), auraNow);
            }
            float rocks = CinematicScript.sample(script.rocks, now);
            if (rocks > 0.0f) {
                spawnRocks(level, entity, rocks);
            }
            if (entity == mc.player) {
                float shake = CinematicScript.sample(script.shake, now);
                if (shake > 0.0f) {
                    CameraEffects.addShake(shake);
                }
                float rush = CinematicScript.sample(script.cloudRush, now);
                if (rush > 0.0f) {
                    skyRush(level, entity, rush);
                }
            }
        }
    }

    private static int formColor(EntityVisual visual) {
        FormDefinition form = targetForm(visual);
        return form == null ? visual.appearance.auraColor : FormDefinition.parseColor(form.visual.auraColor, visual.appearance.auraColor);
    }

    private static void fire(ClientLevel level, Entity entity, EntityVisual visual, CinematicScript script, CinematicScript.Event event) {
        RandomSource random = level.getRandom();
        Vec3 pos = entity.position();
        switch (event.kind()) {
            case LIGHTNING -> {
                double a = random.nextDouble() * Math.PI * 2;
                double r = 3.0 + random.nextDouble() * 5.0 * event.strength();
                lightningBolt(level, pos.add(Math.cos(a) * r, 0, Math.sin(a) * r), script.lightningColor, event.strength());
            }
            case GROUND_BREAK -> {
                CombatFxHandler.groundBreak(level, pos, event.strength());
                for (int i = 0; i < 14 * event.strength(); i++) {
                    double a = random.nextDouble() * Math.PI * 2;
                    double r = 1.0 + random.nextDouble() * 4.0;
                    rock(level, pos.x + Math.cos(a) * r, pos.y, pos.z + Math.sin(a) * r, 0.06 + random.nextDouble() * 0.12);
                }
            }
            case BURST -> CombatFxHandler.transformBurst(level, entity, formColor(visual), event.strength());
            case SOUND_THUNDER -> sound(level, entity, ModSounds.TRANSFORM_THUNDER.get(), 2.0f * event.strength(), 0.85f + random.nextFloat() * 0.3f);
            case SOUND_RUMBLE -> sound(level, entity, ModSounds.TRANSFORM_RUMBLE.get(), 1.4f * event.strength(), 1.0f);
            case SOUND_SCREAM -> sound(level, entity, ModSounds.TRANSFORM_SCREAM.get(), 1.6f * event.strength(), 1.0f);
            case SOUND_BURST -> sound(level, entity, ModSounds.AURA_BURST.get(), 2.0f * event.strength(), 0.85f);
            case SOUND_POWER -> sound(level, entity, ModSounds.TRANSFORM.get(), 1.4f * event.strength(), 1.0f);
        }
    }

    private static void sound(ClientLevel level, Entity entity, SoundEvent sound, float volume, float pitch) {
        level.playLocalSound(entity.getX(), entity.getY() + 1.0, entity.getZ(), sound, SoundSource.PLAYERS, volume, pitch, false);
    }

    private static double groundY(ClientLevel level, double x, double y, double z) {
        BlockPos.MutableBlockPos cursor = BlockPos.containing(x, y + 2, z).mutable();
        for (int i = 0; i < 12; i++) {
            if (!level.getBlockState(cursor).isAir()) {
                return cursor.getY() + 1.0;
            }
            cursor.move(0, -1, 0);
        }
        return y;
    }

    private static void skyRush(ClientLevel level, Entity entity, float rush) {
        RandomSource random = level.getRandom();
        double direction = Math.signum(Math.sin((ClientState.clientTicks) * 0.42)) * 1.6 * rush;
        int count = Math.max(1, Math.round(4 * rush));
        for (int i = 0; i < count; i++) {
            double angle = random.nextDouble() * Math.PI * 2;
            double radius = 8 + random.nextDouble() * 26;
            double x = entity.getX() + Math.cos(angle) * radius;
            double z = entity.getZ() + Math.sin(angle) * radius;
            double y = entity.getY() + 14 + random.nextDouble() * 14;
            int shade = 0xE8ECF2 - (random.nextInt(3) * 0x0A0A0A);
            CombatFxHandler.spawn(ModParticles.DUST_PUFF, shade, x, y, z, direction, 0.0, direction * 0.35, 9.0f + random.nextFloat() * 6.0f);
        }
    }

    public static void lightningBolt(ClientLevel level, Vec3 ground, int color, float strength) {
        RandomSource random = level.getRandom();
        double gy = groundY(level, ground.x, ground.y, ground.z);
        double x = ground.x;
        double z = ground.z;
        double top = gy + 24.0;
        Vec3 last = new Vec3(x, top, z);
        for (double y = top; y > gy; y -= 1.1) {
            x += (random.nextDouble() - 0.5) * 0.9;
            z += (random.nextDouble() - 0.5) * 0.9;
            Vec3 point = new Vec3(x, y, z);
            Vec3 mid = last.add(point).scale(0.5);
            CombatFxHandler.spawn(ModParticles.LIGHTNING, color, mid.x, mid.y, mid.z, 0, 0, 0, 4.0f * strength);
            CombatFxHandler.spawn(ModParticles.LIGHTNING, 0xFFFFFF, mid.x, mid.y, mid.z, 0, 0, 0, 2.4f * strength);
            CombatFxHandler.spawn(ModParticles.ENERGY_ORB, color, mid.x, mid.y, mid.z, 0, 0, 0, 2.2f * strength);
            last = point;
        }
        Vec3 impact = new Vec3(x, gy + 0.2, z);
        CombatFxHandler.spawn(ModParticles.IMPACT_STAR, 0xFFFFFF, impact.x, impact.y + 0.5, impact.z, 0, 0, 0, 4.0f * strength);
        CombatFxHandler.spawn(ModParticles.SHOCK_RING, color, impact.x, impact.y + 0.4, impact.z, 0, 0, 0, 4.0f * strength);
        CombatFxHandler.groundBreak(level, impact, 0.8f * strength);
        CameraEffects.addShake(0.8f * strength);
    }

    private static void auraColumn(Entity entity, int color, float level) {
        RandomSource random = entity.level().getRandom();
        int n = Math.max(2, Math.round(8 * level * com.dbtenkaichi.config.ConfigManager.client().quality().particleBudget));
        for (int i = 0; i < n; i++) {
            double a = random.nextDouble() * Math.PI * 2;
            double r = 0.4 + random.nextDouble() * 0.9;
            double y = entity.getY() + random.nextDouble() * 1.5;
            CombatFxHandler.spawn(ModParticles.AURA_FLAME, random.nextFloat() < 0.3f ? 0xFFFFFF : color, entity.getX() + Math.cos(a) * r, y, entity.getZ() + Math.sin(a) * r,
                    0, 0.35 + random.nextDouble() * 0.3, 0, 2.2f + random.nextFloat());
        }
        for (int i = 0; i < 2; i++) {
            double y = entity.getY() + random.nextDouble() * 8.0;
            CombatFxHandler.spawn(ModParticles.ENERGY_ORB, color, entity.getX() + (random.nextDouble() - 0.5), y, entity.getZ() + (random.nextDouble() - 0.5), 0, 0.2, 0, 5.0f);
        }
        if (random.nextFloat() < 0.4f) {
            CombatFxHandler.spawn(ModParticles.LIGHTNING, 0xFFF4B0, entity.getX() + (random.nextDouble() - 0.5) * 2, entity.getY() + random.nextDouble() * 2.5,
                    entity.getZ() + (random.nextDouble() - 0.5) * 2, 0, 0, 0, 1.4f);
        }
    }

    private static void spawnRocks(ClientLevel level, Entity entity, float rate) {
        RandomSource random = level.getRandom();
        int n = Math.round(rate * 1.5f * com.dbtenkaichi.config.ConfigManager.client().quality().particleBudget);
        for (int i = 0; i < Math.max(1, n); i++) {
            if (random.nextFloat() > rate) {
                continue;
            }
            double a = random.nextDouble() * Math.PI * 2;
            double r = 1.2 + random.nextDouble() * 5.0;
            rock(level, entity.getX() + Math.cos(a) * r, entity.getY(), entity.getZ() + Math.sin(a) * r, 0.02 + random.nextDouble() * 0.04);
        }
    }

    private static void rock(ClientLevel level, double x, double y, double z, double lift) {
        double gy = groundY(level, x, y, z);
        BlockState state = level.getBlockState(BlockPos.containing(x, gy - 0.5, z));
        int color = state.isAir() ? 0x8A7A66 : state.getMapColor(level, BlockPos.containing(x, gy - 0.5, z)).col;
        if (color == 0) {
            color = 0x8A7A66;
        }
        color = EntityVisual.mix(color, 0x7A6A58, 0.55f);
        RandomSource random = level.getRandom();
        CombatFxHandler.spawn(ModParticles.ROCK, color, x, gy + 0.05, z, (random.nextDouble() - 0.5) * 0.02, lift, (random.nextDouble() - 0.5) * 0.02, 0.6f + random.nextFloat() * 0.8f);
    }

    public static float partial() {
        return PoseApplier.partial();
    }
}
