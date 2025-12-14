package com.nothomealone.entity.custom;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.level.Level;

/**
 * Fisher NPC - Catches fish from water sources
 */
public class FisherEntity extends WorkerEntity {

    public FisherEntity(EntityType<? extends PathfinderMob> entityType, Level level) {
        super(entityType, level);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return WorkerEntity.createAttributes();
    }

    @Override
    protected void registerWorkerGoals() {
        // TODO: Add fisher-specific AI goals
        // - Find water sources
        // - Fish for items
        // - Collect fish and treasure
    }

    @Override
    public void performWork() {
        // TODO: Implement fisher work logic
    }
}
