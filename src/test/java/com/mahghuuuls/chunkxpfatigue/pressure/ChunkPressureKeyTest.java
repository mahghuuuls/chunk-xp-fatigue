package com.mahghuuuls.chunkxpfatigue.pressure;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

class ChunkPressureKeyTest {

    @Test
    void dimensionAndBothChunkCoordinatesDefineIdentity() {
        ChunkPressureKey key = new ChunkPressureKey(-1, -3, 7);

        assertEquals(key, new ChunkPressureKey(-1, -3, 7));
        assertEquals(key.hashCode(), new ChunkPressureKey(-1, -3, 7).hashCode());
        assertNotEquals(key, new ChunkPressureKey(0, -3, 7));
        assertNotEquals(key, new ChunkPressureKey(-1, -2, 7));
        assertNotEquals(key, new ChunkPressureKey(-1, -3, 8));
    }
}
