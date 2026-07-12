package com.mahghuuuls.chunkxpfatigue.pressure;

final class ChunkPressureRecord {

    private final double pressure;
    private final long lastUpdateTick;
    private final long version;

    ChunkPressureRecord(double pressure, long lastUpdateTick, long version) {
        this.pressure = pressure;
        this.lastUpdateTick = lastUpdateTick;
        this.version = version;
    }

    double getPressure() {
        return pressure;
    }

    long getLastUpdateTick() {
        return lastUpdateTick;
    }

    long getVersion() {
        return version;
    }
}
