package com.mahghuuuls.chunkxpfatigue.fatigue;

import com.mahghuuuls.chunkxpfatigue.config.ValidatedFatigueConfig;
import com.mahghuuuls.chunkxpfatigue.pressure.ChunkPressureKey;
import com.mahghuuuls.chunkxpfatigue.pressure.PressureStore;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class XpFatigueServiceTest {

    @Test
    void calculatesFromPreContributionPressureAndFloorsXp() {
        XpFatigueService service = new XpFatigueService(defaultConfig());

        FatigueCalculation result = service.calculate(7, 60.0D);

        assertEquals(7, result.getInputXp());
        assertEquals(3, result.getAdjustedXp());
        assertEquals(60.0D, result.getPressureBefore(), 0.0D);
        assertEquals(61.0D, result.getPressureAfter(), 0.0D);
        assertEquals(0.55D, result.getMultiplier(), 0.000001D);
    }

    @Test
    void capsPressureAndStillAddsItWhenXpFloorsToZero() {
        XpFatigueService service = new XpFatigueService(defaultConfig());

        FatigueCalculation result = service.calculate(1, 99.5D);

        assertEquals(0, result.getAdjustedXp());
        assertEquals(100.0D, result.getPressureAfter(), 0.0D);
    }

    @Test
    void firstDeathAtEmptyPressureGetsFullXp() {
        XpFatigueService service = new XpFatigueService(defaultConfig());

        FatigueCalculation result = service.calculate(5, 0.0D);

        assertEquals(5, result.getAdjustedXp());
        assertEquals(1.0D, result.getPressureAfter(), 0.0D);
    }

    @Test
    void sevenXpAtSixtyPercentMultiplierFloorsToFour() {
        ValidatedFatigueConfig config = ValidatedFatigueConfig.validate(
                1.0D,
                100.0D,
                3.0D,
                20.0D,
                10.0D,
                new String[]{"20:100", "100:20"},
                false,
                false
        );
        XpFatigueService service = new XpFatigueService(config);

        FatigueCalculation result = service.calculate(7, 60.0D);

        assertEquals(0.60D, result.getMultiplier(), 0.000001D);
        assertEquals(4, result.getAdjustedXp());
    }

    @Test
    void scalesCurrentPayableXpAfterCompatiblePriorModifiers() {
        ValidatedFatigueConfig config = ValidatedFatigueConfig.validate(
                1.0D,
                100.0D,
                3.0D,
                20.0D,
                0.0D,
                new String[]{"20:100", "100:0"},
                false,
                false
        );
        XpFatigueService service = new XpFatigueService(config);

        FatigueCalculation result = service.calculate(10, 60.0D);

        assertEquals(0.50D, result.getMultiplier(), 0.000001D);
        assertEquals(5, result.getAdjustedXp());
    }

    @Test
    void rejectsNegativePayableXp() {
        XpFatigueService service = new XpFatigueService(defaultConfig());
        assertThrows(IllegalArgumentException.class, () -> service.calculate(-1, 0.0D));
    }

    @Test
    void processReadsPreContributionPressureAndPersistsPostContributionPressure() {
        XpFatigueService service = new XpFatigueService(defaultConfig());
        InMemoryPressureStore store = new InMemoryPressureStore();
        ChunkPressureKey key = new ChunkPressureKey(0, 4, -2);
        store.setPressure(key, 60.0D);

        FatigueCalculation result = service.process(store, key, 7);

        assertEquals(60.0D, result.getPressureBefore(), 0.0D);
        assertEquals(61.0D, result.getPressureAfter(), 0.0D);
        assertEquals(61.0D, store.getPressure(key), 0.0D);
    }

    @Test
    void crowdingAcceleratesOnlyPostXpPressureGain() {
        XpFatigueService service = new XpFatigueService(defaultConfig());

        FatigueCalculation isolated = service.calculate(7, 60.0D, 0);
        FatigueCalculation crowded = service.calculate(7, 60.0D, 8);

        assertEquals(isolated.getAdjustedXp(), crowded.getAdjustedXp());
        assertEquals(isolated.getMultiplier(), crowded.getMultiplier(), 0.0D);
        assertEquals(61.0D, isolated.getPressureAfter(), 0.0D);
        assertEquals(62.0D, crowded.getPressureAfter(), 0.0D);
        assertEquals(8, crowded.getNearbyMobCount());
        assertEquals(2.0D, crowded.getCrowdingMultiplier(), 0.0D);
        assertEquals(2.0D, crowded.getEffectivePressureGain(), 0.0D);
    }

    @Test
    void crowdedGainStillObeysMaximumPressure() {
        XpFatigueService service = new XpFatigueService(defaultConfig());

        FatigueCalculation crowded = service.calculate(5, 99.0D, 16);

        assertEquals(100.0D, crowded.getPressureAfter(), 0.0D);
        assertEquals(4.0D, crowded.getCrowdingMultiplier(), 0.0D);
        assertEquals(4.0D, crowded.getEffectivePressureGain(), 0.0D);
    }

    @Test
    void processPersistsAcceleratedGainForNearbyCount() {
        XpFatigueService service = new XpFatigueService(defaultConfig());
        InMemoryPressureStore store = new InMemoryPressureStore();
        ChunkPressureKey key = new ChunkPressureKey(0, 4, -2);

        FatigueCalculation result = service.process(store, key, 5, 5);

        assertEquals(5, result.getAdjustedXp());
        assertEquals(1.25D, result.getPressureAfter(), 0.0D);
        assertEquals(1.25D, store.getPressure(key), 0.0D);
    }

    private static ValidatedFatigueConfig defaultConfig() {
        return ValidatedFatigueConfig.validate(
                1.0D,
                100.0D,
                3.0D,
                20.0D,
                10.0D,
                new String[]{"20:100", "100:10"},
                false,
                false
        );
    }

    private static final class InMemoryPressureStore implements PressureStore {

        private final Map<ChunkPressureKey, Double> values =
                new HashMap<ChunkPressureKey, Double>();

        @Override
        public double getPressure(ChunkPressureKey key) {
            Double value = values.get(key);
            return value == null ? 0.0D : value;
        }

        @Override
        public void setPressure(ChunkPressureKey key, double pressure) {
            values.put(key, pressure);
        }

        @Override
        public int clearDimension(int dimension) {
            throw new UnsupportedOperationException();
        }

        @Override
        public int clearAll() {
            int count = values.size();
            values.clear();
            return count;
        }
    }
}
