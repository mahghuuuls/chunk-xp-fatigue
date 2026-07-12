package com.mahghuuuls.chunkxpfatigue.pressure;

import com.mahghuuuls.chunkxpfatigue.Tags;
import com.mahghuuuls.chunkxpfatigue.fatigue.PressureRecovery;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.world.World;
import net.minecraft.world.storage.MapStorage;
import net.minecraft.world.storage.WorldSavedData;
import net.minecraftforge.common.util.Constants;

import java.util.HashMap;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Iterator;

public final class PressureWorldData extends WorldSavedData implements PressureStore {

    public static final String DATA_NAME = Tags.MOD_ID + "_pressure";
    static final int CLEANUP_BUDGET_PER_TICK = 1024;
    private static final int SCHEMA_VERSION = 2;

    private static final String TAG_SCHEMA = "schema";
    private static final String TAG_UPTIME = "uptime";
    private static final String TAG_RECOVERY_RATE = "recoveryMinutesPerPressure";
    private static final String TAG_RECORDS = "records";
    private static final String TAG_DIMENSION = "dimension";
    private static final String TAG_CHUNK_X = "chunkX";
    private static final String TAG_CHUNK_Z = "chunkZ";
    private static final String TAG_PRESSURE = "pressure";
    private static final String TAG_LAST_UPDATE = "lastUpdate";

    private final Map<ChunkPressureKey, ChunkPressureRecord> pressureByChunk =
            new HashMap<ChunkPressureKey, ChunkPressureRecord>();
    private final PriorityQueue<CleanupEntry> cleanupQueue = new PriorityQueue<CleanupEntry>();

    private long uptimeTicks;
    private long nextVersion = 1L;
    private double recoveryMinutesPerPressure;
    private boolean recoveryConfigured;

    public PressureWorldData() {
        super(DATA_NAME);
    }

    public PressureWorldData(String name) {
        super(name);
    }

    public static PressureWorldData get(World world, double recoveryMinutesPerPressure) {
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
        data.configureRecovery(recoveryMinutesPerPressure);
        return data;
    }

    void configureRecovery(double minutesPerPressure) {
        validateRecoveryRate(minutesPerPressure);
        if (recoveryConfigured
                && Double.compare(recoveryMinutesPerPressure, minutesPerPressure) == 0) {
            return;
        }
        recoveryMinutesPerPressure = minutesPerPressure;
        recoveryConfigured = true;
        rebuildCleanupQueue();
    }

    public void advanceServerTick() {
        if (!recoveryConfigured) {
            throw new IllegalStateException("recovery must be configured before ticking");
        }
        if (uptimeTicks < Long.MAX_VALUE) {
            uptimeTicks++;
        }
        processDueCleanup(CLEANUP_BUDGET_PER_TICK);
        markDirty();
    }

    @Override
    public double getPressure(ChunkPressureKey key) {
        requireKey(key);
        ChunkPressureRecord record = pressureByChunk.get(key);
        if (record == null) {
            return 0.0D;
        }
        double recovered = recoveredPressure(record);
        if (recovered <= 0.0D) {
            pressureByChunk.remove(key);
            markDirty();
            return 0.0D;
        }
        return recovered;
    }

    @Override
    public void setPressure(ChunkPressureKey key, double pressure) {
        requireKey(key);
        validatePressure(pressure);
        if (pressure == 0.0D) {
            if (pressureByChunk.remove(key) != null) {
                markDirty();
            }
            return;
        }
        replaceRecord(key, pressure, uptimeTicks);
    }

    int getRecordCount() {
        return pressureByChunk.size();
    }

    long getUptimeTicks() {
        return uptimeTicks;
    }

    int getCleanupQueueSize() {
        return cleanupQueue.size();
    }

    public int clearDimension(int dimension) {
        int removed = 0;
        Iterator<ChunkPressureKey> iterator = pressureByChunk.keySet().iterator();
        while (iterator.hasNext()) {
            if (iterator.next().getDimension() == dimension) {
                iterator.remove();
                removed++;
            }
        }
        if (removed > 0) {
            rebuildCleanupQueue();
            markDirty();
        }
        return removed;
    }

    public int clearAll() {
        int removed = pressureByChunk.size();
        if (removed > 0) {
            pressureByChunk.clear();
            cleanupQueue.clear();
            markDirty();
        }
        return removed;
    }

    int processDueCleanup(int budget) {
        if (budget < 0) {
            throw new IllegalArgumentException("budget cannot be negative");
        }
        int processed = 0;
        while (processed < budget && !cleanupQueue.isEmpty()) {
            CleanupEntry entry = cleanupQueue.peek();
            if (entry.dueTick > uptimeTicks) {
                break;
            }
            cleanupQueue.poll();
            processed++;
            ChunkPressureRecord record = pressureByChunk.get(entry.key);
            if (record == null || record.getVersion() != entry.version) {
                continue;
            }
            if (recoveredPressure(record) <= 0.0D) {
                pressureByChunk.remove(entry.key);
                markDirty();
            }
        }
        return processed;
    }

    @Override
    public void readFromNBT(NBTTagCompound compound) {
        pressureByChunk.clear();
        cleanupQueue.clear();
        uptimeTicks = Math.max(0L, compound.getLong(TAG_UPTIME));
        nextVersion = 1L;
        double storedRecoveryRate = compound.getDouble(TAG_RECOVERY_RATE);
        boolean hasStoredRecoveryRate = compound.hasKey(TAG_RECOVERY_RATE, Constants.NBT.TAG_DOUBLE)
                && isValidRecoveryRate(storedRecoveryRate);
        NBTTagList records = compound.getTagList(TAG_RECORDS, Constants.NBT.TAG_COMPOUND);
        for (int index = 0; index < records.tagCount(); index++) {
            NBTTagCompound stored = records.getCompoundTagAt(index);
            double pressure = stored.getDouble(TAG_PRESSURE);
            long lastUpdate = stored.hasKey(TAG_LAST_UPDATE, Constants.NBT.TAG_LONG)
                    ? stored.getLong(TAG_LAST_UPDATE)
                    : uptimeTicks;
            if (!isValidStoredPressure(pressure) || lastUpdate < 0L || lastUpdate > uptimeTicks) {
                continue;
            }
            if (hasStoredRecoveryRate) {
                pressure = PressureRecovery.recover(
                        pressure,
                        uptimeTicks - lastUpdate,
                        storedRecoveryRate
                );
                lastUpdate = uptimeTicks;
                if (pressure <= 0.0D) {
                    continue;
                }
            }
            ChunkPressureKey key = new ChunkPressureKey(
                    stored.getInteger(TAG_DIMENSION),
                    stored.getInteger(TAG_CHUNK_X),
                    stored.getInteger(TAG_CHUNK_Z)
            );
            pressureByChunk.put(key, new ChunkPressureRecord(pressure, lastUpdate, nextVersion++));
        }
        if (recoveryConfigured) {
            rebuildCleanupQueue();
        }
    }

    @Override
    public NBTTagCompound writeToNBT(NBTTagCompound compound) {
        compound.setInteger(TAG_SCHEMA, SCHEMA_VERSION);
        compound.setLong(TAG_UPTIME, uptimeTicks);
        if (recoveryConfigured) {
            compound.setDouble(TAG_RECOVERY_RATE, recoveryMinutesPerPressure);
        }
        NBTTagList records = new NBTTagList();
        for (Map.Entry<ChunkPressureKey, ChunkPressureRecord> entry : pressureByChunk.entrySet()) {
            ChunkPressureKey key = entry.getKey();
            ChunkPressureRecord value = entry.getValue();
            NBTTagCompound stored = new NBTTagCompound();
            stored.setInteger(TAG_DIMENSION, key.getDimension());
            stored.setInteger(TAG_CHUNK_X, key.getChunkX());
            stored.setInteger(TAG_CHUNK_Z, key.getChunkZ());
            stored.setDouble(TAG_PRESSURE, value.getPressure());
            stored.setLong(TAG_LAST_UPDATE, value.getLastUpdateTick());
            records.appendTag(stored);
        }
        compound.setTag(TAG_RECORDS, records);
        return compound;
    }

    private void replaceRecord(ChunkPressureKey key, double pressure, long updateTick) {
        ChunkPressureRecord previous = pressureByChunk.put(
                key,
                new ChunkPressureRecord(pressure, updateTick, nextVersion++)
        );
        ChunkPressureRecord current = pressureByChunk.get(key);
        scheduleCleanup(key, current);
        if (previous == null
                || Double.compare(previous.getPressure(), pressure) != 0
                || previous.getLastUpdateTick() != updateTick) {
            markDirty();
        }
    }

    private double recoveredPressure(ChunkPressureRecord record) {
        if (!recoveryConfigured) {
            return record.getPressure();
        }
        return PressureRecovery.recover(
                record.getPressure(),
                uptimeTicks - record.getLastUpdateTick(),
                recoveryMinutesPerPressure
        );
    }

    private void rebuildCleanupQueue() {
        cleanupQueue.clear();
        if (recoveryMinutesPerPressure == 0.0D) {
            return;
        }
        for (Map.Entry<ChunkPressureKey, ChunkPressureRecord> entry : pressureByChunk.entrySet()) {
            scheduleCleanup(entry.getKey(), entry.getValue());
        }
    }

    private void scheduleCleanup(ChunkPressureKey key, ChunkPressureRecord record) {
        if (!recoveryConfigured || recoveryMinutesPerPressure == 0.0D) {
            return;
        }
        double duration = record.getPressure()
                * recoveryMinutesPerPressure
                * PressureRecovery.SERVER_TICKS_PER_MINUTE;
        long durationTicks = duration >= Long.MAX_VALUE
                ? Long.MAX_VALUE
                : (long) Math.ceil(duration);
        long dueTick = saturatedAdd(record.getLastUpdateTick(), durationTicks);
        cleanupQueue.add(new CleanupEntry(dueTick, key, record.getVersion()));
    }

    private static long saturatedAdd(long left, long right) {
        if (right > Long.MAX_VALUE - left) {
            return Long.MAX_VALUE;
        }
        return left + right;
    }

    private static boolean isValidStoredPressure(double pressure) {
        return !Double.isNaN(pressure) && !Double.isInfinite(pressure) && pressure > 0.0D;
    }

    private static void validatePressure(double pressure) {
        if (Double.isNaN(pressure) || Double.isInfinite(pressure) || pressure < 0.0D) {
            throw new IllegalArgumentException("pressure must be finite and nonnegative");
        }
    }

    private static void validateRecoveryRate(double minutesPerPressure) {
        if (!isValidRecoveryRate(minutesPerPressure)) {
            throw new IllegalArgumentException(
                    "recoveryMinutesPerPressure must be finite and nonnegative"
            );
        }
    }

    private static boolean isValidRecoveryRate(double minutesPerPressure) {
        return !Double.isNaN(minutesPerPressure)
                && !Double.isInfinite(minutesPerPressure)
                && minutesPerPressure >= 0.0D;
    }

    private static void requireKey(ChunkPressureKey key) {
        if (key == null) {
            throw new IllegalArgumentException("key cannot be null");
        }
    }

    private static final class CleanupEntry implements Comparable<CleanupEntry> {

        private final long dueTick;
        private final ChunkPressureKey key;
        private final long version;

        private CleanupEntry(long dueTick, ChunkPressureKey key, long version) {
            this.dueTick = dueTick;
            this.key = key;
            this.version = version;
        }

        @Override
        public int compareTo(CleanupEntry other) {
            if (dueTick < other.dueTick) {
                return -1;
            }
            if (dueTick > other.dueTick) {
                return 1;
            }
            return Long.compare(version, other.version);
        }
    }
}
