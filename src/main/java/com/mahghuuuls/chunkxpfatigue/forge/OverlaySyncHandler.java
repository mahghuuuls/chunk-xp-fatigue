package com.mahghuuuls.chunkxpfatigue.forge;

import com.mahghuuuls.chunkxpfatigue.config.ValidatedFatigueConfig;
import com.mahghuuuls.chunkxpfatigue.network.ModNetwork;
import com.mahghuuuls.chunkxpfatigue.network.OverlaySnapshot;
import com.mahghuuuls.chunkxpfatigue.pressure.ChunkPressureKey;
import com.mahghuuuls.chunkxpfatigue.pressure.PressureWorldData;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.math.MathHelper;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

public final class OverlaySyncHandler {
    private final ValidatedFatigueConfig config;
    private final ModNetwork network;
    private int ticks;

    public OverlaySyncHandler(ValidatedFatigueConfig config, ModNetwork network) {
        this.config = config;
        this.network = network;
    }

    @SubscribeEvent
    public void serverTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || ++ticks < 20) return;
        ticks = 0;
        MinecraftServer server = FMLCommonHandler.instance().getMinecraftServerInstance();
        if (server == null) return;
        for (EntityPlayerMP player : server.getPlayerList().getPlayers()) {
            ChunkPressureKey key = new ChunkPressureKey(player.dimension,
                    MathHelper.floor(player.posX) >> 4, MathHelper.floor(player.posZ) >> 4);
            double pressure = PressureWorldData.get(player.world, config.getRecoveryMinutesPerPressure())
                    .getPressure(key);
            double normalized = pressure / config.getMaximumPressure();
            network.send(player, new OverlaySnapshot(normalized,
                    config.getXpCurve().multiplierAt(normalized)));
        }
    }
}
