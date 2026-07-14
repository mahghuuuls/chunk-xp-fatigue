package com.mahghuuuls.chunkxpfatigue.fatigue;

public final class FatigueCalculation {

    private final int inputXp;
    private final int adjustedXp;
    private final double pressureBefore;
    private final double pressureAfter;
    private final double multiplier;
    private final int nearbyMobCount;
    private final double crowdingMultiplier;
    private final double effectivePressureGain;

    public FatigueCalculation(
            int inputXp,
            int adjustedXp,
            double pressureBefore,
            double pressureAfter,
            double multiplier
    ) {
        this(
                inputXp,
                adjustedXp,
                pressureBefore,
                pressureAfter,
                multiplier,
                0,
                1.0D,
                pressureAfter - pressureBefore
        );
    }

    public FatigueCalculation(
            int inputXp,
            int adjustedXp,
            double pressureBefore,
            double pressureAfter,
            double multiplier,
            int nearbyMobCount,
            double crowdingMultiplier,
            double effectivePressureGain
    ) {
        this.inputXp = inputXp;
        this.adjustedXp = adjustedXp;
        this.pressureBefore = pressureBefore;
        this.pressureAfter = pressureAfter;
        this.multiplier = multiplier;
        this.nearbyMobCount = nearbyMobCount;
        this.crowdingMultiplier = crowdingMultiplier;
        this.effectivePressureGain = effectivePressureGain;
    }

    public int getInputXp() {
        return inputXp;
    }

    public int getAdjustedXp() {
        return adjustedXp;
    }

    public double getPressureBefore() {
        return pressureBefore;
    }

    public double getPressureAfter() {
        return pressureAfter;
    }

    public double getMultiplier() {
        return multiplier;
    }

    public int getNearbyMobCount() {
        return nearbyMobCount;
    }

    public double getCrowdingMultiplier() {
        return crowdingMultiplier;
    }

    public double getEffectivePressureGain() {
        return effectivePressureGain;
    }
}
