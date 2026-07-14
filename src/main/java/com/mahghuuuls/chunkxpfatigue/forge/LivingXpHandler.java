package com.mahghuuuls.chunkxpfatigue.forge;

import com.mahghuuuls.chunkxpfatigue.config.ValidatedFatigueConfig;
import com.mahghuuuls.chunkxpfatigue.fatigue.FatigueCalculation;
import com.mahghuuuls.chunkxpfatigue.fatigue.XpFatigueService;
import com.mahghuuuls.chunkxpfatigue.pressure.ChunkPressureKey;
import com.mahghuuuls.chunkxpfatigue.pressure.PressureStore;
import com.mahghuuuls.chunkxpfatigue.pressure.PressureWorldData;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.World;
import net.minecraftforge.event.entity.living.LivingExperienceDropEvent;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

public final class LivingXpHandler {

    private final XpFatigueService fatigueService;
    private final double recoveryMinutesPerPressure;
    private final double maximumPressure;
    private final DeathDebugLogger debugLogger;
    private final CrowdingSnapshotCache crowdingSnapshots;

    public LivingXpHandler(ValidatedFatigueConfig config) {
        this(config, new CrowdingSnapshotCache());
    }

    public LivingXpHandler(
            ValidatedFatigueConfig config,
            CrowdingSnapshotCache crowdingSnapshots
    ) {
        this.fatigueService = new XpFatigueService(config);
        this.recoveryMinutesPerPressure = config.getRecoveryMinutesPerPressure();
        this.maximumPressure = config.getMaximumPressure();
        this.debugLogger = new DeathDebugLogger(config.isDebugLoggingEnabled());
        this.crowdingSnapshots = crowdingSnapshots;
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public void onLivingExperienceDrop(LivingExperienceDropEvent event) {
        EntityLivingBase entity = event.getEntityLiving();
        World world = entity.world;
        int payableXp = event.getDroppedExperience();
        if (!shouldProcess(world.isRemote, entity instanceof EntityPlayer, payableXp)) {
            return;
        }

        int nearbyMobCount = crowdingSnapshots.take(entity);
        ChunkPressureKey key = keyFor(entity);
        PressureStore store = PressureWorldData.get(world, recoveryMinutesPerPressure, maximumPressure);
        if (!store.isWritable()) {
            return;
        }
        FatigueCalculation calculation = apply(event, store, key, nearbyMobCount);
        debugLogger.log(entity, key, calculation);
    }

    FatigueCalculation apply(LivingExperienceDropEvent event, PressureStore store,
                             ChunkPressureKey key) {
        return apply(event, store, key, 0);
    }

    FatigueCalculation apply(
            LivingExperienceDropEvent event,
            PressureStore store,
            ChunkPressureKey key,
            int nearbyMobCount
    ) {
        FatigueCalculation calculation = fatigueService.process(
                store, key, event.getDroppedExperience(), nearbyMobCount);
        event.setDroppedExperience(calculation.getAdjustedXp());
        return calculation;
    }

    static boolean shouldProcess(boolean remoteWorld, boolean playerEntity, int payableXp) {
        return !remoteWorld && !playerEntity && payableXp > 0;
    }

    private static ChunkPressureKey keyFor(EntityLivingBase entity) {
        return new ChunkPressureKey(
                entity.dimension,
                MathHelper.floor(entity.posX) >> 4,
                MathHelper.floor(entity.posZ) >> 4
        );
    }
}
