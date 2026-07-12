package com.mahghuuuls.chunkxpfatigue.network;

import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

public final class NoOpOverlayMessageHandler implements IMessageHandler<OverlaySnapshotMessage, IMessage> {
    @Override public IMessage onMessage(OverlaySnapshotMessage message, MessageContext ctx) { return null; }
}
