package com.mahghuuuls.chunkxpfatigue.network;

import io.netty.buffer.ByteBuf;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;

public final class OverlaySnapshotMessage implements IMessage {
    private double normalizedPressure;
    private double multiplier;

    public OverlaySnapshotMessage() { }
    public OverlaySnapshotMessage(OverlaySnapshot snapshot) {
        normalizedPressure = snapshot.getNormalizedPressure();
        multiplier = snapshot.getMultiplier();
    }

    public OverlaySnapshot snapshot() { return new OverlaySnapshot(normalizedPressure, multiplier); }
    @Override public void fromBytes(ByteBuf buf) { normalizedPressure = buf.readDouble(); multiplier = buf.readDouble(); }
    @Override public void toBytes(ByteBuf buf) { buf.writeDouble(normalizedPressure); buf.writeDouble(multiplier); }
}
