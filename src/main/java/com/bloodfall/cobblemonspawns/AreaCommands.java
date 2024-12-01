package com.bloodfall.cobblemonspawns;

import com.bloodfall.cobblemonspawns.Area;
import com.bloodfall.cobblemonspawns.CobblemonSpawnsConfig;
import com.bloodfall.cobblemonspawns.AreaManager;
import com.bloodfall.cobblemonspawns.SelectionManager; // Ensure this import exists
import com.bloodfall.cobblemonspawns.client.CobblemonSpawnsClient;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.ParseResults;
import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.arguments.DoubleArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.sun.jna.WString;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.MarkerEntity;
import net.minecraft.entity.decoration.ArmorStandEntity;
import net.minecraft.entity.decoration.DisplayEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import java.util.*;
import java.util.function.Supplier;

public class AreaCommands
{
    private static final Set<UUID> debugPlayers = new HashSet<>();
    private static final Map<UUID, List<UUID>> debugEntities = new HashMap<>();

    public static void register(CommandDispatcher<ServerCommandSource> dispatcher) {
        dispatcher.register(
                CommandManager.literal("area")
                        // /area add <name> [minX minY minZ maxX maxY maxZ]
                        .then(
                                CommandManager.literal("add")
                                        .then(
                                                CommandManager.argument("name", StringArgumentType.word())
                                                        .executes(context -> {
                                                            String name = StringArgumentType.getString(context, "name");
                                                            ServerPlayerEntity player = context.getSource().getPlayer();
                                                            ServerCommandSource source = context.getSource();

                                                            if (player == null) {
                                                                source.sendError(Text.literal("Only players can execute this command."));
                                                                return 0;
                                                            }

                                                            AreaManager manager = AreaManager.get((ServerWorld) player.getWorld());

                                                            // Check if area with the same name exists
                                                            boolean exists = manager.getAllAreas().stream()
                                                                    .anyMatch(a -> a.getName().equalsIgnoreCase(name));
                                                            if (exists) {
                                                                source.sendError(Text.literal("Area with this name already exists."));
                                                                return 0;
                                                            }

                                                            // Check if player has a complete selection
                                                            UUID playerId = player.getUuid();
                                                            if (SelectionManager.hasCompleteSelection(playerId)) {
                                                                SelectionManager.SelectionData data = SelectionManager.consumeSelection(playerId);
                                                                BlockPos minPos = data.getFirstPos();
                                                                BlockPos maxPos = data.getSecondPos();

                                                                // Create and add the area
                                                                Area area = new Area(name, minPos, maxPos);
                                                                manager.addArea(area);

                                                                source.sendFeedback(() -> Text.literal("Area '" + name + "' added successfully using selected points."), true);
                                                                return 1;
                                                            } else {
                                                                source.sendError(Text.literal("You have not selected two points. Use the AreaSelectionTool to select two blocks or provide coordinates."));
                                                                return 0;
                                                            }
                                                        })
                                        )
                                        // Allow specifying coordinates directly
                                        .then(
                                                CommandManager.argument("name", StringArgumentType.word())
                                                        .then(
                                                                CommandManager.argument("minX", IntegerArgumentType.integer())
                                                                        .then(
                                                                                CommandManager.argument("minY", IntegerArgumentType.integer())
                                                                                        .then(
                                                                                                CommandManager.argument("minZ", IntegerArgumentType.integer())
                                                                                                        .then(
                                                                                                                CommandManager.argument("maxX", IntegerArgumentType.integer())
                                                                                                                        .then(
                                                                                                                                CommandManager.argument("maxY", IntegerArgumentType.integer())
                                                                                                                                        .then(
                                                                                                                                                CommandManager.argument("maxZ", IntegerArgumentType.integer())
                                                                                                                                                        .executes(context -> {
                                                                                                                                                            String name = StringArgumentType.getString(context, "name");
                                                                                                                                                            int minX = IntegerArgumentType.getInteger(context, "minX");
                                                                                                                                                            int minY = IntegerArgumentType.getInteger(context, "minY");
                                                                                                                                                            int minZ = IntegerArgumentType.getInteger(context, "minZ");
                                                                                                                                                            int maxX = IntegerArgumentType.getInteger(context, "maxX");
                                                                                                                                                            int maxY = IntegerArgumentType.getInteger(context, "maxY");
                                                                                                                                                            int maxZ = IntegerArgumentType.getInteger(context, "maxZ");

                                                                                                                                                            ServerPlayerEntity player = context.getSource().getPlayer();
                                                                                                                                                            ServerCommandSource source = context.getSource();

                                                                                                                                                            if (player == null) {
                                                                                                                                                                source.sendError(Text.literal("Only players can execute this command."));
                                                                                                                                                                return 0;
                                                                                                                                                            }

                                                                                                                                                            AreaManager manager = AreaManager.get((ServerWorld) player.getWorld());

                                                                                                                                                            // Check if area with the same name exists
                                                                                                                                                            boolean exists = manager.getAllAreas().stream()
                                                                                                                                                                    .anyMatch(a -> a.getName().equalsIgnoreCase(name));
                                                                                                                                                            if (exists) {
                                                                                                                                                                source.sendError(Text.literal("Area with this name already exists."));
                                                                                                                                                                return 0;
                                                                                                                                                            }

                                                                                                                                                            BlockPos minPos = new BlockPos(minX, minY, minZ);
                                                                                                                                                            BlockPos maxPos = new BlockPos(maxX, maxY, maxZ);
                                                                                                                                                            Area area = new Area(name, minPos, maxPos);
                                                                                                                                                            manager.addArea(area);

                                                                                                                                                            source.sendFeedback(() -> Text.literal("Area '" + name + "' added successfully with specified coordinates."), true);
                                                                                                                                                            return 1;
                                                                                                                                                        })
                                                                                                                                        )
                                                                                                                        )
                                                                                                        )
                                                                                        )
                                                                        )
                                                        )
                                        )
                        )
                        // /area remove <name>
                        .then(
                                CommandManager.literal("remove")
                                        .then(
                                                CommandManager.argument("name", StringArgumentType.word())
                                                        .executes(context -> {
                                                            String name = StringArgumentType.getString(context, "name");
                                                            ServerPlayerEntity player = context.getSource().getPlayer();
                                                            ServerCommandSource source = context.getSource();
                                                            if (player == null) {
                                                                source.sendError(Text.literal("Only players can execute this command."));
                                                                return 0;
                                                            }

                                                            AreaManager manager = AreaManager.get((ServerWorld) player.getWorld());

                                                            Area areaToRemove = manager.getAllAreas().stream()
                                                                    .filter(a -> a.getName().equalsIgnoreCase(name))
                                                                    .findFirst()
                                                                    .orElse(null);

                                                            if (areaToRemove == null) {
                                                                source.sendError(Text.literal("Area with this name does not exist."));
                                                                return 0;
                                                            }

                                                            manager.removeArea(areaToRemove.getId());
                                                            source.sendFeedback(() -> Text.literal("Area '" + name + "' removed successfully."), true);
                                                            return 1;
                                                        })
                                        )
                        )
                        // /area list
                        .then(
                                CommandManager.literal("list")
                                        .executes(context -> {
                                            ServerPlayerEntity player = context.getSource().getPlayer();
                                            ServerCommandSource source = context.getSource();
                                            if (player == null) {
                                                source.sendError(Text.literal("Only players can execute this command."));
                                                return 0;
                                            }

                                            AreaManager manager = AreaManager.get((ServerWorld) player.getWorld());

                                            if (manager.getAllAreas().isEmpty()) {
                                                source.sendFeedback(() -> Text.literal("No areas defined."), false);
                                                return 1;
                                            }

                                            StringBuilder sb = new StringBuilder("Defined Areas:\n");
                                            for (Area area : manager.getAllAreas()) {
                                                sb.append("- ").append(area.getName())
                                                        .append(" [Min: ")
                                                        .append(area.getMinPos().getX()).append(", ")
                                                        .append(area.getMinPos().getY()).append(", ")
                                                        .append(area.getMinPos().getZ()).append(" | Max: ")
                                                        .append(area.getMaxPos().getX()).append(", ")
                                                        .append(area.getMaxPos().getY()).append(", ")
                                                        .append(area.getMaxPos().getZ()).append("]\n");
                                            }

                                            source.sendFeedback(() -> Text.literal(sb.toString()), false);
                                            return 1;
                                        })
                        )
                        // /area addpokemon <areaName> <pokemonName> <spawnRate> <minLevel> <maxLevel>
                        .then(
                                CommandManager.literal("addpokemon")
                                        .then(
                                                CommandManager.argument("areaName", StringArgumentType.word())
                                                        .then(
                                                                CommandManager.argument("pokemonName", StringArgumentType.word())
                                                                        .then(
                                                                                CommandManager.argument("spawnRate", DoubleArgumentType.doubleArg(0.0, 1.0))
                                                                                        .then(
                                                                                                CommandManager.argument("minLevel", IntegerArgumentType.integer(1))
                                                                                                        .then(
                                                                                                                CommandManager.argument("maxLevel", IntegerArgumentType.integer(1))
                                                                                                                        .executes(context -> {
                                                                                                                            String areaName = StringArgumentType.getString(context, "areaName");
                                                                                                                            String pokemonName = StringArgumentType.getString(context, "pokemonName");
                                                                                                                            double spawnRate = DoubleArgumentType.getDouble(context, "spawnRate");
                                                                                                                            int minLevel = IntegerArgumentType.getInteger(context, "minLevel");
                                                                                                                            int maxLevel = IntegerArgumentType.getInteger(context, "maxLevel");

                                                                                                                            if (minLevel > maxLevel) {
                                                                                                                                context.getSource().sendError(Text.literal("Min level cannot be greater than max level."));
                                                                                                                                return 0;
                                                                                                                            }

                                                                                                                            ServerPlayerEntity player = context.getSource().getPlayer();
                                                                                                                            ServerCommandSource source = context.getSource();
                                                                                                                            if (player == null) {
                                                                                                                                source.sendError(Text.literal("Only players can execute this command."));
                                                                                                                                return 0;
                                                                                                                            }

                                                                                                                            AreaManager manager = AreaManager.get((ServerWorld) player.getWorld());

                                                                                                                            Area targetArea = manager.getAllAreas().stream()
                                                                                                                                    .filter(a -> a.getName().equalsIgnoreCase(areaName))
                                                                                                                                    .findFirst()
                                                                                                                                    .orElse(null);

                                                                                                                            if (targetArea == null) {
                                                                                                                                source.sendError(Text.literal("Area with this name does not exist."));
                                                                                                                                return 0;
                                                                                                                            }

                                                                                                                            // Check if Pokémon already exists in the area
                                                                                                                            boolean exists = targetArea.getSpawnConfigs().stream()
                                                                                                                                    .anyMatch(p -> p.getCobblemonName().equalsIgnoreCase(pokemonName));
                                                                                                                            if (exists) {
                                                                                                                                source.sendError(Text.literal("Pokémon with this name already exists in the area."));
                                                                                                                                return 0;
                                                                                                                            }

                                                                                                                            CobblemonSpawnsConfig spawnConfig = new CobblemonSpawnsConfig(pokemonName, spawnRate, minLevel, maxLevel);
                                                                                                                            targetArea.addSpawnConfig(spawnConfig);
                                                                                                                            manager.markDirty();

                                                                                                                            source.sendFeedback(() -> Text.literal("Pokémon '" + pokemonName + "' added to area '" + areaName + "'."), true);
                                                                                                                            return 1;
                                                                                                                        })
                                                                                                        )
                                                                                        )
                                                                        )
                                                        )
                                        )
                        )
                        // /area removepokemon <areaName> <pokemonName>
                        .then(
                                CommandManager.literal("removepokemon")
                                        .then(
                                                CommandManager.argument("areaName", StringArgumentType.word())
                                                        .then(
                                                                CommandManager.argument("pokemonName", StringArgumentType.word())
                                                                        .executes(context -> {
                                                                            String areaName = StringArgumentType.getString(context, "areaName");
                                                                            String pokemonName = StringArgumentType.getString(context, "pokemonName");

                                                                            ServerPlayerEntity player = context.getSource().getPlayer();
                                                                            ServerCommandSource source = context.getSource();
                                                                            if (player == null) {
                                                                                source.sendError(Text.literal("Only players can execute this command."));
                                                                                return 0;
                                                                            }

                                                                            AreaManager manager = AreaManager.get((ServerWorld) player.getWorld());

                                                                            Area targetArea = manager.getAllAreas().stream()
                                                                                    .filter(a -> a.getName().equalsIgnoreCase(areaName))
                                                                                    .findFirst()
                                                                                    .orElse(null);

                                                                            if (targetArea == null) {
                                                                                source.sendError(Text.literal("Area with this name does not exist."));
                                                                                return 0;
                                                                            }

                                                                            CobblemonSpawnsConfig spawnConfig = targetArea.getSpawnConfigs().stream()
                                                                                    .filter(p -> p.getCobblemonName().equalsIgnoreCase(pokemonName))
                                                                                    .findFirst()
                                                                                    .orElse(null);

                                                                            if (spawnConfig == null) {
                                                                                source.sendError(Text.literal("Pokémon with this name does not exist in the area."));
                                                                                return 0;
                                                                            }

                                                                            targetArea.removeSpawnConfig(spawnConfig);
                                                                            manager.markDirty();

                                                                            source.sendFeedback(() -> Text.literal("Pokémon '" + pokemonName + "' removed from area '" + areaName + "'."), true);
                                                                            return 1;
                                                                        })
                                                        )
                                        )
                        )
                        // /area listpokemon <areaName>
                        .then(
                                CommandManager.literal("listpokemon")
                                        .then(
                                                CommandManager.argument("areaName", StringArgumentType.word())
                                                        .executes(context -> {
                                                            String areaName = StringArgumentType.getString(context, "areaName");

                                                            ServerPlayerEntity player = context.getSource().getPlayer();
                                                            ServerCommandSource source = context.getSource();
                                                            if (player == null) {
                                                                source.sendError(Text.literal("Only players can execute this command."));
                                                                return 0;
                                                            }

                                                            AreaManager manager = AreaManager.get((ServerWorld) player.getWorld());

                                                            Area targetArea = manager.getAllAreas().stream()
                                                                    .filter(a -> a.getName().equalsIgnoreCase(areaName))
                                                                    .findFirst()
                                                                    .orElse(null);

                                                            if (targetArea == null) {
                                                                source.sendError(Text.literal("Area with this name does not exist."));
                                                                return 0;
                                                            }

                                                            if (targetArea.getSpawnConfigs().isEmpty()) {
                                                                source.sendFeedback(() -> Text.literal("No Pokémon configured for area '" + areaName + "'."), false);
                                                                return 1;
                                                            }

                                                            StringBuilder sb = new StringBuilder("Pokémon in Area '" + areaName + "':\n");
                                                            for (CobblemonSpawnsConfig config : targetArea.getSpawnConfigs()) {
                                                                sb.append("- ").append(config.getCobblemonName())
                                                                        .append(" (Rate: ").append(config.getSpawnRate())
                                                                        .append(", Levels: ").append(config.getMinLevel())
                                                                        .append("-").append(config.getMaxLevel()).append(")\n");
                                                            }

                                                            source.sendFeedback(() -> Text.literal(sb.toString()), false);
                                                            return 1;
                                                        })
                                        )
                        )
                        .then(
                                CommandManager.literal("debug")
                                        .executes(context -> {
                                            ServerPlayerEntity player = context.getSource().getPlayer();
                                            if (player == null) return 0;

                                            UUID playerId = player.getUuid();
                                            ServerWorld world = (ServerWorld)player.getWorld();
                                            AreaManager manager = AreaManager.get(world);

                                            if (debugPlayers.contains(playerId)) {
                                                // Disable debug mode
                                                debugPlayers.remove(playerId);
                                                playerTickCounters.remove(playerId);
                                                removeDebugEntities(player);
                                                //context.getSource().sendFeedback(Text.literal("Debug mode disabled."), true);
                                            } else {
                                                // Enable debug mode
                                                debugPlayers.add(playerId);

                                                for (Area area : manager.getAllAreas()) {
                                                    debugArea(world, area, playerId);
                                                }

                                                //context.getSource().sendFeedback(Text.literal("Debug mode enabled."), true);
                                            }
                                            return 1;
                                        })
                        )
        );
    }
    private static void debugArea(ServerWorld world, Area area, UUID playerId) {
        BlockPos minPos = area.getMinPos();
        BlockPos maxPos = area.getMaxPos();

        createFloatingText(world, area);
        spawnPersistentParticles(world, minPos, maxPos, playerId);
    }

    private static final int PARTICLE_TICK_RATE = 20; // Spawn particles every 10 ticks
    private static final Map<UUID, Integer> playerTickCounters = new HashMap<>();

    private static void spawnPersistentParticles(ServerWorld world, BlockPos minPos, BlockPos maxPos, UUID playerId) {
        // Normalize bounds
        int minX = Math.min(minPos.getX(), maxPos.getX());
        int minY = Math.min(minPos.getY(), maxPos.getY());
        int minZ = Math.min(minPos.getZ(), maxPos.getZ());

        int maxX = Math.max(minPos.getX(), maxPos.getX());
        int maxY = Math.max(minPos.getY(), maxPos.getY());
        int maxZ = Math.max(minPos.getZ(), maxPos.getZ());

        // Initialize the player's tick counter if not present
        playerTickCounters.putIfAbsent(playerId, 0);

        // Register a server tick event
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            // Ensure the world matches
            if (!world.getServer().getWorld(world.getRegistryKey()).equals(world)) return;

            // Check if the player is still in debug mode
            if (!debugPlayers.contains(playerId)) {
                playerTickCounters.remove(playerId); // Remove the tick counter for this player
                return;
            }

            // Increment and check tick counter
            int currentTick = playerTickCounters.get(playerId);
            if (currentTick >= PARTICLE_TICK_RATE) {
                playerTickCounters.put(playerId, 0); // Reset counter

                // Spawn particles at the edges
                for (int x = minX; x <= maxX; x += 2) {
                    for (int y = minY; y <= maxY; y += 2) {
                        for (int z = minZ; z <= maxZ; z += 2) {
                            if (isEdge(new BlockPos(minX, minY, minZ), new BlockPos(maxX, maxY, maxZ), x, y, z)) {
                                world.spawnParticles(ParticleTypes.END_ROD, x + 0.5, y + 0.5, z + 0.5, 1, 0, 0, 0, 0);
                            }
                        }
                    }
                }
            } else {
                // Increment counter
                playerTickCounters.put(playerId, currentTick + 1);
            }
        });
    }

    private static void createFloatingText(ServerWorld world, Area area) {
        BlockPos minPos = area.getMinPos();
        BlockPos maxPos = area.getMaxPos();
        BlockPos center = new BlockPos(
                (minPos.getX() + maxPos.getX()) / 2,
                (minPos.getY() + maxPos.getY()) / 2,
                (minPos.getZ() + maxPos.getZ()) / 2
        );

        String s = area.getName();
        createArmorStandText(center, center.getY(), world, area, s);

        int index = 1;
        for (CobblemonSpawnsConfig config : area.getSpawnConfigs()) {
            s = config.getCobblemonName() + " (" + config.getMinLevel() + " - " + config.getMaxLevel() + ") | " + config.getSpawnRate();
            BlockPos c = center;
            createArmorStandText(center, c.getY() - index, world, area, s);
            index++;
        }


    }

    private static void createArmorStandText(BlockPos center, int y, ServerWorld world, Area area, String text)
    {
        ArmorStandEntity marker = new ArmorStandEntity(EntityType.ARMOR_STAND, world);
        marker.refreshPositionAndAngles(center.getX() + 0.5, y + 0.5, center.getZ() + 0.5, 0, 0);
        marker.setInvisible(true);
        marker.setInvulnerable(true);
        marker.setNoGravity(true);
        marker.setCustomName(Text.literal(text).formatted(Formatting.BOLD, Formatting.UNDERLINE, Formatting.GREEN));
        marker.setCustomNameVisible(true);
        marker.addCommandTag("debug_area");
        world.spawnEntity(marker);
    }

    private static void removeDebugEntities(ServerPlayerEntity player) {
        World world = player.getWorld();
        if (!(world instanceof ServerWorld serverWorld)) return;

        UUID playerId = player.getUuid();
        List<UUID> entities = debugEntities.getOrDefault(playerId, Collections.emptyList());

        for (UUID entityId : entities) {
            Entity entity = serverWorld.getEntity(entityId);
            if (entity instanceof ArmorStandEntity armorStand) {
                if (armorStand.getCommandTags().contains("debug_area")) { // Check for the tag
                    entity.remove(Entity.RemovalReason.DISCARDED);
                }
            }
        }

        debugEntities.remove(playerId);


        MinecraftServer server = player.getServer();
        ServerWorld sWorld = (ServerWorld) player.getWorld();

        ServerCommandSource commandSource = new ServerCommandSource(
                player,
                player.getPos(),
                player.getRotationClient(),
                sWorld,
                4,
                player.getName().getString(),
                Text.of(player.getName().getString()),
                server,
                player
        );

        CommandManager commandManager = server.getCommandManager();
        String command = "kill @e[type=armor_stand,distance=..50]";
        ParseResults<ServerCommandSource> parseResults = commandManager.getDispatcher().parse(new StringReader(command), commandSource);
        try {
            // Execute the parsed command
            commandManager.getDispatcher().execute(parseResults);
        } catch (CommandSyntaxException e) {
            player.sendMessage(Text.of("Failed to execute command: " + e.getMessage()), false);
        }
    }

    private static boolean isEdge(BlockPos minPos, BlockPos maxPos, int x, int y, int z) {
        return x == minPos.getX() || x == maxPos.getX()
                || y == minPos.getY() || y == maxPos.getY()
                || z == minPos.getZ() || z == maxPos.getZ();
    }


    private static PacketByteBuf serializeAreas(Collection<Area> areas) {
        PacketByteBuf buf = PacketByteBufs.create();
        buf.writeInt(areas.size());
        for (Area area : areas) {
            buf.writeUuid(area.getId());
            buf.writeString(area.getName());
            buf.writeBlockPos(area.getMinPos());
            buf.writeBlockPos(area.getMaxPos());
            buf.writeInt(area.getSpawnConfigs().size());
            for (CobblemonSpawnsConfig config : area.getSpawnConfigs()) {
                buf.writeString(config.getCobblemonName());
                buf.writeDouble(config.getSpawnRate());
                buf.writeInt(config.getMinLevel());
                buf.writeInt(config.getMaxLevel());
            }
        }
        return buf;
    }

}
