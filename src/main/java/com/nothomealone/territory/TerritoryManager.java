package com.nothomealone.territory;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;

import java.util.ArrayList;
import java.util.List;

/**
 * Manages territory claims for worker stations.
 * Prevents overlapping territories.
 */
public class TerritoryManager {
    private static final List<Territory> territories = new ArrayList<>();

    /**
     * Attempts to claim a territory around the given position.
     * @param center The center position (station block)
     * @param territoryRadius The radius of the claimed area (prevents other stations)
     * @param workRadius The radius where the NPC can work
     * @return The claimed Territory, or null if the area overlaps with existing territories
     */
    public static Territory claimTerritory(Level level, BlockPos center, int territoryRadius, int workRadius) {
        BlockPos min = center.offset(-territoryRadius, -territoryRadius, -territoryRadius);
        BlockPos max = center.offset(territoryRadius, territoryRadius, territoryRadius);

        // Check for overlaps
        if (isAreaClaimed(level, min, max)) {
            return null;
        }

        Territory territory = new Territory(level.dimension().location().toString(), center, min, max, 
                                           territoryRadius, workRadius);
        territories.add(territory);
        return territory;
    }

    /**
     * Checks if any part of the given area is already claimed.
     */
    public static boolean isAreaClaimed(Level level, BlockPos min, BlockPos max) {
        String dimension = level.dimension().location().toString();
        
        for (Territory territory : territories) {
            if (!territory.dimension().equals(dimension)) {
                continue;
            }

            // Check for overlap
            if (boxesOverlap(min, max, territory.min(), territory.max())) {
                return true;
            }
        }
        return false;
    }

    /**
     * Releases a territory claim.
     */
    public static void releaseTerritory(Level level, BlockPos center) {
        String dimension = level.dimension().location().toString();
        territories.removeIf(t -> t.dimension().equals(dimension) && t.center().equals(center));
    }

    /**
     * Gets the territory at a specific position.
     */
    public static Territory getTerritoryAt(Level level, BlockPos pos) {
        String dimension = level.dimension().location().toString();
        
        for (Territory territory : territories) {
            if (!territory.dimension().equals(dimension)) {
                continue;
            }

            if (isPositionInTerritory(pos, territory.min(), territory.max())) {
                return territory;
            }
        }
        return null;
    }

    private static boolean boxesOverlap(BlockPos min1, BlockPos max1, BlockPos min2, BlockPos max2) {
        return min1.getX() <= max2.getX() && max1.getX() >= min2.getX() &&
               min1.getY() <= max2.getY() && max1.getY() >= min2.getY() &&
               min1.getZ() <= max2.getZ() && max1.getZ() >= min2.getZ();
    }

    private static boolean isPositionInTerritory(BlockPos pos, BlockPos min, BlockPos max) {
        return pos.getX() >= min.getX() && pos.getX() <= max.getX() &&
               pos.getY() >= min.getY() && pos.getY() <= max.getY() &&
               pos.getZ() >= min.getZ() && pos.getZ() <= max.getZ();
    }

    public static List<Territory> getAllTerritories() {
        return new ArrayList<>(territories);
    }

    public static void clearAll() {
        territories.clear();
    }
}
