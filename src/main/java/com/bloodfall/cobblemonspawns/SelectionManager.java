package com.bloodfall.cobblemonspawns;

import net.minecraft.util.math.BlockPos;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class SelectionManager {

    private static final Map<UUID, SelectionData> selections = new ConcurrentHashMap<>();

    public static class SelectionData {
        private BlockPos firstPos;
        private BlockPos secondPos;

        public SelectionData(BlockPos firstPos) {
            this.firstPos = firstPos;
        }

        public BlockPos getFirstPos() {
            return firstPos;
        }

        public void setSecondPos(BlockPos secondPos) {
            this.secondPos = secondPos;
        }

        public BlockPos getSecondPos() {
            return secondPos;
        }

        public boolean isComplete() {
            return firstPos != null && secondPos != null;
        }
    }

    public static boolean hasCompleteFirstPoint(UUID playerId) {
        SelectionData data = selections.get(playerId);
        return data != null && data.getFirstPos() != null;
    }

    public static boolean hasCompleteSelection(UUID playerId) {
        SelectionData data = selections.get(playerId);
        return data != null && data.isComplete();
    }

    public static void setFirstPoint(UUID playerId, BlockPos pos) {
        selections.put(playerId, new SelectionData(pos));
        CobblemonSpawns.LOGGER.info("Player {} set first point at {}", playerId, pos);
    }

    public static void setSecondPoint(UUID playerId, BlockPos pos) {
        SelectionData data = selections.get(playerId);
        if (data != null) {
            data.setSecondPos(pos);
            CobblemonSpawns.LOGGER.info("Player {} set second point at {}", playerId, pos);
        } else {
            CobblemonSpawns.LOGGER.warn("Player {} attempted to set second point without a first point.", playerId);
        }
    }

    public static SelectionData consumeSelection(UUID playerId) {
        SelectionData data = selections.remove(playerId);
        if (data != null) {
            CobblemonSpawns.LOGGER.info("Player {} consumed selection: First Pos {}, Second Pos {}", playerId, data.getFirstPos(), data.getSecondPos());
        } else {
            CobblemonSpawns.LOGGER.warn("Player {} attempted to consume selection but none was found.", playerId);
        }
        return data;
    }
}
