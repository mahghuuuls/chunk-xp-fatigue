package com.mahghuuuls.chunkxpfatigue.network;

public final class OverlaySnapshot {
    private final double normalizedPressure;
    private final double multiplier;

    public OverlaySnapshot(double normalizedPressure, double multiplier) {
        this.normalizedPressure = bounded(normalizedPressure);
        this.multiplier = bounded(multiplier);
    }

    public double getNormalizedPressure() { return normalizedPressure; }
    public double getMultiplier() { return multiplier; }

    public String displayText() {
        return "Chunk pressure: " + Math.round(normalizedPressure * 100.0D)
                + "% | XP multiplier: " + Math.round(multiplier * 100.0D) + "%";
    }

    private static double bounded(double value) {
        if (Double.isNaN(value) || Double.isInfinite(value)) return 0.0D;
        return Math.max(0.0D, Math.min(1.0D, value));
    }
}
