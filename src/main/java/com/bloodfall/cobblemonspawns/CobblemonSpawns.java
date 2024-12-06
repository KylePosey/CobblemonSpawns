package com.bloodfall.cobblemonspawns;

import com.bloodfall.cobblemonspawns.client.AreaManagementScreen;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.client.MinecraftClient;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
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

    public static final Identifier DELETE_AREA_PACKET_ID = new Identifier(MOD_ID, "delete_area");
    public static final Identifier UPDATE_AREA_PACKET_ID = new Identifier(MOD_ID, "update_area");
    public static final Identifier OPEN_AREA_GUI_PACKET_ID = new Identifier(MOD_ID, "open_area_gui");
    public static final Identifier REFRESH_DEBUG_VIEW_PACKET = new Identifier(MOD_ID, "refresh_debug_view");
    public static final Identifier START_BOUNDING_BOX_PACKET_ID = new Identifier("cobblemonspawns", "start_bounding_box");
    public static final Identifier STOP_BOUNDING_BOX_PACKET_ID = new Identifier("cobblemonspawns", "stop_bounding_box");
    public static final Identifier FLOATING_TEXT_PACKET_ID = new Identifier(MOD_ID, "floating_text");
    public static final Identifier CLEAR_FLOATING_TEXT_PACKET_ID = new Identifier(MOD_ID, "clear_floating_text");

    public static ServerWorld sWorld;

    @Override
    public void onInitialize()
    {
        registerModItems();

        CommandRegistrationCallback.EVENT.register((dispatcher, dedicated, environment) -> {
            AreaCommands.register(dispatcher);
        });

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

            server.execute(() -> {
                if (player.getWorld() instanceof ServerWorld serverWorld) {
                    sWorld = serverWorld;
                    AreaManager manager = AreaManager.get(serverWorld);
                    Area area = manager.getArea(areaId);

                    if (area != null) {
                        // Validate name uniqueness
                        boolean duplicateName = manager.getAllAreas().stream()
                                .anyMatch(a -> !a.getId().equals(areaId) && a.getName().equalsIgnoreCase(name));
                        if (duplicateName) {
                            player.sendMessage(Text.literal("An area with this name already exists."), false);
                            return;
                        }

                        area.setName(name);
                        area.setMinPos(minPos);
                        area.setMaxPos(maxPos);

                        // Mark the manager as dirty to save changes
                        manager.markDirty();

                        player.sendMessage(Text.literal("Area updated: " + area.getName()), false);
                    } else {
                        player.sendMessage(Text.literal("Area not found."), false);
                    }
                }
            });
        });

        ServerPlayNetworking.registerGlobalReceiver(DELETE_AREA_PACKET_ID, (server, player, handler, buf, responseSender) -> {
            UUID areaId = buf.readUuid();

            server.execute(() -> {
                ServerWorld world = (ServerWorld) player.getWorld();
                AreaManager manager = AreaManager.get(world);
                Area areaToDelete = manager.getArea(areaId);
                if (areaToDelete != null) {
                    manager.removeArea(areaId);
                    player.sendMessage(Text.literal("Area deleted: " + areaToDelete.getName()), false);
                } else {
                    player.sendMessage(Text.literal("Area not found."), false);
                }
            });
        });

        // Update Area Packet Handler
        ServerPlayNetworking.registerGlobalReceiver(UPDATE_AREA_PACKET_ID, (server, player, handler, buf, responseSender) -> {
            UUID areaId = buf.readUuid();
            String newName = buf.readString(32767);
            BlockPos newMinPos = buf.readBlockPos();
            BlockPos newMaxPos = buf.readBlockPos();

            server.execute(() -> {
                ServerWorld world = (ServerWorld) player.getWorld();
                AreaManager manager = AreaManager.get(world);
                Area areaToUpdate = manager.getArea(areaId);
                if (areaToUpdate != null) {
                    // Validate name uniqueness
                    boolean duplicateName = manager.getAllAreas().stream()
                            .anyMatch(area -> !area.getId().equals(areaId) && area.getName().equalsIgnoreCase(newName));
                    if (duplicateName) {
                        player.sendMessage(Text.literal("An area with this name already exists."), false);
                        return;
                    }

                    areaToUpdate.setName(newName);
                    areaToUpdate.setMinPos(newMinPos);
                    areaToUpdate.setMaxPos(newMaxPos);
                    manager.markDirty();

                    player.sendMessage(Text.literal("Area updated: " + areaToUpdate.getName()), false);
                } else {
                    player.sendMessage(Text.literal("Area not found."), false);
                }
            });
        });

        ServerPlayNetworking.registerGlobalReceiver(CobblemonSpawns.REFRESH_DEBUG_VIEW_PACKET, (server, player, handler, buf, responseSender) -> {
            // Handle the packet on the server thread
            server.execute(() -> {
                // Retrieve any additional data from buf if sent
                // For example:
                // UUID areaId = buf.readUuid();
                // BlockPos pos = buf.readBlockPos();

                // Perform the refreshDebugView action
                AreaCommands.refreshDebugView(server.getOverworld(), player);
            });
        });

        ServerPlayNetworking.registerGlobalReceiver(CLEAR_FLOATING_TEXT_PACKET_ID, (server, player, handler, buf, responseSender) -> {
            server.execute(() -> {
                // You can handle additional logic here if needed
                // For now, it's just a signal to the client to clear texts
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
