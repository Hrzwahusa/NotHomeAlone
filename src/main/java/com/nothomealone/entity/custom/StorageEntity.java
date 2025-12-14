package com.nothomealone.entity.custom;

import com.nothomealone.territory.SettlementHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.level.Level;

import java.util.List;

/**
 * Storage/Warehouse Keeper NPC - Manages and organizes settlement resources
 * Must be able to reach all other stations to collect and distribute items
 */
public class StorageEntity extends WorkerEntity {

    public StorageEntity(EntityType<? extends PathfinderMob> entityType, Level level) {
        super(entityType, level);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return WorkerEntity.createAttributes();
    }

    @Override
    protected void registerWorkerGoals() {
        // TODO: Add storage keeper-specific AI goals
        // - Collect items from workers
        // - Organize items in storage chests
        // - Distribute resources to workers who need them
        // - Keep inventory of settlement resources
    }

    @Override
    public void performWork() {
        // TODO: Implement storage keeper work logic
    }

    /**
     * Gets all station positions in this settlement.
     */
    public List<BlockPos> getAllStations() {
        return SettlementHelper.getAllStations(this.level());
    }

    /**
     * Finds stations within collection range.
     */
    public List<BlockPos> getStationsInRange() {
        if (stationPos == null) return List.of();
        return SettlementHelper.getStationsInRange(this.level(), stationPos, workRadius);
    }

    /**
     * Checks if this storage keeper can reach all stations.
     */
    public boolean canReachAllStations() {
        if (stationPos == null) return false;
        return SettlementHelper.canReachAllStations(this.level(), stationPos, workRadius);
    }

    /**
     * Gets list of unreachable stations (for warning/notification).
     */
    public List<BlockPos> getUnreachableStations() {
        if (stationPos == null) return List.of();
        return SettlementHelper.getUnreachableStations(this.level(), stationPos, workRadius);
    }
}
