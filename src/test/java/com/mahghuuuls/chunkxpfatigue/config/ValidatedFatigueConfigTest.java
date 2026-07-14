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
        assertTrue(config.isCrowdingEnabled());
        assertEquals(3.0D, config.getCrowdingRadius(), 0.0D);
        assertEquals(4, config.getCrowdingAllowance());
        assertEquals(0.25D, config.getCrowdingBonusPerExcessMob(), 0.0D);
        assertEquals(4.0D, config.getMaximumCrowdingMultiplier(), 0.0D);
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

    @Test
    void validFractionalCrowdingSettingsArePreserved() {
        ValidatedFatigueConfig config = ValidatedFatigueConfig.validate(
                1.0D,
                100.0D,
                3.0D,
                20.0D,
                10.0D,
                new String[]{"20:100", "100:10"},
                false,
                2.75D,
                7,
                0.375D,
                6.5D,
                false,
                false
        );

        assertFalse(config.isCrowdingEnabled());
        assertEquals(2.75D, config.getCrowdingRadius(), 0.0D);
        assertEquals(7, config.getCrowdingAllowance());
        assertEquals(0.375D, config.getCrowdingBonusPerExcessMob(), 0.0D);
        assertEquals(6.5D, config.getMaximumCrowdingMultiplier(), 0.0D);
        assertTrue(config.getWarnings().isEmpty());
    }

    @Test
    void invalidCrowdingSettingsClampOrFallbackWithIdentifyingWarnings() {
        ValidatedFatigueConfig config = ValidatedFatigueConfig.validate(
                1.0D,
                100.0D,
                3.0D,
                20.0D,
                10.0D,
                new String[]{"20:100", "100:10"},
                true,
                Double.NaN,
                101,
                -0.5D,
                Double.POSITIVE_INFINITY,
                false,
                false
        );

        assertEquals(ValidatedFatigueConfig.DEFAULT_CROWDING_RADIUS,
                config.getCrowdingRadius(), 0.0D);
        assertEquals(ValidatedFatigueConfig.MAXIMUM_CROWDING_ALLOWANCE,
                config.getCrowdingAllowance());
        assertEquals(ValidatedFatigueConfig.MINIMUM_CROWDING_BONUS_PER_EXCESS_MOB,
                config.getCrowdingBonusPerExcessMob(), 0.0D);
        assertEquals(ValidatedFatigueConfig.DEFAULT_MAXIMUM_CROWDING_MULTIPLIER,
                config.getMaximumCrowdingMultiplier(), 0.0D);
        assertEquals(4, config.getWarnings().size());
        assertTrue(config.getWarnings().get(0).contains("crowdingRadius"));
        assertTrue(config.getWarnings().get(1).contains("crowdingAllowance"));
        assertTrue(config.getWarnings().get(2).contains("crowdingBonusPerExcessMob"));
        assertTrue(config.getWarnings().get(3).contains("maximumCrowdingMultiplier"));
    }

    @Test
    void crowdingSettingsBelowMinimumClampToMinimum() {
        ValidatedFatigueConfig config = crowdingConfig(
                0.49D,
                -1,
                -0.01D,
                0.99D
        );

        assertEquals(ValidatedFatigueConfig.MINIMUM_CROWDING_RADIUS,
                config.getCrowdingRadius(), 0.0D);
        assertEquals(ValidatedFatigueConfig.MINIMUM_CROWDING_ALLOWANCE,
                config.getCrowdingAllowance());
        assertEquals(ValidatedFatigueConfig.MINIMUM_CROWDING_BONUS_PER_EXCESS_MOB,
                config.getCrowdingBonusPerExcessMob(), 0.0D);
        assertEquals(ValidatedFatigueConfig.MINIMUM_CROWDING_MULTIPLIER,
                config.getMaximumCrowdingMultiplier(), 0.0D);
        assertEquals(4, config.getWarnings().size());
    }

    @Test
    void crowdingSettingsAboveMaximumClampToMaximum() {
        ValidatedFatigueConfig config = crowdingConfig(
                8.01D,
                101,
                10.01D,
                100.01D
        );

        assertEquals(ValidatedFatigueConfig.MAXIMUM_CROWDING_RADIUS,
                config.getCrowdingRadius(), 0.0D);
        assertEquals(ValidatedFatigueConfig.MAXIMUM_CROWDING_ALLOWANCE,
                config.getCrowdingAllowance());
        assertEquals(ValidatedFatigueConfig.MAXIMUM_CROWDING_BONUS_PER_EXCESS_MOB,
                config.getCrowdingBonusPerExcessMob(), 0.0D);
        assertEquals(ValidatedFatigueConfig.MAXIMUM_CROWDING_MULTIPLIER,
                config.getMaximumCrowdingMultiplier(), 0.0D);
        assertEquals(4, config.getWarnings().size());
    }

    @Test
    void exactCrowdingEndpointsAreAcceptedWithoutWarnings() {
        ValidatedFatigueConfig minimum = crowdingConfig(
                0.5D,
                0,
                0.0D,
                1.0D
        );
        ValidatedFatigueConfig maximum = crowdingConfig(
                8.0D,
                100,
                10.0D,
                100.0D
        );

        assertTrue(minimum.getWarnings().isEmpty());
        assertEquals(0.5D, minimum.getCrowdingRadius(), 0.0D);
        assertEquals(0, minimum.getCrowdingAllowance());
        assertEquals(0.0D, minimum.getCrowdingBonusPerExcessMob(), 0.0D);
        assertEquals(1.0D, minimum.getMaximumCrowdingMultiplier(), 0.0D);

        assertTrue(maximum.getWarnings().isEmpty());
        assertEquals(8.0D, maximum.getCrowdingRadius(), 0.0D);
        assertEquals(100, maximum.getCrowdingAllowance());
        assertEquals(10.0D, maximum.getCrowdingBonusPerExcessMob(), 0.0D);
        assertEquals(100.0D, maximum.getMaximumCrowdingMultiplier(), 0.0D);
    }

    @Test
    void everyNonfiniteCrowdingDecimalUsesItsNamedDefault() {
        double[] nonfiniteValues = {
                Double.NaN,
                Double.NEGATIVE_INFINITY,
                Double.POSITIVE_INFINITY
        };

        for (double value : nonfiniteValues) {
            ValidatedFatigueConfig radius = crowdingConfig(value, 4, 0.25D, 4.0D);
            assertEquals(3.0D, radius.getCrowdingRadius(), 0.0D);
            assertEquals("crowdingRadius was not finite; using 3.0",
                    radius.getWarnings().get(0));

            ValidatedFatigueConfig bonus = crowdingConfig(3.0D, 4, value, 4.0D);
            assertEquals(0.25D, bonus.getCrowdingBonusPerExcessMob(), 0.0D);
            assertEquals("crowdingBonusPerExcessMob was not finite; using 0.25",
                    bonus.getWarnings().get(0));

            ValidatedFatigueConfig maximum = crowdingConfig(3.0D, 4, 0.25D, value);
            assertEquals(4.0D, maximum.getMaximumCrowdingMultiplier(), 0.0D);
            assertEquals("maximumCrowdingMultiplier was not finite; using 4.0",
                    maximum.getWarnings().get(0));
        }
    }

    private static ValidatedFatigueConfig crowdingConfig(
            double radius,
            int allowance,
            double bonus,
            double maximumMultiplier
    ) {
        return ValidatedFatigueConfig.validate(
                1.0D,
                100.0D,
                3.0D,
                20.0D,
                10.0D,
                new String[]{"20:100", "100:10"},
                true,
                radius,
                allowance,
                bonus,
                maximumMultiplier,
                false,
                false
        );
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
