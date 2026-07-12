package com.mahghuuuls.chunkxpfatigue.forge;

import com.mahghuuuls.chunkxpfatigue.fatigue.FatigueCalculation;
import com.mahghuuuls.chunkxpfatigue.pressure.ChunkPressureKey;
import net.minecraft.util.ResourceLocation;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class DeathDebugLoggerTest {

    @Test
    void formatsEveryApprovedFieldFromAuthoritativeCalculation() {
        ChunkPressureKey key = new ChunkPressureKey(-1, 12, -7);
        FatigueCalculation calculation = new FatigueCalculation(
                10,
                5,
                50.0D,
                51.0D,
                0.5D
        );

        String line = DeathDebugLogger.format("minecraft:zombie", key, calculation);

        assertEquals(
                "XP fatigue mob=minecraft:zombie dimension=-1 chunk=(12,-7) "
                        + "inputXp=10 adjustedXp=5 pressureBefore=50.000000 "
                        + "pressureAfter=51.000000 multiplier=0.500000",
                line
        );
    }

    @Test
    void identifierUsesRegistryNameOrDefensiveClassFallback() {
        assertEquals(
                "minecraft:skeleton",
                DeathDebugLogger.identifier(new ResourceLocation("minecraft", "skeleton"), Object.class)
        );
        assertEquals(
                "unregistered:java.lang.Object",
                DeathDebugLogger.identifier(null, Object.class)
        );
        assertEquals("unregistered:unknown", DeathDebugLogger.identifier(null, null));
    }

    @Test
    void enabledWritesExactlyOnceAndDisabledWritesNothing() {
        CountingSink enabledSink = new CountingSink();
        CountingSink disabledSink = new CountingSink();
        ChunkPressureKey key = new ChunkPressureKey(0, 0, 0);
        FatigueCalculation calculation = new FatigueCalculation(5, 4, 20.0D, 21.0D, 0.8D);

        new DeathDebugLogger(true, enabledSink).log("minecraft:zombie", key, calculation);
        new DeathDebugLogger(false, disabledSink).log("minecraft:zombie", key, calculation);

        assertEquals(1, enabledSink.writeCount);
        assertEquals(0, disabledSink.writeCount);
    }

    private static final class CountingSink implements DeathDebugLogger.Sink {

        private int writeCount;

        @Override
        public void write(String line) {
            writeCount++;
        }
    }
}
