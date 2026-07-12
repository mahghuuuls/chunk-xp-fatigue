package com.mahghuuuls.chunkxpfatigue.pressure;

public final class ChunkPressureKey {

    private final int dimension;
    private final int chunkX;
    private final int chunkZ;

    public ChunkPressureKey(int dimension, int chunkX, int chunkZ) {
        this.dimension = dimension;
        this.chunkX = chunkX;
        this.chunkZ = chunkZ;
    }

    public int getDimension() {
        return dimension;
    }

    public int getChunkX() {
        return chunkX;
    }

    public int getChunkZ() {
        return chunkZ;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof ChunkPressureKey)) {
            return false;
        }
        ChunkPressureKey that = (ChunkPressureKey) other;
        return dimension == that.dimension && chunkX == that.chunkX && chunkZ == that.chunkZ;
    }

    @Override
    public int hashCode() {
        int result = dimension;
        result = 31 * result + chunkX;
        result = 31 * result + chunkZ;
        return result;
    }

    @Override
    public String toString() {
        return dimension + ":" + chunkX + ":" + chunkZ;
    }
}
