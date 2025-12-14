package com.nothomealone.territory;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Helper class to find and manage all stations in a settlement.
 * Used by Builder and Storage NPCs to locate other stations.
 */
public class SettlementHelper {
    
    /**
     * Gets all station positions in the same dimension.
     * @param level The level/dimension to search in
     * @return List of all station center positions
     */
    public static List<BlockPos> getAllStations(Level level) {
        String dimension = level.dimension().location().toString();
        return TerritoryManager.getAllTerritories().stream()
                .filter(t -> t.dimension().equals(dimension))
                .map(Territory::center)
                .collect(Collectors.toList());
    }
    
    /**
     * Gets all stations within a certain distance of a position.
     * @param level The level
     * @param center The center position
     * @param maxDistance Maximum distance to search
     * @return List of station positions within range
     */
    public static List<BlockPos> getStationsInRange(Level level, BlockPos center, double maxDistance) {
        String dimension = level.dimension().location().toString();
        double maxDistSqr = maxDistance * maxDistance;
        
        return TerritoryManager.getAllTerritories().stream()
                .filter(t -> t.dimension().equals(dimension))
                .filter(t -> t.center().distSqr(center) <= maxDistSqr)
                .map(Territory::center)
                .collect(Collectors.toList());
    }
    
    /**
     * Finds the nearest station of a specific type.
     * @param level The level
     * @param fromPos Starting position
     * @param workerType The worker type to find (e.g., "storage", "builder")
     * @return The nearest station position, or null if none found
     */
    public static BlockPos findNearestStation(Level level, BlockPos fromPos, String workerType) {
        // For now, just return nearest station
        // Later: can filter by block type/worker type
        return getStationsInRange(level, fromPos, 256).stream()
                .min((a, b) -> Double.compare(fromPos.distSqr(a), fromPos.distSqr(b)))
                .orElse(null);
    }
    
    /**
     * Checks if Builder/Storage can reach a position from their station.
     * These NPCs need to be able to reach all other stations.
     */
    public static boolean canReachAllStations(Level level, BlockPos builderStation, int workRadius) {
        List<BlockPos> allStations = getAllStations(level);
        double maxDistSqr = workRadius * workRadius;
        
        for (BlockPos station : allStations) {
            if (station.equals(builderStation)) continue;
            
            if (builderStation.distSqr(station) > maxDistSqr) {
                return false;
            }
        }
        return true;
    }
    
    /**
     * Gets all stations that this Builder/Storage cannot reach.
     */
    public static List<BlockPos> getUnreachableStations(Level level, BlockPos builderStation, int workRadius) {
        List<BlockPos> unreachable = new ArrayList<>();
        List<BlockPos> allStations = getAllStations(level);
        double maxDistSqr = workRadius * workRadius;
        
        for (BlockPos station : allStations) {
            if (station.equals(builderStation)) continue;
            
            if (builderStation.distSqr(station) > maxDistSqr) {
                unreachable.add(station);
            }
        }
        return unreachable;
    }
}
