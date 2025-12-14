package com.nothomealone.entity.ai;

import com.nothomealone.entity.custom.WorkerEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.level.pathfinder.Path;

import java.util.EnumSet;

/**
 * AI Goal for workers to return to their station when idle.
 */
public class ReturnToStationGoal extends Goal {
    private final WorkerEntity worker;
    private final double speedModifier;
    private Path path;

    public ReturnToStationGoal(WorkerEntity worker, double speedModifier) {
        this.worker = worker;
        this.speedModifier = speedModifier;
        this.setFlags(EnumSet.of(Goal.Flag.MOVE));
    }

    @Override
    public boolean canUse() {
        BlockPos stationPos = worker.getStationPos();
        if (stationPos == null) return false;
        
        // Return to station if far away and not doing anything important
        double distanceSq = worker.blockPosition().distSqr(stationPos);
        return distanceSq > 64; // More than 8 blocks away
    }

    @Override
    public boolean canContinueToUse() {
        BlockPos stationPos = worker.getStationPos();
        if (stationPos == null) return false;
        
        // Keep going until close to station
        double distanceSq = worker.blockPosition().distSqr(stationPos);
        return distanceSq > 9 && !worker.getNavigation().isDone(); // More than 3 blocks away
    }

    @Override
    public void start() {
        BlockPos stationPos = worker.getStationPos();
        if (stationPos != null) {
            path = worker.getNavigation().createPath(stationPos, 2);
            if (path != null) {
                worker.getNavigation().moveTo(path, speedModifier);
            }
        }
    }

    @Override
    public void stop() {
        path = null;
        worker.getNavigation().stop();
    }
}
