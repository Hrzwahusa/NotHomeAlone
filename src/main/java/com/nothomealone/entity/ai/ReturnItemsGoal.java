package com.nothomealone.entity.ai;

import com.nothomealone.block.entity.StationBlockEntity;
import com.nothomealone.entity.custom.BuilderEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.NonNullList;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;

import java.util.EnumSet;
import java.util.Map;

/**
 * AI Goal for Builder to return unnecessary items to the station.
 * Returns everything that's not a tool and not needed for building.
 */
public class ReturnItemsGoal extends Goal {
    private final BuilderEntity builder;
    private boolean returning = false;
    private int returnTimer = 0;

    public ReturnItemsGoal(BuilderEntity builder) {
        this.builder = builder;
        this.setFlags(EnumSet.of(Goal.Flag.MOVE));
    }

    @Override
    public boolean canUse() {
        com.nothomealone.structure.BuildTask task = builder.getCurrentTask();
        BlockPos stationPos = builder.getStationPos();
        if (stationPos == null) return false;
        
        // If task is completed, return ALL items except tools
        if (task != null && task.isCompleted()) {
            return hasAnyNonToolItems();
        }
        
        // If task is active, only return items that are not needed
        if (task != null) {
            return hasUnnecessaryItems(task);
        }
        
        return false;
    }

    @Override
    public boolean canContinueToUse() {
        return returning && returnTimer < 100;
    }

    @Override
    public void start() {
        returning = true;
        returnTimer = 0;
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
            // At station, return unnecessary items
            BlockEntity blockEntity = builder.level().getBlockEntity(stationPos);
            if (blockEntity instanceof StationBlockEntity stationEntity) {
                returnUnnecessaryItems(stationEntity);
            }
            returning = false;
        }
        
        returnTimer++;
    }

    @Override
    public void stop() {
        returning = false;
        returnTimer = 0;
    }

    /**
     * Check if builder has any items that are not tools and not needed for building.
     */
    private boolean hasUnnecessaryItems(com.nothomealone.structure.BuildTask task) {
        NonNullList<ItemStack> inventory = builder.getInventory();
        Map<Block, Integer> requiredMaterials = task.getRequiredMaterials();
        
        for (ItemStack stack : inventory) {
            if (stack.isEmpty()) continue;
            
            // Keep tools
            if (isTool(stack)) continue;
            
            // Check if this is a required building material
            Block block = Block.byItem(stack.getItem());
            if (!requiredMaterials.containsKey(block)) {
                return true; // Found an unnecessary item
            }
        }
        
        return false;
    }

    /**
     * Check if builder has any non-tool items (for after task completion).
     */
    private boolean hasAnyNonToolItems() {
        NonNullList<ItemStack> inventory = builder.getInventory();
        
        for (ItemStack stack : inventory) {
            if (stack.isEmpty()) continue;
            if (isTool(stack)) continue;
            return true; // Found a non-tool item
        }
        
        return false;
    }

    /**
     * Returns all unnecessary items to the station.
     */
    private void returnUnnecessaryItems(StationBlockEntity station) {
        NonNullList<ItemStack> builderInventory = builder.getInventory();
        NonNullList<ItemStack> stationInventory = station.getCraftingMaterials();
        com.nothomealone.structure.BuildTask task = builder.getCurrentTask();
        
        // If task is completed, return ALL non-tool items
        boolean taskCompleted = (task != null && task.isCompleted());
        Map<Block, Integer> requiredMaterials = taskCompleted ? null : task.getRequiredMaterials();
        
        int returned = 0;
        for (int i = 0; i < builderInventory.size(); i++) {
            ItemStack builderStack = builderInventory.get(i);
            if (builderStack.isEmpty()) continue;
            
            // Keep tools
            if (isTool(builderStack)) continue;
            
            boolean shouldReturn = false;
            
            if (taskCompleted) {
                // Task completed: return everything that's not a tool
                shouldReturn = true;
            } else if (requiredMaterials != null) {
                // Task active: only return items not needed for building
                Block block = Block.byItem(builderStack.getItem());
                shouldReturn = !requiredMaterials.containsKey(block);
            }
            
            if (shouldReturn) {
                // Return this item to station
                if (addToStation(stationInventory, builderStack)) {
                    builderInventory.set(i, ItemStack.EMPTY);
                    returned++;
                }
            }
        }
        
        if (returned > 0) {
            station.setChanged();
            if (taskCompleted) {
                System.out.println("[ReturnItemsGoal] Task completed! Returned " + returned + " item stacks to station");
            } else {
                System.out.println("[ReturnItemsGoal] Returned " + returned + " unnecessary item stacks to station");
            }
        }
    }

    /**
     * Checks if an item is a tool (pickaxe, axe, shovel, etc.)
     */
    private boolean isTool(ItemStack stack) {
        return stack.getItem() instanceof net.minecraft.world.item.DiggerItem ||
               stack.getItem() instanceof net.minecraft.world.item.SwordItem ||
               stack.getItem() instanceof net.minecraft.world.item.TridentItem ||
               stack.getItem() instanceof net.minecraft.world.item.BowItem ||
               stack.getItem() instanceof net.minecraft.world.item.CrossbowItem ||
               stack.getItem() instanceof net.minecraft.world.item.FishingRodItem ||
               stack.getItem() instanceof net.minecraft.world.item.ShearsItem;
    }

    /**
     * Adds an item stack to the station inventory.
     */
    private boolean addToStation(NonNullList<ItemStack> stationInventory, ItemStack toAdd) {
        // Try to merge with existing stacks first
        for (int i = 0; i < stationInventory.size(); i++) {
            ItemStack stationStack = stationInventory.get(i);
            if (stationStack.isEmpty()) continue;
            
            if (ItemStack.isSameItemSameTags(stationStack, toAdd)) {
                int maxSize = Math.min(stationStack.getMaxStackSize(), 64);
                int canAdd = maxSize - stationStack.getCount();
                if (canAdd > 0) {
                    int toTransfer = Math.min(canAdd, toAdd.getCount());
                    stationStack.grow(toTransfer);
                    toAdd.shrink(toTransfer);
                    if (toAdd.isEmpty()) return true;
                }
            }
        }
        
        // Find empty slot
        for (int i = 0; i < stationInventory.size(); i++) {
            if (stationInventory.get(i).isEmpty()) {
                stationInventory.set(i, toAdd.copy());
                return true;
            }
        }
        
        return false; // No space
    }
}
