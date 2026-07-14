package com.mahghuuuls.chunkxpfatigue.config;

import com.mahghuuuls.chunkxpfatigue.Tags;
import net.minecraftforge.common.config.Config;

@Config(modid = Tags.MOD_ID)
public final class FatigueConfig {

    @Config.Comment("Pressure added after each qualifying mob death.")
    @Config.RequiresWorldRestart
    public static double pressureGain = ValidatedFatigueConfig.DEFAULT_PRESSURE_GAIN;

    @Config.Comment("Maximum raw pressure stored for one chunk. Must be greater than zero.")
    @Config.RequiresWorldRestart
    public static double maximumPressure = ValidatedFatigueConfig.DEFAULT_MAXIMUM_PRESSURE;

    @Config.Comment("Server-uptime minutes required to recover one pressure. Zero disables recovery.")
    @Config.RequiresWorldRestart
    public static double recoveryMinutesPerPressure =
            ValidatedFatigueConfig.DEFAULT_RECOVERY_MINUTES_PER_PRESSURE;

    @Config.Comment("Pressure percentage that retains full XP, from 0 through 100.")
    @Config.RequiresWorldRestart
    public static double graceThresholdPercent = ValidatedFatigueConfig.DEFAULT_GRACE_PERCENT;

    @Config.Comment("Minimum XP multiplier percentage, from 0 through 100.")
    @Config.RequiresWorldRestart
    public static double minimumMultiplierPercent =
            ValidatedFatigueConfig.DEFAULT_MINIMUM_MULTIPLIER_PERCENT;

    @Config.Comment({
            "Piecewise-linear pressure-to-XP points formatted as pressure%:xp%.",
            "Pressure points must increase strictly and the final point must be at 100%."
    })
    @Config.RequiresWorldRestart
    public static String[] curvePoints = {"20:100", "100:10"};

    @Config.Comment("Accelerate pressure gain when qualifying mobs die near crowded live mobs.")
    @Config.RequiresWorldRestart
    public static boolean crowdingEnabled = ValidatedFatigueConfig.DEFAULT_CROWDING_ENABLED;

    @Config.Comment("Three-dimensional crowding radius in blocks, from 0.5 through 8.0.")
    @Config.RequiresWorldRestart
    public static double crowdingRadius = ValidatedFatigueConfig.DEFAULT_CROWDING_RADIUS;

    @Config.Comment("Nearby live mobs allowed before crowding accelerates pressure, from 0 through 100.")
    @Config.RequiresWorldRestart
    public static int crowdingAllowance = ValidatedFatigueConfig.DEFAULT_CROWDING_ALLOWANCE;

    @Config.Comment("Additional base-gain multiplier per nearby mob above the allowance, from 0 through 10.")
    @Config.RequiresWorldRestart
    public static double crowdingBonusPerExcessMob =
            ValidatedFatigueConfig.DEFAULT_CROWDING_BONUS_PER_EXCESS_MOB;

    @Config.Comment("Maximum crowding multiplier applied to base pressure gain, from 1 through 100.")
    @Config.RequiresWorldRestart
    public static double maximumCrowdingMultiplier =
            ValidatedFatigueConfig.DEFAULT_MAXIMUM_CROWDING_MULTIPLIER;

    @Config.Comment("Show current chunk pressure and XP multiplier to compatible modded clients.")
    @Config.RequiresWorldRestart
    public static boolean debugOverlayEnabled = false;

    @Config.Comment("Log qualifying mob-death XP and pressure calculations on the server.")
    @Config.RequiresWorldRestart
    public static boolean debugLoggingEnabled = false;

    private FatigueConfig() {
    }

    public static ValidatedFatigueConfig validate() {
        return ValidatedFatigueConfig.validate(
                pressureGain,
                maximumPressure,
                recoveryMinutesPerPressure,
                graceThresholdPercent,
                minimumMultiplierPercent,
                curvePoints,
                crowdingEnabled,
                crowdingRadius,
                crowdingAllowance,
                crowdingBonusPerExcessMob,
                maximumCrowdingMultiplier,
                debugOverlayEnabled,
                debugLoggingEnabled
        );
    }
}
