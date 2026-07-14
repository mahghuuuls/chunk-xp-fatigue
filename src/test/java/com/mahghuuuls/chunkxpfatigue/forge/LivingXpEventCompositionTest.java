package com.mahghuuuls.chunkxpfatigue.forge;

import com.mahghuuuls.chunkxpfatigue.config.ValidatedFatigueConfig;
import com.mahghuuuls.chunkxpfatigue.pressure.ChunkPressureKey;
import com.mahghuuuls.chunkxpfatigue.pressure.PressureStore;
import net.minecraftforge.event.entity.living.LivingExperienceDropEvent;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class LivingXpEventCompositionTest {
    @Test
    void scalesCurrentPayableXpAfterCompatiblePriorHandler() {
        ValidatedFatigueConfig config = ValidatedFatigueConfig.validate(
                1.0D, 100.0D, 3.0D, 20.0D, 10.0D,
                new String[]{"20:100", "100:10"}, false, false);
        LivingXpHandler handler = new LivingXpHandler(config);
        MemoryStore store = new MemoryStore(60.0D);
        LivingExperienceDropEvent event = new LivingExperienceDropEvent(null, null, 20);

        event.setDroppedExperience(10); // representative compatible earlier modifier
        handler.apply(event, store, new ChunkPressureKey(0, 0, 0));

        assertEquals(5, event.getDroppedExperience());
        assertEquals(61.0D, store.pressure, 0.0D);
    }

    @Test
    void subscribesAtLowestForgePriority() throws NoSuchMethodException {
        SubscribeEvent subscription = LivingXpHandler.class
                .getDeclaredMethod("onLivingExperienceDrop", LivingExperienceDropEvent.class)
                .getAnnotation(SubscribeEvent.class);

        assertNotNull(subscription);
        assertEquals(EventPriority.LOWEST, subscription.priority());
    }

    @Test
    void crowdingChangesPressureGainWithoutChangingCurrentXpComposition() {
        ValidatedFatigueConfig config = ValidatedFatigueConfig.validate(
                1.0D, 100.0D, 3.0D, 20.0D, 10.0D,
                new String[]{"20:100", "100:10"}, false, false);
        LivingXpHandler handler = new LivingXpHandler(config);
        MemoryStore store = new MemoryStore(60.0D);
        LivingExperienceDropEvent event = new LivingExperienceDropEvent(null, null, 20);

        event.setDroppedExperience(10);
        handler.apply(event, store, new ChunkPressureKey(0, 0, 0), 8);

        assertEquals(5, event.getDroppedExperience());
        assertEquals(62.0D, store.pressure, 0.0D);
    }

    private static final class MemoryStore implements PressureStore {
        private double pressure;
        private MemoryStore(double pressure) { this.pressure = pressure; }
        @Override public double getPressure(ChunkPressureKey key) { return pressure; }
        @Override public void setPressure(ChunkPressureKey key, double value) { pressure = value; }
        @Override public int clearDimension(int dimension) { return 0; }
        @Override public int clearAll() { return 0; }
    }
}
