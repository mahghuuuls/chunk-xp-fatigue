package com.mahghuuuls.chunkxpfatigue.forge;

import net.minecraftforge.event.entity.living.LivingDropsEvent;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CrowdingDeathHandlerTest {

    @Test
    void subscribesLastAndReceivesCanceledDropsEvents() throws NoSuchMethodException {
        SubscribeEvent subscription = CrowdingDeathHandler.class
                .getDeclaredMethod("onLivingDrops", LivingDropsEvent.class)
                .getAnnotation(SubscribeEvent.class);

        assertNotNull(subscription);
        assertEquals(EventPriority.LOWEST, subscription.priority());
        assertTrue(subscription.receiveCanceled());
    }

    @Test
    void rejectsEveryNoncandidateBeforeSampling() {
        assertTrue(CrowdingDeathHandler.shouldSample(true, false, true, true, true));
        assertFalse(CrowdingDeathHandler.shouldSample(false, false, true, true, true));
        assertFalse(CrowdingDeathHandler.shouldSample(true, true, true, true, true));
        assertFalse(CrowdingDeathHandler.shouldSample(true, false, false, true, true));
        assertFalse(CrowdingDeathHandler.shouldSample(true, false, true, false, true));
        assertFalse(CrowdingDeathHandler.shouldSample(true, false, true, true, false));
    }
}
