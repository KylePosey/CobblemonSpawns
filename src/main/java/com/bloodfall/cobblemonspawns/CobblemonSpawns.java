package com.bloodfall.cobblemonspawns;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static com.bloodfall.cobblemonspawns.ModItems.registerModItems;

public class CobblemonSpawns implements ModInitializer {
    public static final String MOD_ID = "cobblemonspawns";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    public static final Identifier START_BOUNDING_BOX_PACKET_ID = new Identifier("cobblemonspawns", "start_bounding_box");
    public static final Identifier STOP_BOUNDING_BOX_PACKET_ID = new Identifier("cobblemonspawns", "stop_bounding_box");


    @Override
    public void onInitialize()
    {
        registerModItems();

        // Register the /area commands
        CommandRegistrationCallback.EVENT.register((dispatcher, dedicated, environment) -> {
            AreaCommands.register(dispatcher);
        });

        // Register player tick event for movement tracking
        ServerTickEvents.END_WORLD_TICK.register(world -> {
            if (!world.isClient()) {
                AreaManager manager = AreaManager.get(world);
                world.getPlayers().forEach(player -> MovementTracker.onPlayerTick(player, manager));
            }
        });

        ServerPlayNetworking.registerGlobalReceiver(new Identifier("cobblemonspawns", "update_area"), (server, player, handler, buf, responseSender) -> {
            UUID areaId = buf.readUuid();
            String name = buf.readString(32767);
            BlockPos minPos = buf.readBlockPos();
            BlockPos maxPos = buf.readBlockPos();

            int configSize = buf.readInt();
            List<CobblemonSpawnsConfig> configs = new ArrayList<>();
            for (int i = 0; i < configSize; i++) {
                String pokemonName = buf.readString(32767);
                double spawnRate = buf.readDouble();
                int minLevel = buf.readInt();
                int maxLevel = buf.readInt();
                configs.add(new CobblemonSpawnsConfig(pokemonName, spawnRate, minLevel, maxLevel));
            }

            // Update the AreaManager on the server
            server.execute(() -> {
                if (player.getWorld() instanceof ServerWorld serverWorld) {
                    AreaManager manager = AreaManager.get(serverWorld);
                    Area area = manager.getArea(areaId);

                    if (area != null) {
                        area.setName(name);
                        area.setMinPos(minPos);
                        area.setMaxPos(maxPos);
                        area.getSpawnConfigs().clear();
                        area.getSpawnConfigs().addAll(configs);

                        // Mark the manager as dirty to save changes
                        manager.markDirty();
                    }
                }
            });
        });

    }

    public static void sendStartBoundingBoxToClient(ServerPlayerEntity player, BlockPos minPos, BlockPos maxPos) {
        PacketByteBuf buf = PacketByteBufs.create();
        buf.writeBlockPos(minPos);
        buf.writeBlockPos(maxPos);
        ServerPlayNetworking.send(player, START_BOUNDING_BOX_PACKET_ID, buf);
    }

    public static void sendStopBoundingBoxToClient(ServerPlayerEntity player) {
        PacketByteBuf buf = PacketByteBufs.create();
        ServerPlayNetworking.send(player, STOP_BOUNDING_BOX_PACKET_ID, buf);
    }
}
