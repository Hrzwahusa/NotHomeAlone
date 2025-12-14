package com.nothomealone.entity.custom;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.level.Level;

/**
 * Miner NPC - Mines ores and resources underground
 */
public class MinerEntity extends WorkerEntity {

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
}
