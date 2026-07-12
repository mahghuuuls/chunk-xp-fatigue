package com.mahghuuuls.chunkxpfatigue.command;

import com.mahghuuuls.chunkxpfatigue.config.ValidatedFatigueConfig;
import com.mahghuuuls.chunkxpfatigue.pressure.ChunkPressureKey;
import com.mahghuuuls.chunkxpfatigue.pressure.PressureStore;
import net.minecraft.command.CommandBase;
import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSender;
import net.minecraft.command.WrongUsageException;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.text.TextComponentString;

import java.util.Collections;
import java.util.List;
import java.util.Locale;

public final class ChunkXpFatigueCommand extends CommandBase {

    private final ValidatedFatigueConfig config;
    private final PressureStoreProvider storeProvider;

    public ChunkXpFatigueCommand(ValidatedFatigueConfig config) {
        this(config, new WorldPressureStoreProvider(config.getRecoveryMinutesPerPressure(),
                config.getMaximumPressure()));
    }

    ChunkXpFatigueCommand(ValidatedFatigueConfig config, PressureStoreProvider storeProvider) {
        this.config = config;
        this.storeProvider = storeProvider;
    }

    @Override public String getName() { return "chunkxpfatigue"; }
    @Override public String getUsage(ICommandSender sender) {
        return "/chunkxpfatigue inspect [dimension chunkX chunkZ] | set <pressure> [dimension chunkX chunkZ] | clear chunk [dimension chunkX chunkZ] | clear dimension [dimension] confirm | clear world confirm (bracketed locations may be omitted by players; console must provide them)";
    }
    @Override public int getRequiredPermissionLevel() { return 2; }

    @Override
    public void execute(MinecraftServer server, ICommandSender sender, String[] args)
            throws CommandException {
        if (args.length == 0) throw new WrongUsageException(getUsage(sender));
        PressureStore data = storeProvider.get(server);
        if ("inspect".equals(args[0])) {
            ChunkPressureKey key = target(sender, args, 1);
            double pressure = data.getPressure(key);
            double normalized = pressure / config.getMaximumPressure();
            double multiplier = config.getXpCurve().multiplierAt(normalized);
            reply(sender, String.format(Locale.ROOT,
                    "Chunk XP pressure dimension=%d chunk=(%d,%d) raw=%.6f normalized=%.2f%% multiplier=%.2f%%",
                    key.getDimension(), key.getChunkX(), key.getChunkZ(), pressure,
                    normalized * 100.0D, multiplier * 100.0D));
        } else if ("set".equals(args[0])) {
            requireWritable(data);
            if (args.length != 2 && args.length != 5) throw new WrongUsageException(getUsage(sender));
            double value = parseDouble(args[1], 0.0D, config.getMaximumPressure());
            ChunkPressureKey key = target(sender, args, 2);
            data.setPressure(key, value);
            reply(sender, "Set pressure to " + value + " for " + key);
        } else if ("clear".equals(args[0])) {
            requireWritable(data);
            clear(sender, args, data);
        } else {
            throw new WrongUsageException(getUsage(sender));
        }
    }

    private static void requireWritable(PressureStore store) throws CommandException {
        if (!store.isWritable()) {
            throw new CommandException(
                    "Pressure data uses a newer unsupported schema; mutations are disabled");
        }
    }

    private void clear(ICommandSender sender, String[] args, PressureStore data)
            throws CommandException {
        if (args.length < 2) throw new WrongUsageException(getUsage(sender));
        if ("chunk".equals(args[1])) {
            ChunkPressureKey key = target(sender, args, 2);
            boolean existed = data.getPressure(key) > 0.0D;
            data.setPressure(key, 0.0D);
            reply(sender, "Cleared chunk " + key + " removed=" + (existed ? 1 : 0));
        } else if ("dimension".equals(args[1])) {
            int dimension;
            if (args.length == 3 && "confirm".equals(args[2]) && sender instanceof EntityPlayerMP) {
                dimension = ((EntityPlayerMP) sender).dimension;
            } else if (args.length == 4 && "confirm".equals(args[3])) {
                dimension = parseInt(args[2]);
            } else throw new CommandException("Dimension clear requires exact confirm token");
            reply(sender, "Cleared dimension " + dimension + " removed=" + data.clearDimension(dimension));
        } else if ("world".equals(args[1])) {
            if (args.length != 3 || !"confirm".equals(args[2]))
                throw new CommandException("World clear requires exact confirm token");
            reply(sender, "Cleared world pressure removed=" + data.clearAll());
        } else throw new WrongUsageException(getUsage(sender));
    }

    private ChunkPressureKey target(ICommandSender sender, String[] args, int offset)
            throws CommandException {
        if (args.length == offset && sender instanceof EntityPlayerMP) {
            EntityPlayerMP player = (EntityPlayerMP) sender;
            return new ChunkPressureKey(player.dimension,
                    MathHelper.floor(player.posX) >> 4, MathHelper.floor(player.posZ) >> 4);
        }
        if (args.length == offset + 3) {
            return new ChunkPressureKey(parseInt(args[offset]), parseInt(args[offset + 1]),
                    parseInt(args[offset + 2]));
        }
        throw new WrongUsageException("Console requires <dimension> <chunkX> <chunkZ>");
    }

    private static void reply(ICommandSender sender, String message) {
        sender.sendMessage(new TextComponentString(message));
    }

    @Override public List<String> getAliases() { return Collections.emptyList(); }
}
