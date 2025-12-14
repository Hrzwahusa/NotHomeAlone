package com.nothomealone.entity.custom;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.level.Level;

/**
 * Farmer/Rancher NPC - Plants crops and breeds animals
 */
public class FarmerEntity extends WorkerEntity {

    public FarmerEntity(EntityType<? extends PathfinderMob> entityType, Level level) {
        super(entityType, level);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return WorkerEntity.createAttributes();
    }

    @Override
    protected void registerWorkerGoals() {
        // TODO: Add farmer-specific AI goals
        // - Plant crops
        // - Harvest crops
        // - Feed and breed animals
        // - Collect animal products (eggs, wool, milk)
    }

    @Override
    public void performWork() {
        // TODO: Implement farmer work logic
    }
}
