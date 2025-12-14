package com.nothomealone.entity.custom;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.level.Level;

/**
 * Builder NPC - Constructs and repairs buildings
 */
public class BuilderEntity extends WorkerEntity {

    public BuilderEntity(EntityType<? extends PathfinderMob> entityType, Level level) {
        super(entityType, level);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return WorkerEntity.createAttributes();
    }

    @Override
    protected void registerWorkerGoals() {
        // TODO: Add builder-specific AI goals
        // - Read building blueprints
        // - Gather building materials
        // - Place blocks according to blueprint
        // - Repair damaged structures
    }

    @Override
    public void performWork() {
        // TODO: Implement builder work logic
    }
}
