package com.mahghuuuls.chunkxpfatigue.pressure;

public interface PressureStore {

    double getPressure(ChunkPressureKey key);

    void setPressure(ChunkPressureKey key, double pressure);

    int clearDimension(int dimension);

    int clearAll();
}
