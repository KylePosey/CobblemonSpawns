package com.bloodfall.cobblemonspawns;

import net.minecraft.nbt.NbtCompound;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.world.PersistentState;

import java.util.ArrayList;
import java.util.List;

public class AreaStorage extends PersistentState {
    private final List<Area> areas = new ArrayList<>();

    public List<Area> getAreas() {
        return areas;
    }

    public void addArea(Area area) {
        areas.add(area);
        markDirty();
    }

    @Override
    public NbtCompound writeNbt(NbtCompound nbt) {
        // Serialize areas to NBT
        // TODO: Implement serialization
        return nbt;
    }

    public static AreaStorage fromNbt(NbtCompound nbt) {
        AreaStorage storage = new AreaStorage();
        // Deserialize areas from NBT
        // TODO: Implement deserialization
        return storage;
    }

    public static AreaStorage get(ServerWorld world) {
        return world.getPersistentStateManager().getOrCreate(AreaStorage::fromNbt, AreaStorage::new, "cobblemonspawns_areas");
    }
}
