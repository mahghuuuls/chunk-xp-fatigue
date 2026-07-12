package com.mahghuuuls.chunkxpfatigue.pressure;

import com.mahghuuuls.chunkxpfatigue.Tags;
import com.mahghuuuls.chunkxpfatigue.fatigue.PressureRecovery;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.world.World;
import net.minecraft.world.storage.MapStorage;
import net.minecraft.world.storage.WorldSavedData;
import net.minecraftforge.common.util.Constants;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.HashMap;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Iterator;

public final class PressureWorldData extends WorldSavedData implements PressureStore {
    private static final Logger LOGGER = LogManager.getLogger(Tags.MOD_NAME);

    public static final String DATA_NAME = Tags.MOD_ID + "_pressure";
    static final int CLEANUP_BUDGET_PER_TICK = 1024;
    private static final int QUEUE_STALE_ALLOWANCE = 4096;
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
    private double maximumPressure = Double.POSITIVE_INFINITY;
    private boolean recoveryConfigured;
    private NBTTagCompound unsupportedFutureData;

    public PressureWorldData() {
        super(DATA_NAME);
    }

    public PressureWorldData(String name) {
        super(name);
    }

    public static PressureWorldData get(World world, double recoveryMinutesPerPressure,
                                        double maximumPressure) {
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
        data.configure(recoveryMinutesPerPressure, maximumPressure);
        return data;
    }

    void configureRecovery(double minutesPerPressure) {
        configure(minutesPerPressure, Double.MAX_VALUE);
    }

    void configure(double minutesPerPressure, double configuredMaximum) {
        validateRecoveryRate(minutesPerPressure);
        if (!isValidStoredPressure(configuredMaximum)) {
            throw new IllegalArgumentException("maximumPressure must be finite and positive");
        }
        if (recoveryConfigured
                && Double.compare(recoveryMinutesPerPressure, minutesPerPressure) == 0
                && Double.compare(maximumPressure, configuredMaximum) == 0) {
            return;
        }
        recoveryMinutesPerPressure = minutesPerPressure;
        maximumPressure = configuredMaximum;
        recoveryConfigured = true;
        clampRecordsToMaximum();
        rebuildCleanupQueue();
    }

    public void advanceServerTick() {
        if (!recoveryConfigured) {
            throw new IllegalStateException("recovery must be configured before ticking");
        }
        if (unsupportedFutureData != null) return;
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
        if (unsupportedFutureData != null) return;
        if (pressure == 0.0D) {
            if (pressureByChunk.remove(key) != null) {
                markDirty();
            }
            return;
        }
        replaceRecord(key, Math.min(pressure, maximumPressure), uptimeTicks);
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
        if (unsupportedFutureData != null) return 0;
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
        if (unsupportedFutureData != null) return 0;
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
    public boolean isWritable() {
        return unsupportedFutureData == null;
    }

    @Override
    public void readFromNBT(NBTTagCompound compound) {
        pressureByChunk.clear();
        cleanupQueue.clear();
        unsupportedFutureData = null;
        nextVersion = 1L;
        if (!compound.hasKey(TAG_SCHEMA, Constants.NBT.TAG_INT)) {
            LOGGER.error("Pressure data ignored: missing integer schema");
            uptimeTicks = 0L;
            return;
        }
        int schema = compound.getInteger(TAG_SCHEMA);
        if (schema < 1 || schema > SCHEMA_VERSION) {
            LOGGER.error("Pressure data ignored: unsupported schema {}", schema);
            if (schema > SCHEMA_VERSION) {
                unsupportedFutureData = (NBTTagCompound) compound.copy();
            }
            uptimeTicks = 0L;
            return;
        }
        if (schema >= 2 && !compound.hasKey(TAG_UPTIME, Constants.NBT.TAG_LONG)) {
            LOGGER.error("Pressure data ignored: schema {} missing long uptime", schema);
            uptimeTicks = 0L;
            return;
        }
        uptimeTicks = schema == 1 ? 0L : compound.getLong(TAG_UPTIME);
        if (uptimeTicks < 0L) {
            LOGGER.error("Pressure data ignored: negative uptime {}", uptimeTicks);
            uptimeTicks = 0L;
            return;
        }
        if (!compound.hasKey(TAG_RECORDS, Constants.NBT.TAG_LIST)) {
            LOGGER.error("Pressure data ignored: missing records list");
            return;
        }
        double storedRecoveryRate = compound.getDouble(TAG_RECOVERY_RATE);
        boolean hasStoredRecoveryRate = compound.hasKey(TAG_RECOVERY_RATE, Constants.NBT.TAG_DOUBLE)
                && isValidRecoveryRate(storedRecoveryRate);
        if (schema >= 2 && !hasStoredRecoveryRate) {
            LOGGER.warn("Pressure data has no valid stored recovery rate; preserving raw pressure "
                    + "and discarding ambiguous elapsed recovery");
        }
        NBTTagList records = compound.getTagList(TAG_RECORDS, Constants.NBT.TAG_COMPOUND);
        for (int index = 0; index < records.tagCount(); index++) {
            NBTTagCompound stored = records.getCompoundTagAt(index);
            if (!hasRequiredRecordFields(stored, schema)) {
                LOGGER.warn("Skipping pressure record {}: missing or mistyped required field", index);
                continue;
            }
            double pressure = stored.getDouble(TAG_PRESSURE);
            long lastUpdate = stored.hasKey(TAG_LAST_UPDATE, Constants.NBT.TAG_LONG)
                    ? stored.getLong(TAG_LAST_UPDATE)
                    : uptimeTicks;
            if (!isValidStoredPressure(pressure) || lastUpdate < 0L || lastUpdate > uptimeTicks) {
                LOGGER.warn("Skipping pressure record {} at {}:{}:{}: invalid pressure or timestamp",
                        index, stored.getInteger(TAG_DIMENSION), stored.getInteger(TAG_CHUNK_X),
                        stored.getInteger(TAG_CHUNK_Z));
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
            } else if (schema >= 2) {
                lastUpdate = uptimeTicks;
            }
            ChunkPressureKey key = new ChunkPressureKey(
                    stored.getInteger(TAG_DIMENSION),
                    stored.getInteger(TAG_CHUNK_X),
                    stored.getInteger(TAG_CHUNK_Z)
            );
            if (pressureByChunk.containsKey(key)) {
                LOGGER.warn("Skipping duplicate pressure record {} for {}", index, key);
                continue;
            }
            pressureByChunk.put(key, new ChunkPressureRecord(pressure, lastUpdate, nextVersion++));
        }
        if (recoveryConfigured) {
            rebuildCleanupQueue();
        }
    }

    @Override
    public NBTTagCompound writeToNBT(NBTTagCompound compound) {
        if (unsupportedFutureData != null) {
            return (NBTTagCompound) unsupportedFutureData.copy();
        }
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
        compactCleanupQueueIfNeeded();
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

    private void compactCleanupQueueIfNeeded() {
        long maximumUsefulSize = pressureByChunk.size() * 2L + QUEUE_STALE_ALLOWANCE;
        if (cleanupQueue.size() > maximumUsefulSize) rebuildCleanupQueue();
    }

    private void clampRecordsToMaximum() {
        for (Map.Entry<ChunkPressureKey, ChunkPressureRecord> entry : pressureByChunk.entrySet()) {
            ChunkPressureRecord record = entry.getValue();
            if (record.getPressure() > maximumPressure) {
                entry.setValue(new ChunkPressureRecord(maximumPressure, uptimeTicks, nextVersion++));
                markDirty();
            }
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

    private static boolean hasRequiredRecordFields(NBTTagCompound stored, int schema) {
        return stored.hasKey(TAG_DIMENSION, Constants.NBT.TAG_INT)
                && stored.hasKey(TAG_CHUNK_X, Constants.NBT.TAG_INT)
                && stored.hasKey(TAG_CHUNK_Z, Constants.NBT.TAG_INT)
                && stored.hasKey(TAG_PRESSURE, Constants.NBT.TAG_DOUBLE)
                && (schema == 1 || stored.hasKey(TAG_LAST_UPDATE, Constants.NBT.TAG_LONG));
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
