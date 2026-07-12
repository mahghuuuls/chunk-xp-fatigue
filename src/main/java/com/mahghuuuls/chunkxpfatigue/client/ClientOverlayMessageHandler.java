package com.mahghuuuls.chunkxpfatigue.client;

import com.mahghuuuls.chunkxpfatigue.network.OverlaySnapshotMessage;
import net.minecraft.client.Minecraft;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

public final class ClientOverlayMessageHandler implements IMessageHandler<OverlaySnapshotMessage, IMessage> {
    @Override public IMessage onMessage(final OverlaySnapshotMessage message, MessageContext ctx) {
        final long generation = ClientOverlayState.currentGeneration();
        Minecraft.getMinecraft().addScheduledTask(new Runnable() {
            @Override public void run() { ClientOverlayState.setIfCurrent(message.snapshot(), generation); }
        });
        return null;
    }
}
