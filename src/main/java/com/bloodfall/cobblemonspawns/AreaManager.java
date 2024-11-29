package com.bloodfall.cobblemonspawns;

import com.bloodfall.cobblemonspawns.Area;
import com.bloodfall.cobblemonspawns.CobblemonSpawnsConfig;
import com.google.gson.*;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.PersistentState;
import net.minecraft.world.World;

import java.lang.reflect.Type;
import java.util.*;

public class AreaManager extends PersistentState {
    private static final String DATA_NAME = "cobblemonspawns_areas";
    private Map<UUID, Area> areas;

    public AreaManager() {
        this.areas = new HashMap<>();
    }

    private static List<Area> clientAreas = new ArrayList<>();

    // Load data from NBT
    public static AreaManager fromTag(NbtCompound  tag) {
        AreaManager manager = new AreaManager();
        JsonParser parser = new JsonParser();
        if (tag.contains(DATA_NAME, 8)) { // 8 is the ID for strings
            String jsonString = tag.getString(DATA_NAME);
            JsonArray jsonArray = parser.parse(jsonString).getAsJsonArray();
            for (JsonElement element : jsonArray) {
                JsonObject obj = element.getAsJsonObject();
                UUID id = UUID.fromString(obj.get("id").getAsString());
                String name = obj.get("name").getAsString();
                JsonObject minObj = obj.get("minPos").getAsJsonObject();
                BlockPos minPos = new BlockPos(
                        minObj.get("x").getAsInt(),
                        minObj.get("y").getAsInt(),
                        minObj.get("z").getAsInt()
                );
                JsonObject maxObj = obj.get("maxPos").getAsJsonObject();
                BlockPos maxPos = new BlockPos(
                        maxObj.get("x").getAsInt(),
                        maxObj.get("y").getAsInt(),
                        maxObj.get("z").getAsInt()
                );

                Area area = new Area(name, minPos, maxPos);
                area.id = id; // Assign the UUID

                JsonArray spawnArray = obj.get("spawnConfigs").getAsJsonArray();
                for (JsonElement spawnElement : spawnArray) {
                    JsonObject spawnObj = spawnElement.getAsJsonObject();
                    String pokemonName = spawnObj.get("pokemonName").getAsString();
                    double spawnRate = spawnObj.get("spawnRate").getAsDouble();
                    int minLevel = spawnObj.get("minLevel").getAsInt();
                    int maxLevel = spawnObj.get("maxLevel").getAsInt();

                    CobblemonSpawnsConfig spawnConfig = new CobblemonSpawnsConfig(pokemonName, spawnRate, minLevel, maxLevel);
                    area.addSpawnConfig(spawnConfig);
                }

                manager.areas.put(id, area);
            }
        }
        return manager;
    }

    // Save data to NBT
    @Override
    public NbtCompound writeNbt(NbtCompound tag) {
        JsonArray jsonArray = new JsonArray();
        for (Area area : areas.values()) {
            JsonObject obj = new JsonObject();
            obj.addProperty("id", area.getId().toString());
            obj.addProperty("name", area.getName());

            JsonObject minObj = new JsonObject();
            minObj.addProperty("x", area.getMinPos().getX());
            minObj.addProperty("y", area.getMinPos().getY());
            minObj.addProperty("z", area.getMinPos().getZ());
            obj.add("minPos", minObj);

            JsonObject maxObj = new JsonObject();
            maxObj.addProperty("x", area.getMaxPos().getX());
            maxObj.addProperty("y", area.getMaxPos().getY());
            maxObj.addProperty("z", area.getMaxPos().getZ());
            obj.add("maxPos", maxObj);

            JsonArray spawnArray = new JsonArray();
            for (CobblemonSpawnsConfig spawnConfig : area.getSpawnConfigs()) {
                JsonObject spawnObj = new JsonObject();
                spawnObj.addProperty("pokemonName", spawnConfig.getCobblemonName());
                spawnObj.addProperty("spawnRate", spawnConfig.getSpawnRate());
                spawnObj.addProperty("minLevel", spawnConfig.getMinLevel());
                spawnObj.addProperty("maxLevel", spawnConfig.getMaxLevel());
                spawnArray.add(spawnObj);
            }
            obj.add("spawnConfigs", spawnArray);

            jsonArray.add(obj);
        }
        tag.putString(DATA_NAME, jsonArray.toString());
        return tag;
    }

    // Singleton pattern to access the manager
    public static AreaManager get(ServerWorld world) {
        return (AreaManager) world.getPersistentStateManager().getOrCreate(
                AreaManager::fromTag,
                AreaManager::new,
                DATA_NAME
        );
    }

    public static void updateClientAreas(List<Area> updatedAreas) {
        clientAreas = updatedAreas;
    }

    public static List<Area> getClientAreas(ClientWorld world) {
        return clientAreas;
    }

    // CRUD Operations
    public void addArea(Area area) {
        areas.put(area.getId(), area);
        markDirty();
    }

    public void removeArea(UUID id) {
        areas.remove(id);
        markDirty();
    }

    public Collection<Area> getAllAreas() {
        return areas.values();
    }

    public Area getArea(UUID id) {
        return areas.get(id);
    }
}
