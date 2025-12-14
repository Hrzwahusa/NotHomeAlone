package com.nothomealone.entity.custom;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

/**
 * Base class for all worker entities in the settlement system.
 * Provides common attributes and behaviors for all worker types.
 */
public abstract class WorkerEntity extends PathfinderMob {
    
    protected BlockPos stationPos;
    protected int workRadius = 32; // Default work radius
    
    protected WorkerEntity(EntityType<? extends PathfinderMob> entityType, Level level) {
        super(entityType, level);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return PathfinderMob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 20.0)
                .add(Attributes.MOVEMENT_SPEED, 0.25)
                .add(Attributes.FOLLOW_RANGE, 32.0);
    }

    @Override
    protected void registerGoals() {
        System.out.println("[WorkerEntity] registerGoals() called for " + this.getClass().getSimpleName());
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(1, new PanicGoal(this, 1.4));
        this.goalSelector.addGoal(5, new WaterAvoidingRandomStrollGoal(this, 1.0));
        this.goalSelector.addGoal(6, new LookAtPlayerGoal(this, Player.class, 6.0F));
        this.goalSelector.addGoal(7, new RandomLookAroundGoal(this));
        
        registerWorkerGoals();
        System.out.println("[WorkerEntity] Goals registered, total goals: " + this.goalSelector.getAvailableGoals().size());
    }

    /**
     * Override this method to add worker-specific AI goals.
     */
    protected abstract void registerWorkerGoals();

    /**

    /**
     * Sets the home station position and work radius for this worker.
     */
    public void setHomeStation(BlockPos pos, int radius) {
        this.stationPos = pos;
        this.workRadius = radius;
    }

    /**
     * Gets the home station position.
     */
    public BlockPos getStationPos() {
        return stationPos;
    }

    /**
     * Gets the work radius (how far from station this worker can operate).
     */
    public int getWorkRadius() {
        return workRadius;
    }

    /**
     * Checks if a position is within this worker's work area.
     */
    public boolean isWithinWorkArea(BlockPos pos) {
        if (stationPos == null) {
            return true; // No restrictions if no station assigned
        }
        return stationPos.distSqr(pos) <= workRadius * workRadius;
    }

    /**
     * Checks if the worker is too far from their station.
     */
    public boolean isTooFarFromStation() {
        if (stationPos == null) {
            return false;
        }
        return this.blockPosition().distSqr(stationPos) > workRadius * workRadius;
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        if (stationPos != null) {
            tag.putInt("StationX", stationPos.getX());
            tag.putInt("StationY", stationPos.getY());
            tag.putInt("StationZ", stationPos.getZ());
        }
        tag.putInt("WorkRadius", workRadius);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        if (tag.contains("StationX") && tag.contains("StationY") && tag.contains("StationZ")) {
            int x = tag.getInt("StationX");
            int y = tag.getInt("StationY");
            int z = tag.getInt("StationZ");
            this.stationPos = new BlockPos(x, y, z);
        }
        if (tag.contains("WorkRadius")) {
            this.workRadius = tag.getInt("WorkRadius");
        }
    }

    /**
     * Called when the worker should perform their work task.
     */
    public abstract void performWork();
}
