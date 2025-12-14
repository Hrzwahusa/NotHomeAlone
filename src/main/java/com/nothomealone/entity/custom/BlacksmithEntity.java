package com.nothomealone.entity.custom;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.level.Level;

/**
 * Blacksmith NPC - Crafts tools, weapons, and armor for the settlement
 */
public class BlacksmithEntity extends WorkerEntity {

    public BlacksmithEntity(EntityType<? extends PathfinderMob> entityType, Level level) {
        super(entityType, level);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return WorkerEntity.createAttributes();
    }

    @Override
    protected void registerWorkerGoals() {
        // TODO: Add blacksmith-specific AI goals
        // - Craft tools and weapons
        // - Repair damaged equipment
        // - Smelt ores into ingots
        // - Work at anvil and furnace
    }

    @Override
    public void performWork() {
        // TODO: Implement blacksmith work logic
    }
}
