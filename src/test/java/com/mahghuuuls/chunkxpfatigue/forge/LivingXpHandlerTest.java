package com.mahghuuuls.chunkxpfatigue.forge;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LivingXpHandlerTest {

    @Test
    void processesOnlyPositiveServerSideNonPlayerXp() {
        assertTrue(LivingXpHandler.shouldProcess(false, false, 1));

        assertFalse(LivingXpHandler.shouldProcess(true, false, 1));
        assertFalse(LivingXpHandler.shouldProcess(false, true, 1));
        assertFalse(LivingXpHandler.shouldProcess(false, false, 0));
        assertFalse(LivingXpHandler.shouldProcess(false, false, -1));
    }
}
