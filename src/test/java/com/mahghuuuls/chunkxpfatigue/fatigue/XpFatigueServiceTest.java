package com.mahghuuuls.chunkxpfatigue.fatigue;

import com.mahghuuuls.chunkxpfatigue.config.ValidatedFatigueConfig;
import org.junit.jupiter.api.Test;

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
