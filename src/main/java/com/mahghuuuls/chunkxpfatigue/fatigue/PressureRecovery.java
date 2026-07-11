package com.mahghuuuls.chunkxpfatigue.fatigue;

public final class PressureRecovery {

    public static final long SERVER_TICKS_PER_MINUTE = 20L * 60L;

    private PressureRecovery() {
    }

    public static double recover(
            double pressure,
            long elapsedServerTicks,
            double recoveryMinutesPerPressure
    ) {
        if (elapsedServerTicks < 0L) {
            throw new IllegalArgumentException("elapsedServerTicks cannot be negative");
        }
        double nonnegativePressure = Math.max(0.0D, pressure);
        if (recoveryMinutesPerPressure == 0.0D || elapsedServerTicks == 0L) {
            return nonnegativePressure;
        }
        if (Double.isNaN(recoveryMinutesPerPressure)
                || Double.isInfinite(recoveryMinutesPerPressure)
                || recoveryMinutesPerPressure < 0.0D) {
            throw new IllegalArgumentException("recoveryMinutesPerPressure must be finite and nonnegative");
        }

        double recoveredPressure = elapsedServerTicks
                / (recoveryMinutesPerPressure * SERVER_TICKS_PER_MINUTE);
        return Math.max(0.0D, nonnegativePressure - recoveredPressure);
    }
}
