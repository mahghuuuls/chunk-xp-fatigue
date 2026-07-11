package com.mahghuuuls.chunkxpfatigue.config;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class XpCurveTest {

    @Test
    void rejectsDuplicateOutOfRangeAndBelowMinimumPoints() {
        assertThrows(IllegalArgumentException.class,
                () -> XpCurve.parse(new String[]{"20:100", "20:80", "100:10"}, 0.20D, 0.10D));
        assertThrows(IllegalArgumentException.class,
                () -> XpCurve.parse(new String[]{"20:100", "101:10"}, 0.20D, 0.10D));
        assertThrows(IllegalArgumentException.class,
                () -> XpCurve.parse(new String[]{"20:100", "100:9"}, 0.20D, 0.10D));
    }

    @Test
    void pressureOutsideNormalizedRangeIsClamped() {
        XpCurve curve = XpCurve.parse(new String[]{"20:100", "100:10"}, 0.20D, 0.10D);

        assertEquals(1.0D, curve.multiplierAt(-1.0D), 0.0D);
        assertEquals(0.10D, curve.multiplierAt(2.0D), 0.000001D);
    }
}
