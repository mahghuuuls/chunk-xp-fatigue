package com.mahghuuuls.chunkxpfatigue.config;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class ValidatedFatigueConfig {

    public static final double DEFAULT_PRESSURE_GAIN = 1.0D;
    public static final double DEFAULT_MAXIMUM_PRESSURE = 100.0D;
    public static final double DEFAULT_RECOVERY_MINUTES_PER_PRESSURE = 3.0D;
    public static final double DEFAULT_GRACE_PERCENT = 20.0D;
    public static final double DEFAULT_MINIMUM_MULTIPLIER_PERCENT = 10.0D;

    private static final double MINIMUM_MAX_PRESSURE = 0.000001D;
    private static final double MAXIMUM_CONFIG_VALUE = 1_000_000_000.0D;

    private final double pressureGain;
    private final double maximumPressure;
    private final double recoveryMinutesPerPressure;
    private final double graceThreshold;
    private final double minimumMultiplier;
    private final XpCurve xpCurve;
    private final boolean debugOverlayEnabled;
    private final boolean debugLoggingEnabled;
    private final List<String> warnings;
    private final List<String> errors;

    private ValidatedFatigueConfig(
            double pressureGain,
            double maximumPressure,
            double recoveryMinutesPerPressure,
            double graceThreshold,
            double minimumMultiplier,
            XpCurve xpCurve,
            boolean debugOverlayEnabled,
            boolean debugLoggingEnabled,
            List<String> warnings,
            List<String> errors
    ) {
        this.pressureGain = pressureGain;
        this.maximumPressure = maximumPressure;
        this.recoveryMinutesPerPressure = recoveryMinutesPerPressure;
        this.graceThreshold = graceThreshold;
        this.minimumMultiplier = minimumMultiplier;
        this.xpCurve = xpCurve;
        this.debugOverlayEnabled = debugOverlayEnabled;
        this.debugLoggingEnabled = debugLoggingEnabled;
        this.warnings = Collections.unmodifiableList(new ArrayList<String>(warnings));
        this.errors = Collections.unmodifiableList(new ArrayList<String>(errors));
    }

    public static ValidatedFatigueConfig validate(
            double pressureGain,
            double maximumPressure,
            double recoveryMinutesPerPressure,
            double graceThresholdPercent,
            double minimumMultiplierPercent,
            String[] curvePoints,
            boolean debugOverlayEnabled,
            boolean debugLoggingEnabled
    ) {
        List<String> warnings = new ArrayList<String>();
        List<String> errors = new ArrayList<String>();

        double effectiveGain = clampFinite(
                "pressureGain", pressureGain, 0.0D, MAXIMUM_CONFIG_VALUE,
                DEFAULT_PRESSURE_GAIN, warnings
        );
        double effectiveMaximum = clampFinite(
                "maximumPressure", maximumPressure, MINIMUM_MAX_PRESSURE, MAXIMUM_CONFIG_VALUE,
                DEFAULT_MAXIMUM_PRESSURE, warnings
        );
        double effectiveRecovery = clampFinite(
                "recoveryMinutesPerPressure", recoveryMinutesPerPressure, 0.0D, MAXIMUM_CONFIG_VALUE,
                DEFAULT_RECOVERY_MINUTES_PER_PRESSURE, warnings
        );
        double effectiveGracePercent = clampFinite(
                "graceThresholdPercent", graceThresholdPercent, 0.0D, 100.0D,
                DEFAULT_GRACE_PERCENT, warnings
        );
        double effectiveMinimumPercent = clampFinite(
                "minimumMultiplierPercent", minimumMultiplierPercent, 0.0D, 100.0D,
                DEFAULT_MINIMUM_MULTIPLIER_PERCENT, warnings
        );

        double graceThreshold = effectiveGracePercent / 100.0D;
        double minimumMultiplier = effectiveMinimumPercent / 100.0D;
        XpCurve curve;
        try {
            curve = XpCurve.parse(curvePoints, graceThreshold, minimumMultiplier);
        } catch (IllegalArgumentException exception) {
            errors.add("Invalid curvePoints: " + exception.getMessage()
                    + "; using the built-in default grace, minimum, and curve");
            graceThreshold = DEFAULT_GRACE_PERCENT / 100.0D;
            minimumMultiplier = DEFAULT_MINIMUM_MULTIPLIER_PERCENT / 100.0D;
            curve = XpCurve.defaultCurve(graceThreshold, minimumMultiplier);
        }

        return new ValidatedFatigueConfig(
                effectiveGain,
                effectiveMaximum,
                effectiveRecovery,
                graceThreshold,
                minimumMultiplier,
                curve,
                debugOverlayEnabled,
                debugLoggingEnabled,
                warnings,
                errors
        );
    }

    private static double clampFinite(
            String name,
            double value,
            double minimum,
            double maximum,
            double fallback,
            List<String> warnings
    ) {
        if (Double.isNaN(value) || Double.isInfinite(value)) {
            warnings.add(name + " was not finite; using " + fallback);
            return fallback;
        }
        if (value < minimum) {
            warnings.add(name + " was below " + minimum + "; using " + minimum);
            return minimum;
        }
        if (value > maximum) {
            warnings.add(name + " was above " + maximum + "; using " + maximum);
            return maximum;
        }
        return value;
    }

    public double getPressureGain() {
        return pressureGain;
    }

    public double getMaximumPressure() {
        return maximumPressure;
    }

    public double getRecoveryMinutesPerPressure() {
        return recoveryMinutesPerPressure;
    }

    public double getGraceThreshold() {
        return graceThreshold;
    }

    public double getMinimumMultiplier() {
        return minimumMultiplier;
    }

    public XpCurve getXpCurve() {
        return xpCurve;
    }

    public boolean isDebugOverlayEnabled() {
        return debugOverlayEnabled;
    }

    public boolean isDebugLoggingEnabled() {
        return debugLoggingEnabled;
    }

    public List<String> getWarnings() {
        return warnings;
    }

    public List<String> getErrors() {
        return errors;
    }
}
