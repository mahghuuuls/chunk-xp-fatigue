package com.mahghuuuls.chunkxpfatigue.crowding;

import com.mahghuuuls.chunkxpfatigue.config.ValidatedFatigueConfig;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class CrowdingAccelerationTest {

    @Test
    void defaultProgressionMatchesApprovedExamples() {
        CrowdingAcceleration acceleration = new CrowdingAcceleration(config(true, 4, 0.25D, 4.0D));

        assertResult(acceleration.calculate(1.0D, 0), 0, 1.0D, 1.0D);
        assertResult(acceleration.calculate(1.0D, 4), 4, 1.0D, 1.0D);
        assertResult(acceleration.calculate(1.0D, 5), 5, 1.25D, 1.25D);
        assertResult(acceleration.calculate(1.0D, 8), 8, 2.0D, 2.0D);
        assertResult(acceleration.calculate(1.0D, 16), 16, 4.0D, 4.0D);
        assertResult(acceleration.calculate(1.0D, 1000), 1000, 4.0D, 4.0D);
    }

    @Test
    void fractionalBaseGainAndBonusRemainPrecise() {
        CrowdingAcceleration acceleration = new CrowdingAcceleration(config(true, 2, 0.375D, 6.5D));

        assertResult(acceleration.calculate(1.5D, 4), 4, 1.75D, 2.625D);
    }

    @Test
    void disabledZeroBonusAndOneTimesCapReturnBaseGain() {
        assertResult(new CrowdingAcceleration(config(false, 0, 10.0D, 100.0D))
                .calculate(2.5D, 100), 100, 1.0D, 2.5D);
        assertResult(new CrowdingAcceleration(config(true, 0, 0.0D, 100.0D))
                .calculate(2.5D, 100), 100, 1.0D, 2.5D);
        assertResult(new CrowdingAcceleration(config(true, 0, 10.0D, 1.0D))
                .calculate(2.5D, 100), 100, 1.0D, 2.5D);
    }

    @Test
    void zeroAndMaximumApprovedGainStayFinite() {
        CrowdingAcceleration acceleration = new CrowdingAcceleration(config(true, 0, 10.0D, 100.0D));

        assertResult(acceleration.calculate(0.0D, Integer.MAX_VALUE),
                Integer.MAX_VALUE, 100.0D, 0.0D);
        assertResult(acceleration.calculate(1_000_000_000.0D, Integer.MAX_VALUE),
                Integer.MAX_VALUE, 100.0D, 100_000_000_000.0D);
    }

    @Test
    void invalidRuntimeInputsAreRejected() {
        CrowdingAcceleration acceleration = new CrowdingAcceleration(config(true, 4, 0.25D, 4.0D));

        assertThrows(IllegalArgumentException.class,
                () -> acceleration.calculate(-0.1D, 0));
        assertThrows(IllegalArgumentException.class,
                () -> acceleration.calculate(Double.NaN, 0));
        assertThrows(IllegalArgumentException.class,
                () -> acceleration.calculate(Double.POSITIVE_INFINITY, 0));
        assertThrows(IllegalArgumentException.class,
                () -> acceleration.calculate(1.0D, -1));
    }

    private static ValidatedFatigueConfig config(
            boolean enabled,
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
                enabled,
                3.0D,
                allowance,
                bonus,
                maximumMultiplier,
                false
        );
    }

    private static void assertResult(
            CrowdingResult result,
            int expectedCount,
            double expectedMultiplier,
            double expectedGain
    ) {
        assertEquals(expectedCount, result.getNearbyMobCount());
        assertEquals(expectedMultiplier, result.getMultiplier(), 0.000001D);
        assertEquals(expectedGain, result.getEffectivePressureGain(), 0.000001D);
    }
}
