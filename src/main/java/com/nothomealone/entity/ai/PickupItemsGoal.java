package com.nothomealone.entity.ai;

import com.nothomealone.entity.custom.BuilderEntity;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;

import java.util.EnumSet;
import java.util.List;

/**
 * AI Goal for Builder to pick up items on the ground.
 */
public class PickupItemsGoal extends Goal {
    private final BuilderEntity builder;
    private ItemEntity targetItem;
    private int searchCooldown = 0;

    public PickupItemsGoal(BuilderEntity builder) {
        this.builder = builder;
        this.setFlags(EnumSet.of(Goal.Flag.MOVE));
    }

    @Override
    public boolean canUse() {
        // Search for items every 40 ticks (2 seconds)
        if (searchCooldown > 0) {
            searchCooldown--;
            return false;
        }
        
        searchCooldown = 40;
        
        // Find nearby item entities (within 8 blocks)
        AABB searchArea = builder.getBoundingBox().inflate(8.0);
        List<ItemEntity> items = builder.level().getEntitiesOfClass(ItemEntity.class, searchArea);
        
        if (!items.isEmpty()) {
            // Find closest item
            targetItem = null;
            double closestDistance = Double.MAX_VALUE;
            
            for (ItemEntity item : items) {
                if (item.isAlive() && !item.getItem().isEmpty()) {
                    double distance = builder.distanceToSqr(item);
                    if (distance < closestDistance) {
                        closestDistance = distance;
                        targetItem = item;
                    }
                }
            }
            
            return targetItem != null;
        }
        
        return false;
    }

    @Override
    public boolean canContinueToUse() {
        return targetItem != null && targetItem.isAlive() && !targetItem.getItem().isEmpty();
    }

    @Override
    public void start() {
        // Move to item
        if (targetItem != null) {
            builder.getNavigation().moveTo(targetItem, 1.0);
        }
    }

    @Override
    public void tick() {
        if (targetItem == null || !targetItem.isAlive()) {
            stop();
            return;
        }
        
        // If close enough, pick up the item
        if (builder.distanceToSqr(targetItem) < 4.0) {
            ItemStack itemStack = targetItem.getItem();
            builder.addToInventory(itemStack.copy());
            targetItem.discard();
            System.out.println("[PickupItemsGoal] Builder picked up: " + itemStack.getCount() + "x " + itemStack.getHoverName().getString());
            stop();
        }
    }

    @Override
    public void stop() {
        targetItem = null;
        builder.getNavigation().stop();
    }
}
