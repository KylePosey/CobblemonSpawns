package com.bloodfall.cobblemonspawns;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.DoubleArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.fabricmc.fabric.api.screenhandler.v1.ExtendedScreenHandlerFactory;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.decoration.ArmorStandEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.server.PlayerManager;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import java.util.*;

public class AreaCommands
{
    private static final Identifier FLOATING_TEXT_PACKET_ID = new Identifier("cobblemonspawns", "floating_text");
    private static final Identifier CLEAR_FLOATING_TEXT_PACKET_ID = new Identifier("cobblemonspawns", "clear_floating_text");

    private static final Set<UUID> debugPlayers = new HashSet<>();

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

                                                                refreshDebugView((ServerWorld) player.getWorld(), player);

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

                                                                                                                                                            refreshDebugView((ServerWorld) player.getWorld(), player);

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

                                                            refreshDebugView((ServerWorld) player.getWorld(), player);

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

                                                                                                                            refreshDebugView((ServerWorld) player.getWorld(), player);

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

                                                                            refreshDebugView((ServerWorld) player.getWorld(), player);

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
                                                debugPlayers.remove(playerId);
                                                //playerTickCounters.remove(playerId);
                                                CobblemonSpawns.sendStopBoundingBoxToClient(player);
                                                sendClearFloatingTextToClient(player);
                                                //context.getSource().sendFeedback(Text.literal("Debug mode disabled."), true);
                                            } else {
                                                debugPlayers.add(playerId);

                                                for (Area area : manager.getAllAreas()) {
                                                    debugArea(world, area, playerId);
                                                }

                                                sendFloatingTexts(world, player);

                                                //context.getSource().sendFeedback(Text.literal("Debug mode enabled."), true);
                                            }
                                            return 1;
                                        })
                        )
                        .then(
                                CommandManager.literal("gui")
                                        .executes(context -> {
                                            ServerPlayerEntity player = context.getSource().getPlayer();
                                            if (player != null) {
                                                // Get the list of areas
                                                ServerWorld world = (ServerWorld) player.getWorld();
                                                AreaManager manager = AreaManager.get(world);
                                                Collection<Area> areas = manager.getAllAreas();

                                                // Serialize areas into the buffer
                                                PacketByteBuf buf = PacketByteBufs.create();
                                                serializeAreas(buf, areas);

                                                // Send the packet to the client
                                                ServerPlayNetworking.send(player, CobblemonSpawns.OPEN_AREA_GUI_PACKET_ID, buf);
                                            }
                                            return 1;
                                        })
                        )
        );

        ServerPlayNetworking.registerGlobalReceiver(CobblemonSpawns.KEYBIND_TOGGLE_DEBUG_VIEW_PACKET_ID, (server, player, handler, buf, responseSender) -> {
            server.execute(() -> {
                UUID playerId = player.getUuid();
                ServerWorld world = (ServerWorld) player.getWorld();
                AreaManager manager = AreaManager.get(world);

                if (debugPlayers.contains(playerId)) {
                    debugPlayers.remove(playerId);
                    CobblemonSpawns.sendStopBoundingBoxToClient(player);
                    sendClearFloatingTextToClient(player);
                } else {
                    debugPlayers.add(playerId);

                    for (Area area : manager.getAllAreas()) {
                        debugArea(world, area, playerId);
                    }

                    sendFloatingTexts(world, player);
                }
            });
        });
    }

    private static void debugArea(ServerWorld world, Area area, UUID playerId) {
        BlockPos minPos = area.getMinPos();
        BlockPos maxPos = area.getMaxPos();
        spawnBoundaryLines(world, minPos, maxPos, playerId);
    }

    private static void spawnBoundaryLines(ServerWorld world, BlockPos minPos, BlockPos maxPos, UUID playerId) {
        // Normalize bounds
        int minX = Math.min(minPos.getX(), maxPos.getX());
        int minY = Math.min(minPos.getY(), maxPos.getY());
        int minZ = Math.min(minPos.getZ(), maxPos.getZ());

        int maxX = Math.max(minPos.getX(), maxPos.getX());
        int maxY = Math.max(minPos.getY(), maxPos.getY());
        int maxZ = Math.max(minPos.getZ(), maxPos.getZ());

        /*PlayerManager playerManager = world.getServer().getPlayerManager();
        ServerPlayerEntity sPlayer = playerManager.getPlayer(playerId);
        CobblemonSpawns.sendStartBoundingBoxToClient(sPlayer, minPos, maxPos);*/

        ServerPlayerEntity player = world.getServer().getPlayerManager().getPlayer(playerId);
        if (player != null) {
            CobblemonSpawns.sendStartBoundingBoxToClient(player, minPos, maxPos);
        }
    }

    /*private static void createFloatingText(ServerWorld world, Area area) {
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
            createArmorStandText(center, c.getY() - (index), world, area, s);
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
    }*/

    private static void sendFloatingTexts(ServerWorld world, ServerPlayerEntity player) {
        Collection<Area> areas = AreaManager.get(world).getAllAreas();
        PacketByteBuf buf = PacketByteBufs.create();
        buf.writeInt(areas.size());

        for (Area area : areas) {
            buf.writeString(area.getName());

            BlockPos center = new BlockPos(
                    (area.getMinPos().getX() + area.getMaxPos().getX()) / 2,
                    (area.getMinPos().getY() + area.getMaxPos().getY()) / 2 + 2, // Slightly above the center
                    (area.getMinPos().getZ() + area.getMaxPos().getZ()) / 2
            );
            buf.writeBlockPos(center);

            buf.writeInt(area.getSpawnConfigs().size());
            for (CobblemonSpawnsConfig config : area.getSpawnConfigs()) {
                buf.writeString(config.getCobblemonName());
                buf.writeInt(config.getMinLevel());
                buf.writeInt(config.getMaxLevel());
                buf.writeDouble(config.getSpawnRate());
            }
        }

        ServerPlayNetworking.send(player, FLOATING_TEXT_PACKET_ID, buf);
    }

    private static void sendClearFloatingTextToClient(ServerPlayerEntity player) {
        PacketByteBuf buf = PacketByteBufs.create();
        ServerPlayNetworking.send(player, new Identifier("cobblemonspawns", "clear_floating_text"), buf);
    }

    public static void refreshDebugView(ServerWorld world, ServerPlayerEntity serverPlayer)
    {
        for (UUID playerId : debugPlayers) {
            ServerPlayerEntity player = Objects.requireNonNull(world.getServer()).getPlayerManager().getPlayer(playerId);
            if (player != null) {
                // Send stop bounding box to clear existing visuals
                CobblemonSpawns.sendStopBoundingBoxToClient(player);
                sendClearFloatingTextToClient(player);
                // For each area, create the debug visuals for the player
                for (Area area : AreaManager.get(world).getAllAreas()) {
                    debugArea(world, area, playerId);
                }

                sendFloatingTexts(world, player);
            }
        }
    }

    /*public static void refreshPacketDebugView(ServerWorld world, ServerPlayerEntity serverPlayer)
    {
        for (UUID playerId : debugPlayers) {
            ServerPlayerEntity player = Objects.requireNonNull(world.getServer()).getPlayerManager().getPlayer(playerId);
            if (player != null) {
                // Send stop bounding box to clear existing visuals
                removeDebugEntities(serverPlayer);
                CobblemonSpawns.sendStopBoundingBoxToClient(player);

                // For each area, create the debug visuals for the player
                for (Area area : AreaManager.get(world).getAllAreas()) {
                    debugArea(world, area, playerId);
                }
            }
        }

        PacketByteBuf buf = PacketByteBufs.create();

        // Example: Send a simple acknowledgment
        buf.writeString("Refresh successful");

        ServerPlayNetworking.send(serverPlayer, new Identifier("cobblemonspawns", "refresh_acknowledgment"), buf);
    }*/

    /*private static void removeAllDebugEntities(ServerPlayerEntity player) {
        World world = player.getWorld();
        if (!(world instanceof ServerWorld serverWorld)) return;

        List<? extends ArmorStandEntity> allArmorStands = serverWorld.getEntitiesByType(
                EntityType.ARMOR_STAND,
                armorStand -> armorStand.getCommandTags().contains("debug_area")
        );
        for (ArmorStandEntity armorStand : allArmorStands) {
            armorStand.remove(Entity.RemovalReason.DISCARDED);
        }
    }*/

    private static boolean isEdge(BlockPos minPos, BlockPos maxPos, int x, int y, int z) {
        return x == minPos.getX() || x == maxPos.getX()
                || y == minPos.getY() || y == maxPos.getY()
                || z == minPos.getZ() || z == maxPos.getZ();
    }

    private static void serializeAreas(PacketByteBuf buf, Collection<Area> areas) {
        buf.writeInt(areas.size());
        for (Area area : areas) {
            buf.writeUuid(area.getId());
            buf.writeString(area.getName());
            buf.writeBlockPos(area.getMinPos());
            buf.writeBlockPos(area.getMaxPos());
        }
    }
}