package com.nothomealone.territory;

import net.minecraft.core.BlockPos;

/**
 * Represents a claimed territory for a worker station.
 * territoryRadius: The radius of the claimed area (where no other stations can be placed)
 * workRadius: The radius where the NPC can actually work (larger than territory)
 */
public record Territory(String dimension, BlockPos center, BlockPos min, BlockPos max, 
                        int territoryRadius, int workRadius) {
    
    /**
     * Gets the minimum position of the work area (larger than territory).
     */
    public BlockPos getWorkAreaMin() {
        return center.offset(-workRadius, -workRadius, -workRadius);
    }
    
    /**
     * Gets the maximum position of the work area (larger than territory).
     */
    public BlockPos getWorkAreaMax() {
        return center.offset(workRadius, workRadius, workRadius);
    }
    
    /**
     * Checks if a position is within the work area.
     */
    public boolean isInWorkArea(BlockPos pos) {
        BlockPos workMin = getWorkAreaMin();
        BlockPos workMax = getWorkAreaMax();
        return pos.getX() >= workMin.getX() && pos.getX() <= workMax.getX() &&
               pos.getY() >= workMin.getY() && pos.getY() <= workMax.getY() &&
               pos.getZ() >= workMin.getZ() && pos.getZ() <= workMax.getZ();
    }
}
