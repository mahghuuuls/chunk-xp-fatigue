package com.mahghuuuls.chunkxpfatigue.forge;

import com.google.common.collect.MapMaker;

import java.util.Map;

public final class CrowdingSnapshotCache {

    private final Map<Object, Integer> snapshots =
            new MapMaker().weakKeys().makeMap();

    void put(Object entityKey, int nearbyMobCount) {
        if (entityKey == null) {
            throw new IllegalArgumentException("entityKey cannot be null");
        }
        if (nearbyMobCount < 0) {
            throw new IllegalArgumentException("nearbyMobCount must be nonnegative");
        }
        snapshots.put(entityKey, nearbyMobCount);
    }

    int take(Object entityKey) {
        Integer count = snapshots.remove(entityKey);
        return count == null ? 0 : count;
    }

    public void clear() {
        snapshots.clear();
    }

    int size() {
        return snapshots.size();
    }
}
