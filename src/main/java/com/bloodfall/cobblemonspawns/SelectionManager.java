package com.bloodfall.cobblemonspawns;

import net.minecraft.util.math.BlockPos;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Manages area selections for players using the AreaSelectionTool.
 */
public class SelectionManager {
    private static final Map<UUID, SelectionData> playerSelections = new HashMap<>();

    /**
     * Sets the first point for a player's area selection.
     *
     * @param playerId The UUID of the player.
     * @param pos      The BlockPos selected by the player.
     */
    public static void setFirstPoint(UUID playerId, BlockPos pos) {
        SelectionData data = playerSelections.getOrDefault(playerId, new SelectionData());
        data.setFirstPos(pos);
        playerSelections.put(playerId, data);
    }

    /**
     * Sets the second point for a player's area selection.
     *
     * @param playerId The UUID of the player.
     * @param pos      The BlockPos selected by the player.
     */
    public static void setSecondPoint(UUID playerId, BlockPos pos) {
        SelectionData data = playerSelections.getOrDefault(playerId, new SelectionData());
        data.setSecondPos(pos);
        playerSelections.put(playerId, data);
    }

    /**
     * Retrieves and removes the selection data for a player.
     *
     * @param playerId The UUID of the player.
     * @return The SelectionData containing both points, or null if incomplete.
     */
    public static SelectionData consumeSelection(UUID playerId) {
        return playerSelections.remove(playerId);
    }

    /**
     * Checks if a player has both points selected.
     *
     * @param playerId The UUID of the player.
     * @return True if both points are set, false otherwise.
     */
    public static boolean hasCompleteSelection(UUID playerId) {
        SelectionData data = playerSelections.get(playerId);
        return data != null && data.getFirstPos() != null && data.getSecondPos() != null;
    }

    /**
     * Inner class to hold selection data.
     */
    public static class SelectionData {
        private BlockPos firstPos;
        private BlockPos secondPos;

        public BlockPos getFirstPos() {
            return firstPos;
        }

        public void setFirstPos(BlockPos firstPos) {
            this.firstPos = firstPos;
        }

        public BlockPos getSecondPos() {
            return secondPos;
        }

        public void setSecondPos(BlockPos secondPos) {
            this.secondPos = secondPos;
        }
    }
}
