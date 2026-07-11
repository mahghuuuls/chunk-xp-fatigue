package com.mahghuuuls.chunkxpfatigue.fatigue;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class PressureRecoveryTest {

    @Test
    void defaultRecoveryTakesFourHoursToGraceAndFiveToZero() {
        long fourHours = 4L * 60L * PressureRecovery.SERVER_TICKS_PER_MINUTE;
        long fiveHours = 5L * 60L * PressureRecovery.SERVER_TICKS_PER_MINUTE;

        assertEquals(20.0D, PressureRecovery.recover(100.0D, fourHours, 3.0D), 0.000001D);
        assertEquals(0.0D, PressureRecovery.recover(100.0D, fiveHours, 3.0D), 0.000001D);
    }

    @Test
    void zeroRecoveryRateKeepsPressureUnchanged() {
        assertEquals(42.0D, PressureRecovery.recover(42.0D, Long.MAX_VALUE, 0.0D), 0.0D);
    }

    @Test
    void negativeElapsedTicksAreRejected() {
        assertThrows(IllegalArgumentException.class, () -> PressureRecovery.recover(10.0D, -1L, 3.0D));
    }
}
