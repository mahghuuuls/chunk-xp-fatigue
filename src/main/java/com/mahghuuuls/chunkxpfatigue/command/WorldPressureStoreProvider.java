package com.mahghuuuls.chunkxpfatigue.command;

import com.mahghuuuls.chunkxpfatigue.pressure.PressureStore;
import com.mahghuuuls.chunkxpfatigue.pressure.PressureWorldData;
import net.minecraft.command.CommandException;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.WorldServer;

final class WorldPressureStoreProvider implements PressureStoreProvider {
    private final double recoveryMinutesPerPressure;

    WorldPressureStoreProvider(double recoveryMinutesPerPressure) {
        this.recoveryMinutesPerPressure = recoveryMinutesPerPressure;
    }

    @Override
    public PressureStore get(MinecraftServer server) throws CommandException {
        WorldServer world = server.getWorld(0);
        if (world == null) throw new CommandException("Overworld is unavailable");
        return PressureWorldData.get(world, recoveryMinutesPerPressure);
    }
}
