package com.mahghuuuls.chunkxpfatigue;

import com.mahghuuuls.chunkxpfatigue.config.FatigueConfig;
import com.mahghuuuls.chunkxpfatigue.config.ValidatedFatigueConfig;
import com.mahghuuuls.chunkxpfatigue.forge.LivingXpHandler;
import com.mahghuuuls.chunkxpfatigue.forge.CommonEventHandler;
import com.mahghuuuls.chunkxpfatigue.forge.CrowdingDeathHandler;
import com.mahghuuuls.chunkxpfatigue.forge.CrowdingSnapshotCache;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.fml.common.event.FMLServerStartingEvent;
import net.minecraftforge.fml.common.event.FMLServerStoppedEvent;
import com.mahghuuuls.chunkxpfatigue.command.ChunkXpFatigueCommand;
import com.mahghuuuls.chunkxpfatigue.forge.OverlaySyncHandler;
import com.mahghuuuls.chunkxpfatigue.network.ModNetwork;
import com.mahghuuuls.chunkxpfatigue.proxy.CommonProxy;
import net.minecraftforge.fml.common.SidedProxy;
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
    private ModNetwork network;
    private CrowdingSnapshotCache crowdingSnapshots;

    @SidedProxy(clientSide = "com.mahghuuuls.chunkxpfatigue.proxy.ClientProxy",
            serverSide = "com.mahghuuuls.chunkxpfatigue.proxy.CommonProxy")
    public static CommonProxy PROXY;

    @Mod.EventHandler
    public void preInit(FMLPreInitializationEvent event) {
        config = FatigueConfig.validate();
        for (String warning : config.getWarnings()) {
            LOGGER.warn("Configuration: {}", warning);
        }
        for (String error : config.getErrors()) {
            LOGGER.error("Configuration: {}", error);
        }
        crowdingSnapshots = new CrowdingSnapshotCache();
        MinecraftForge.EVENT_BUS.register(new CrowdingDeathHandler(config, crowdingSnapshots));
        MinecraftForge.EVENT_BUS.register(new LivingXpHandler(config, crowdingSnapshots));
        MinecraftForge.EVENT_BUS.register(new CommonEventHandler(config));
        network = new ModNetwork(PROXY.overlayHandler());
        PROXY.registerClientEvents();
        if (config.isDebugOverlayEnabled()) {
            MinecraftForge.EVENT_BUS.register(new OverlaySyncHandler(config, network));
        }
    }

    @Mod.EventHandler
    public void serverStarting(FMLServerStartingEvent event) {
        event.registerServerCommand(new ChunkXpFatigueCommand(config));
    }

    @Mod.EventHandler
    public void serverStopped(FMLServerStoppedEvent event) {
        if (crowdingSnapshots != null) {
            crowdingSnapshots.clear();
        }
    }
}
