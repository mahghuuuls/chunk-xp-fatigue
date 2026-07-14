package com.mahghuuuls.chunkxpfatigue.forge;

import com.mahghuuuls.chunkxpfatigue.config.ValidatedFatigueConfig;
import net.minecraft.entity.EntityLiving;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.world.World;
import net.minecraftforge.event.entity.living.LivingDropsEvent;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

public final class CrowdingDeathHandler {

    private final boolean enabled;
    private final double radius;
    private final CrowdingSnapshotCache snapshots;
    private final CrowdingSampler sampler;

    public CrowdingDeathHandler(
            ValidatedFatigueConfig config,
            CrowdingSnapshotCache snapshots
    ) {
        this(config, snapshots, new CrowdingSampler());
    }

    CrowdingDeathHandler(
            ValidatedFatigueConfig config,
            CrowdingSnapshotCache snapshots,
            CrowdingSampler sampler
    ) {
        this.enabled = config.isCrowdingEnabled();
        this.radius = config.getCrowdingRadius();
        this.snapshots = snapshots;
        this.sampler = sampler;
    }

    @SubscribeEvent(priority = EventPriority.LOWEST, receiveCanceled = true)
    public void onLivingDrops(LivingDropsEvent event) {
        EntityLivingBase entity = event.getEntityLiving();
        World world = entity.world;
        if (!shouldSample(
                enabled,
                world.isRemote,
                entity instanceof EntityLiving,
                event.isRecentlyHit(),
                world.getGameRules().getBoolean("doMobLoot")
        )) {
            return;
        }

        EntityLiving dyingMob = (EntityLiving) entity;
        snapshots.put(dyingMob, sampler.countNearby(dyingMob, radius));
    }

    static boolean shouldSample(
            boolean enabled,
            boolean remoteWorld,
            boolean mobEntity,
            boolean recentlyHit,
            boolean doMobLoot
    ) {
        return enabled && !remoteWorld && mobEntity && recentlyHit && doMobLoot;
    }
}
