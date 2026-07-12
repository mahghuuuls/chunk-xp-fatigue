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
    private final DeathDebugLogger debugLogger;

    public LivingXpHandler(ValidatedFatigueConfig config) {
        this.fatigueService = new XpFatigueService(config);
        this.recoveryMinutesPerPressure = config.getRecoveryMinutesPerPressure();
        this.debugLogger = new DeathDebugLogger(config.isDebugLoggingEnabled());
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public void onLivingExperienceDrop(LivingExperienceDropEvent event) {
        EntityLivingBase entity = event.getEntityLiving();
        World world = entity.world;
        int payableXp = event.getDroppedExperience();
        if (!shouldProcess(world.isRemote, entity instanceof EntityPlayer, payableXp)) {
            return;
        }

        ChunkPressureKey key = keyFor(entity);
        PressureStore store = PressureWorldData.get(world, recoveryMinutesPerPressure);
        FatigueCalculation calculation = fatigueService.process(store, key, payableXp);
        event.setDroppedExperience(calculation.getAdjustedXp());
        debugLogger.log(entity, key, calculation);
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
