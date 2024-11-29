package com.bloodfall.cobblemonspawns.client;

import com.bloodfall.cobblemonspawns.Area;
import com.bloodfall.cobblemonspawns.AreaManager;
import com.bloodfall.cobblemonspawns.CobblemonSpawnsConfig;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
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
        // Register the packet receiver for opening the Area Management Screen
    }

    private static List<Area> deserializeAreas(PacketByteBuf buf) {
        List<Area> areas = new ArrayList<>();
        int size = buf.readInt();
        for (int i = 0; i < size; i++) {
            UUID id = buf.readUuid();
            String name = buf.readString(32767);
            BlockPos minPos = buf.readBlockPos();
            BlockPos maxPos = buf.readBlockPos();
            int configSize = buf.readInt();
            List<CobblemonSpawnsConfig> spawnConfigs = new ArrayList<>();
            for (int j = 0; j < configSize; j++) {
                String pokemonName = buf.readString(32767);
                double spawnRate = buf.readDouble();
                int minLevel = buf.readInt();
                int maxLevel = buf.readInt();
                spawnConfigs.add(new CobblemonSpawnsConfig(pokemonName, spawnRate, minLevel, maxLevel));
            }
            Area area = new Area(name, minPos, maxPos);
            area.id = id; // Set the UUID
            area.getSpawnConfigs().addAll(spawnConfigs);
            areas.add(area);
        }
        return areas;
    }
}
