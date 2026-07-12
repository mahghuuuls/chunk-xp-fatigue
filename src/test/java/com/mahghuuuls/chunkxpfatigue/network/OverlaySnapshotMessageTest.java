package com.mahghuuuls.chunkxpfatigue.network;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class OverlaySnapshotMessageTest {
    @Test
    void roundTripPreservesSnapshotAndFormatsRoundedText() {
        OverlaySnapshotMessage original = new OverlaySnapshotMessage(new OverlaySnapshot(0.416D, 0.604D));
        ByteBuf buffer = Unpooled.buffer();
        original.toBytes(buffer);
        OverlaySnapshotMessage restored = new OverlaySnapshotMessage();
        restored.fromBytes(buffer);

        assertEquals(0.416D, restored.snapshot().getNormalizedPressure(), 0.0D);
        assertEquals(0.604D, restored.snapshot().getMultiplier(), 0.0D);
        assertEquals("Chunk pressure: 42% | XP multiplier: 60%", restored.snapshot().displayText());
    }

    @Test
    void snapshotBoundsInvalidWireValues() {
        assertEquals(0.0D, new OverlaySnapshot(Double.NaN, -1.0D).getNormalizedPressure(), 0.0D);
        assertEquals(1.0D, new OverlaySnapshot(2.0D, Double.POSITIVE_INFINITY).getNormalizedPressure(), 0.0D);
    }
}
