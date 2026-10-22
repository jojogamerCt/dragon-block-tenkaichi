package com.dbtenkaichi.server;

import com.dbtenkaichi.Tenkaichi;
import com.dbtenkaichi.form.FormDefinition;
import com.dbtenkaichi.form.FormRegistry;
import com.dbtenkaichi.form.TransformationSystem;
import com.dbtenkaichi.player.Appearance;
import com.dbtenkaichi.player.PlayerData;
import com.dbtenkaichi.race.Race;
import com.dbtenkaichi.registry.ModSounds;
import com.dbtenkaichi.training.Arena;
import com.dbtenkaichi.training.TrialManager;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Blocks;

import java.util.ArrayList;
import java.util.List;

public final class SmokeTest {
    private static final boolean ENABLED = Boolean.getBoolean("dbtenkaichi.smokeTest");
    private static final List<String> FAILURES = new ArrayList<>();
    private static final String EVIL = "dbtenkaichi:evil_buu";
    private static final String SUPER = "dbtenkaichi:super_buu";
    private static final String KID = "dbtenkaichi:kid_buu";
    private static int tick = -40;
    private static boolean done;

    private SmokeTest() {
    }

    public static void tick(MinecraftServer server) {
        if (!ENABLED || done) {
            return;
        }
        tick++;
        if (tick < 0) {
            return;
        }
        try {
            if (tick == 0) {
                formChecks();
                chainChecks();
                appearanceChecks();
                soundChecks();
            } else if (tick == 5) {
                arenaChecks(server);
            } else if (tick == 40) {
                finish(server);
            }
        } catch (RuntimeException e) {
            FAILURES.add("exception at tick " + tick + ": " + e);
            Tenkaichi.LOG.error("[DBT-SMOKE] exception", e);
            finish(server);
        }
    }

    private static void check(boolean condition, String message) {
        if (condition) {
            Tenkaichi.LOG.info("[DBT-SMOKE] PASS {}", message);
        } else {
            FAILURES.add(message);
            Tenkaichi.LOG.error("[DBT-SMOKE] FAIL {}", message);
        }
    }

    private static void formChecks() {
        String[][] expected = {{EVIL, "evil", "majin_fission", PlayerData.BASE_FORM}, {SUPER, "super", "majin_absorption", EVIL}, {KID, "kid", "majin_regression", SUPER}};
        for (String[] e : expected) {
            FormDefinition form = FormRegistry.get(e[0], false);
            check(form != null, e[0] + " loaded from the datapack");
            if (form == null) {
                continue;
            }
            check(form.races.size() == 1 && form.allows(Race.MAJIN) && !form.allows(Race.SAIYAN), e[0] + " is Majin-only");
            check(e[1].equals(form.visual.body) && e[2].equals(form.cinematic) && e[3].equals(form.parent),
                    e[0] + " has body " + form.visual.body + ", cinematic " + form.cinematic + ", parent " + form.parent);
            check("trial".equals(form.requirements.unlock), e[0] + " unlocks through the mirror trial");
        }
        FormDefinition evil = FormRegistry.get(EVIL, false);
        FormDefinition sup = FormRegistry.get(SUPER, false);
        FormDefinition kid = FormRegistry.get(KID, false);
        if (evil != null && sup != null && kid != null) {
            check(FormDefinition.parseColor(evil.visual.skinColor, -1) == 0xA39FA8, "Evil Buu has grey skin");
            check(sup.visual.modelHeight > 1.2f && kid.visual.modelHeight < 0.8f && evil.visual.modelHeight > 1.0f,
                    "form sizes: evil " + evil.visual.modelHeight + ", super " + sup.visual.modelHeight + ", kid " + kid.visual.modelHeight);
            check(evil.bpMultiplier < sup.bpMultiplier && sup.bpMultiplier < kid.bpMultiplier, "each Buu form is stronger than the last");
        }
    }

    private static void chainChecks() {
        PlayerData majin = new PlayerData();
        majin.setRace(Race.MAJIN);
        FormDefinition next = TransformationSystem.nextEquipped(majin, PlayerData.BASE_FORM);
        check(next != null && EVIL.equals(next.id), "a Majin transforms from base into Evil Buu (got " + (next == null ? null : next.id) + ")");
        majin.unlockForm(EVIL);
        next = TransformationSystem.nextEquipped(majin, EVIL);
        check(next != null && SUPER.equals(next.id), "Evil Buu leads to Super Buu");
        majin.unlockForm(SUPER);
        next = TransformationSystem.nextEquipped(majin, SUPER);
        check(next != null && KID.equals(next.id), "Super Buu leads to Kid Buu");
        check(TransformationSystem.nextEquipped(majin, KID) == null, "Kid Buu is the last Majin form");
        majin.setFormEquipped(SUPER, false);
        majin.unlockForm(KID);
        next = TransformationSystem.nextEquipped(majin, EVIL);
        check(next != null && KID.equals(next.id), "with Super Buu unequipped, Evil Buu goes straight to Kid Buu");
        PlayerData saiyan = new PlayerData();
        saiyan.setRace(Race.SAIYAN);
        next = TransformationSystem.nextEquipped(saiyan, PlayerData.BASE_FORM);
        check(next != null && "dbtenkaichi:super_saiyan".equals(next.id), "Saiyans still transform into Super Saiyan");
        check(TrialManager.arenaFor(EVIL) == Arena.BUU_HOUSE && TrialManager.arenaFor(SUPER) == Arena.TIME_CHAMBER && TrialManager.arenaFor(KID) == Arena.SACRED_WORLD,
                "trial arenas: Buu's house, Time Chamber, Sacred World of the Kai");
    }

    private static void appearanceChecks() {
        Appearance majin = Appearance.defaultsFor(Race.MAJIN);
        check(majin.build == Appearance.BUILD_FAT, "a new Majin is fat by default (build " + majin.build + ")");
        check(Appearance.builds(Race.MAJIN) == 4 && Appearance.builds(Race.HUMAN) == 3, "only Majins can pick the fat build");
        majin.sanitize(Race.MAJIN, 0.8f, 1.2f);
        check(majin.build == Appearance.BUILD_FAT, "sanitizing keeps a Majin fat");
        Appearance copy = new Appearance();
        copy.load(majin.save());
        check(copy.build == Appearance.BUILD_FAT, "the fat build is saved and loaded");
        Appearance human = Appearance.defaultsFor(Race.HUMAN);
        human.build = Appearance.BUILD_FAT;
        human.sanitize(Race.HUMAN, 0.8f, 1.2f);
        check(human.build == Appearance.BODY_BUILDS - 1, "other races cannot keep the fat build (got " + human.build + ")");
    }

    private static void soundChecks() {
        List<String> missing = new ArrayList<>();
        for (String name : ModSounds.NAMES) {
            if (!BuiltInRegistries.SOUND_EVENT.containsKey(Identifier.fromNamespaceAndPath(Tenkaichi.MOD_ID, name))) {
                missing.add(name);
            }
        }
        check(missing.isEmpty() && ModSounds.NAMES.size() == 44, "all " + ModSounds.NAMES.size() + " sound events registered " + missing);
        check(ModSounds.TRANSFORM_STEAM.get() != null && ModSounds.TRANSFORM_LAUGH.get() != null && ModSounds.TRANSFORM_GULP.get() != null
                && ModSounds.TRANSFORM_STRETCH.get() != null, "steam, laugh, gulp and stretch sounds registered");
    }

    private static void arenaChecks(MinecraftServer server) {
        ServerLevel realm = server.getLevel(TrialManager.REALM);
        check(realm != null, "training realm available");
        if (realm == null) {
            return;
        }
        BlockPos house = new BlockPos(-40000, 100, 300000);
        Arena.BUU_HOUSE.build(realm, house);
        BlockPos dome = house.offset(0, 7, Arena.BUU_HOUSE.radius() + 13);
        check(realm.getBlockState(dome).is(Blocks.CONCRETE.pink()), "Buu's house: pink dome built (" + realm.getBlockState(dome) + ")");
        check(realm.getBlockState(house.below()).is(Blocks.GRASS_BLOCK) || realm.getBlockState(house.below()).is(Blocks.COARSE_DIRT)
                || realm.getBlockState(house.below()).is(Blocks.DIRT_PATH), "Buu's house: grass and dirt floor");
        check(realm.getBlockState(house).isAir() && realm.getBlockState(house.above()).isAir(), "Buu's house: fighting area is clear");
        BlockPos kai = new BlockPos(-40000, 100, 340000);
        Arena.SACRED_WORLD.build(realm, kai);
        check(realm.getBlockState(kai.below()).is(Blocks.GRASS_BLOCK), "Sacred World: grass floor");
        check(realm.getBlockState(kai).isAir() && realm.getBlockState(kai.offset(0, 1, -7)).isAir(), "Sacred World: fighting area is clear");
        int water = 0;
        for (int dx = -50; dx <= 50; dx++) {
            for (int dz = 20; dz <= 50; dz++) {
                if (realm.getBlockState(kai.offset(dx, -1, dz)).is(Blocks.WATER)) {
                    water++;
                }
            }
        }
        check(water > 20, "Sacred World: a stream runs through the fields (" + water + " water blocks)");
    }

    private static void finish(MinecraftServer server) {
        done = true;
        if (FAILURES.isEmpty()) {
            Tenkaichi.LOG.info("[DBT-SMOKE] RESULT PASS");
        } else {
            Tenkaichi.LOG.error("[DBT-SMOKE] RESULT FAIL {}", FAILURES);
        }
        server.halt(false);
    }
}
