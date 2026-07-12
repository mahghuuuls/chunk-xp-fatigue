package com.mahghuuuls.chunkxpfatigue.fatigue;

import com.mahghuuuls.chunkxpfatigue.config.ValidatedFatigueConfig;
import com.mahghuuuls.chunkxpfatigue.pressure.ChunkPressureKey;
import com.mahghuuuls.chunkxpfatigue.pressure.PressureStore;

public final class XpFatigueService {

    private final ValidatedFatigueConfig config;

    public XpFatigueService(ValidatedFatigueConfig config) {
        if (config == null) {
            throw new IllegalArgumentException("config cannot be null");
        }
        this.config = config;
    }

    public FatigueCalculation calculate(int payableXp, double currentPressure) {
        if (payableXp < 0) {
            throw new IllegalArgumentException("payableXp cannot be negative");
        }

        double pressureBefore = clamp(currentPressure, 0.0D, config.getMaximumPressure());
        double normalizedPressure = pressureBefore / config.getMaximumPressure();
        double multiplier = config.getXpCurve().multiplierAt(normalizedPressure);
        int adjustedXp = (int) Math.floor(payableXp * multiplier);
        double pressureAfter = Math.min(
                config.getMaximumPressure(),
                pressureBefore + config.getPressureGain()
        );
        return new FatigueCalculation(
                payableXp,
                adjustedXp,
                pressureBefore,
                pressureAfter,
                multiplier
        );
    }

    public FatigueCalculation process(
            PressureStore store,
            ChunkPressureKey key,
            int payableXp
    ) {
        if (store == null) {
            throw new IllegalArgumentException("store cannot be null");
        }
        if (key == null) {
            throw new IllegalArgumentException("key cannot be null");
        }
        FatigueCalculation calculation = calculate(payableXp, store.getPressure(key));
        store.setPressure(key, calculation.getPressureAfter());
        return calculation;
    }

    private static double clamp(double value, double minimum, double maximum) {
        if (Double.isNaN(value)) {
            return minimum;
        }
        return Math.max(minimum, Math.min(maximum, value));
    }
}
