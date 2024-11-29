package com.bloodfall.cobblemonspawns;

import com.cobblemon.mod.common.battles.BattleBuilder;
import com.cobblemon.mod.common.entity.pokemon.PokemonEntity;
import com.cobblemon.mod.common.api.events.entity.SpawnEvent;
import com.cobblemon.mod.common.entity.pokemon.PokemonEntity;
import com.cobblemon.mod.common.pokemon.Pokemon;
import com.mojang.brigadier.ParseResults;
import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.text.Text;
import net.minecraft.util.math.Box;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;


public class MovementTracker {
    // To keep track of which areas players are currently in
    //private static final Map<UUID, Set<UUID>> playerAreas = new HashMap<>();
    private static final double SPAWN_CHANCE = 0.05;

    private static Area currentArea;

    public static Map<UUID, BlockPos> lastPlayerPositions = new HashMap<>();
    private static final Map<UUID, PokemonEntity> activePokemonMap = new HashMap<>();

    public static void onPlayerTick(PlayerEntity player, AreaManager manager) {
        UUID playerId = player.getUuid();
        BlockPos pos = player.getBlockPos();
        BlockPos lastPos = lastPlayerPositions.get(playerId);

        ServerWorld world = (ServerWorld) player.getWorld();

        checkPokemonBattleStatus(world);

        if (activePokemonMap.containsKey(playerId)) {
            PokemonEntity activePokemon = activePokemonMap.get(playerId);
            if (activePokemon != null && activePokemon.isAlive()) {
                return;
            }
        }

        if (lastPos != null && lastPos.equals(pos)) {
            return;
        }

        Area currentArea;

        // Check which areas the player is currently in
        for (Area area : manager.getAllAreas()) {
            if (area.contains(pos)) {
                currentArea = area;
                //BlockPos newBlockPos = player.getBlockPos();

                    if (RandomUtils.shouldSpawnSecure(SPAWN_CHANCE))
                    {
                        selectSpawnPokemon(world, area, player);
                    }
                // If the player moves into a new block within this area, attempt a spawn
                //if (!playerAreas.getOrDefault(playerId, Collections.emptySet()).contains(area.getId())) {
                    //if (ThreadLocalRandom.current().nextDouble() < SPAWN_CHANCE) {
                        //attemptSpawnPokemon(world, area, player);
                    //}
                    //return;
                }
            }
            lastPlayerPositions.put(playerId, pos);
    }

        // Update the player's current areas
        //playerAreas.put(playerId, currentArea);
        //lastPlayerPositions.put(playerId, currentPos);

    private static void selectSpawnPokemon(ServerWorld world, Area area, PlayerEntity player) {
        List<CobblemonSpawnsConfig> spawnConfigs = area.getSpawnConfigs();
        if (spawnConfigs.isEmpty()) return;

        // Calculate total spawn rate
        double totalRate = spawnConfigs.stream().mapToDouble(CobblemonSpawnsConfig::getSpawnRate).sum();
        double rand = ThreadLocalRandom.current().nextDouble(0.0, totalRate);

        double cumulative = 0.0;
        CobblemonSpawnsConfig selectedConfig = null;
        for (CobblemonSpawnsConfig config : spawnConfigs) {
            cumulative += config.getSpawnRate();
            if (rand <= cumulative) {
                selectedConfig = config;
                break;
            }
        }

        if (selectedConfig == null) return;

        // Determine Pokémon level
        int level = selectedConfig.getMinLevel();
        if (selectedConfig.getMaxLevel() > selectedConfig.getMinLevel()) {
            level += ThreadLocalRandom.current().nextInt(selectedConfig.getMaxLevel() - selectedConfig.getMinLevel() + 1);
        }

        // Spawn the Pokémon using the Cobblemon API
        String cobblemonName = selectedConfig.getCobblemonName();
        CobblemonSpawns.LOGGER.info("CobblemonSpawns: Spawning a " + cobblemonName);
        //MinecraftClient.getInstance().player.networkHandler.sendChatMessage("CobblemonSpawns: Spawning a " + cobblemonName);
        spawnCobblemon(player, world, player.getX(), player.getY(), player.getZ(), cobblemonName, level);
    }

    private static void spawnCobblemon(PlayerEntity player, ServerWorld world, double x, double y, double z, String pokemonName, int level)
    {
        //PokemonEntity pokemonEntity = new PokemonEntity(world, new Pokemon(), CobblemonEntities.POKEMON);
        //pokemonEntity.setPos(x, y, z);

        MinecraftServer server = player.getServer();

        ServerCommandSource commandSource = new ServerCommandSource(
                player,
                player.getPos(),
                player.getRotationClient(),
                world,
                4,
                player.getName().getString(),
                Text.of(player.getName().getString()),
                server,
                player
        );

        CommandManager commandManager = server.getCommandManager();

        String uniqueTag = "spawnedByPlayer_" + player.getUuidAsString();
        String command = String.format("spawnpokemonat %d %d %d %s level=%d tag=%s", (int) x, (int) y, (int) z, pokemonName, level, uniqueTag);
        //String command = String.format("spawnpokemonat " + x + " " + y + " " + z + " " + pokemonName + " level=" + level);

        ParseResults<ServerCommandSource> parseResults = commandManager.getDispatcher().parse(new StringReader(command), commandSource);

        try {
            // Execute the parsed command
            commandManager.getDispatcher().execute(parseResults);
        } catch (CommandSyntaxException e) {
            player.sendMessage(Text.of("Failed to execute command: " + e.getMessage()), false);
        }

        List<PokemonEntity> nearbyEntities = world.getEntitiesByClass(PokemonEntity.class, new Box(x - 3, y - 3, z - 3, x + 3, y + 3, z + 3), entity -> {
            return !entity.getPokemon().belongsTo(player) && !entity.getPokemon().isPlayerOwned() && entity.getPokemon().getLevel() == level && entity.getPokemon().getSpecies().getName().equalsIgnoreCase(pokemonName);
        });

        if (!nearbyEntities.isEmpty()) {
            PokemonEntity targetPokemon = nearbyEntities.get(0);

            activePokemonMap.put(player.getUuid(), targetPokemon);

            BattleBuilder.INSTANCE.pve((ServerPlayerEntity) player, nearbyEntities.get(0));
        }
    }

    public static void checkPokemonBattleStatus(ServerWorld world) {
        world.getPlayers().forEach(player -> {
            UUID playerId = player.getUuid();
            PokemonEntity activePokemon = activePokemonMap.get(playerId);

            if (activePokemon != null) {
                if (!activePokemon.isBattling()) {
                    activePokemon.discard();
                    activePokemonMap.remove(playerId);

                    player.sendMessage(Text.of("The wild Pokémon has disappeared!"), false);
                }
            }
        });
    }
}
