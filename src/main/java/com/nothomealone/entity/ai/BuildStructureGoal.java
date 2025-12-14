package com.nothomealone.entity.ai;

import com.nothomealone.entity.custom.BuilderEntity;
import com.nothomealone.structure.BuildTask;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.level.pathfinder.Path;

import java.util.EnumSet;

/**
 * AI Goal for Builder to navigate to construction site and place blocks.
 */
public class BuildStructureGoal extends Goal {
    private final BuilderEntity builder;
    private BuildTask.BuildStep currentStep;
    private Path path;
    private int ticksStuck = 0;

    public BuildStructureGoal(BuilderEntity builder) {
        this.builder = builder;
        this.setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.LOOK));
    }

    @Override
    public boolean canUse() {
        // Can only build if within work area, has a task, and task is not completed
        com.nothomealone.structure.BuildTask task = builder.getCurrentTask();
        boolean withinArea = builder.isWithinWorkArea(builder.blockPosition());
        boolean hasStation = builder.getStationPos() != null;
        boolean hasTask = task != null;
        boolean notCompleted = task != null && !task.isCompleted();
        
        // Check if builder has the material for THE NEXT BLOCK to place (exact match or compatible)
        boolean hasMaterials = false;
        if (hasTask && task != null) {
            com.nothomealone.structure.BuildTask.BuildStep nextStep = task.getNextStep();
            if (nextStep != null) {
                net.minecraft.world.level.block.Block requiredBlock = nextStep.state.getBlock();
                net.minecraft.core.NonNullList<net.minecraft.world.item.ItemStack> inventory = builder.getInventory();
                for (net.minecraft.world.item.ItemStack stack : inventory) {
                    if (!stack.isEmpty()) {
                        net.minecraft.world.level.block.Block stackBlock = net.minecraft.world.level.block.Block.byItem(stack.getItem());
                        if (stackBlock == requiredBlock || builder.areBlocksCompatible(stackBlock, requiredBlock)) {
                            hasMaterials = true;
                            break;
                        }
                    }
                }
            }
        }
        
        boolean result = withinArea && hasStation && hasTask && notCompleted && hasMaterials;
        
        // Log every 20 ticks (1 second) for first 200 ticks, then every 100 ticks
        boolean shouldLog = (builder.tickCount < 200 && builder.tickCount % 20 == 0) || 
                           (builder.tickCount >= 200 && builder.tickCount % 100 == 0);
        
        if (shouldLog) {
            System.out.println("[BuildStructureGoal] Builder #" + builder.getId() + " canUse() check at tick " + builder.tickCount);
            System.out.println("  withinArea=" + withinArea + " (pos=" + builder.blockPosition() + ", station=" + builder.getStationPos() + ")");
            System.out.println("  hasStation=" + hasStation);
            System.out.println("  hasTask=" + hasTask);
            System.out.println("  notCompleted=" + notCompleted);
            if (hasTask && task != null) {
                com.nothomealone.structure.BuildTask.BuildStep nextStep = task.getNextStep();
                if (nextStep != null) {
                    System.out.println("  nextBlock needed: " + nextStep.state.getBlock().getName().getString());
                    System.out.println("  nextBlock position: " + nextStep.pos);
                }
            }
            System.out.println("  hasMaterials=" + hasMaterials);
            System.out.println("  result=" + result);
            if (hasTask && task != null) {
                System.out.println("  task progress: " + task.getCompletedSteps() + "/" + task.getTotalSteps());
            }
        }
        
        return result;
    }

    @Override
    public boolean canContinueToUse() {
        com.nothomealone.structure.BuildTask task = builder.getCurrentTask();
        return canUse() && task != null && !task.isCompleted();
    }

    @Override
    public void start() {
        ticksStuck = 0;
        System.out.println("[BuildStructureGoal] Starting build goal for builder #" + builder.getId());
    }
    
    private void clearBuildArea() {
        com.nothomealone.structure.BuildTask task = builder.getCurrentTask();
        if (task == null) return;
        
        // Get all block positions from the task WITHOUT consuming steps
        java.util.List<com.nothomealone.structure.BuildTask.BuildStep> allSteps = task.getAllSteps();
        int cleared = 0;
        
        for (com.nothomealone.structure.BuildTask.BuildStep step : allSteps) {
            net.minecraft.core.BlockPos pos = step.pos;
            net.minecraft.world.level.block.state.BlockState currentState = builder.level().getBlockState(pos);
            
            // Remove vegetation and replaceable blocks
            if (currentState.is(net.minecraft.tags.BlockTags.REPLACEABLE) ||
                currentState.is(net.minecraft.world.level.block.Blocks.GRASS) ||
                currentState.is(net.minecraft.world.level.block.Blocks.TALL_GRASS) ||
                currentState.is(net.minecraft.world.level.block.Blocks.FERN) ||
                currentState.is(net.minecraft.world.level.block.Blocks.LARGE_FERN) ||
                currentState.is(net.minecraft.tags.BlockTags.FLOWERS) ||
                currentState.is(net.minecraft.tags.BlockTags.SMALL_FLOWERS)) {
                builder.level().destroyBlock(pos, true);
                cleared++;
            }
        }
        System.out.println("[BuildStructureGoal] Cleared " + cleared + " blocks from build area");
    }

    @Override
    public void tick() {
        // Perform building work every tick
        builder.performWork();
        
        // Get current task to check if we need to navigate
        com.nothomealone.structure.BuildTask task = builder.getCurrentTask();
        if (task != null && !task.isCompleted()) {
            com.nothomealone.structure.BuildTask.BuildStep nextStep = task.getNextStep();
            if (nextStep != null) {
                BlockPos target = nextStep.pos;
                
                // Check if we're close enough to build (within 2 blocks)
                if (builder.blockPosition().distSqr(target) > 4) { // More than 2 blocks away
                    // Navigate closer to build position
                    if (path == null || ticksStuck > 40 || builder.getNavigation().isDone()) {
                        path = builder.getNavigation().createPath(target, 1);
                        ticksStuck = 0;
                    }
                    
                    if (path != null) {
                        builder.getNavigation().moveTo(path, 1.0);
                        
                        // Check if stuck
                        if (builder.getNavigation().isDone() && builder.blockPosition().distSqr(target) > 4) {
                            ticksStuck++;
                        }
                    }
                } else {
                    // Close enough, stop moving and let performWork handle placement
                    builder.getNavigation().stop();
                }
            }
        }
    }

    @Override
    public void stop() {
        currentStep = null;
        path = null;
        ticksStuck = 0;
    }
}
