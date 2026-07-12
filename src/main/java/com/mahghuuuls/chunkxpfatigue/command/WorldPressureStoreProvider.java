package com.mahghuuuls.chunkxpfatigue.command;

import com.mahghuuuls.chunkxpfatigue.pressure.PressureStore;
import com.mahghuuuls.chunkxpfatigue.pressure.PressureWorldData;
import net.minecraft.command.CommandException;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.WorldServer;

final class WorldPressureStoreProvider implements PressureStoreProvider {
    private final double recoveryMinutesPerPressure;
    private final double maximumPressure;

    WorldPressureStoreProvider(double recoveryMinutesPerPressure, double maximumPressure) {
        this.recoveryMinutesPerPressure = recoveryMinutesPerPressure;
        this.maximumPressure = maximumPressure;
    }

    @Override
    public PressureStore get(MinecraftServer server) throws CommandException {
        WorldServer world = server.getWorld(0);
        if (world == null) throw new CommandException("Overworld is unavailable");
        return PressureWorldData.get(world, recoveryMinutesPerPressure, maximumPressure);
    }
}
