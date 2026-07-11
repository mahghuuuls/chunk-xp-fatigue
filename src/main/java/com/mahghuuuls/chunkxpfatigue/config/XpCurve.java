package com.mahghuuuls.chunkxpfatigue.config;

import java.util.ArrayList;
import java.util.List;

public final class XpCurve {

    private static final double EPSILON = 0.000000001D;

    private final double[] pressurePoints;
    private final double[] multiplierPoints;

    private XpCurve(double[] pressurePoints, double[] multiplierPoints) {
        this.pressurePoints = pressurePoints;
        this.multiplierPoints = multiplierPoints;
    }

    public static XpCurve parse(String[] rawPoints, double graceThreshold, double minimumMultiplier) {
        validateNormalized("grace threshold", graceThreshold);
        validateNormalized("minimum multiplier", minimumMultiplier);
        if (rawPoints == null || rawPoints.length == 0) {
            throw new IllegalArgumentException("at least one curve point is required");
        }

        List<Double> pressures = new ArrayList<Double>();
        List<Double> multipliers = new ArrayList<Double>();
        double previousPressure = -1.0D;

        for (String rawPoint : rawPoints) {
            if (rawPoint == null) {
                throw new IllegalArgumentException("curve points cannot be null");
            }
            String[] parts = rawPoint.trim().split(":", -1);
            if (parts.length != 2) {
                throw new IllegalArgumentException("point '" + rawPoint + "' must use pressure%:xp%");
            }

            double pressure = parsePercentage(parts[0], "pressure", rawPoint);
            double multiplier = parsePercentage(parts[1], "XP multiplier", rawPoint);
            if (pressure + EPSILON < graceThreshold) {
                throw new IllegalArgumentException("point '" + rawPoint + "' is below the grace threshold");
            }
            if (pressure <= previousPressure + EPSILON) {
                throw new IllegalArgumentException("pressure points must increase strictly");
            }
            if (multiplier + EPSILON < minimumMultiplier) {
                throw new IllegalArgumentException("point '" + rawPoint + "' is below the minimum multiplier");
            }
            if (Math.abs(pressure - graceThreshold) <= EPSILON
                    && Math.abs(multiplier - 1.0D) > EPSILON) {
                throw new IllegalArgumentException("a point at the grace threshold must use 100% XP");
            }

            pressures.add(pressure);
            multipliers.add(multiplier);
            previousPressure = pressure;
        }

        if (Math.abs(previousPressure - 1.0D) > EPSILON) {
            throw new IllegalArgumentException("the final pressure point must be 100%");
        }
        if (pressures.get(0) > graceThreshold + EPSILON) {
            pressures.add(0, graceThreshold);
            multipliers.add(0, 1.0D);
        }

        return new XpCurve(toPrimitive(pressures), toPrimitive(multipliers));
    }

    public static XpCurve defaultCurve(double graceThreshold, double minimumMultiplier) {
        validateNormalized("grace threshold", graceThreshold);
        validateNormalized("minimum multiplier", minimumMultiplier);
        if (Math.abs(graceThreshold - 1.0D) <= EPSILON) {
            return new XpCurve(new double[]{1.0D}, new double[]{1.0D});
        }
        return new XpCurve(
                new double[]{graceThreshold, 1.0D},
                new double[]{1.0D, minimumMultiplier}
        );
    }

    public double multiplierAt(double normalizedPressure) {
        double pressure = clamp(normalizedPressure, 0.0D, 1.0D);
        if (pressure <= pressurePoints[0]) {
            return multiplierPoints[0];
        }
        for (int index = 1; index < pressurePoints.length; index++) {
            if (pressure <= pressurePoints[index]) {
                double startPressure = pressurePoints[index - 1];
                double endPressure = pressurePoints[index];
                double fraction = (pressure - startPressure) / (endPressure - startPressure);
                return multiplierPoints[index - 1]
                        + fraction * (multiplierPoints[index] - multiplierPoints[index - 1]);
            }
        }
        return multiplierPoints[multiplierPoints.length - 1];
    }

    private static double parsePercentage(String raw, String label, String point) {
        final double percentage;
        try {
            percentage = Double.parseDouble(raw.trim());
        } catch (NumberFormatException exception) {
            throw new IllegalArgumentException(label + " in point '" + point + "' is not a number");
        }
        if (Double.isNaN(percentage) || Double.isInfinite(percentage)
                || percentage < 0.0D || percentage > 100.0D) {
            throw new IllegalArgumentException(label + " in point '" + point + "' must be from 0 to 100");
        }
        return percentage / 100.0D;
    }

    private static void validateNormalized(String label, double value) {
        if (Double.isNaN(value) || Double.isInfinite(value) || value < 0.0D || value > 1.0D) {
            throw new IllegalArgumentException(label + " must be from 0 to 1");
        }
    }

    private static double[] toPrimitive(List<Double> values) {
        double[] result = new double[values.size()];
        for (int index = 0; index < values.size(); index++) {
            result[index] = values.get(index);
        }
        return result;
    }

    private static double clamp(double value, double minimum, double maximum) {
        if (Double.isNaN(value)) {
            return minimum;
        }
        return Math.max(minimum, Math.min(maximum, value));
    }
}
