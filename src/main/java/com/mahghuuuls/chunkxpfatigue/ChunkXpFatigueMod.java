package com.mahghuuuls.chunkxpfatigue;

import net.minecraftforge.fml.common.Mod;
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

    private ChunkXpFatigueMod() {
    }
}
