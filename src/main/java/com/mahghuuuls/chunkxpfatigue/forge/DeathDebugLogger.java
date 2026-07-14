package com.mahghuuuls.chunkxpfatigue.forge;

import com.mahghuuuls.chunkxpfatigue.ChunkXpFatigueMod;
import com.mahghuuuls.chunkxpfatigue.fatigue.FatigueCalculation;
import com.mahghuuuls.chunkxpfatigue.pressure.ChunkPressureKey;
import net.minecraft.entity.EntityList;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.util.ResourceLocation;

import java.util.Locale;

final class DeathDebugLogger {

    private final boolean enabled;
    private final Sink sink;

    DeathDebugLogger(boolean enabled) {
        this(enabled, new Sink() {
            @Override
            public void write(String line) {
                ChunkXpFatigueMod.LOGGER.info(line);
            }
        });
    }

    DeathDebugLogger(boolean enabled, Sink sink) {
        this.enabled = enabled;
        this.sink = sink;
    }

    void log(EntityLivingBase entity, ChunkPressureKey key, FatigueCalculation calculation) {
        if (!enabled) {
            return;
        }
        ResourceLocation registryName = EntityList.getKey(entity);
        String mobIdentifier = identifier(registryName, entity.getClass());
        log(mobIdentifier, key, calculation);
    }

    void log(String mobIdentifier, ChunkPressureKey key, FatigueCalculation calculation) {
        if (!enabled) {
            return;
        }
        sink.write(format(mobIdentifier, key, calculation));
    }

    static String identifier(ResourceLocation registryName, Class<?> entityClass) {
        if (registryName != null) {
            return registryName.toString();
        }
        String className = entityClass == null ? "unknown" : entityClass.getName();
        return "unregistered:" + className;
    }

    static String format(
            String mobIdentifier,
            ChunkPressureKey key,
            FatigueCalculation calculation
    ) {
        return String.format(
                Locale.ROOT,
                "XP fatigue mob=%s dimension=%d chunk=(%d,%d) inputXp=%d adjustedXp=%d "
                        + "pressureBefore=%.6f pressureAfter=%.6f multiplier=%.6f "
                        + "nearbyMobCount=%d crowdingMultiplier=%.6f "
                        + "effectivePressureGain=%.6f",
                mobIdentifier,
                key.getDimension(),
                key.getChunkX(),
                key.getChunkZ(),
                calculation.getInputXp(),
                calculation.getAdjustedXp(),
                calculation.getPressureBefore(),
                calculation.getPressureAfter(),
                calculation.getMultiplier(),
                calculation.getNearbyMobCount(),
                calculation.getCrowdingMultiplier(),
                calculation.getEffectivePressureGain()
        );
    }

    interface Sink {
        void write(String line);
    }
}
