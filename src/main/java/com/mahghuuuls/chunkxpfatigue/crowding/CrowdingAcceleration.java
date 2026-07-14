package com.mahghuuuls.chunkxpfatigue.crowding;

import com.mahghuuuls.chunkxpfatigue.config.ValidatedFatigueConfig;

public final class CrowdingAcceleration {

    private final boolean enabled;
    private final int allowance;
    private final double bonusPerExcessMob;
    private final double maximumMultiplier;

    public CrowdingAcceleration(ValidatedFatigueConfig config) {
        this.enabled = config.isCrowdingEnabled();
        this.allowance = config.getCrowdingAllowance();
        this.bonusPerExcessMob = config.getCrowdingBonusPerExcessMob();
        this.maximumMultiplier = config.getMaximumCrowdingMultiplier();
    }

    public CrowdingResult calculate(double basePressureGain, int nearbyMobCount) {
        if (!Double.isFinite(basePressureGain) || basePressureGain < 0.0D) {
            throw new IllegalArgumentException("basePressureGain must be finite and nonnegative");
        }
        if (nearbyMobCount < 0) {
            throw new IllegalArgumentException("nearbyMobCount must be nonnegative");
        }

        double multiplier = multiplierFor(nearbyMobCount);
        double effectiveGain = basePressureGain * multiplier;
        if (!Double.isFinite(effectiveGain)) {
            throw new IllegalArgumentException("effective pressure gain must be finite");
        }
        return new CrowdingResult(nearbyMobCount, multiplier, effectiveGain);
    }

    private double multiplierFor(int nearbyMobCount) {
        if (!enabled || nearbyMobCount <= allowance || bonusPerExcessMob == 0.0D
                || maximumMultiplier == 1.0D) {
            return 1.0D;
        }

        long excessMobs = (long) nearbyMobCount - allowance;
        double progressiveMultiplier = 1.0D + excessMobs * bonusPerExcessMob;
        return Math.min(maximumMultiplier, progressiveMultiplier);
    }
}
