package com.nothomealone.entity.custom;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.Level;

/**
 * Hunter NPC - Hunts animals for food and resources
 */
public class HunterEntity extends WorkerEntity {

    public HunterEntity(EntityType<? extends PathfinderMob> entityType, Level level) {
        super(entityType, level);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return WorkerEntity.createAttributes()
                .add(Attributes.ATTACK_DAMAGE, 4.0)
                .add(Attributes.ATTACK_SPEED, 1.2);
    }

    @Override
    protected void registerWorkerGoals() {
        // TODO: Add hunter-specific AI goals
        // - Search for animals
        // - Hunt animals
        // - Collect meat and leather
    }

    @Override
    public void performWork() {
        // TODO: Implement hunter work logic
    }
}
