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
 * AI Goal for Builder to replace broken/damaged tools from station.
 */
public class ReplaceToolsGoal extends Goal {
    private final BuilderEntity builder;
    private boolean fetching = false;
    private int fetchTimer = 0;

    public ReplaceToolsGoal(BuilderEntity builder) {
        this.builder = builder;
        this.setFlags(EnumSet.of(Goal.Flag.MOVE));
    }

    @Override
    public boolean canUse() {
        BlockPos stationPos = builder.getStationPos();
        if (stationPos == null) return false;
        
        // Check if builder has damaged or missing tools
        return needsTools();
    }

    @Override
    public boolean canContinueToUse() {
        return fetching && fetchTimer < 100;
    }

    @Override
    public void start() {
        fetching = true;
        fetchTimer = 0;
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
            // At station, get tools
            BlockEntity blockEntity = builder.level().getBlockEntity(stationPos);
            if (blockEntity instanceof StationBlockEntity stationEntity) {
                fetchTools(stationEntity);
            }
            fetching = false;
        }
        
        fetchTimer++;
    }

    @Override
    public void stop() {
        fetching = false;
        fetchTimer = 0;
    }

    /**
     * Checks if builder needs new tools (damaged or missing).
     */
    private boolean needsTools() {
        NonNullList<ItemStack> inventory = builder.getInventory();
        
        boolean hasPickaxe = false;
        boolean hasAxe = false;
        boolean hasShovel = false;
        
        for (ItemStack stack : inventory) {
            if (stack.isEmpty()) continue;
            
            if (stack.getItem() instanceof net.minecraft.world.item.PickaxeItem) {
                // Check if damaged (less than 20% durability)
                if (!stack.isDamageableItem() || stack.getDamageValue() < stack.getMaxDamage() * 0.8) {
                    hasPickaxe = true;
                }
            } else if (stack.getItem() instanceof net.minecraft.world.item.AxeItem) {
                if (!stack.isDamageableItem() || stack.getDamageValue() < stack.getMaxDamage() * 0.8) {
                    hasAxe = true;
                }
            } else if (stack.getItem() instanceof net.minecraft.world.item.ShovelItem) {
                if (!stack.isDamageableItem() || stack.getDamageValue() < stack.getMaxDamage() * 0.8) {
                    hasShovel = true;
                }
            }
        }
        
        return !hasPickaxe || !hasAxe || !hasShovel;
    }

    /**
     * Fetches tools from station to replace damaged ones.
     */
    private void fetchTools(StationBlockEntity station) {
        NonNullList<ItemStack> stationInventory = station.getCraftingMaterials();
        NonNullList<ItemStack> builderInventory = builder.getInventory();
        
        int fetched = 0;
        
        for (int i = 0; i < stationInventory.size(); i++) {
            ItemStack stationStack = stationInventory.get(i);
            if (stationStack.isEmpty()) continue;
            
            // Check if this is a tool
            if (isTool(stationStack)) {
                // Find damaged or missing tool slot in builder inventory
                boolean shouldTake = false;
                int replacementSlot = -1;
                
                if (stationStack.getItem() instanceof net.minecraft.world.item.PickaxeItem) {
                    replacementSlot = findDamagedToolSlot(builderInventory, net.minecraft.world.item.PickaxeItem.class);
                    shouldTake = replacementSlot != -1 || !hasToolType(builderInventory, net.minecraft.world.item.PickaxeItem.class);
                } else if (stationStack.getItem() instanceof net.minecraft.world.item.AxeItem) {
                    replacementSlot = findDamagedToolSlot(builderInventory, net.minecraft.world.item.AxeItem.class);
                    shouldTake = replacementSlot != -1 || !hasToolType(builderInventory, net.minecraft.world.item.AxeItem.class);
                } else if (stationStack.getItem() instanceof net.minecraft.world.item.ShovelItem) {
                    replacementSlot = findDamagedToolSlot(builderInventory, net.minecraft.world.item.ShovelItem.class);
                    shouldTake = replacementSlot != -1 || !hasToolType(builderInventory, net.minecraft.world.item.ShovelItem.class);
                }
                
                if (shouldTake) {
                    ItemStack toTransfer = stationStack.copy();
                    
                    // Replace damaged tool or add to inventory
                    if (replacementSlot != -1) {
                        builderInventory.set(replacementSlot, toTransfer);
                    } else {
                        builder.addToInventory(toTransfer);
                    }
                    
                    station.setItem(i, ItemStack.EMPTY);
                    fetched++;
                    System.out.println("[ReplaceToolsGoal] Fetched tool: " + toTransfer.getHoverName().getString());
                }
            }
        }
        
        if (fetched > 0) {
            station.setChanged();
        }
    }

    /**
     * Finds a slot with a damaged tool of the given type.
     */
    private int findDamagedToolSlot(NonNullList<ItemStack> inventory, Class<?> toolClass) {
        for (int i = 0; i < inventory.size(); i++) {
            ItemStack stack = inventory.get(i);
            if (stack.isEmpty()) continue;
            
            if (toolClass.isInstance(stack.getItem())) {
                if (stack.isDamageableItem() && stack.getDamageValue() >= stack.getMaxDamage() * 0.8) {
                    return i; // Damaged tool found
                }
            }
        }
        return -1;
    }

    /**
     * Checks if inventory has a tool of the given type.
     */
    private boolean hasToolType(NonNullList<ItemStack> inventory, Class<?> toolClass) {
        for (ItemStack stack : inventory) {
            if (!stack.isEmpty() && toolClass.isInstance(stack.getItem())) {
                return true;
            }
        }
        return false;
    }

    /**
     * Checks if an item is a tool.
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
}
