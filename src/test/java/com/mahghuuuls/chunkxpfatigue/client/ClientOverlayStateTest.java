package com.mahghuuuls.chunkxpfatigue.client;

import com.mahghuuuls.chunkxpfatigue.network.OverlaySnapshot;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ClientOverlayStateTest {
    @Test
    void queuedSnapshotFromOldSessionIsRejected() {
        long oldSession = ClientOverlayState.beginSession();
        ClientOverlayState.endSession();
        long newSession = ClientOverlayState.beginSession();

        assertFalse(ClientOverlayState.setIfCurrent(new OverlaySnapshot(1.0D, 0.1D), oldSession));
        assertNull(ClientOverlayState.get());
        assertTrue(ClientOverlayState.setIfCurrent(new OverlaySnapshot(0.5D, 0.6D), newSession));
    }
}
