package com.mahghuuuls.chunkxpfatigue;

import com.mahghuuuls.chunkxpfatigue.config.FatigueConfig;
import com.mahghuuuls.chunkxpfatigue.config.ValidatedFatigueConfig;
import com.mahghuuuls.chunkxpfatigue.forge.LivingXpHandler;
import com.mahghuuuls.chunkxpfatigue.forge.CommonEventHandler;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
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

    @Mod.EventHandler
    public void preInit(FMLPreInitializationEvent event) {
        ValidatedFatigueConfig config = FatigueConfig.validate();
        for (String warning : config.getWarnings()) {
            LOGGER.warn("Configuration: {}", warning);
        }
        for (String error : config.getErrors()) {
            LOGGER.error("Configuration: {}", error);
        }
        MinecraftForge.EVENT_BUS.register(new LivingXpHandler(config));
        MinecraftForge.EVENT_BUS.register(new CommonEventHandler(config));
    }
}
