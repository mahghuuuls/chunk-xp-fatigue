package com.mahghuuuls.chunkxpfatigue.pressure;

import net.minecraft.nbt.NBTTagCompound;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PressureWorldDataTest {

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
}
