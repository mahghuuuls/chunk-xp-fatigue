package com.mahghuuuls.chunkxpfatigue.forge;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class CrowdingSnapshotCacheTest {

    @Test
    void takeIsDestructiveAndMissingEntryFallsBackToZero() {
        CrowdingSnapshotCache cache = new CrowdingSnapshotCache();
        Object entity = new Object();

        cache.put(entity, 8);

        assertEquals(1, cache.size());
        assertEquals(8, cache.take(entity));
        assertEquals(0, cache.take(entity));
        assertEquals(0, cache.size());
    }

    @Test
    void clearRemovesAllSessionSnapshots() {
        CrowdingSnapshotCache cache = new CrowdingSnapshotCache();
        Object first = new Object();
        Object second = new Object();
        cache.put(first, 4);
        cache.put(second, 12);

        cache.clear();

        assertEquals(0, cache.size());
        assertEquals(0, cache.take(first));
        assertEquals(0, cache.take(second));
    }

    @Test
    void invalidSnapshotInputIsRejected() {
        CrowdingSnapshotCache cache = new CrowdingSnapshotCache();
        Object entity = new Object();

        assertThrows(IllegalArgumentException.class, () -> cache.put(null, 1));
        assertThrows(IllegalArgumentException.class, () -> cache.put(entity, -1));
    }

    @Test
    void distinctEqualsEqualKeysRetainIndependentSnapshots() {
        CrowdingSnapshotCache cache = new CrowdingSnapshotCache();
        EqualKey first = new EqualKey(7);
        EqualKey second = new EqualKey(7);

        cache.put(first, 3);
        cache.put(second, 8);

        assertEquals(2, cache.size());
        assertEquals(3, cache.take(first));
        assertEquals(8, cache.take(second));
    }

    private static final class EqualKey {

        private final int value;

        private EqualKey(int value) {
            this.value = value;
        }

        @Override
        public boolean equals(Object other) {
            return other instanceof EqualKey && ((EqualKey) other).value == value;
        }

        @Override
        public int hashCode() {
            return value;
        }
    }
}
