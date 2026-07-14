package com.mahghuuuls.chunkxpfatigue.forge;

import net.minecraft.entity.EntityLiving;
import net.minecraft.init.Bootstrap;
import net.minecraft.profiler.Profiler;
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

import java.util.HashMap;
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
        assertTrue(world.getChunkChecks() > world.getChunkRequests());
        assertEquals(0, world.getUnloadedChunkRequests());
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

        private int getChunkChecks() {
            return chunkChecks;
        }

        private int getChunkRequests() {
            return chunkRequests;
        }

        private int getUnloadedChunkRequests() {
            return unloadedChunkRequests;
        }
    }

    private static final class TestMob extends EntityLiving {

        private TestMob(World world) {
            super(world);
        }
    }
}
