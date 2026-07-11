package com.mahghuuuls.chunkxpfatigue.fatigue;

public final class FatigueCalculation {

    private final int inputXp;
    private final int adjustedXp;
    private final double pressureBefore;
    private final double pressureAfter;
    private final double multiplier;

    public FatigueCalculation(
            int inputXp,
            int adjustedXp,
            double pressureBefore,
            double pressureAfter,
            double multiplier
    ) {
        this.inputXp = inputXp;
        this.adjustedXp = adjustedXp;
        this.pressureBefore = pressureBefore;
        this.pressureAfter = pressureAfter;
        this.multiplier = multiplier;
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
}
