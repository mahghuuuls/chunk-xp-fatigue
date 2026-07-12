package com.mahghuuuls.chunkxpfatigue.proxy;

import com.mahghuuuls.chunkxpfatigue.network.NoOpOverlayMessageHandler;
import com.mahghuuuls.chunkxpfatigue.network.OverlaySnapshotMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;

public class CommonProxy {
    public IMessageHandler<OverlaySnapshotMessage, IMessage> overlayHandler() {
        return new NoOpOverlayMessageHandler();
    }
    public void registerClientEvents() { }
}
