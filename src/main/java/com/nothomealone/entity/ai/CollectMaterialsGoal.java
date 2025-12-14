package com.nothomealone.entity.ai;

import com.nothomealone.block.entity.StationBlockEntity;
import com.nothomealone.entity.custom.BuilderEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.NonNullList;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;

import java.util.EnumSet;

/**
 * AI Goal for Builder to return to station and collect materials.
 */
public class CollectMaterialsGoal extends Goal {
    private final BuilderEntity builder;
    private boolean collecting = false;
    private int collectTimer = 0;

    public CollectMaterialsGoal(BuilderEntity builder) {
        this.builder = builder;
        this.setFlags(EnumSet.of(Goal.Flag.MOVE));
    }

    @Override
    public boolean canUse() {
        // Only collect materials if we have a build task
        com.nothomealone.structure.BuildTask task = builder.getCurrentTask();
        if (task == null) return false;
        if (task.isCompleted()) return false;
        
        BlockPos stationPos = builder.getStationPos();
        if (stationPos == null) return false;
        
        // Check if builder is missing the material for THE NEXT BLOCK to place
        com.nothomealone.structure.BuildTask.BuildStep nextStep = task.getNextStep();
        if (nextStep == null) return false;
        
        net.minecraft.world.level.block.Block requiredBlock = nextStep.state.getBlock();
        NonNullList<ItemStack> inventory = builder.getInventory();
        
        // Check if we have at least one of the required block (or compatible block)
        boolean hasMaterial = false;
        for (ItemStack stack : inventory) {
            if (!stack.isEmpty()) {
                net.minecraft.world.level.block.Block stackBlock = net.minecraft.world.level.block.Block.byItem(stack.getItem());
                if (stackBlock == requiredBlock || builder.areBlocksCompatible(stackBlock, requiredBlock)) {
                    hasMaterial = true;
                    break;
                }
            }
        }
        
        // If we have the material, no need to collect/craft
        if (hasMaterial) {
            return false;
        }
        
        // If we don't have the material, check if we can craft it
        boolean canCraft = builder.canCraftBlock(requiredBlock);
        
        // We need this goal if we can craft OR need to collect from station
        boolean needsAction = canCraft || !hasMaterial;
        
        if (builder.tickCount % 40 == 0) {
            System.out.println("[CollectMaterialsGoal] Builder #" + builder.getId() + " canUse check:");
            System.out.println("  nextBlock=" + requiredBlock.getName().getString());
            System.out.println("  hasMaterial=" + hasMaterial);
            System.out.println("  canCraft=" + canCraft);
            System.out.println("  needsAction=" + needsAction);
        }
        
        return needsAction;
    }

    @Override
    public boolean canContinueToUse() {
        return collecting && collectTimer < 100;
    }

    @Override
    public void start() {
        collecting = true;
        collectTimer = 0;
    }

    @Override
    public void tick() {
        BlockPos stationPos = builder.getStationPos();
        if (stationPos == null) {
            stop();
            return;
        }
        
        // Navigate to station
        if (builder.blockPosition().distSqr(stationPos) > 4) {
            builder.getNavigation().moveTo(stationPos.getX(), stationPos.getY(), stationPos.getZ(), 1.0);
        } else {
            // At station - first try to craft missing materials, then collect from station
            BlockEntity blockEntity = builder.level().getBlockEntity(stationPos);
            if (blockEntity instanceof StationBlockEntity stationEntity) {
                // Try to craft any missing materials we can craft
                craftMissingMaterials();
                // Then transfer remaining needed materials from station
                transferMaterials(stationEntity);
            }
            collecting = false;
        }
        
        collectTimer++;
    }

    @Override
    public void stop() {
        collecting = false;
        collectTimer = 0;
    }

    /**
     * Transfers materials from station to builder inventory.
     * Builder takes ALL materials needed for building, not just 16.
     */
    private void transferMaterials(StationBlockEntity station) {
        NonNullList<ItemStack> stationInventory = station.getCraftingMaterials();
        com.nothomealone.structure.BuildTask task = builder.getCurrentTask();
        if (task == null) return;
        
        // Get all required materials for the build
        java.util.Map<net.minecraft.world.level.block.Block, Integer> requiredMaterials = task.getRequiredMaterials();
        
        int transferred = 0;
        for (int i = 0; i < stationInventory.size(); i++) {
            ItemStack stationStack = stationInventory.get(i);
            if (!stationStack.isEmpty()) {
                net.minecraft.world.level.block.Block block = net.minecraft.world.level.block.Block.byItem(stationStack.getItem());
                
                // Check if this material is needed for building (exact match or compatible)
                boolean isNeeded = false;
                for (net.minecraft.world.level.block.Block requiredBlock : requiredMaterials.keySet()) {
                    if (block == requiredBlock || builder.areBlocksCompatible(block, requiredBlock)) {
                        isNeeded = true;
                        break;
                    }
                }
                
                if (isNeeded) {
                    // Take entire stack (not just 16!)
                    ItemStack toTransfer = stationStack.copy();
                    builder.addToInventory(toTransfer);
                    station.setItem(i, ItemStack.EMPTY);
                    transferred++;
                }
            }
        }
        
        // Only log if something was actually transferred
        if (transferred > 0) {
            System.out.println("[CollectMaterialsGoal] Transferred " + transferred + " item stacks to builder");
        }
    }
    
    /**
     * Try to craft any missing materials that we can craft with available resources.
     */
    private void craftMissingMaterials() {
        com.nothomealone.structure.BuildTask task = builder.getCurrentTask();
        if (task == null) return;
        
        // Get next building step to see what we need
        com.nothomealone.structure.BuildTask.BuildStep nextStep = task.getNextStep();
        if (nextStep == null) return;
        
        net.minecraft.world.level.block.Block requiredBlock = nextStep.state.getBlock();
        NonNullList<ItemStack> inventory = builder.getInventory();
        
        // Check if we already have this material
        for (ItemStack stack : inventory) {
            if (!stack.isEmpty()) {
                net.minecraft.world.level.block.Block stackBlock = net.minecraft.world.level.block.Block.byItem(stack.getItem());
                if (stackBlock == requiredBlock || builder.areBlocksCompatible(stackBlock, requiredBlock)) {
                    // Already have it, no need to craft
                    return;
                }
            }
        }
        
        // Don't have it - try to craft it using builder's crafting system
        System.out.println("[CollectMaterialsGoal] Attempting to craft missing material: " + requiredBlock.getName().getString());
        boolean crafted = builder.craftMissingMaterial(requiredBlock);
        if (crafted) {
            System.out.println("[CollectMaterialsGoal] Successfully crafted material!");
        } else {
            System.out.println("[CollectMaterialsGoal] Could not craft material (will collect from station instead)");
        }
    }
}
