package com.bloodfall.cobblemonspawns;

import com.bloodfall.cobblemonspawns.CobblemonSpawnsConfig;

import net.minecraft.util.math.BlockPos;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class Area {
    public UUID id; // Unique identifier for the area
    private String name;
    private BlockPos minPos;
    private BlockPos maxPos;
    private List<CobblemonSpawnsConfig> spawnConfigs;

    public Area(String name, BlockPos minPos, BlockPos maxPos) {
        this.id = UUID.randomUUID();
        this.name = name;
        this.minPos = minPos;
        this.maxPos = maxPos;
        this.spawnConfigs = new ArrayList<>();
    }

    public Area(UUID id, String name, BlockPos minPos, BlockPos maxPos) {
        this.id = id;
        this.name = name;
        this.minPos = minPos;
        this.maxPos = maxPos;
        this.spawnConfigs = new ArrayList<>();
    }

    // Getters and Setters
    public UUID getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public BlockPos getMinPos() {
        return minPos;
    }

    public BlockPos getMaxPos() {
        return maxPos;
    }

    public List<CobblemonSpawnsConfig> getSpawnConfigs() {
        return spawnConfigs;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setMinPos(BlockPos minPos) {
        this.minPos = minPos;
    }

    public void setMaxPos(BlockPos maxPos) {
        this.maxPos = maxPos;
    }

    public void addSpawnConfig(CobblemonSpawnsConfig config) {
        this.spawnConfigs.add(config);
    }

    public void removeSpawnConfig(CobblemonSpawnsConfig config) {
        this.spawnConfigs.remove(config);
    }

    // Check if a position is within this area
    public boolean contains(BlockPos pos) {
        int x = pos.getX();
        int y = pos.getY();
        int z = pos.getZ();

        return x >= minPos.getX() && x <= maxPos.getX()
                && y >= minPos.getY() && y <= maxPos.getY()
                && z >= minPos.getZ() && z <= maxPos.getZ();
    }
}
