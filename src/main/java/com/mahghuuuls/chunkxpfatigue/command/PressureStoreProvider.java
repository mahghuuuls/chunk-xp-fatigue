package com.mahghuuuls.chunkxpfatigue.command;

import com.mahghuuuls.chunkxpfatigue.pressure.PressureStore;
import net.minecraft.command.CommandException;
import net.minecraft.server.MinecraftServer;

interface PressureStoreProvider {
    PressureStore get(MinecraftServer server) throws CommandException;
}
