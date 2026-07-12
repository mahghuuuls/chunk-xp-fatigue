package com.mahghuuuls.chunkxpfatigue.proxy;

import com.mahghuuuls.chunkxpfatigue.client.ChunkPressureOverlay;
import com.mahghuuuls.chunkxpfatigue.client.ClientOverlayMessageHandler;
import com.mahghuuuls.chunkxpfatigue.client.ClientConnectionHandler;
import com.mahghuuuls.chunkxpfatigue.network.OverlaySnapshotMessage;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.FMLCommonHandler;

public final class ClientProxy extends CommonProxy {
    @Override public IMessageHandler<OverlaySnapshotMessage, IMessage> overlayHandler() {
        return new ClientOverlayMessageHandler();
    }
    @Override public void registerClientEvents() {
        MinecraftForge.EVENT_BUS.register(new ChunkPressureOverlay());
        FMLCommonHandler.instance().bus().register(new ClientConnectionHandler());
    }
}
