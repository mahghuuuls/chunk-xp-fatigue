package com.mahghuuuls.chunkxpfatigue.network;

import com.mahghuuuls.chunkxpfatigue.Tags;
import io.netty.channel.ChannelHandlerContext;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.network.NetworkManager;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.network.FMLNetworkEvent;
import net.minecraftforge.fml.common.network.NetworkRegistry;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.SimpleNetworkWrapper;
import net.minecraftforge.fml.relauncher.Side;

import java.util.Collections;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public final class ModNetwork {
    public static final String CHANNEL_NAME = Tags.MOD_ID;
    private final SimpleNetworkWrapper channel = NetworkRegistry.INSTANCE.newSimpleChannel(CHANNEL_NAME);
    private final Set<NetworkManager> compatibleClients = Collections.newSetFromMap(
            new ConcurrentHashMap<NetworkManager, Boolean>());

    public ModNetwork(IMessageHandler<OverlaySnapshotMessage, IMessage> clientHandler) {
        channel.registerMessage(clientHandler, OverlaySnapshotMessage.class, 0, Side.CLIENT);
        FMLCommonHandler.instance().bus().register(this);
    }

    public void send(EntityPlayerMP player, OverlaySnapshot snapshot) {
        if (compatibleClients.contains(player.connection.netManager)) {
            channel.sendTo(new OverlaySnapshotMessage(snapshot), player);
        }
    }

    @SubscribeEvent
    public void registration(FMLNetworkEvent.CustomPacketRegistrationEvent<?> event) {
        if (event.getSide() != Side.SERVER) return;
        NetworkManager manager = event.getManager();
        if ("REGISTER".equals(event.getOperation()) && event.getRegistrations().contains(CHANNEL_NAME)) {
            compatibleClients.add(manager);
        } else if ("UNREGISTER".equals(event.getOperation())
                && event.getRegistrations().contains(CHANNEL_NAME)) {
            compatibleClients.remove(manager);
        }
    }

    @SubscribeEvent
    public void disconnected(FMLNetworkEvent.ServerDisconnectionFromClientEvent event) {
        compatibleClients.remove(event.getManager());
    }
}
