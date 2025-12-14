package com.nothomealone.entity.custom;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.level.Level;

/**
 * Lumberjack/Forester NPC - Chops down trees and plants saplings
 */
public class LumberjackEntity extends WorkerEntity {

    public LumberjackEntity(EntityType<? extends PathfinderMob> entityType, Level level) {
        super(entityType, level);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return WorkerEntity.createAttributes()
                .add(net.minecraft.world.entity.ai.attributes.Attributes.ATTACK_DAMAGE, 3.0);
    }

    @Override
    protected void registerWorkerGoals() {
        // TODO: Add lumberjack-specific AI goals
        // - Search for nearby trees
        // - Chop down trees
        // - Collect wood
        // - Plant saplings
    }

    @Override
    public void performWork() {
        // TODO: Implement lumberjack work logic
    }
}
