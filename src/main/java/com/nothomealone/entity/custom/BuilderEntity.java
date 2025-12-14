package com.nothomealone.entity.custom;

import com.nothomealone.structure.BuildTask;
import com.nothomealone.territory.SettlementHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.core.NonNullList;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Builder NPC - Constructs and repairs buildings and structures
 * Must be able to reach all other stations in the settlement
 */
public class BuilderEntity extends WorkerEntity {

    public BuilderEntity(EntityType<? extends PathfinderMob> entityType, Level level) {
        super(entityType, level);
        System.out.println("[BuilderEntity] Constructor called - Entity created!");
    }

    public static AttributeSupplier.Builder createAttributes() {
        return WorkerEntity.createAttributes();
    }

    private BuildTask currentTask;
    private NonNullList<ItemStack> inventory = NonNullList.withSize(27, ItemStack.EMPTY);
    private int buildCooldown = 0;
    private Map<Block, Integer> requestedMaterials = new HashMap<>();
    private int savedTaskProgress = 0; // Saved progress from NBT

    @Override
    protected void registerWorkerGoals() {
        System.out.println("[BuilderEntity] Registering worker goals!");
        // Priority 1: Build structure if have task (HIGHEST PRIORITY!)
        this.goalSelector.addGoal(1, new com.nothomealone.entity.ai.BuildStructureGoal(this));
        // Priority 2: Collect materials if needed for building
        this.goalSelector.addGoal(2, new com.nothomealone.entity.ai.CollectMaterialsGoal(this));
        // Priority 3: Replace damaged/broken tools from station
        this.goalSelector.addGoal(3, new com.nothomealone.entity.ai.ReplaceToolsGoal(this));
        // Priority 4: Return unnecessary items to station
        this.goalSelector.addGoal(4, new com.nothomealone.entity.ai.ReturnItemsGoal(this));
        // Priority 5: Pick up items on the ground
        this.goalSelector.addGoal(5, new com.nothomealone.entity.ai.PickupItemsGoal(this));
        // Priority 6: Return to station when idle
        this.goalSelector.addGoal(6, new com.nothomealone.entity.ai.ReturnToStationGoal(this, 1.0));
        // Priority 7: Wander near station when nothing to do
        this.goalSelector.addGoal(7, new net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal(this, 0.8));
        // Priority 8: Look around
        this.goalSelector.addGoal(8, new net.minecraft.world.entity.ai.goal.RandomLookAroundGoal(this));
        System.out.println("[BuilderEntity] Worker goals registered: " + this.goalSelector.getAvailableGoals().size() + " goals total");
    }
    
    public BuildTask getCurrentTask() {
        return currentTask;
    }

    private int performWorkCallCount = 0;
    
    @Override
    public void performWork() {
        performWorkCallCount++;
        if (performWorkCallCount % 100 == 1) {
            System.out.println("[BuilderEntity] performWork() called " + performWorkCallCount + " times for builder #" + getId());
        }
        
        if (!(level() instanceof ServerLevel serverLevel)) return;
        
        // Cooldown between block placements
        if (buildCooldown > 0) {
            buildCooldown--;
            return;
        }
        
        // If no task, nothing to do
        if (currentTask == null || currentTask.isCompleted()) {
            if (currentTask != null && currentTask.isCompleted()) {
                System.out.println("[BuilderEntity] Task completed!");
            }
            return;
        }
        
        // Get next building step
        BuildTask.BuildStep step = currentTask.getNextStep();
        if (step == null) {
            System.out.println("[BuilderEntity] No more steps in task");
            currentTask = null;
            return;
        }
        
        // Get required block for this step
        Block requiredBlock = step.state.getBlock();
        
        // Skip air blocks automatically
        if (requiredBlock == Blocks.AIR || requiredBlock == Blocks.CAVE_AIR || requiredBlock == Blocks.VOID_AIR) {
            System.out.println("[BuilderEntity] Skipping air block at " + step.pos);
            currentTask.completeCurrentStep();
            buildCooldown = 1;
            return;
        }
        
        System.out.println("[BuilderEntity] Building at " + step.pos + " block: " + step.state.getBlock().getName().getString());
        
        // Check what's currently at the position
        BlockState currentState = serverLevel.getBlockState(step.pos);
        System.out.println("[BuilderEntity] Current block at position: " + currentState.getBlock().getName().getString());
        
        // Check if we have the required material
        if (!hasBlockInInventory(requiredBlock)) {
            System.out.println("[BuilderEntity] Missing material: " + requiredBlock.getName().getString() + " - requesting...");
            requestMaterial(requiredBlock);
            return;
        }
        
        // Clear obstacle if needed (and wait for next tick before placing)
        if (!currentState.isAir() && !currentState.is(requiredBlock)) {
            if (currentState.is(Blocks.DIRT) && requiredBlock == Blocks.DIRT) {
                // Dirt already there, skip
                System.out.println("[BuilderEntity] Block already present, skipping step");
                currentTask.completeCurrentStep();
                return;
            }
            // Clear the block and wait for next tick
            System.out.println("[BuilderEntity] Clearing obstacle: " + currentState.getBlock().getName().getString());
            serverLevel.destroyBlock(step.pos, false);
            buildCooldown = 5; // Wait 5 ticks for block to be cleared
            return; // Don't try to place yet, wait for next tick
        }
        
        // Place the block (only if position is now air or has the right block)
        System.out.println("[BuilderEntity] Attempting to place block at " + step.pos);
        boolean placed = placeBlock(serverLevel, step.pos, step.state);
        System.out.println("[BuilderEntity] Block placement result: " + placed);
        
        if (placed) {
            removeBlockFromInventory(requiredBlock);
            currentTask.completeCurrentStep();
            buildCooldown = 10; // Wait 10 ticks between placements
            System.out.println("[BuilderEntity] Step completed! Progress: " + currentTask.getCompletedSteps() + "/" + currentTask.getTotalSteps());
        } else {
            System.out.println("[BuilderEntity] ERROR: Failed to place block! Trying to clear position...");
            // Force clear the position
            serverLevel.destroyBlock(step.pos, false);
            buildCooldown = 5;
        }
    }

    /**
     * Sets the current build task for this builder.
     */
    public void setBuildTask(BuildTask task) {
        System.out.println("[BuilderEntity] Builder #" + this.getId() + " setBuildTask called with " + (task != null ? task.getTotalSteps() + " steps" : "null task"));
        System.out.println("[BuilderEntity] Builder #" + this.getId() + " station=" + this.getStationPos());
        this.currentTask = task;
        this.requestedMaterials.clear();
        
        // Populate requestedMaterials with what we need from the task
        if (task != null) {
            this.requestedMaterials.putAll(task.getRequiredMaterials());
            System.out.println("[BuilderEntity] Builder #" + this.getId() + " requestedMaterials populated with " + this.requestedMaterials.size() + " material types");
        }
    }

    /**
     * Gets all station positions in this settlement.
     */
    public List<BlockPos> getAllStations() {
        return SettlementHelper.getAllStations(this.level());
    }

    /**
     * Finds the nearest station that needs construction/repair.
     */
    public BlockPos findNextBuildTask() {
        return SettlementHelper.findNearestStation(this.level(), this.blockPosition(), "any");
    }

    /**
     * Checks if this builder can reach all stations in the settlement.
     */
    public boolean canReachAllStations() {
        if (stationPos == null) return false;
        return SettlementHelper.canReachAllStations(this.level(), stationPos, workRadius);
    }

    /**
     * Checks if builder has the specified block in inventory.
     */
    private boolean hasBlockInInventory(Block block) {
        for (ItemStack stack : inventory) {
            if (Block.byItem(stack.getItem()) == block && !stack.isEmpty()) {
                return true;
            }
        }
        return false;
    }

    /**
     * Removes one block from builder's inventory.
     */
    private void removeBlockFromInventory(Block block) {
        for (int i = 0; i < inventory.size(); i++) {
            ItemStack stack = inventory.get(i);
            if (Block.byItem(stack.getItem()) == block && !stack.isEmpty()) {
                stack.shrink(1);
                return;
            }
        }
    }

    /**
     * Places a block in the world.
     */
    private boolean placeBlock(ServerLevel level, BlockPos pos, BlockState state) {
        // Check if builder is close enough to place the block
        double distance = this.blockPosition().distSqr(pos);
        System.out.println("[BuilderEntity] Distance to block: " + Math.sqrt(distance) + " blocks");
        
        if (distance > 25) { // More than 5 blocks away
            System.out.println("[BuilderEntity] Too far away to place block!");
            return false;
        }
        
        boolean result = level.setBlock(pos, state, 3);
        if (!result) {
            System.out.println("[BuilderEntity] level.setBlock() returned false");
        }
        return result;
    }

    /**
     * Requests material from the station block.
     * Sends chat message to nearby players if material is missing.
     */
    private void requestMaterial(Block block) {
        // Only request each material once
        if (requestedMaterials.containsKey(block)) {
            return;
        }

        requestedMaterials.put(block, 1);

        // Send chat message to nearby players
        if (level() instanceof ServerLevel serverLevel) {
            Component message = Component.literal("[Builder] Missing material: " + block.getName().getString());
            serverLevel.players().forEach(player -> {
                if (player.distanceToSqr(this) < 1024) { // Within 32 blocks
                    player.sendSystemMessage(message);
                }
            });
        }
    }

    /**
     * Adds items to builder's inventory from station block.
     */
    public void addToInventory(ItemStack stack) {
        for (int i = 0; i < inventory.size(); i++) {
            ItemStack slotStack = inventory.get(i);
            if (slotStack.isEmpty()) {
                inventory.set(i, stack.copy());
                return;
            } else if (ItemStack.isSameItem(slotStack, stack)) {
                int maxSize = Math.min(slotStack.getMaxStackSize(), inventory.size());
                int toAdd = Math.min(stack.getCount(), maxSize - slotStack.getCount());
                if (toAdd > 0) {
                    slotStack.grow(toAdd);
                    stack.shrink(toAdd);
                    if (stack.isEmpty()) return;
                }
            }
        }
    }

    public NonNullList<ItemStack> getInventory() {
        return inventory;
    }
    
    public Map<Block, Integer> getRequestedMaterials() {
        return requestedMaterials;
    }
    
    @Override
    public void addAdditionalSaveData(net.minecraft.nbt.CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        
        // Save current task progress
        if (currentTask != null) {
            tag.putInt("TaskProgress", currentTask.getCurrentStepIndex());
            tag.putBoolean("HasTask", true);
        } else {
            tag.putBoolean("HasTask", false);
        }
    }
    
    @Override
    public void readAdditionalSaveData(net.minecraft.nbt.CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        
        // Task will be reassigned by BuilderTaskManager.onWorldLoad()
        // Store the saved progress so it can be restored
        if (tag.contains("HasTask") && tag.getBoolean("HasTask")) {
            this.savedTaskProgress = tag.getInt("TaskProgress");
            System.out.println("[BuilderEntity] Loaded builder with saved task progress: " + savedTaskProgress);
        }
    }
    
    /**
     * Gets the saved task progress from NBT (used by BuilderTaskManager to restore progress).
     */
    public int getSavedTaskProgress() {
        return savedTaskProgress;
    }
}
