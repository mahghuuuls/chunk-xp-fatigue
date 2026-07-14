package com.mahghuuuls.chunkxpfatigue.fatigue;

import com.mahghuuuls.chunkxpfatigue.config.ValidatedFatigueConfig;
import com.mahghuuuls.chunkxpfatigue.crowding.CrowdingAcceleration;
import com.mahghuuuls.chunkxpfatigue.crowding.CrowdingResult;
import com.mahghuuuls.chunkxpfatigue.pressure.ChunkPressureKey;
import com.mahghuuuls.chunkxpfatigue.pressure.PressureStore;

public final class XpFatigueService {

    private final ValidatedFatigueConfig config;
    private final CrowdingAcceleration crowdingAcceleration;

    public XpFatigueService(ValidatedFatigueConfig config) {
        if (config == null) {
            throw new IllegalArgumentException("config cannot be null");
        }
        this.config = config;
        this.crowdingAcceleration = new CrowdingAcceleration(config);
    }

    public FatigueCalculation calculate(int payableXp, double currentPressure) {
        return calculate(payableXp, currentPressure, 0);
    }

    public FatigueCalculation calculate(
            int payableXp,
            double currentPressure,
            int nearbyMobCount
    ) {
        if (payableXp < 0) {
            throw new IllegalArgumentException("payableXp cannot be negative");
        }

        double pressureBefore = clamp(currentPressure, 0.0D, config.getMaximumPressure());
        double normalizedPressure = pressureBefore / config.getMaximumPressure();
        double multiplier = config.getXpCurve().multiplierAt(normalizedPressure);
        int adjustedXp = (int) Math.floor(payableXp * multiplier);
        CrowdingResult crowding = crowdingAcceleration.calculate(
                config.getPressureGain(), nearbyMobCount);
        double pressureAfter = Math.min(
                config.getMaximumPressure(),
                pressureBefore + crowding.getEffectivePressureGain()
        );
        return new FatigueCalculation(
                payableXp,
                adjustedXp,
                pressureBefore,
                pressureAfter,
                multiplier,
                nearbyMobCount,
                crowding.getMultiplier(),
                crowding.getEffectivePressureGain()
        );
    }

    public FatigueCalculation process(
            PressureStore store,
            ChunkPressureKey key,
            int payableXp
    ) {
        return process(store, key, payableXp, 0);
    }

    public FatigueCalculation process(
            PressureStore store,
            ChunkPressureKey key,
            int payableXp,
            int nearbyMobCount
    ) {
        if (store == null) {
            throw new IllegalArgumentException("store cannot be null");
        }
        if (key == null) {
            throw new IllegalArgumentException("key cannot be null");
        }
        FatigueCalculation calculation = calculate(
                payableXp, store.getPressure(key), nearbyMobCount);
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
