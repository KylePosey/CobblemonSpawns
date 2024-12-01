package com.bloodfall.cobblemonspawns.client;

import com.bloodfall.cobblemonspawns.Area;
import com.bloodfall.cobblemonspawns.AreaManager;
import com.bloodfall.cobblemonspawns.CobblemonSpawns;
import com.bloodfall.cobblemonspawns.CobblemonSpawnsConfig;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class CobblemonSpawnsClient implements ClientModInitializer {

    public static final Identifier OPEN_AREA_GUI = new Identifier("cobblemonspawns", "open_area_gui");

    @Override
    public void onInitializeClient() {
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
}
