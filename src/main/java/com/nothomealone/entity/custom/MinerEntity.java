package com.nothomealone.entity.custom;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.level.Level;

/**
 * Miner NPC - Mines ores and resources underground
 * Works primarily vertically (downward) and expands horizontally as depth increases
 */
public class MinerEntity extends WorkerEntity {

    private int currentMiningDepth = 0;
    private int maxMiningDepth = 64; // Can mine up to 64 blocks down
    private int horizontalExpansion = 0; // Expands horizontally as it goes deeper

    public MinerEntity(EntityType<? extends PathfinderMob> entityType, Level level) {
        super(entityType, level);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return WorkerEntity.createAttributes()
                .add(net.minecraft.world.entity.ai.attributes.Attributes.ATTACK_DAMAGE, 2.0);
    }

    @Override
    protected void registerWorkerGoals() {
        // TODO: Add miner-specific AI goals
        // - Navigate to mining area
        // - Mine ores and stone
        // - Collect resources
        // - Return to storage
    }

    @Override
    public void performWork() {
        // TODO: Implement miner work logic
    }

    /**
     * Checks if a position is within the miner's current work area.
     * Miners work primarily vertically (downward) and expand horizontally as they go deeper.
     */
    @Override
    public boolean isWithinWorkArea(net.minecraft.core.BlockPos pos) {
        if (stationPos == null) return true;

        // Calculate current horizontal radius based on depth
        int currentHorizontalRadius = workRadius + (horizontalExpansion / 8);

        // Check if within horizontal radius from station
        double horizontalDistSqr = Math.pow(pos.getX() - stationPos.getX(), 2) + 
                                   Math.pow(pos.getZ() - stationPos.getZ(), 2);
        
        if (horizontalDistSqr > currentHorizontalRadius * currentHorizontalRadius) {
            return false;
        }

        // Check vertical range (must be below station, within max depth)
        int relativeY = pos.getY() - stationPos.getY();
        return relativeY <= 0 && relativeY >= -maxMiningDepth;
    }

    /**
     * Updates mining progress - expands area as claim is worked out.
     */
    public void expandMiningArea() {
        if (currentMiningDepth < maxMiningDepth) {
            currentMiningDepth++;
            if (currentMiningDepth % 8 == 0) {
                horizontalExpansion++;
            }
        }
    }

    public int getCurrentMiningDepth() {
        return currentMiningDepth;
    }

    public int getMaxMiningDepth() {
        return maxMiningDepth;
    }
}
