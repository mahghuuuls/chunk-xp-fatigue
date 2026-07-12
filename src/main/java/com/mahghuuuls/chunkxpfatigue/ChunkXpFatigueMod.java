package com.mahghuuuls.chunkxpfatigue;

import com.mahghuuuls.chunkxpfatigue.config.FatigueConfig;
import com.mahghuuuls.chunkxpfatigue.config.ValidatedFatigueConfig;
import com.mahghuuuls.chunkxpfatigue.forge.LivingXpHandler;
import com.mahghuuuls.chunkxpfatigue.forge.CommonEventHandler;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.fml.common.event.FMLServerStartingEvent;
import com.mahghuuuls.chunkxpfatigue.command.ChunkXpFatigueCommand;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

@Mod(
        modid = Tags.MOD_ID,
        name = Tags.MOD_NAME,
        version = Tags.VERSION,
        acceptableRemoteVersions = "*"
)
public final class ChunkXpFatigueMod {

    public static final Logger LOGGER = LogManager.getLogger(Tags.MOD_NAME);
    private ValidatedFatigueConfig config;

    @Mod.EventHandler
    public void preInit(FMLPreInitializationEvent event) {
        config = FatigueConfig.validate();
        for (String warning : config.getWarnings()) {
            LOGGER.warn("Configuration: {}", warning);
        }
        for (String error : config.getErrors()) {
            LOGGER.error("Configuration: {}", error);
        }
        MinecraftForge.EVENT_BUS.register(new LivingXpHandler(config));
        MinecraftForge.EVENT_BUS.register(new CommonEventHandler(config));
    }

    @Mod.EventHandler
    public void serverStarting(FMLServerStartingEvent event) {
        event.registerServerCommand(new ChunkXpFatigueCommand(config));
    }
}
