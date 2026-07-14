package com.mahghuuuls.chunkxpfatigue.forge;

import net.minecraft.entity.EntityLiving;
import net.minecraft.util.math.AxisAlignedBB;

import java.util.List;

final class CrowdingSampler {

    private static final double COARSE_QUERY_MARGIN = 1.0E-7D;

    int countNearby(EntityLiving dyingMob, double radius) {
        AxisAlignedBB searchBox = searchBox(
                dyingMob.posX, dyingMob.posY, dyingMob.posZ, radius);
        List<EntityLiving> candidates = dyingMob.world.getEntitiesWithinAABB(
                EntityLiving.class, searchBox);
        int count = 0;
        for (EntityLiving candidate : candidates) {
            if (shouldCount(
                    candidate == dyingMob,
                    candidate.isEntityAlive(),
                    isWithinRadius(dyingMob, candidate, radius)
            )) {
                ++count;
            }
        }
        return count;
    }

    static AxisAlignedBB searchBox(double x, double y, double z, double radius) {
        return new AxisAlignedBB(
                x - radius,
                y - radius,
                z - radius,
                x + radius,
                y + radius,
                z + radius
        ).grow(COARSE_QUERY_MARGIN);
    }

    static boolean shouldCount(boolean dyingMobItself, boolean live, boolean withinRadius) {
        return !dyingMobItself && live && withinRadius;
    }

    static boolean isWithinRadius(EntityLiving source, EntityLiving candidate, double radius) {
        return isWithinRadius(
                source.posX,
                source.posY,
                source.posZ,
                candidate.posX,
                candidate.posY,
                candidate.posZ,
                radius
        );
    }

    static boolean isWithinRadius(
            double sourceX,
            double sourceY,
            double sourceZ,
            double candidateX,
            double candidateY,
            double candidateZ,
            double radius
    ) {
        double deltaX = candidateX - sourceX;
        double deltaY = candidateY - sourceY;
        double deltaZ = candidateZ - sourceZ;
        return deltaX * deltaX + deltaY * deltaY + deltaZ * deltaZ <= radius * radius;
    }
}
