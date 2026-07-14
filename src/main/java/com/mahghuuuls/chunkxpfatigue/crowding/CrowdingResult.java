package com.mahghuuuls.chunkxpfatigue.crowding;

public final class CrowdingResult {

    private final int nearbyMobCount;
    private final double multiplier;
    private final double effectivePressureGain;

    CrowdingResult(int nearbyMobCount, double multiplier, double effectivePressureGain) {
        this.nearbyMobCount = nearbyMobCount;
        this.multiplier = multiplier;
        this.effectivePressureGain = effectivePressureGain;
    }

    public int getNearbyMobCount() {
        return nearbyMobCount;
    }

    public double getMultiplier() {
        return multiplier;
    }

    public double getEffectivePressureGain() {
        return effectivePressureGain;
    }
}
