package com.mahghuuuls.chunkxpfatigue.forge;

import com.mahghuuuls.chunkxpfatigue.config.ValidatedFatigueConfig;
import com.mahghuuuls.chunkxpfatigue.pressure.PressureWorldData;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.WorldServer;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

public final class CommonEventHandler {

    private final double recoveryMinutesPerPressure;

    public CommonEventHandler(ValidatedFatigueConfig config) {
        this.recoveryMinutesPerPressure = config.getRecoveryMinutesPerPressure();
    }

    @SubscribeEvent
    public void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }
        MinecraftServer server = FMLCommonHandler.instance().getMinecraftServerInstance();
        if (server == null) {
            return;
        }
        WorldServer overworld = server.getWorld(0);
        if (overworld != null) {
            PressureWorldData.get(overworld, recoveryMinutesPerPressure).advanceServerTick();
        }
    }
}
