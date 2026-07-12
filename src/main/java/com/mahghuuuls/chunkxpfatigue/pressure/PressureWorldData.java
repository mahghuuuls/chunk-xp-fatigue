package com.mahghuuuls.chunkxpfatigue.pressure;

import com.mahghuuuls.chunkxpfatigue.Tags;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.world.World;
import net.minecraft.world.storage.MapStorage;
import net.minecraft.world.storage.WorldSavedData;
import net.minecraftforge.common.util.Constants;

import java.util.HashMap;
import java.util.Map;

public final class PressureWorldData extends WorldSavedData implements PressureStore {

    public static final String DATA_NAME = Tags.MOD_ID + "_pressure";
    private static final int SCHEMA_VERSION = 1;

    private static final String TAG_SCHEMA = "schema";
    private static final String TAG_RECORDS = "records";
    private static final String TAG_DIMENSION = "dimension";
    private static final String TAG_CHUNK_X = "chunkX";
    private static final String TAG_CHUNK_Z = "chunkZ";
    private static final String TAG_PRESSURE = "pressure";

    private final Map<ChunkPressureKey, Double> pressureByChunk =
            new HashMap<ChunkPressureKey, Double>();

    public PressureWorldData() {
        super(DATA_NAME);
    }

    public PressureWorldData(String name) {
        super(name);
    }

    public static PressureWorldData get(World world) {
        if (world == null || world.isRemote) {
            throw new IllegalArgumentException("PressureWorldData requires a logical-server world");
        }
        MapStorage storage = world.getMapStorage();
        PressureWorldData data = (PressureWorldData) storage.getOrLoadData(
                PressureWorldData.class,
                DATA_NAME
        );
        if (data == null) {
            data = new PressureWorldData();
            storage.setData(DATA_NAME, data);
        }
        return data;
    }

    @Override
    public double getPressure(ChunkPressureKey key) {
        requireKey(key);
        Double pressure = pressureByChunk.get(key);
        return pressure == null ? 0.0D : pressure;
    }

    @Override
    public void setPressure(ChunkPressureKey key, double pressure) {
        requireKey(key);
        if (Double.isNaN(pressure) || Double.isInfinite(pressure) || pressure < 0.0D) {
            throw new IllegalArgumentException("pressure must be finite and nonnegative");
        }
        if (pressure == 0.0D) {
            if (pressureByChunk.remove(key) != null) {
                markDirty();
            }
            return;
        }

        Double previous = pressureByChunk.put(key, pressure);
        if (previous == null || Double.compare(previous, pressure) != 0) {
            markDirty();
        }
    }

    int getRecordCount() {
        return pressureByChunk.size();
    }

    @Override
    public void readFromNBT(NBTTagCompound compound) {
        pressureByChunk.clear();
        NBTTagList records = compound.getTagList(TAG_RECORDS, Constants.NBT.TAG_COMPOUND);
        for (int index = 0; index < records.tagCount(); index++) {
            NBTTagCompound record = records.getCompoundTagAt(index);
            double pressure = record.getDouble(TAG_PRESSURE);
            if (Double.isNaN(pressure) || Double.isInfinite(pressure) || pressure <= 0.0D) {
                continue;
            }
            ChunkPressureKey key = new ChunkPressureKey(
                    record.getInteger(TAG_DIMENSION),
                    record.getInteger(TAG_CHUNK_X),
                    record.getInteger(TAG_CHUNK_Z)
            );
            pressureByChunk.put(key, pressure);
        }
    }

    @Override
    public NBTTagCompound writeToNBT(NBTTagCompound compound) {
        compound.setInteger(TAG_SCHEMA, SCHEMA_VERSION);
        NBTTagList records = new NBTTagList();
        for (Map.Entry<ChunkPressureKey, Double> entry : pressureByChunk.entrySet()) {
            ChunkPressureKey key = entry.getKey();
            NBTTagCompound record = new NBTTagCompound();
            record.setInteger(TAG_DIMENSION, key.getDimension());
            record.setInteger(TAG_CHUNK_X, key.getChunkX());
            record.setInteger(TAG_CHUNK_Z, key.getChunkZ());
            record.setDouble(TAG_PRESSURE, entry.getValue());
            records.appendTag(record);
        }
        compound.setTag(TAG_RECORDS, records);
        return compound;
    }

    private static void requireKey(ChunkPressureKey key) {
        if (key == null) {
            throw new IllegalArgumentException("key cannot be null");
        }
    }
}
