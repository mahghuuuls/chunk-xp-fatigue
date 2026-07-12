package com.mahghuuuls.chunkxpfatigue.pressure;

import com.mahghuuuls.chunkxpfatigue.fatigue.PressureRecovery;
import net.minecraft.nbt.NBTTagCompound;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

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
