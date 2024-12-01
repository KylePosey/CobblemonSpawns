package com.bloodfall.cobblemonspawns.client;

import com.bloodfall.cobblemonspawns.*;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;
import net.fabricmc.fabric.api.client.screenhandler.v1.ScreenRegistry;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class CobblemonSpawnsClient implements ClientModInitializer {

    @Override
    public void onInitializeClient()
    {
        ClientPlayNetworking.registerGlobalReceiver(CobblemonSpawns.OPEN_AREA_GUI_PACKET_ID, (client, handler, buf, responseSender) -> {
            // Deserialize areas
            List<Area> areas = deserializeAreas(buf);

            // Open the GUI on the client thread
            client.execute(() -> {
                client.setScreen(new AreaManagementScreen(areas));
            });
        });

        ClientPlayNetworking.registerGlobalReceiver(CobblemonSpawns.START_BOUNDING_BOX_PACKET_ID, (client, handler, buf, responseSender) -> {
            BlockPos minPos = buf.readBlockPos();
            BlockPos maxPos = buf.readBlockPos();
            client.execute(() -> {
                BoundingBoxRenderer.addBoundingBox(minPos, maxPos);
            });
        });

        // Handler for stopping rendering
        ClientPlayNetworking.registerGlobalReceiver(CobblemonSpawns.STOP_BOUNDING_BOX_PACKET_ID, (client, handler, buf, responseSender) -> {
            client.execute(BoundingBoxRenderer::clearBoundingBoxes);
        });

        WorldRenderEvents.BEFORE_DEBUG_RENDER.register(BoundingBoxRenderer::render);
    }

    private List<Area> deserializeAreas(PacketByteBuf buf) {
        int areaCount = buf.readInt();
        List<Area> areas = new ArrayList<>();
        for (int i = 0; i < areaCount; i++) {
            UUID id = buf.readUuid();
            String name = buf.readString(32767);
            BlockPos minPos = buf.readBlockPos();
            BlockPos maxPos = buf.readBlockPos();
            Area area = new Area(id, name, minPos, maxPos);
            areas.add(area);
        }
        return areas;
    }
}
