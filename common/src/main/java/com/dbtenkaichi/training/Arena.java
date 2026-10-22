package com.dbtenkaichi.training;

import com.dbtenkaichi.world.Landmarks;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;

public enum Arena {
    MOUNTAIN(22),
    KAME_ISLAND(14),
    KORIN_FOREST(18),
    TIME_CHAMBER(26),
    NAMEK(20),
    CELL_RING(15),
    CANYON(18),
    BUU_HOUSE(18),
    SACRED_WORLD(20);

    private final int radius;

    Arena(int radius) {
        this.radius = radius;
    }

    public int radius() {
        return radius;
    }

    public String translationKey() {
        return "arena.dbtenkaichi." + name().toLowerCase(java.util.Locale.ROOT);
    }

    public void build(ServerLevel level, BlockPos center) {
        RandomSource random = RandomSource.create(center.asLong() + ordinal());
        switch (this) {
            case MOUNTAIN -> mountain(level, center, random);
            case KAME_ISLAND -> kameIsland(level, center);
            case KORIN_FOREST -> korinForest(level, center, random);
            case TIME_CHAMBER -> timeChamber(level, center);
            case NAMEK -> namek(level, center, random);
            case CELL_RING -> cellRing(level, center, random);
            case CANYON -> canyon(level, center, random);
            case BUU_HOUSE -> buuHouse(level, center, random);
            case SACRED_WORLD -> sacredWorld(level, center, random);
        }
    }

    private static void floor(ServerLevel level, BlockPos center, int r, Block surface, Block fill, int depth) {
        for (int dx = -r; dx <= r; dx++) {
            for (int dz = -r; dz <= r; dz++) {
                if (dx * dx + dz * dz > r * r + r) {
                    continue;
                }
                BlockPos top = center.offset(dx, -1, dz);
                for (int up = 1; up <= 14; up++) {
                    Structures.set(level, top.above(up), Blocks.AIR);
                }
                Structures.column(level, top, surface, fill, depth);
            }
        }
    }

    private void mountain(ServerLevel level, BlockPos center, RandomSource random) {
        floor(level, center, radius, Blocks.STONE, Blocks.STONE, 4);
        for (int i = 0; i < 90; i++) {
            int dx = random.nextInt(radius * 2 + 1) - radius;
            int dz = random.nextInt(radius * 2 + 1) - radius;
            if (dx * dx + dz * dz <= radius * radius) {
                Structures.set(level, center.offset(dx, -1, dz), random.nextBoolean() ? Blocks.GRAVEL : Blocks.ANDESITE);
            }
        }
        for (int i = 0; i < 6; i++) {
            double a = Math.PI * 2 * i / 6 + 0.3;
            BlockPos base = center.offset((int) Math.round(Math.cos(a) * (radius - 3)), 0, (int) Math.round(Math.sin(a) * (radius - 3)));
            int h = 3 + random.nextInt(5);
            for (int y = 0; y < h; y++) {
                Structures.set(level, base.above(y), ((y + i) & 1) == 0 ? Blocks.STONE : Blocks.ANDESITE);
                Structures.set(level, base.above(y).east(), Blocks.STONE);
            }
        }
    }

    private void kameIsland(ServerLevel level, BlockPos center) {
        int sea = 56;
        for (int dx = -sea; dx <= sea; dx++) {
            for (int dz = -sea; dz <= sea; dz++) {
                double d = Math.sqrt(dx * dx + dz * dz);
                if (d > sea) {
                    continue;
                }
                BlockPos top = center.offset(dx, -1, dz);
                boolean house = Math.abs(dx) <= 10 && dz >= 18 && dz <= 34;
                if (d <= radius + 0.5 || house) {
                    Structures.column(level, top, d <= 6 && !house ? Blocks.GRASS_BLOCK : Blocks.SAND, Blocks.SANDSTONE, 3);
                } else {
                    Structures.set(level, top, Blocks.WATER);
                    Structures.set(level, top.below(), Blocks.WATER);
                    Structures.set(level, top.below(2), Blocks.SAND);
                }
                for (int up = 1; up <= 14; up++) {
                    Structures.set(level, top.above(up), Blocks.AIR);
                }
            }
        }
        Landmarks.kameHouseBuilding(level, center.offset(0, 0, 25));
        Structures.palm(level, center.offset(-10, 0, -6), 7);
        Structures.palm(level, center.offset(11, 0, 3), 6);
    }

    private void korinForest(ServerLevel level, BlockPos center, RandomSource random) {
        floor(level, center, 52, Blocks.GRASS_BLOCK, Blocks.DIRT, 3);
        BlockPos pillar = center.offset(0, 0, -38);
        BlockPos npc = Landmarks.korinTower(level, pillar, center.getY() - 1, 92, false);
        Landmarks.lookout(level, new BlockPos(npc.getX(), Math.min(level.getMaxY() - 18, npc.getY() + 46), npc.getZ() - 6), false);
        for (int i = 0; i < 70; i++) {
            double a = random.nextDouble() * Math.PI * 2;
            double r = radius + 4 + random.nextDouble() * 28;
            BlockPos base = center.offset((int) Math.round(Math.cos(a) * r), 0, (int) Math.round(Math.sin(a) * r));
            if (base.distSqr(pillar) < 64) {
                continue;
            }
            if (random.nextInt(3) == 0) {
                Structures.conifer(level, base);
            } else {
                Structures.tree(level, base);
            }
        }
    }

    private void timeChamber(ServerLevel level, BlockPos center) {
        floor(level, center, 60, Blocks.CONCRETE.white(), Blocks.CONCRETE.white(), 2);
        BlockPos e = center.offset(0, 0, -radius - 8);
        for (int dx = -6; dx <= 6; dx++) {
            for (int dz = -4; dz <= 4; dz++) {
                for (int y = 0; y < 7; y++) {
                    boolean edge = Math.abs(dx) == 6 || Math.abs(dz) == 4 || y == 6;
                    Structures.set(level, e.offset(dx, y, dz), edge ? Blocks.QUARTZ_BLOCK : Blocks.AIR);
                }
            }
        }
        for (int x : new int[]{-5, -2, 2, 5}) {
            for (int y = 0; y < 6; y++) {
                Structures.set(level, e.offset(x, y, 5), Blocks.QUARTZ_PILLAR);
            }
        }
        for (int dx = -6; dx <= 6; dx++) {
            Structures.set(level, e.offset(dx, 6, 5), Blocks.QUARTZ_BLOCK);
        }
        for (int y = 0; y < 3; y++) {
            Structures.set(level, e.offset(0, y, 4), Blocks.AIR);
        }
        for (int side : new int[]{-1, 1}) {
            BlockPos glass = e.offset(side * 9, 0, 4);
            Structures.set(level, glass, Blocks.CONCRETE.yellow());
            Structures.set(level, glass.above(), Blocks.GLASS);
            Structures.set(level, glass.above(2), Blocks.SAND);
            Structures.set(level, glass.above(3), Blocks.GLASS);
            Structures.set(level, glass.above(4), Blocks.CONCRETE.yellow());
        }
    }

    private void namek(ServerLevel level, BlockPos center, RandomSource random) {
        int outer = 58;
        for (int dx = -outer; dx <= outer; dx++) {
            for (int dz = -outer; dz <= outer; dz++) {
                double d = Math.sqrt(dx * dx + dz * dz);
                if (d > outer) {
                    continue;
                }
                BlockPos top = center.offset(dx, -1, dz);
                for (int up = 1; up <= 14; up++) {
                    Structures.set(level, top.above(up), Blocks.AIR);
                }
                double islands = Math.sin(dx * 0.21) * Math.cos(dz * 0.17);
                if (d <= radius + 0.5 || islands > 0.55) {
                    Structures.column(level, top, Blocks.MOSS_BLOCK, Blocks.DIRT, 3);
                } else {
                    Structures.set(level, top, Blocks.WATER);
                    Structures.set(level, top.below(), Blocks.WATER);
                    Structures.set(level, top.below(2), Blocks.SAND);
                }
            }
        }
        for (int i = 0; i < 14; i++) {
            double a = random.nextDouble() * Math.PI * 2;
            double r = radius + 3 + random.nextDouble() * 12;
            ajisa(level, center.offset((int) Math.round(Math.cos(a) * r), 0, (int) Math.round(Math.sin(a) * r)), 5 + random.nextInt(3));
        }
        for (int i = 0; i < 5; i++) {
            double a = Math.PI * 2 * i / 5 + 0.4;
            BlockPos base = center.offset((int) Math.round(Math.cos(a) * 44), -1, (int) Math.round(Math.sin(a) * 44));
            int h = 10 + random.nextInt(10);
            for (int y = 0; y < h; y++) {
                Structures.disc(level, base.above(y), y > h - 4 ? 4.5 : 3.0, y > h - 4 ? Blocks.MOSS_BLOCK : Blocks.CALCITE);
            }
        }
    }

    private static void ajisa(ServerLevel level, BlockPos base, int height) {
        for (int y = 0; y < height; y++) {
            Structures.set(level, base.above(y), Blocks.STRIPPED_BIRCH_LOG);
        }
        BlockPos top = base.above(height);
        for (int dx = -2; dx <= 2; dx++) {
            for (int dy = -1; dy <= 1; dy++) {
                for (int dz = -2; dz <= 2; dz++) {
                    if (dx * dx + dy * dy * 2 + dz * dz <= 5) {
                        Structures.set(level, top.offset(dx, dy, dz), Structures.leaves(Blocks.AZALEA_LEAVES));
                    }
                }
            }
        }
    }

    private void cellRing(ServerLevel level, BlockPos center, RandomSource random) {
        floor(level, center, 54, Blocks.COARSE_DIRT, Blocks.DIRT, 3);
        for (int i = 0; i < 160; i++) {
            int dx = random.nextInt(109) - 54;
            int dz = random.nextInt(109) - 54;
            if (dx * dx + dz * dz < 26 * 26 || dx * dx + dz * dz > 54 * 54) {
                continue;
            }
            Structures.set(level, center.offset(dx, -1, dz), random.nextBoolean() ? Blocks.GRAVEL : Blocks.TERRACOTTA);
        }
        int half = 14;
        for (int dx = -half - 1; dx <= half + 1; dx++) {
            for (int dz = -half - 1; dz <= half + 1; dz++) {
                boolean edge = Math.abs(dx) > half || Math.abs(dz) > half;
                boolean grid = Math.floorMod(dx, 4) == 2 || Math.floorMod(dz, 4) == 2;
                BlockPos top = center.offset(dx, edge ? -2 : -1, dz);
                Structures.set(level, top, edge ? Blocks.SMOOTH_STONE : grid ? Blocks.CONCRETE.lightGray() : Blocks.SMOOTH_QUARTZ);
                Structures.set(level, top.below(), Blocks.STONE_BRICKS);
                Structures.set(level, top.below(2), Blocks.STONE_BRICKS);
                if (edge) {
                    Structures.set(level, top.above(), Blocks.AIR);
                }
            }
        }
        for (int[] c : new int[][]{{-1, -1}, {1, -1}, {-1, 1}, {1, 1}}) {
            BlockPos base = center.offset(c[0] * (half + 3), -1, c[1] * (half + 3));
            for (int y = 0; y < 3; y++) {
                Structures.disc(level, base.above(y), 1.5, Blocks.SMOOTH_QUARTZ);
            }
            Structures.disc(level, base.above(3), 1.0, Blocks.CONCRETE.white());
            for (int y = 4; y < 17; y++) {
                Structures.set(level, base.above(y), Blocks.QUARTZ_PILLAR);
            }
            Structures.set(level, base.above(17), Blocks.END_ROD);
        }
        for (int i = 0; i < 6; i++) {
            double a = Math.PI * 2 * i / 6;
            BlockPos base = center.offset((int) Math.round(Math.cos(a) * 42), -1, (int) Math.round(Math.sin(a) * 42));
            int h = 8 + random.nextInt(8);
            for (int y = 0; y < h; y++) {
                Structures.disc(level, base.above(y), 3.5 - y * 0.12, (y & 3) == 0 ? Blocks.DYED_TERRACOTTA.orange() : Blocks.TERRACOTTA);
            }
        }
    }

    /** Grass and dirt flats with Majin Buu's pink dome house, its head-like holes and the curled antenna on top. */
    private void buuHouse(ServerLevel level, BlockPos center, RandomSource random) {
        floor(level, center, 54, Blocks.GRASS_BLOCK, Blocks.DIRT, 3);
        for (int i = 0; i < 260; i++) {
            int dx = random.nextInt(109) - 54;
            int dz = random.nextInt(109) - 54;
            if (dx * dx + dz * dz > 54 * 54) {
                continue;
            }
            Block patch = random.nextInt(3) == 0 ? Blocks.DIRT_PATH : Blocks.COARSE_DIRT;
            Structures.set(level, center.offset(dx, -1, dz), patch);
            Structures.set(level, center.offset(dx + 1, -1, dz), patch);
        }
        BlockPos house = center.offset(0, 0, radius + 13);
        Structures.disc(level, house.below(), 8.5, Blocks.SMOOTH_STONE);
        Structures.dome(level, house, 7, Blocks.CONCRETE.pink());
        for (int side = -1; side <= 1; side += 2) {
            Structures.dome(level, house.offset(side * 8, 0, 3), 4, Blocks.CONCRETE.pink());
        }
        for (int dx = -1; dx <= 1; dx++) {
            for (int y = 0; y < 3; y++) {
                Structures.set(level, house.offset(dx, y, -7), Blocks.AIR);
                Structures.set(level, house.offset(dx, y, -6), Blocks.AIR);
            }
        }
        for (int side = -1; side <= 1; side += 2) {
            for (int i = 0; i < 3; i++) {
                double angle = Math.toRadians(42 + i * 18) * side;
                int y = 4 + (i == 1 ? 1 : 0);
                double r = Math.sqrt(49 - y * y) - 0.4;
                BlockPos hole = house.offset((int) Math.round(Math.sin(angle) * r), y, (int) Math.round(-Math.cos(angle) * r));
                Structures.set(level, hole, Blocks.CONCRETE.black());
            }
        }
        int[][] antenna = {{0, 7}, {0, 8}, {0, 9}, {0, 10}, {1, 11}, {2, 11}, {3, 10}};
        for (int[] a : antenna) {
            Structures.set(level, house.offset(0, a[1], a[0]), Blocks.CONCRETE.pink());
        }
        for (int i = 0; i < 5; i++) {
            double a = Math.PI * 2 * i / 5 + 1.1;
            BlockPos base = center.offset((int) Math.round(Math.cos(a) * 46), -1, (int) Math.round(Math.sin(a) * 46));
            int h = 6 + random.nextInt(8);
            for (int y = 0; y < h; y++) {
                Structures.disc(level, base.above(y), 3.6 - y * 0.15, (y & 3) == 0 ? Blocks.DYED_TERRACOTTA.brown() : Blocks.TERRACOTTA);
            }
        }
    }

    /** The Supreme Kai's world: rolling green fields, a stream, rocky outcrops and scattered trees under a clear sky. */
    private void sacredWorld(ServerLevel level, BlockPos center, RandomSource random) {
        floor(level, center, 58, Blocks.GRASS_BLOCK, Blocks.DIRT, 3);
        for (int dx = -58; dx <= 58; dx++) {
            for (int dz = -58; dz <= 58; dz++) {
                double d = Math.sqrt(dx * dx + dz * dz);
                if (d > 58 || d < radius + 6) {
                    continue;
                }
                double hill = Math.sin(dx * 0.13 + 1.3) * Math.cos(dz * 0.11) * 3.2 + (d - radius - 6) * 0.12;
                int h = (int) Math.round(Math.max(0, hill));
                for (int y = 0; y < h; y++) {
                    Structures.set(level, center.offset(dx, y, dz), y == h - 1 ? Blocks.GRASS_BLOCK : Blocks.DIRT);
                }
                double stream = Math.abs(dz - 34 - Math.sin(dx * 0.09) * 6);
                if (stream < 1.6 && h == 0) {
                    Structures.set(level, center.offset(dx, -1, dz), Blocks.WATER);
                    Structures.set(level, center.offset(dx, -2, dz), Blocks.SAND);
                }
            }
        }
        for (int i = 0; i < 6; i++) {
            double a = Math.PI * 2 * i / 6 + 0.5;
            BlockPos base = center.offset((int) Math.round(Math.cos(a) * 40), -1, (int) Math.round(Math.sin(a) * 40));
            int h = 5 + random.nextInt(7);
            for (int y = 0; y < h; y++) {
                Structures.disc(level, base.above(y), 2.6 - y * 0.12, (y + i) % 3 == 0 ? Blocks.ANDESITE : Blocks.STONE);
            }
        }
        for (int i = 0; i < 26; i++) {
            double a = random.nextDouble() * Math.PI * 2;
            double r = radius + 6 + random.nextDouble() * 26;
            int x = (int) Math.round(Math.cos(a) * r);
            int z = (int) Math.round(Math.sin(a) * r);
            BlockPos ground = center.offset(x, 0, z);
            while (!level.getBlockState(ground).isAir() && ground.getY() < center.getY() + 12) {
                ground = ground.above();
            }
            if (level.getBlockState(ground.below()).is(Blocks.GRASS_BLOCK)) {
                Structures.tree(level, ground);
            }
        }
    }

    private void canyon(ServerLevel level, BlockPos center, RandomSource random) {
        floor(level, center, 50, Blocks.RED_SAND, Blocks.RED_SANDSTONE, 3);
        for (int dx = -50; dx <= 50; dx++) {
            for (int dz = -50; dz <= 50; dz++) {
                double d = Math.sqrt(dx * dx + dz * dz);
                if (d < radius + 6 || d > 50) {
                    continue;
                }
                double noise = Math.sin(dx * 0.31) * 3 + Math.cos(dz * 0.27) * 3;
                int h = (int) Math.round(Math.min(26, (d - radius - 6) * 1.6 + 6 + noise));
                for (int y = 0; y < h; y++) {
                    Block layer = switch ((y / 3) % 4) {
                        case 0 -> Blocks.TERRACOTTA;
                        case 1 -> Blocks.DYED_TERRACOTTA.orange();
                        case 2 -> Blocks.DYED_TERRACOTTA.red();
                        default -> Blocks.DYED_TERRACOTTA.brown();
                    };
                    Structures.set(level, center.offset(dx, y, dz), layer);
                }
            }
        }
    }
}
