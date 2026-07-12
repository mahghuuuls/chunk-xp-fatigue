package com.mahghuuuls.chunkxpfatigue.pressure;

public interface PressureStore {

    default boolean isWritable() { return true; }

    double getPressure(ChunkPressureKey key);

    void setPressure(ChunkPressureKey key, double pressure);

    int clearDimension(int dimension);

    int clearAll();
}
