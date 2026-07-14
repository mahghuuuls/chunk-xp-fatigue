package com.mahghuuuls.chunkxpfatigue.forge;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLiving;
import net.minecraft.init.Bootstrap;
import net.minecraft.profiler.Profiler;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.world.GameType;
import net.minecraft.world.World;
import net.minecraft.world.WorldProvider;
import net.minecraft.world.WorldProviderSurface;
import net.minecraft.world.WorldSettings;
import net.minecraft.world.WorldType;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.IChunkProvider;
import net.minecraft.world.storage.WorldInfo;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CrowdingSamplerTest {

    @Test
    void worldQueryIncludesEntityAtInclusiveUpperVerticalBoundary() {
        QueryWorld world = new QueryWorld();
        TestMob source = world.addMob(0.0D, 0.0D, 0.0D);
        world.addMob(0.0D, 3.0D, 0.0D);

        assertEquals(1, new CrowdingSampler().countNearby(source, 3.0D));
        assertEquals(0, world.getUnloadedChunkRequests());
    }

    @Test
    void worldQueryCountsLoadedCrossChunkMobWithoutRequestingUnloadedChunks() {
        QueryWorld world = new QueryWorld();
        TestMob source = world.addMob(15.0D, 0.0D, 0.0D);
        world.addMob(16.0D, 0.0D, 0.0D);

        assertEquals(1, new CrowdingSampler().countNearby(source, 3.0D));
        assertEquals(1, world.getEntityQueryCount());
        assertTrue(world.getChunkChecks() > world.getChunkRequests());
        assertEquals(0, world.getUnloadedChunkRequests());
    }

    @Test
    void maximumRadiusDenseChunkCornerHasBoundedStructureAndRecordsPerformance() {
        QueryWorld world = new QueryWorld();
        TestMob dyingMob = world.addMob(16.0D, 64.0D, 16.0D);
        for (int i = 0; i < 200; ++i) {
            double angle = 2.0D * Math.PI * i / 200.0D;
            double horizontalRadius = 1.0D + (i % 8) * 0.75D;
            double yOffset = (i % 5 - 2) * 0.5D;
            world.addMob(
                    16.0D + Math.cos(angle) * horizontalRadius,
                    64.0D + yOffset,
                    16.0D + Math.sin(angle) * horizontalRadius
            );
        }

        CrowdingSampler sampler = new CrowdingSampler();
        for (int i = 0; i < 100; ++i) {
            sampler.countNearby(dyingMob, 8.0D);
        }

        world.resetEntityQueryCount();
        long[] singleSamples = new long[101];
        int sampledCount = 0;
        for (int i = 0; i < singleSamples.length; ++i) {
            long start = System.nanoTime();
            sampledCount = sampler.countNearby(dyingMob, 8.0D);
            singleSamples[i] = System.nanoTime() - start;
        }
        assertEquals(200, sampledCount);
        assertEquals(singleSamples.length, world.getEntityQueryCount());

        long[] orderedSamples = singleSamples.clone();
        Arrays.sort(orderedSamples);
        long medianNanos = orderedSamples[orderedSamples.length / 2];
        long maximumNanos = orderedSamples[orderedSamples.length - 1];

        world.resetEntityQueryCount();
        long batchStart = System.nanoTime();
        for (int i = 0; i < 20; ++i) {
            assertEquals(200, sampler.countNearby(dyingMob, 8.0D));
        }
        long batchNanos = System.nanoTime() - batchStart;
        boolean thresholdsEnforced = Boolean.getBoolean(
                "chunkxpfatigue.enforceCrowdingPerformance");

        System.out.println(String.format(
                Locale.ROOT,
                "Crowding stress java=%s os=%s/%s warmup=100 totalLiveMobs=201 nearby=200 "
                        + "radius=8.0 singleSamples=%d medianMs=%.6f maxMs=%.6f "
                        + "batchSamples=20 batchMs=%.6f loadedChunks=%d "
                        + "thresholdsEnforced=%s",
                System.getProperty("java.version"),
                System.getProperty("os.name"),
                System.getProperty("os.arch"),
                singleSamples.length,
                medianNanos / 1_000_000.0D,
                maximumNanos / 1_000_000.0D,
                batchNanos / 1_000_000.0D,
                world.getLoadedChunkCount(),
                thresholdsEnforced
        ));

        assertEquals(20, world.getEntityQueryCount());
        assertEquals(0, world.getUnloadedChunkRequests());
        if (thresholdsEnforced) {
            assertTrue(medianNanos < 5_000_000L, "median sample exceeded 5 ms");
            assertTrue(maximumNanos < 50_000_000L, "single sample reached 50 ms");
            assertTrue(batchNanos < 50_000_000L, "20-sample batch reached 50 ms");
        }
    }

    @Test
    void usesInclusiveThreeDimensionalSphericalDistance() {
        assertTrue(CrowdingSampler.isWithinRadius(
                0.0D, 0.0D, 0.0D,
                3.0D, 0.0D, 0.0D,
                3.0D));
        assertTrue(CrowdingSampler.isWithinRadius(
                0.0D, 0.0D, 0.0D,
                0.0D, 3.0D, 0.0D,
                3.0D));
        assertTrue(CrowdingSampler.isWithinRadius(
                0.0D, 0.0D, 0.0D,
                1.0D, 2.0D, 2.0D,
                3.0D));
        assertFalse(CrowdingSampler.isWithinRadius(
                0.0D, 0.0D, 0.0D,
                3.0D, 3.0D, 0.0D,
                3.0D));
        assertFalse(CrowdingSampler.isWithinRadius(
                0.0D, 0.0D, 0.0D,
                3.000001D, 0.0D, 0.0D,
                3.0D));
    }

    @Test
    void preservesFractionalPositionsAndRadius() {
        assertTrue(CrowdingSampler.isWithinRadius(
                -2.5D, 10.25D, 4.75D,
                -1.0D, 12.25D, 4.75D,
                2.5D));
        assertFalse(CrowdingSampler.isWithinRadius(
                -2.5D, 10.25D, 4.75D,
                0.0D, 12.75D, 4.75D,
                2.5D));
    }

    @Test
    void excludesSelfDeadAndOutOfRadiusCandidates() {
        assertTrue(CrowdingSampler.shouldCount(false, true, true));
        assertFalse(CrowdingSampler.shouldCount(true, true, true));
        assertFalse(CrowdingSampler.shouldCount(false, false, true));
        assertFalse(CrowdingSampler.shouldCount(false, true, false));
    }

    private static final class QueryWorld extends World {

        private final Map<String, Chunk> chunks = new HashMap<String, Chunk>();
        private int chunkChecks;
        private int chunkRequests;
        private int unloadedChunkRequests;
        private int entityQueryCount;

        private QueryWorld() {
            super(
                    null,
                    new WorldInfo(
                            new WorldSettings(0L, GameType.SURVIVAL, false, false,
                                    WorldType.DEFAULT),
                            "crowding-query-test"),
                    provider(),
                    new Profiler(),
                    false
            );
            Bootstrap.register();
            provider.setWorld(this);
        }

        private static WorldProvider provider() {
            return new WorldProviderSurface();
        }

        private TestMob addMob(double x, double y, double z) {
            TestMob mob = new TestMob(this);
            mob.setPosition(x, y, z);
            mob.setHealth(mob.getMaxHealth());
            int chunkX = ((int) Math.floor(x)) >> 4;
            int chunkZ = ((int) Math.floor(z)) >> 4;
            chunk(chunkX, chunkZ).addEntity(mob);
            return mob;
        }

        private Chunk chunk(int chunkX, int chunkZ) {
            String key = chunkX + ":" + chunkZ;
            Chunk chunk = chunks.get(key);
            if (chunk == null) {
                chunk = new Chunk(this, chunkX, chunkZ);
                chunks.put(key, chunk);
            }
            return chunk;
        }

        @Override
        protected IChunkProvider createChunkProvider() {
            return null;
        }

        @Override
        protected boolean isChunkLoaded(int x, int z, boolean allowEmpty) {
            ++chunkChecks;
            return chunks.containsKey(x + ":" + z);
        }

        @Override
        public Chunk getChunk(int chunkX, int chunkZ) {
            ++chunkRequests;
            Chunk chunk = chunks.get(chunkX + ":" + chunkZ);
            if (chunk == null) {
                ++unloadedChunkRequests;
            }
            return chunk;
        }

        @Override
        public <T extends Entity> List<T> getEntitiesWithinAABB(
                Class<? extends T> entityType,
                AxisAlignedBB boundingBox
        ) {
            ++entityQueryCount;
            return super.getEntitiesWithinAABB(entityType, boundingBox);
        }

        private int getChunkChecks() {
            return chunkChecks;
        }

        private int getChunkRequests() {
            return chunkRequests;
        }

        private int getUnloadedChunkRequests() {
            return unloadedChunkRequests;
        }

        private int getEntityQueryCount() {
            return entityQueryCount;
        }

        private void resetEntityQueryCount() {
            entityQueryCount = 0;
        }

        private int getLoadedChunkCount() {
            return chunks.size();
        }
    }

    private static final class TestMob extends EntityLiving {

        private TestMob(World world) {
            super(world);
        }
    }
}
