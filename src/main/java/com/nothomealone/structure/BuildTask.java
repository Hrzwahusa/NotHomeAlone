package com.nothomealone.structure;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Represents a building task for the Builder NPC.
 * Contains all blocks that need to be placed in order.
 */
public class BuildTask {
    private final String structureName;
    private final BlockPos stationPos;
    private final List<BuildStep> steps;
    private int currentStep = 0;
    private boolean completed = false;
    
    public BuildTask(String structureName, BlockPos stationPos) {
        this.structureName = structureName;
        this.stationPos = stationPos;
        this.steps = new ArrayList<>();
    }
    
    /**
     * Adds a build step (a block to place).
     * @param pos Position to place block
     * @param state Block state to place
     * @param priority Higher priority = built first (0 = normal)
     */
    public void addStep(BlockPos pos, BlockState state, int priority) {
        steps.add(new BuildStep(pos, state, priority));
    }
    
    /**
     * Sorts steps by priority (higher first).
     */
    public void sortSteps() {
        steps.sort((a, b) -> Integer.compare(b.priority, a.priority));
    }
    
    /**
     * Gets the next step to build.
     */
    public BuildStep getNextStep() {
        if (currentStep >= steps.size()) {
            completed = true;
            return null;
        }
        return steps.get(currentStep);
    }
    
    /**
     * Marks current step as completed and moves to next.
     */
    public void completeCurrentStep() {
        currentStep++;
        if (currentStep >= steps.size()) {
            completed = true;
        }
    }
    
    /**
     * Gets required materials for this build task.
     */
    public Map<Block, Integer> getRequiredMaterials() {
        Map<Block, Integer> materials = new HashMap<>();
        for (BuildStep step : steps) {
            Block block = step.state.getBlock();
            materials.put(block, materials.getOrDefault(block, 0) + 1);
        }
        return materials;
    }
    
    public boolean isCompleted() {
        return completed;
    }
    
    public int getTotalSteps() {
        return steps.size();
    }
    
    public int getCompletedSteps() {
        return currentStep;
    }
    
    public int getCurrentStepNumber() {
        return currentStep;
    }
    
    public int getCurrentStepIndex() {
        return currentStep;
    }
    
    /**
     * Sets the current step index (used when restoring progress from NBT).
     */
    public void setCurrentStepIndex(int index) {
        if (index >= 0 && index <= steps.size()) {
            this.currentStep = index;
            this.completed = (index >= steps.size());
            System.out.println("[BuildTask] Restored progress: " + currentStep + "/" + steps.size());
        }
    }
    
    public BlockPos getStationPos() {
        return stationPos;
    }
    
    public String getStructureName() {
        return structureName;
    }
    
    /**
     * Gets all build steps without consuming them.
     */
    public List<BuildStep> getAllSteps() {
        return steps;
    }
    
    /**
     * Represents a single block placement step.
     */
    public static class BuildStep {
        public final BlockPos pos;
        public final BlockState state;
        public final int priority;
        
        public BuildStep(BlockPos pos, BlockState state, int priority) {
            this.pos = pos;
            this.state = state;
            this.priority = priority;
        }
    }
}
