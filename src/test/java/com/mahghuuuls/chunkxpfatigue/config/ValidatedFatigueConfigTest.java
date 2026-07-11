package com.mahghuuuls.chunkxpfatigue.config;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ValidatedFatigueConfigTest {

    @Test
    void defaultValuesProduceApprovedCurve() {
        ValidatedFatigueConfig config = defaultConfig();

        assertEquals(1.0D, config.getXpCurve().multiplierAt(0.20D), 0.000001D);
        assertEquals(0.55D, config.getXpCurve().multiplierAt(0.60D), 0.000001D);
        assertEquals(0.10D, config.getXpCurve().multiplierAt(1.0D), 0.000001D);
        assertTrue(config.getWarnings().isEmpty());
        assertTrue(config.getErrors().isEmpty());
        assertFalse(config.isDebugOverlayEnabled());
        assertFalse(config.isDebugLoggingEnabled());
    }

    @Test
    void outOfRangeAndNonfiniteScalarsAreCorrectedWithWarnings() {
        ValidatedFatigueConfig config = ValidatedFatigueConfig.validate(
                -2.0D,
                Double.NaN,
                -1.0D,
                150.0D,
                -5.0D,
                new String[]{"100:100"},
                true,
                true
        );

        assertEquals(0.0D, config.getPressureGain(), 0.0D);
        assertEquals(ValidatedFatigueConfig.DEFAULT_MAXIMUM_PRESSURE, config.getMaximumPressure(), 0.0D);
        assertEquals(0.0D, config.getRecoveryMinutesPerPressure(), 0.0D);
        assertEquals(1.0D, config.getGraceThreshold(), 0.0D);
        assertEquals(0.0D, config.getMinimumMultiplier(), 0.0D);
        assertEquals(5, config.getWarnings().size());
        assertTrue(config.getErrors().isEmpty());
        assertTrue(config.isDebugOverlayEnabled());
        assertTrue(config.isDebugLoggingEnabled());
    }

    @Test
    void malformedCurveUsesLinearFallbackAndReportsError() {
        ValidatedFatigueConfig config = ValidatedFatigueConfig.validate(
                1.0D,
                100.0D,
                3.0D,
                40.0D,
                30.0D,
                new String[]{"80:40", "70:30"},
                false,
                false
        );

        assertEquals(1, config.getErrors().size());
        assertTrue(config.getErrors().get(0).contains("pressure points must increase strictly"));
        assertEquals(0.20D, config.getGraceThreshold(), 0.0D);
        assertEquals(0.10D, config.getMinimumMultiplier(), 0.0D);
        assertEquals(1.0D, config.getXpCurve().multiplierAt(0.20D), 0.000001D);
        assertEquals(0.55D, config.getXpCurve().multiplierAt(0.60D), 0.000001D);
        assertEquals(0.10D, config.getXpCurve().multiplierAt(1.0D), 0.000001D);
    }

    @Test
    void curveSupportsImplicitGraceAnchorAndMultipleSegments() {
        ValidatedFatigueConfig config = ValidatedFatigueConfig.validate(
                1.0D,
                100.0D,
                3.0D,
                20.0D,
                10.0D,
                new String[]{"60:70", "100:10"},
                false,
                false
        );

        assertTrue(config.getErrors().isEmpty());
        assertEquals(0.85D, config.getXpCurve().multiplierAt(0.40D), 0.000001D);
        assertEquals(0.40D, config.getXpCurve().multiplierAt(0.80D), 0.000001D);
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
}
