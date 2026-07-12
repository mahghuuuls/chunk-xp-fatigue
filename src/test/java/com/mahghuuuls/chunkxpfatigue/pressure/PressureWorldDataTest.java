package com.mahghuuuls.chunkxpfatigue.pressure;

import com.mahghuuuls.chunkxpfatigue.fatigue.PressureRecovery;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.nbt.CompressedStreamTools;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

class PressureWorldDataTest {

    private static final double ONE_TICK_PER_PRESSURE_MINUTES =
            1.0D / 1200.0D;

    @Test
    void roundTripPreservesIndependentSparseRecords() {
        PressureWorldData original = new PressureWorldData();
        ChunkPressureKey overworld = new ChunkPressureKey(0, 2, -4);
        ChunkPressureKey nether = new ChunkPressureKey(-1, 2, -4);
        original.setPressure(overworld, 21.5D);
        original.setPressure(nether, 99.0D);

        NBTTagCompound serialized = original.writeToNBT(new NBTTagCompound());
        PressureWorldData restored = new PressureWorldData();
        restored.readFromNBT(serialized);

        assertEquals(21.5D, restored.getPressure(overworld), 0.0D);
        assertEquals(99.0D, restored.getPressure(nether), 0.0D);
        assertEquals(2, restored.getRecordCount());
    }

    @Test
    void zeroPressureRemovesRecord() {
        PressureWorldData data = new PressureWorldData();
        ChunkPressureKey key = new ChunkPressureKey(0, 0, 0);
        data.setPressure(key, 1.0D);

        data.setPressure(key, 0.0D);

        assertEquals(0.0D, data.getPressure(key), 0.0D);
        assertEquals(0, data.getRecordCount());
    }

    @Test
    void invalidPressureIsRejected() {
        PressureWorldData data = new PressureWorldData();
        ChunkPressureKey key = new ChunkPressureKey(0, 0, 0);

        assertThrows(IllegalArgumentException.class, () -> data.setPressure(key, -1.0D));
        assertThrows(IllegalArgumentException.class, () -> data.setPressure(key, Double.NaN));
        assertTrue(data.getRecordCount() == 0);
    }

    @Test
    void lazyRecoveryIsEquivalentAcrossIndependentKeys() {
        PressureWorldData data = configured(ONE_TICK_PER_PRESSURE_MINUTES);
        ChunkPressureKey loadedEquivalent = new ChunkPressureKey(0, 0, 0);
        ChunkPressureKey unloadedEquivalent = new ChunkPressureKey(0, 1000, 1000);
        data.setPressure(loadedEquivalent, 10.0D);
        data.setPressure(unloadedEquivalent, 10.0D);

        advance(data, 3);

        assertEquals(7.0D, data.getPressure(loadedEquivalent), 0.000000001D);
        assertEquals(7.0D, data.getPressure(unloadedEquivalent), 0.000000001D);
    }

    @Test
    void restartSettlesOldRateAndExcludesShutdownTime() {
        PressureWorldData original = configured(ONE_TICK_PER_PRESSURE_MINUTES);
        ChunkPressureKey key = new ChunkPressureKey(0, 4, 8);
        original.setPressure(key, 10.0D);
        advance(original, 2);

        NBTTagCompound saved = original.writeToNBT(new NBTTagCompound());
        PressureWorldData restored = new PressureWorldData();
        restored.readFromNBT(saved);
        restored.configureRecovery(ONE_TICK_PER_PRESSURE_MINUTES * 2.0D);

        assertEquals(2L, restored.getUptimeTicks());
        assertEquals(8.0D, restored.getPressure(key), 0.000000001D);

        advance(restored, 2);

        assertEquals(7.0D, restored.getPressure(key), 0.000000001D);
    }

    @Test
    void disabledRecoveryPreservesRecordsAndSchedulesNoCleanup() {
        PressureWorldData data = configured(0.0D);
        ChunkPressureKey key = new ChunkPressureKey(0, 1, 1);
        data.setPressure(key, 3.0D);

        advance(data, 20);

        assertEquals(3.0D, data.getPressure(key), 0.0D);
        assertEquals(0, data.getCleanupQueueSize());
    }

    @Test
    void staleCleanupEntryCannotDeleteUpdatedRecord() {
        PressureWorldData data = configured(ONE_TICK_PER_PRESSURE_MINUTES);
        ChunkPressureKey key = new ChunkPressureKey(0, 2, 2);
        data.setPressure(key, 1.0D);
        data.setPressure(key, 10.0D);

        data.advanceServerTick();

        assertEquals(9.0D, data.getPressure(key), 0.000000001D);
        assertEquals(1, data.getRecordCount());
    }

    @Test
    void cleanupWorkIsBoundedAndCatchesUpAfterDueBurst() {
        PressureWorldData data = configured(ONE_TICK_PER_PRESSURE_MINUTES);
        int totalRecords = PressureWorldData.CLEANUP_BUDGET_PER_TICK * 5 + 10;
        for (int index = 0; index < totalRecords; index++) {
            data.setPressure(new ChunkPressureKey(0, index, 0), 1.0D);
        }

        data.advanceServerTick();

        assertEquals(totalRecords - PressureWorldData.CLEANUP_BUDGET_PER_TICK,
                data.getRecordCount());

        while (data.getRecordCount() > 0) {
            data.advanceServerTick();
        }

        assertEquals(0, data.getRecordCount());
        assertTrue(data.getUptimeTicks() <= PressureRecovery.SERVER_TICKS_PER_MINUTE * 10L);
    }

    @Test
    void loadRebuildsCleanupQueueFromAuthoritativeRecords() {
        PressureWorldData original = configured(ONE_TICK_PER_PRESSURE_MINUTES);
        original.setPressure(new ChunkPressureKey(0, 9, 9), 5.0D);
        NBTTagCompound saved = original.writeToNBT(new NBTTagCompound());

        PressureWorldData restored = new PressureWorldData();
        restored.readFromNBT(saved);
        restored.configureRecovery(ONE_TICK_PER_PRESSURE_MINUTES);

        assertEquals(1, restored.getCleanupQueueSize());
    }

    @Test
    void bulkClearsReportCountsAndRespectDimensionScope() {
        PressureWorldData data = configured(0.0D);
        data.setPressure(new ChunkPressureKey(0, 1, 1), 2.0D);
        data.setPressure(new ChunkPressureKey(0, 2, 2), 3.0D);
        data.setPressure(new ChunkPressureKey(-1, 1, 1), 4.0D);

        assertEquals(2, data.clearDimension(0));
        assertEquals(1, data.getRecordCount());
        assertEquals(0, data.clearDimension(0));
        assertEquals(1, data.clearAll());
        assertEquals(0, data.clearAll());
    }

    @Test
    void mixedCorruptFixturePreservesValidNeighbor() {
        NBTTagCompound root = new NBTTagCompound();
        root.setInteger("schema", 2);
        root.setLong("uptime", 10L);
        NBTTagList records = new NBTTagList();
        records.appendTag(record(0, 1, 2, 25.0D, 5L));
        NBTTagCompound missingCoordinate = new NBTTagCompound();
        missingCoordinate.setDouble("pressure", 50.0D);
        missingCoordinate.setLong("lastUpdate", 5L);
        records.appendTag(missingCoordinate);
        records.appendTag(record(0, 3, 4, Double.NaN, 5L));
        root.setTag("records", records);

        PressureWorldData data = new PressureWorldData();
        data.readFromNBT(root);
        data.configure(0.0D, 100.0D);

        assertEquals(1, data.getRecordCount());
        assertEquals(25.0D, data.getPressure(new ChunkPressureKey(0, 1, 2)), 0.0D);
    }

    @Test
    void futureSchemaIsRejectedWithoutGuessing() {
        NBTTagCompound root = new NBTTagCompound();
        root.setInteger("schema", 999);
        root.setLong("uptime", 0L);
        NBTTagList records = new NBTTagList();
        records.appendTag(record(0, 1, 1, 10.0D, 0L));
        root.setTag("records", records);

        PressureWorldData data = new PressureWorldData();
        data.readFromNBT(root);

        assertEquals(0, data.getRecordCount());
        assertFalse(data.isWritable());
        data.setPressure(new ChunkPressureKey(0, 2, 2), 50.0D);
        assertEquals(0.0D, data.getPressure(new ChunkPressureKey(0, 2, 2)), 0.0D);
        assertEquals(999, data.writeToNBT(new NBTTagCompound()).getInteger("schema"));
    }

    @Test
    void missingStoredRecoveryRatePreservesRawPressureWithoutRetroactiveGuess() {
        NBTTagCompound root = new NBTTagCompound();
        root.setInteger("schema", 2);
        root.setLong("uptime", 10L);
        NBTTagList records = new NBTTagList();
        records.appendTag(record(0, 7, 7, 10.0D, 0L));
        root.setTag("records", records);

        PressureWorldData data = new PressureWorldData();
        data.readFromNBT(root);
        data.configure(ONE_TICK_PER_PRESSURE_MINUTES, 100.0D);

        assertEquals(10.0D, data.getPressure(new ChunkPressureKey(0, 7, 7)), 0.0D);
    }

    @Test
    void loweredMaximumClampsWhileRaisedMaximumPreservesRawPressure() {
        PressureWorldData data = new PressureWorldData();
        data.configure(0.0D, 100.0D);
        ChunkPressureKey key = new ChunkPressureKey(0, 5, 5);
        data.setPressure(key, 80.0D);

        data.configure(0.0D, 200.0D);
        assertEquals(80.0D, data.getPressure(key), 0.0D);

        data.configure(0.0D, 50.0D);
        assertEquals(50.0D, data.getPressure(key), 0.0D);
    }

    @Test
    void representativeLargeSparseStateRoundTripsAndRebuildsQueue() throws IOException {
        int count = 100_000;
        PressureWorldData original = new PressureWorldData();
        original.configure(3.0D, 100.0D);
        for (int index = 0; index < count; index++) {
            original.setPressure(new ChunkPressureKey(index % 3 - 1, index, -index),
                    index % 100 + 1.0D);
        }

        long saveStart = System.nanoTime();
        NBTTagCompound root = original.writeToNBT(new NBTTagCompound());
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        CompressedStreamTools.writeCompressed(root, output);
        long saveNanos = System.nanoTime() - saveStart;

        long loadStart = System.nanoTime();
        PressureWorldData restored = new PressureWorldData();
        restored.readFromNBT(CompressedStreamTools.readCompressed(
                new ByteArrayInputStream(output.toByteArray())));
        restored.configure(3.0D, 100.0D);
        long loadNanos = System.nanoTime() - loadStart;

        assertEquals(count, restored.getRecordCount());
        assertEquals(count, restored.getCleanupQueueSize());
        assertTrue(output.size() < 10 * 1024 * 1024);
        assertTrue(saveNanos < 10_000_000_000L);
        assertTrue(loadNanos < 10_000_000_000L);
        System.out.println("large-state records=" + count + " compressedBytes=" + output.size()
                + " saveMs=" + saveNanos / 1_000_000L + " loadMs=" + loadNanos / 1_000_000L);
    }

    @Test
    void hundredThousandSimultaneousExpiriesCatchUpWithinBound() {
        int count = 100_000;
        PressureWorldData data = configured(ONE_TICK_PER_PRESSURE_MINUTES);
        for (int index = 0; index < count; index++) {
            data.setPressure(new ChunkPressureKey(0, index, 0), 1.0D);
        }
        long start = System.nanoTime();
        while (data.getRecordCount() > 0) {
            data.advanceServerTick();
        }
        long elapsed = System.nanoTime() - start;

        assertTrue(data.getUptimeTicks() <= PressureRecovery.SERVER_TICKS_PER_MINUTE * 10L);
        assertEquals(98L, data.getUptimeTicks());
        System.out.println("cleanup-burst records=" + count + " ticks=" + data.getUptimeTicks()
                + " processMs=" + elapsed / 1_000_000L);
    }

    @Test
    void hotChunkUpdatesCannotGrowCleanupQueueWithoutBound() {
        PressureWorldData data = configured(3.0D);
        ChunkPressureKey key = new ChunkPressureKey(0, 0, 0);
        for (int update = 0; update < 100_000; update++) {
            data.setPressure(key, 100.0D);
        }

        assertEquals(1, data.getRecordCount());
        assertTrue(data.getCleanupQueueSize() <= 4098);
    }

    private static NBTTagCompound record(int dimension, int chunkX, int chunkZ,
                                         double pressure, long lastUpdate) {
        NBTTagCompound record = new NBTTagCompound();
        record.setInteger("dimension", dimension);
        record.setInteger("chunkX", chunkX);
        record.setInteger("chunkZ", chunkZ);
        record.setDouble("pressure", pressure);
        record.setLong("lastUpdate", lastUpdate);
        return record;
    }

    private static PressureWorldData configured(double recoveryMinutesPerPressure) {
        PressureWorldData data = new PressureWorldData();
        data.configureRecovery(recoveryMinutesPerPressure);
        return data;
    }

    private static void advance(PressureWorldData data, int ticks) {
        for (int tick = 0; tick < ticks; tick++) {
            data.advanceServerTick();
        }
    }
}
