package com.nothomealone.entity.custom;

import com.nothomealone.block.entity.StationBlockEntity;
import com.nothomealone.structure.BuildTask;
import com.nothomealone.territory.SettlementHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.core.NonNullList;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Builder NPC - Constructs and repairs buildings and structures
 * Must be able to reach all other stations in the settlement
 */
public class BuilderEntity extends WorkerEntity implements net.minecraft.world.Container {

    public BuilderEntity(EntityType<? extends PathfinderMob> entityType, Level level) {
        super(entityType, level);
        System.out.println("[BuilderEntity] Constructor called - Entity created!");
    }
    
    @Override
    public boolean requiresCustomPersistence() {
        // Builder should never despawn naturally
        return true;
    }
    
    @Override
    public boolean removeWhenFarAway(double distanceToClosestPlayer) {
        // Never remove builder due to distance
        return false;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return WorkerEntity.createAttributes();
    }

    private BuildTask currentTask;
    private NonNullList<ItemStack> inventory = NonNullList.withSize(27, ItemStack.EMPTY);
    private int buildCooldown = 0;
    private Map<Block, Integer> requestedMaterials = new HashMap<>();
    private int savedTaskProgress = 0; // Saved progress from NBT
    private boolean toolsRequested = false; // Track if we already requested tools
    
    // Static list for crafting recipes (initialized after CraftingRecipe class is defined)
    private static List<CraftingRecipe> CRAFTING_RECIPES;

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
            System.out.println("[BuilderEntity] Task status: " + (currentTask == null ? "NULL" : (currentTask.isCompleted() ? "COMPLETED" : "ACTIVE")));
            if (currentTask != null) {
                System.out.println("[BuilderEntity] Progress: " + currentTask.getCompletedSteps() + "/" + currentTask.getTotalSteps());
            }
        }
        
        if (!(level() instanceof ServerLevel serverLevel)) return;
        
        // Cooldown between block placements
        if (buildCooldown > 0) {
            buildCooldown--;
            return;
        }
        
        // If no task, nothing to do
        if (currentTask == null) {
            if (performWorkCallCount % 100 == 1) {
                System.out.println("[BuilderEntity] No task assigned!");
            }
            return;
        }
        
        // Check if task completed
        if (currentTask.isCompleted()) {
            if (performWorkCallCount % 100 == 1) {
                System.out.println("[BuilderEntity] Task completed! Progress: " + currentTask.getCompletedSteps() + "/" + currentTask.getTotalSteps());
            }
            return;
        }
        
        if (currentTask.isCompleted()) {
            if (performWorkCallCount % 100 == 1) {
                System.out.println("[BuilderEntity] Task completed!");
            }
            return;
        }
        
        // Get next building step
        BuildTask.BuildStep step = currentTask.getNextStep();
        if (step == null) {
            System.out.println("[BuilderEntity] ERROR: getNextStep() returned null but task not completed! Progress: " + currentTask.getCompletedSteps() + "/" + currentTask.getTotalSteps());
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
            System.out.println("[BuilderEntity] Missing material: " + requiredBlock.getName().getString() + " - trying to craft...");
            
            // Try to craft missing material
            if (craftMissingMaterial(requiredBlock)) {
                System.out.println("[BuilderEntity] Successfully crafted missing material!");
                buildCooldown = 5; // Short wait, then try again
            } else {
                System.out.println("[BuilderEntity] Cannot craft, requesting material...");
                requestMaterial(requiredBlock);
            }
            return;
        }
        
        // Clear obstacle if needed (and wait for next tick before placing)
        if (!currentState.isAir() && !currentState.is(requiredBlock)) {
            // Check if blocks are compatible (e.g. grass block and dirt are the same)
            if (areBlocksCompatible(currentState.getBlock(), requiredBlock)) {
                System.out.println("[BuilderEntity] Block already present (compatible), skipping step");
                currentTask.completeCurrentStep();
                return;
            }
            // Clear the block with tools and wait for next tick
            System.out.println("[BuilderEntity] Clearing obstacle: " + currentState.getBlock().getName().getString());
            if (!breakBlockWithTool(serverLevel, step.pos, currentState)) {
                // No tool available - wait for tools to be added to station
                System.out.println("[BuilderEntity] No tool found, waiting for tools...");
                buildCooldown = 100; // Wait 5 seconds before trying again
                return;
            }
            buildCooldown = 5; // Wait 5 ticks for block to be cleared
            return; // Don't try to place yet, wait for next tick
        }
        
        // Place the block (prefer a compatible variant from inventory if available)
        System.out.println("[BuilderEntity] Attempting to place block at " + step.pos);
        Block blockToPlace = step.state.getBlock();
        Block compatibleFromInv = getCompatibleInventoryBlock(blockToPlace);
        BlockState stateToPlace = (compatibleFromInv != null ? compatibleFromInv.defaultBlockState() : step.state);
        boolean placed = placeBlock(serverLevel, step.pos, stateToPlace);
        System.out.println("[BuilderEntity] Block placement result: " + placed);
        
        if (placed) {
            // Remove the block that was actually placed (compatible variant or exact)
            removeBlockFromInventory(stateToPlace.getBlock());
            currentTask.completeCurrentStep();
            buildCooldown = 10; // Wait 10 ticks between placements
            System.out.println("[BuilderEntity] Step completed! Progress: " + currentTask.getCompletedSteps() + "/" + currentTask.getTotalSteps());
        } else {
            System.out.println("[BuilderEntity] ERROR: Failed to place block! Trying to clear position...");
            // Force clear the position with tool
            BlockState obstacleState = serverLevel.getBlockState(step.pos);
            if (!breakBlockWithTool(serverLevel, step.pos, obstacleState)) {
                // No tool available - wait for tools
                System.out.println("[BuilderEntity] No tool found, waiting for tools...");
                buildCooldown = 100; // Wait 5 seconds before trying again
            } else {
                buildCooldown = 5;
            }
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
        this.toolsRequested = false; // Reset tool request flag for new task
        
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
     * Checks if two blocks are compatible (can be treated as the same).
     * For example: Grass Block and Dirt are compatible, all wood planks are compatible, etc.
     */
    public boolean areBlocksCompatible(Block current, Block required) {
        // Same block is always compatible
        if (current == required) {
            return true;
        }
        
        // Grass block and dirt are compatible
        if ((current == Blocks.GRASS_BLOCK && required == Blocks.DIRT) ||
            (current == Blocks.DIRT && required == Blocks.GRASS_BLOCK)) {
            return true;
        }
        
        // All wood planks are compatible with each other (oak, birch, spruce, etc.)
        if (current.defaultBlockState().is(BlockTags.PLANKS) && 
            required.defaultBlockState().is(BlockTags.PLANKS)) {
            return true;
        }
        
        // All logs are compatible with each other
        if (current.defaultBlockState().is(BlockTags.LOGS) && 
            required.defaultBlockState().is(BlockTags.LOGS)) {
            return true;
        }
        
        // All wooden slabs are compatible
        if (current.defaultBlockState().is(BlockTags.WOODEN_SLABS) && 
            required.defaultBlockState().is(BlockTags.WOODEN_SLABS)) {
            return true;
        }
        
        // All wooden stairs are compatible
        if (current.defaultBlockState().is(BlockTags.WOODEN_STAIRS) && 
            required.defaultBlockState().is(BlockTags.WOODEN_STAIRS)) {
            return true;
        }
        
        // All wooden fences are compatible
        if (current.defaultBlockState().is(BlockTags.WOODEN_FENCES) && 
            required.defaultBlockState().is(BlockTags.WOODEN_FENCES)) {
            return true;
        }
        
        // All wooden doors are compatible
        if (current.defaultBlockState().is(BlockTags.WOODEN_DOORS) && 
            required.defaultBlockState().is(BlockTags.WOODEN_DOORS)) {
            return true;
        }
        
        // All wooden trapdoors are compatible
        if (current.defaultBlockState().is(BlockTags.WOODEN_TRAPDOORS) && 
            required.defaultBlockState().is(BlockTags.WOODEN_TRAPDOORS)) {
            return true;
        }
        
        return false;
    }
    
    /**
     * Simple crafting recipe definition
     */
    private static class CraftingRecipe {
        Object input1; // Can be Item or ItemTags.TagKey
        int input1Count;
        Object input2; // Optional second input
        int input2Count;
        Object output; // Can be Item or ItemTags.TagKey
        int outputCount;
        
        // Recipe with one input
        CraftingRecipe(Object input1, int input1Count, Object output, int outputCount) {
            this.input1 = input1;
            this.input1Count = input1Count;
            this.output = output;
            this.outputCount = outputCount;
            this.input2 = null;
            this.input2Count = 0;
        }
        
        // Recipe with two inputs
        CraftingRecipe(Object input1, int input1Count, Object input2, int input2Count, Object output, int outputCount) {
            this.input1 = input1;
            this.input1Count = input1Count;
            this.input2 = input2;
            this.input2Count = input2Count;
            this.output = output;
            this.outputCount = outputCount;
        }
    }
    
    // Initialize crafting recipes after CraftingRecipe class is defined
    static {
        CRAFTING_RECIPES = new ArrayList<>();
        
        // === WOOD RECIPES ===
        // Logs to Planks (1 log → 4 planks)
        CRAFTING_RECIPES.add(new CraftingRecipe(ItemTags.LOGS, 1, ItemTags.PLANKS, 4));
        // Planks to Sticks (2 planks → 4 sticks)
        CRAFTING_RECIPES.add(new CraftingRecipe(ItemTags.PLANKS, 2, Items.STICK, 4));
        // Planks to Stairs (6 planks → 4 stairs)
        CRAFTING_RECIPES.add(new CraftingRecipe(ItemTags.PLANKS, 6, ItemTags.WOODEN_STAIRS, 4));
        // Planks to Slabs (3 planks → 6 slabs)
        CRAFTING_RECIPES.add(new CraftingRecipe(ItemTags.PLANKS, 3, ItemTags.WOODEN_SLABS, 6));
        // Planks to Doors (6 planks → 3 doors)
        CRAFTING_RECIPES.add(new CraftingRecipe(ItemTags.PLANKS, 6, ItemTags.WOODEN_DOORS, 3));
        // Planks to Trapdoors (6 planks → 2 trapdoors)
        CRAFTING_RECIPES.add(new CraftingRecipe(ItemTags.PLANKS, 6, ItemTags.WOODEN_TRAPDOORS, 2));
        // Planks to Pressure Plate (2 planks → 1 plate)
        CRAFTING_RECIPES.add(new CraftingRecipe(ItemTags.PLANKS, 2, ItemTags.WOODEN_PRESSURE_PLATES, 1));
        // Planks to Button (1 plank → 1 button)
        CRAFTING_RECIPES.add(new CraftingRecipe(ItemTags.PLANKS, 1, ItemTags.WOODEN_BUTTONS, 1));
        // Planks to Crafting Table (4 planks → 1 table)
        CRAFTING_RECIPES.add(new CraftingRecipe(ItemTags.PLANKS, 4, Items.CRAFTING_TABLE, 1));
        // Planks to Chest (8 planks → 1 chest)
        CRAFTING_RECIPES.add(new CraftingRecipe(ItemTags.PLANKS, 8, Items.CHEST, 1));
        // Sticks + Planks to Fence (2 sticks + 4 planks → 3 fence)
        CRAFTING_RECIPES.add(new CraftingRecipe(Items.STICK, 2, ItemTags.PLANKS, 4, ItemTags.WOODEN_FENCES, 3));
        // Sticks + Planks to Fence Gate (4 sticks + 2 planks → 1 gate)
        CRAFTING_RECIPES.add(new CraftingRecipe(Items.STICK, 4, ItemTags.PLANKS, 2, ItemTags.FENCE_GATES, 1));
        // Planks + Stick to Sign (6 planks + 1 stick → 3 signs)
        CRAFTING_RECIPES.add(new CraftingRecipe(ItemTags.PLANKS, 6, Items.STICK, 1, ItemTags.SIGNS, 3));
        // Planks + Stick to Ladder (7 sticks → 3 ladders)
        CRAFTING_RECIPES.add(new CraftingRecipe(Items.STICK, 7, Items.LADDER, 3));
        
        // === STONE RECIPES ===
        // Cobblestone to Stairs (6 cobble → 4 stairs)
        CRAFTING_RECIPES.add(new CraftingRecipe(Items.COBBLESTONE, 6, Items.COBBLESTONE_STAIRS, 4));
        // Cobblestone to Slabs (3 cobble → 6 slabs)
        CRAFTING_RECIPES.add(new CraftingRecipe(Items.COBBLESTONE, 3, Items.COBBLESTONE_SLAB, 6));
        // Cobblestone to Wall (6 cobble → 6 walls)
        CRAFTING_RECIPES.add(new CraftingRecipe(Items.COBBLESTONE, 6, Items.COBBLESTONE_WALL, 6));
        // Cobblestone to Furnace (8 cobble → 1 furnace)
        CRAFTING_RECIPES.add(new CraftingRecipe(Items.COBBLESTONE, 8, Items.FURNACE, 1));
        // Cobblestone + Stick to Lever (1 cobble + 1 stick → 1 lever)
        CRAFTING_RECIPES.add(new CraftingRecipe(Items.COBBLESTONE, 1, Items.STICK, 1, Items.LEVER, 1));
        
        // Stone variants
        CRAFTING_RECIPES.add(new CraftingRecipe(Items.STONE, 6, Items.STONE_STAIRS, 4));
        CRAFTING_RECIPES.add(new CraftingRecipe(Items.STONE, 3, Items.STONE_SLAB, 6));
        CRAFTING_RECIPES.add(new CraftingRecipe(Items.STONE, 2, Items.STONE_PRESSURE_PLATE, 1));
        CRAFTING_RECIPES.add(new CraftingRecipe(Items.STONE, 1, Items.STONE_BUTTON, 1));
        
        // Stone Bricks
        CRAFTING_RECIPES.add(new CraftingRecipe(Items.STONE, 4, Items.STONE_BRICKS, 4));
        CRAFTING_RECIPES.add(new CraftingRecipe(Items.STONE_BRICKS, 6, Items.STONE_BRICK_STAIRS, 4));
        CRAFTING_RECIPES.add(new CraftingRecipe(Items.STONE_BRICKS, 3, Items.STONE_BRICK_SLAB, 6));
        CRAFTING_RECIPES.add(new CraftingRecipe(Items.STONE_BRICKS, 6, Items.STONE_BRICK_WALL, 6));
        
        // Sandstone
        CRAFTING_RECIPES.add(new CraftingRecipe(Items.SAND, 4, Items.SANDSTONE, 1));
        CRAFTING_RECIPES.add(new CraftingRecipe(Items.SANDSTONE, 6, Items.SANDSTONE_STAIRS, 4));
        CRAFTING_RECIPES.add(new CraftingRecipe(Items.SANDSTONE, 3, Items.SANDSTONE_SLAB, 6));
        CRAFTING_RECIPES.add(new CraftingRecipe(Items.SANDSTONE, 6, Items.SANDSTONE_WALL, 6));
        
        // Red Sandstone
        CRAFTING_RECIPES.add(new CraftingRecipe(Items.RED_SAND, 4, Items.RED_SANDSTONE, 1));
        CRAFTING_RECIPES.add(new CraftingRecipe(Items.RED_SANDSTONE, 6, Items.RED_SANDSTONE_STAIRS, 4));
        CRAFTING_RECIPES.add(new CraftingRecipe(Items.RED_SANDSTONE, 3, Items.RED_SANDSTONE_SLAB, 6));
        CRAFTING_RECIPES.add(new CraftingRecipe(Items.RED_SANDSTONE, 6, Items.RED_SANDSTONE_WALL, 6));
        
        // === BRICKS ===
        CRAFTING_RECIPES.add(new CraftingRecipe(Items.BRICKS, 6, Items.BRICK_STAIRS, 4));
        CRAFTING_RECIPES.add(new CraftingRecipe(Items.BRICKS, 3, Items.BRICK_SLAB, 6));
        CRAFTING_RECIPES.add(new CraftingRecipe(Items.BRICKS, 6, Items.BRICK_WALL, 6));
        
        // === QUARTZ ===
        CRAFTING_RECIPES.add(new CraftingRecipe(Items.QUARTZ_BLOCK, 6, Items.QUARTZ_STAIRS, 4));
        CRAFTING_RECIPES.add(new CraftingRecipe(Items.QUARTZ_BLOCK, 3, Items.QUARTZ_SLAB, 6));
        CRAFTING_RECIPES.add(new CraftingRecipe(Items.QUARTZ, 4, Items.QUARTZ_BLOCK, 1));
        
        // === GLASS ===
        CRAFTING_RECIPES.add(new CraftingRecipe(Items.GLASS, 6, Items.GLASS_PANE, 16));
        
        // === IRON ===
        CRAFTING_RECIPES.add(new CraftingRecipe(Items.IRON_INGOT, 6, Items.IRON_BARS, 16));
        CRAFTING_RECIPES.add(new CraftingRecipe(Items.IRON_INGOT, 6, Items.IRON_DOOR, 3));
        CRAFTING_RECIPES.add(new CraftingRecipe(Items.IRON_INGOT, 4, Items.IRON_TRAPDOOR, 1));
        CRAFTING_RECIPES.add(new CraftingRecipe(Items.IRON_BLOCK, 1, Items.IRON_INGOT, 9));
        CRAFTING_RECIPES.add(new CraftingRecipe(Items.IRON_INGOT, 9, Items.IRON_BLOCK, 1));
        
        // === GOLD ===
        CRAFTING_RECIPES.add(new CraftingRecipe(Items.GOLD_BLOCK, 1, Items.GOLD_INGOT, 9));
        CRAFTING_RECIPES.add(new CraftingRecipe(Items.GOLD_INGOT, 9, Items.GOLD_BLOCK, 1));
        
        // === DIAMOND ===
        CRAFTING_RECIPES.add(new CraftingRecipe(Items.DIAMOND_BLOCK, 1, Items.DIAMOND, 9));
        CRAFTING_RECIPES.add(new CraftingRecipe(Items.DIAMOND, 9, Items.DIAMOND_BLOCK, 1));
        
        // === WOOL & CARPET ===
        CRAFTING_RECIPES.add(new CraftingRecipe(ItemTags.WOOL, 2, ItemTags.WOOL_CARPETS, 3));
        
        // === OTHER ===
        // Stick + Coal to Torch (1 stick + 1 coal → 4 torches)
        CRAFTING_RECIPES.add(new CraftingRecipe(Items.STICK, 1, Items.COAL, 1, Items.TORCH, 4));
        CRAFTING_RECIPES.add(new CraftingRecipe(Items.STICK, 1, Items.CHARCOAL, 1, Items.TORCH, 4));
    }
    
    /**
     * Checks if the builder could craft the needed block with available materials.
     * Does NOT actually craft, just checks possibility.
     * Also checks if COMPATIBLE blocks can be crafted (e.g. Birch Fence for Oak Fence).
     */
    public boolean canCraftBlock(Block neededBlock) {
        Item neededItem = neededBlock.asItem();
        if (neededItem == Items.AIR) return false;
        
        System.out.println("[BuilderEntity] canCraftBlock checking for: " + neededItem.getDescriptionId());
        
        // Check each recipe to see if we can craft the needed item OR a compatible item
        for (CraftingRecipe recipe : CRAFTING_RECIPES) {
            boolean produces = false;
            
            // Check if recipe output matches what we need
            if (recipe.output instanceof Item) {
                Item outputItem = (Item) recipe.output;
                Block outputBlock = Block.byItem(outputItem);
                produces = (outputItem == neededItem) || 
                          (outputBlock != Blocks.AIR && areBlocksCompatible(outputBlock, neededBlock));
            } else if (recipe.output instanceof net.minecraft.tags.TagKey) {
                // For tags, check if the needed item is IN this tag OR if we can craft compatible alternatives
                net.minecraft.tags.TagKey<Item> outputTag = (net.minecraft.tags.TagKey<Item>) recipe.output;
                
                // Direct match: needed item is in the output tag
                if (neededItem.builtInRegistryHolder().is(outputTag)) {
                    produces = true;
                    System.out.println("[BuilderEntity]   Recipe has tag output. Needed item in tag: true");
                } else {
                    // Check if we can craft a compatible alternative from this tag
                    // For example: recipe produces WOODEN_FENCES (any fence), we need Oak Fence
                    // but we could craft Birch Fence (also in WOODEN_FENCES) which is compatible
                    // Pass the recipe so we can check if materials are available for this specific alternative
                    produces = canCraftCompatibleFromTag(outputTag, neededBlock, recipe);
                    if (produces) {
                        System.out.println("[BuilderEntity]   Recipe can produce compatible alternative from tag (with materials available)");
                    }
                }
            }
            
            if (!produces) {
                continue;
            }
            
            System.out.println("[BuilderEntity]   Found recipe that produces " + neededItem.getDescriptionId());
            
            // For exact matches or direct tag matches, check materials
            // (for compatible alternatives, materials were already checked in canCraftCompatibleFromTag)
            if (recipe.output instanceof Item || 
                (recipe.output instanceof net.minecraft.tags.TagKey && neededItem.builtInRegistryHolder().is((net.minecraft.tags.TagKey<Item>) recipe.output))) {
                
                // Check if we have the required inputs in inventory
                if (hasInputsForRecipe(recipe)) {
                    System.out.println("[BuilderEntity]   -> Has inputs in INVENTORY");
                    return true;
                }
                
                // Check if inputs are in the station
                if (hasInputsInStation(recipe)) {
                    System.out.println("[BuilderEntity]   -> Has inputs in STATION");
                    return true;
                }
                
                // Check if we could recursively craft the inputs (simplified check, depth 1 only)
                if (canCraftInputsForRecipe(recipe)) {
                    System.out.println("[BuilderEntity]   -> Can craft inputs recursively");
                    return true;
                }
                
                System.out.println("[BuilderEntity]   -> Cannot craft (missing materials for exact match)");
                
                // If this is a tag output and we can't craft the exact item, try compatible alternatives
                if (recipe.output instanceof net.minecraft.tags.TagKey) {
                    net.minecraft.tags.TagKey<Item> outputTag = (net.minecraft.tags.TagKey<Item>) recipe.output;
                    if (canCraftCompatibleFromTag(outputTag, neededBlock, recipe)) {
                        System.out.println("[BuilderEntity]   -> Found compatible alternative with available materials!");
                        return true;
                    }
                }
            } else {
                // Compatible alternative - materials were already checked
                return true;
            }
        }
        
        System.out.println("[BuilderEntity] canCraftBlock: No recipe found or materials missing");
        return false;
    }
    
    /**
     * Checks if we can craft a compatible alternative block from the given tag.
     * For example: if tag is WOODEN_FENCES and we need Oak Fence, check if we can craft
     * Birch Fence, Spruce Fence, etc. (which are compatible with Oak Fence).
     * This method needs the original recipe to check materials!
     */
    private boolean canCraftCompatibleFromTag(net.minecraft.tags.TagKey<Item> outputTag, Block neededBlock, CraftingRecipe originalRecipe) {
        // Get common items that might be in this tag
        List<Item> potentialItems = new ArrayList<>();
        
        // Check for fence variants
        if (outputTag == ItemTags.WOODEN_FENCES) {
            potentialItems.addAll(Arrays.asList(
                Items.OAK_FENCE, Items.BIRCH_FENCE, Items.SPRUCE_FENCE,
                Items.JUNGLE_FENCE, Items.ACACIA_FENCE, Items.DARK_OAK_FENCE,
                Items.CHERRY_FENCE, Items.MANGROVE_FENCE, Items.BAMBOO_FENCE,
                Items.CRIMSON_FENCE, Items.WARPED_FENCE
            ));
        }
        // Check for plank variants
        else if (outputTag == ItemTags.PLANKS) {
            potentialItems.addAll(Arrays.asList(
                Items.OAK_PLANKS, Items.BIRCH_PLANKS, Items.SPRUCE_PLANKS,
                Items.JUNGLE_PLANKS, Items.ACACIA_PLANKS, Items.DARK_OAK_PLANKS,
                Items.CHERRY_PLANKS, Items.MANGROVE_PLANKS, Items.BAMBOO_PLANKS,
                Items.CRIMSON_PLANKS, Items.WARPED_PLANKS
            ));
        }
        
        // Check each potential item
        for (Item item : potentialItems) {
            // Is this item actually in the tag?
            if (!item.builtInRegistryHolder().is(outputTag)) {
                continue;
            }
            
            // Is this item compatible with what we need?
            Block itemBlock = Block.byItem(item);
            if (itemBlock != Blocks.AIR && areBlocksCompatible(itemBlock, neededBlock)) {
                System.out.println("[BuilderEntity]     Checking if we can craft compatible alternative: " + item.getDescriptionId());
                
                // Now check if we have materials to craft this specific alternative!
                // For example: to craft Birch Fence, we need Birch Planks (same wood family)
                // The recipe uses ItemTags.PLANKS, so we need to check if we have the RIGHT planks
                
                // For fences, we need to check if the planks match the wood type
                if (outputTag == ItemTags.WOODEN_FENCES) {
                    Item requiredPlank = getMatchingPlankForFence(item);
                    if (requiredPlank != null) {
                        System.out.println("[BuilderEntity]       Need " + requiredPlank.getDescriptionId() + " (x" + originalRecipe.input2Count + ") + " + originalRecipe.input1 + " (x" + originalRecipe.input1Count + ")");
                        
                        // Check if we have the specific planks needed for this fence
                        boolean hasPlank = hasItemsInInventory(requiredPlank, originalRecipe.input2Count);
                        boolean hasStick = hasItemsInInventory(originalRecipe.input1, originalRecipe.input1Count);
                        System.out.println("[BuilderEntity]       Inventory: plank=" + hasPlank + ", stick=" + hasStick);
                        
                        if (hasPlank && hasStick) {
                            System.out.println("[BuilderEntity]       -> Has " + requiredPlank.getDescriptionId() + " in INVENTORY for " + item.getDescriptionId());
                            return true;
                        }
                        
                        boolean hasPlankStation = hasItemsInStationSpecific(requiredPlank, originalRecipe.input2Count);
                        boolean hasStickStation = hasItemsInStationSpecific(originalRecipe.input1, originalRecipe.input1Count);
                        System.out.println("[BuilderEntity]       Station: plank=" + hasPlankStation + ", stick=" + hasStickStation);
                        
                        if (hasPlankStation && hasStickStation) {
                            System.out.println("[BuilderEntity]       -> Has " + requiredPlank.getDescriptionId() + " in STATION for " + item.getDescriptionId());
                            return true;
                        }
                        
                        // Check if we can craft planks from logs
                        Item matchingLog = getMatchingLogForPlank(requiredPlank);
                        if (matchingLog != null) {
                            System.out.println("[BuilderEntity]       Checking for logs: " + matchingLog.getDescriptionId());
                            boolean hasLog = hasItemsInStationSpecific(matchingLog, 1); // Need 1 log to make 4 planks
                            System.out.println("[BuilderEntity]       Station has logs: " + hasLog);
                            
                            if (hasLog && hasStickStation) {
                                System.out.println("[BuilderEntity]       -> Can craft planks from logs in STATION for " + item.getDescriptionId());
                                return true;
                            }
                        }
                    }
                } else {
                    // For non-fence items, use generic check
                    if (hasInputsForRecipe(originalRecipe)) {
                        System.out.println("[BuilderEntity]       -> Has inputs in INVENTORY for this alternative");
                        return true;
                    }
                    
                    if (hasInputsInStation(originalRecipe)) {
                        System.out.println("[BuilderEntity]       -> Has inputs in STATION for this alternative");
                        return true;
                    }
                }
                
                // Check if we could recursively craft the inputs
                if (canCraftInputsForRecipe(originalRecipe)) {
                    System.out.println("[BuilderEntity]       -> Can craft inputs recursively for this alternative");
                    return true;
                }
                
                System.out.println("[BuilderEntity]       -> Cannot craft (missing materials for this alternative)");
            }
        }
        
        return false;
    }
    
    /**
     * Checks if we could craft the inputs for a recipe (one level deep).
     */
    private boolean canCraftInputsForRecipe(CraftingRecipe recipe) {
        boolean canCraftInput1 = false;
        boolean canCraftInput2 = true; // Default true for optional input
        
        // Check if we can craft input1
        if (recipe.input1 instanceof Item input1Item) {
            Block input1Block = Block.byItem(input1Item);
            if (input1Block != Blocks.AIR) {
                // Look for a recipe that produces this input
                for (CraftingRecipe subRecipe : CRAFTING_RECIPES) {
                    if (canProduceItem(subRecipe.output, input1Item)) {
                        // Check inventory OR station for sub-recipe inputs
                        if (hasInputsForRecipe(subRecipe) || hasInputsInStation(subRecipe)) {
                            canCraftInput1 = true;
                            break;
                        }
                    }
                }
            }
        }
        
        // Check if we can craft input2 (if it exists)
        if (recipe.input2 != null) {
            canCraftInput2 = false;
            if (recipe.input2 instanceof Item input2Item) {
                Block input2Block = Block.byItem(input2Item);
                if (input2Block != Blocks.AIR) {
                    for (CraftingRecipe subRecipe : CRAFTING_RECIPES) {
                        if (canProduceItem(subRecipe.output, input2Item)) {
                            // Check inventory OR station for sub-recipe inputs
                            if (hasInputsForRecipe(subRecipe) || hasInputsInStation(subRecipe)) {
                                canCraftInput2 = true;
                                break;
                            }
                        }
                    }
                }
            }
        }
        
        return canCraftInput1 && canCraftInput2;
    }
    
    /**
     * Try to craft a missing material from available resources
     */
    public boolean craftMissingMaterial(Block neededBlock) {
        return craftMissingMaterial(neededBlock, 0);
    }
    
    /**
     * Try to craft a missing material from available resources (with recursion depth limit)
     */
    private boolean craftMissingMaterial(Block neededBlock, int depth) {
        // Limit recursion depth to prevent infinite loops
        if (depth > 3) {
            return false;
        }
        
        Item neededItem = neededBlock.asItem();
        if (neededItem == Items.AIR) return false;
        
        // Check each recipe to see if we can craft the needed item OR a compatible alternative
        for (CraftingRecipe recipe : CRAFTING_RECIPES) {
            // For tag outputs, find the BEST craftable alternative first (based on available materials)
            Item itemToCraft = null;
            boolean isTagOutput = recipe.output instanceof net.minecraft.tags.TagKey;
            
            if (isTagOutput) {
                net.minecraft.tags.TagKey<Item> outputTag = (net.minecraft.tags.TagKey<Item>) recipe.output;
                
                // Check if needed item is in the tag
                boolean neededItemInTag = canProduceItem(recipe.output, neededItem);
                
                // For fences: pick variant based on available materials (prioritize alternatives over direct match)
                if (outputTag == ItemTags.WOODEN_FENCES) {
                    System.out.println("[BuilderEntity] [CRAFT] Fence recipe - searching for best variant based on materials...");
                    Item[] fenceVariants = new Item[] {
                        Items.OAK_FENCE, Items.BIRCH_FENCE, Items.SPRUCE_FENCE,
                        Items.JUNGLE_FENCE, Items.ACACIA_FENCE, Items.DARK_OAK_FENCE,
                        Items.CHERRY_FENCE, Items.MANGROVE_FENCE, Items.BAMBOO_FENCE,
                        Items.CRIMSON_FENCE, Items.WARPED_FENCE
                    };
                    for (Item fenceVariant : fenceVariants) {
                        Block variantBlock = Block.byItem(fenceVariant);
                        if (variantBlock == Blocks.AIR || !areBlocksCompatible(variantBlock, neededBlock)) continue;
                        
                        Item requiredPlank = getMatchingPlankForFence(fenceVariant);
                        Item matchingLog = requiredPlank != null ? getMatchingLogForPlank(requiredPlank) : null;
                        
                        // Check if we have exact planks + sticks
                        boolean havePlanks = requiredPlank != null && (hasItemsInInventory(requiredPlank, recipe.input2Count) || hasItemsInStationSpecific(requiredPlank, recipe.input2Count));
                        boolean haveSticks = hasItemsInInventory(recipe.input1, recipe.input1Count) || hasItemsInStationSpecific(recipe.input1, recipe.input1Count);
                        
                        // Or check if we have logs (can craft planks then sticks)
                        boolean haveLogs = matchingLog != null && hasItemsInStationSpecific(matchingLog, 1);
                        
                        if ((havePlanks && haveSticks) || haveLogs) {
                            itemToCraft = fenceVariant;
                            System.out.println("[BuilderEntity] [CRAFT] Selected " + fenceVariant.getDescriptionId() + 
                                               " (planks=" + havePlanks + ", sticks=" + haveSticks + ", logs=" + haveLogs + ")");
                            break;
                        }
                    }
                } else if (neededItemInTag) {
                    // For other tags, if needed item is in tag, use it
                    itemToCraft = neededItem;
                    System.out.println("[BuilderEntity] [CRAFT] Tag output, using needed item: " + neededItem.getDescriptionId());
                } else {
                    // Try to find any compatible item from tag
                    Item compatibleItem = findCraftableCompatibleItem(outputTag, neededBlock, recipe);
                    if (compatibleItem != null) {
                        itemToCraft = compatibleItem;
                        System.out.println("[BuilderEntity] [CRAFT] Found compatible alternative: " + compatibleItem.getDescriptionId());
                    }
                }
            } else if (canProduceItem(recipe.output, neededItem)) {
                // Direct item match (not a tag)
                itemToCraft = neededItem;
                System.out.println("[BuilderEntity] [CRAFT] Direct item recipe for: " + neededItem.getDescriptionId());
            }
            
            // Skip if no suitable item found
            if (itemToCraft == null) {
                continue;
            }
            
            System.out.println("[BuilderEntity] [CRAFT] Attempting to craft " + itemToCraft.getDescriptionId());
            
            // Check if we have the required inputs in inventory
            // General batch crafting for all recipes (not just fences)
            int craftedBatches = 0;
            int neededBatches = getNeededOutputBatches(neededBlock, recipe.outputCount);
            int maxBatches = getMaxCraftableBatches(recipe);
            int batchesToCraft = neededBatches == -1 ? maxBatches : Math.min(neededBatches, maxBatches);
            while (batchesToCraft > 0) {
                if (!hasInputsForRecipe(recipe)) break;
                System.out.println("[BuilderEntity] Crafting " + itemToCraft.getDescriptionId() + " batch " + (craftedBatches+1) + "/" + batchesToCraft);
                removeItemsForCrafting(recipe.input1, recipe.input1Count);
                if (recipe.input2 != null) {
                    removeItemsForCrafting(recipe.input2, recipe.input2Count);
                }
                addCraftedItems(recipe.output, recipe.outputCount, itemToCraft);
                craftedBatches++;
                batchesToCraft--;
            }
            if (craftedBatches > 0) return true;

            // For fence variants, check if SPECIFIC planks + sticks are in station
            if (isTagOutput && recipe.output == ItemTags.WOODEN_FENCES) {
                Item requiredPlank = getMatchingPlankForFence(itemToCraft);
                if (requiredPlank != null) {
                    boolean hasPlankStation = hasItemsInStationSpecific(requiredPlank, recipe.input2Count);
                    boolean hasStickStation = hasItemsInStationSpecific(recipe.input1, recipe.input1Count);

                    if (hasPlankStation && hasStickStation) {
                        System.out.println("[BuilderEntity] Fetching specific fence materials from station: " + requiredPlank.getDescriptionId() + " + sticks");
                        fetchMaterialsFromStation(requiredPlank, recipe.input2Count);
                        fetchMaterialsFromStation(recipe.input1, recipe.input1Count);

                        System.out.println("[BuilderEntity] Crafting " + itemToCraft.getDescriptionId() + " with specific station materials");
                        removeItemsForCrafting(requiredPlank, recipe.input2Count);
                        removeItemsForCrafting(recipe.input1, recipe.input1Count);
                        addCraftedItems(recipe.output, recipe.outputCount, itemToCraft);
                        return true;
                    }
                }
            }

            // Try to craft the missing inputs recursively
            boolean inputsReady = false;
            // If crafting a fence variant from a tag, use fence-specific input crafting
            if (isTagOutput && recipe.output == ItemTags.WOODEN_FENCES) {
                System.out.println("[BuilderEntity] [CRAFT] Crafting inputs for fence variant " + itemToCraft.getDescriptionId());
                inputsReady = craftInputsForFenceVariant(recipe, itemToCraft, depth + 1);
            } else {
                inputsReady = tryToCraftInputs(recipe, depth);
            }

            if (inputsReady) {
                System.out.println("[BuilderEntity] Successfully crafted inputs, now crafting " + itemToCraft.getDescriptionId());
                removeItemsForCrafting(recipe.input1, recipe.input1Count);
                if (recipe.input2 != null) {
                    removeItemsForCrafting(recipe.input2, recipe.input2Count);
                }
                addCraftedItems(recipe.output, recipe.outputCount, itemToCraft);
                return true;
            }

            // Check if we can get the materials from station (generic tag-based)
            if (hasInputsInStation(recipe)) {
                System.out.println("[BuilderEntity] Fetching crafting materials from station for " + itemToCraft.getDescriptionId());
                fetchMaterialsFromStation(recipe.input1, recipe.input1Count);
                if (recipe.input2 != null) {
                    fetchMaterialsFromStation(recipe.input2, recipe.input2Count);
                }
                // Batch craft with fetched materials
                int craftedBatchesStation = 0;
                int neededBatchesStation = getNeededOutputBatches(neededBlock, recipe.outputCount);
                while (hasInputsForRecipe(recipe) && (neededBatchesStation == -1 || craftedBatchesStation < neededBatchesStation)) {
                    System.out.println("[BuilderEntity] Crafting " + itemToCraft.getDescriptionId() + " with station materials batch " + (craftedBatchesStation+1));
                    removeItemsForCrafting(recipe.input1, recipe.input1Count);
                    if (recipe.input2 != null) {
                        removeItemsForCrafting(recipe.input2, recipe.input2Count);
                    }
                    addCraftedItems(recipe.output, recipe.outputCount, itemToCraft);
                    craftedBatchesStation++;
                }
                return craftedBatchesStation > 0;
            }

        }

        return false;
    }
    
    /**
     * Try to craft the missing inputs for a recipe
     */
    private boolean tryToCraftInputs(CraftingRecipe recipe, int depth) {
        boolean input1Crafted = false;
        boolean input2Crafted = false;
        
        // Check if input1 is missing and try to craft it
        if (!hasItemsInInventory(recipe.input1, recipe.input1Count)) {
            // Try to craft it from a tag or item
            if (recipe.input1 instanceof net.minecraft.tags.TagKey) {
                // For tags, try to craft any item that matches
                net.minecraft.tags.TagKey<Item> tag = (net.minecraft.tags.TagKey<Item>) recipe.input1;
                for (CraftingRecipe subRecipe : CRAFTING_RECIPES) {
                    if (subRecipe.output instanceof net.minecraft.tags.TagKey && subRecipe.output == tag) {
                        // This recipe produces items with the needed tag
                        if (craftRecipeIfPossible(subRecipe, depth + 1)) {
                            input1Crafted = true;
                            break;
                        }
                    } else if (subRecipe.output instanceof Item) {
                        ItemStack testStack = new ItemStack((Item) subRecipe.output);
                        if (testStack.is(tag)) {
                            if (craftRecipeIfPossible(subRecipe, depth + 1)) {
                                input1Crafted = true;
                                break;
                            }
                        }
                    }
                }
            } else if (recipe.input1 instanceof Item) {
                Item neededInputItem = (Item) recipe.input1;
                Block neededInputBlock = Block.byItem(neededInputItem);
                if (neededInputBlock != null && neededInputBlock != Blocks.AIR && neededInputBlock.asItem() == neededInputItem) {
                    // If this item maps to a block, use block-based crafting path
                    input1Crafted = craftMissingMaterial(neededInputBlock, depth + 1);
                } else {
                    // Item does not map to a placeable block (e.g., STICK). Find a recipe that produces it and craft.
                    for (CraftingRecipe subRecipe : CRAFTING_RECIPES) {
                        if (canProduceItem(subRecipe.output, neededInputItem)) {
                            if (craftRecipeIfPossible(subRecipe, depth + 1)) {
                                input1Crafted = true;
                                break;
                            }
                        }
                    }
                }
            }
            
            // If we couldn't craft it, return false
            if (!input1Crafted && !hasItemsInInventory(recipe.input1, recipe.input1Count)) {
                return false;
            }
        } else {
            input1Crafted = true; // Already have it
        }
        
        // Check if input2 is missing and try to craft it
        if (recipe.input2 != null && !hasItemsInInventory(recipe.input2, recipe.input2Count)) {
            if (recipe.input2 instanceof Item) {
                Item neededInputItem = (Item) recipe.input2;
                Block neededInputBlock = Block.byItem(neededInputItem);
                if (neededInputBlock != null && neededInputBlock != Blocks.AIR && neededInputBlock.asItem() == neededInputItem) {
                    input2Crafted = craftMissingMaterial(neededInputBlock, depth + 1);
                } else {
                    for (CraftingRecipe subRecipe : CRAFTING_RECIPES) {
                        if (canProduceItem(subRecipe.output, neededInputItem)) {
                            if (craftRecipeIfPossible(subRecipe, depth + 1)) {
                                input2Crafted = true;
                                break;
                            }
                        }
                    }
                }
            }
            
            if (!input2Crafted && !hasItemsInInventory(recipe.input2, recipe.input2Count)) {
                return false;
            }
        } else {
            input2Crafted = true; // Already have it or not needed
        }
        
        return input1Crafted && input2Crafted;
    }

    /**
     * Craft inputs specifically for a target wooden fence variant.
     * Ensures matching planks are crafted from logs and sticks from those planks.
     */
    private boolean craftInputsForFenceVariant(CraftingRecipe fenceRecipe, Item fenceItem, int depth) {
        if (depth > 3) return false;

        // Determine required plank type for this fence
        Item requiredPlank = getMatchingPlankForFence(fenceItem);
        if (requiredPlank == null) return false;
        System.out.println("[BuilderEntity] [FenceInputs] Target fence: " + fenceItem.getDescriptionId() + ", required plank: " + requiredPlank.getDescriptionId());

        // 1) Ensure we have sticks: craft sticks from matching planks if needed
        boolean haveSticks = hasItemsInInventory(fenceRecipe.input1, fenceRecipe.input1Count) ||
                             hasItemsInStationSpecific(fenceRecipe.input1, fenceRecipe.input1Count);
        System.out.println("[BuilderEntity] [FenceInputs] Initial sticks availability: " + haveSticks);

        // 2) Ensure we have matching planks: craft planks from logs if needed
        boolean havePlanks = hasItemsInInventory(requiredPlank, fenceRecipe.input2Count) ||
                             hasItemsInStationSpecific(requiredPlank, fenceRecipe.input2Count);
        System.out.println("[BuilderEntity] [FenceInputs] Initial planks availability: " + havePlanks);

        // If missing planks, try to craft them from logs via the PLANKS recipe
        if (!havePlanks) {
            Item matchingLog = getMatchingLogForPlank(requiredPlank);
            if (matchingLog != null) {
                // Find the LOGS -> PLANKS recipe
                for (CraftingRecipe sub : CRAFTING_RECIPES) {
                    if (sub.output instanceof net.minecraft.tags.TagKey && sub.output == ItemTags.PLANKS) {
                        // Need logs in inventory or station
                        if (hasItemsInInventory(matchingLog, sub.input1Count) || hasItemsInStationSpecific(matchingLog, sub.input1Count)) {
                            // Fetch logs from station if not in inventory
                            if (!hasItemsInInventory(matchingLog, sub.input1Count)) {
                                fetchMaterialsFromStation(matchingLog, sub.input1Count);
                                System.out.println("[BuilderEntity] [FenceInputs] Fetched logs for planks: " + matchingLog.getDescriptionId());
                            }
                            // Craft planks (use default addCraftedItems for tag; we'll adjust stacks to requiredPlank)
                            removeItemsForCrafting(matchingLog, sub.input1Count);
                            System.out.println("[BuilderEntity] [FenceInputs] Crafting planks for variant using: " + requiredPlank.getDescriptionId());
                            addCraftedItems(sub.output, sub.outputCount, requiredPlank);
                            havePlanks = hasItemsInInventory(requiredPlank, fenceRecipe.input2Count) ||
                                        hasItemsInStationSpecific(requiredPlank, fenceRecipe.input2Count);
                            System.out.println("[BuilderEntity] [FenceInputs] Post-craft planks availability: " + havePlanks);
                        }
                        break;
                    }
                }
            }
        }

        // If missing sticks, craft sticks from PLANKS
        if (!haveSticks) {
            for (CraftingRecipe sub : CRAFTING_RECIPES) {
                if (sub.output instanceof Item && sub.output == Items.STICK) {
                    // Ensure we have matching planks for stick recipe
                    if (!hasItemsInInventory(requiredPlank, sub.input1Count)) {
                        if (hasItemsInStationSpecific(requiredPlank, sub.input1Count)) {
                            fetchMaterialsFromStation(requiredPlank, sub.input1Count);
                            System.out.println("[BuilderEntity] [FenceInputs] Fetched planks for sticks: " + requiredPlank.getDescriptionId());
                        }
                    }
                    if (hasItemsInInventory(requiredPlank, sub.input1Count)) {
                        removeItemsForCrafting(requiredPlank, sub.input1Count);
                        System.out.println("[BuilderEntity] [FenceInputs] Crafting sticks from matching planks: " + requiredPlank.getDescriptionId());
                        addCraftedItems(sub.output, sub.outputCount, null);
                        haveSticks = hasItemsInInventory(fenceRecipe.input1, fenceRecipe.input1Count) ||
                                     hasItemsInStationSpecific(fenceRecipe.input1, fenceRecipe.input1Count);
                        System.out.println("[BuilderEntity] [FenceInputs] Post-craft sticks availability: " + haveSticks);
                    }
                }
            }
        }

        if (!havePlanks || !haveSticks) {
            System.out.println("[BuilderEntity] [FenceInputs] Missing inputs after attempt. planks=" + havePlanks + ", sticks=" + haveSticks);
        }
        return havePlanks && haveSticks;
    }
    
    /**
     * Try to craft a specific recipe if possible
     */
    private boolean craftRecipeIfPossible(CraftingRecipe recipe, int depth) {
        if (depth > 3) return false;
        
        // Check if we have inputs or can get them from station
        if (hasInputsForRecipe(recipe)) {
            removeItemsForCrafting(recipe.input1, recipe.input1Count);
            if (recipe.input2 != null) {
                removeItemsForCrafting(recipe.input2, recipe.input2Count);
            }
            // For intermediate crafting, use default oak variants
            addCraftedItems(recipe.output, recipe.outputCount, null);
            return true;
        }
        
        if (hasInputsInStation(recipe)) {
            fetchMaterialsFromStation(recipe.input1, recipe.input1Count);
            if (recipe.input2 != null) {
                fetchMaterialsFromStation(recipe.input2, recipe.input2Count);
            }
            removeItemsForCrafting(recipe.input1, recipe.input1Count);
            if (recipe.input2 != null) {
                removeItemsForCrafting(recipe.input2, recipe.input2Count);
            }
            addCraftedItems(recipe.output, recipe.outputCount, null);
            return true;
        }
        
        return false;
    }
    
    /**
     * Check if a recipe output can produce the needed item
     */
    private boolean canProduceItem(Object output, Item needed) {
        if (output instanceof Item) {
            return output == needed;
        } else if (output instanceof net.minecraft.tags.TagKey) {
            ItemStack testStack = new ItemStack(needed);
            return testStack.is((net.minecraft.tags.TagKey<Item>) output);
        }
        return false;
    }
    
    /**
     * Find a compatible item from a tag that we can actually craft with available materials.
     * This is used when we need a specific item but can craft a compatible alternative.
     * For example: Need Oak Fence but can craft Birch Fence with available Birch materials.
     */
    private Item findCraftableCompatibleItem(net.minecraft.tags.TagKey<Item> outputTag, Block neededBlock, CraftingRecipe recipe) {
        // Get all potential items from the tag
        net.minecraft.core.Registry<Item> itemRegistry = level().registryAccess().registryOrThrow(net.minecraft.core.registries.Registries.ITEM);
        java.util.Optional<net.minecraft.core.HolderSet.Named<Item>> tagHolders = itemRegistry.getTag(outputTag);
        
        if (tagHolders.isEmpty()) {
            return null;
        }
        
        // Try each item in the tag
        for (net.minecraft.core.Holder<Item> holder : tagHolders.get()) {
            Item potentialItem = holder.value();
            Block potentialBlock = Block.byItem(potentialItem);
            
            // Skip if it's air
            if (potentialBlock == Blocks.AIR) {
                continue;
            }
            
            // Check if this block is compatible with the needed block
            if (!areBlocksCompatible(potentialBlock, neededBlock)) {
                continue;
            }
            
            // Check if we can craft this item with available materials
            // For fences, check if we have the matching wood type planks
            if (outputTag == ItemTags.WOODEN_FENCES) {
                Item requiredPlank = getMatchingPlankForFence(potentialItem);
                if (requiredPlank != null) {
                    System.out.println("[BuilderEntity] [findCraftable] Checking fence " + potentialItem.getDescriptionId() + " needs " + requiredPlank.getDescriptionId());
                    
                    // Check for specific planks + sticks in inventory
                    boolean hasPlankInv = hasItemsInInventory(requiredPlank, recipe.input2Count);
                    boolean hasStickInv = hasItemsInInventory(recipe.input1, recipe.input1Count);
                    System.out.println("[BuilderEntity] [findCraftable]   Inventory: plank=" + hasPlankInv + ", stick=" + hasStickInv);
                    
                    if (hasPlankInv && hasStickInv) {
                        System.out.println("[BuilderEntity] [findCraftable]   -> Found in INVENTORY!");
                        return potentialItem;
                    }
                    
                    // Check for specific planks + sticks in station
                    boolean hasPlankStation = hasItemsInStationSpecific(requiredPlank, recipe.input2Count);
                    boolean hasStickStation = hasItemsInStationSpecific(recipe.input1, recipe.input1Count);
                    System.out.println("[BuilderEntity] [findCraftable]   Station: plank=" + hasPlankStation + ", stick=" + hasStickStation);
                    
                    if (hasPlankStation && hasStickStation) {
                        System.out.println("[BuilderEntity] [findCraftable]   -> Found in STATION!");
                        return potentialItem;
                    }
                    
                    // Check if we can craft planks from logs in station
                    Item matchingLog = getMatchingLogForPlank(requiredPlank);
                    if (matchingLog != null) {
                        System.out.println("[BuilderEntity] [findCraftable]   Checking for logs: " + matchingLog.getDescriptionId());
                        boolean hasLog = hasItemsInStationSpecific(matchingLog, 1);
                        System.out.println("[BuilderEntity] [findCraftable]   Station has logs: " + hasLog + ", has sticks: " + hasStickStation);
                        
                        // If we have logs, we can craft planks; sticks can be crafted from planks as well.
                        // Allow crafting path to proceed if logs are present (even without sticks yet).
                        if (hasLog) {
                            System.out.println("[BuilderEntity] [findCraftable]   -> Can craft via LOGS path (planks -> sticks -> fence)!");
                            return potentialItem;
                        }
                    }
                }
            } else {
                // For other items, use generic check
                if (hasInputsForRecipe(recipe)) {
                    return potentialItem;
                }
                
                if (hasInputsInStation(recipe)) {
                    return potentialItem;
                }
            }
            
            // Check if we can recursively craft the inputs
            if (canCraftInputsForRecipe(recipe)) {
                return potentialItem;
            }
        }
        
        return null; // No compatible craftable item found
    }
    
    /**
     * Get the specific plank item that matches a fence type (wood family matching)
     */
    private Item getMatchingPlankForFence(Item fence) {
        if (fence == Items.OAK_FENCE) return Items.OAK_PLANKS;
        if (fence == Items.BIRCH_FENCE) return Items.BIRCH_PLANKS;
        if (fence == Items.SPRUCE_FENCE) return Items.SPRUCE_PLANKS;
        if (fence == Items.JUNGLE_FENCE) return Items.JUNGLE_PLANKS;
        if (fence == Items.ACACIA_FENCE) return Items.ACACIA_PLANKS;
        if (fence == Items.DARK_OAK_FENCE) return Items.DARK_OAK_PLANKS;
        if (fence == Items.CHERRY_FENCE) return Items.CHERRY_PLANKS;
        if (fence == Items.MANGROVE_FENCE) return Items.MANGROVE_PLANKS;
        if (fence == Items.BAMBOO_FENCE) return Items.BAMBOO_PLANKS;
        if (fence == Items.CRIMSON_FENCE) return Items.CRIMSON_PLANKS;
        if (fence == Items.WARPED_FENCE) return Items.WARPED_PLANKS;
        return null;
    }
    
    /**
     * Get the log item that can be crafted into the given plank type
     */
    private Item getMatchingLogForPlank(Item plank) {
        if (plank == Items.OAK_PLANKS) return Items.OAK_LOG;
        if (plank == Items.BIRCH_PLANKS) return Items.BIRCH_LOG;
        if (plank == Items.SPRUCE_PLANKS) return Items.SPRUCE_LOG;
        if (plank == Items.JUNGLE_PLANKS) return Items.JUNGLE_LOG;
        if (plank == Items.ACACIA_PLANKS) return Items.ACACIA_LOG;
        if (plank == Items.DARK_OAK_PLANKS) return Items.DARK_OAK_LOG;
        if (plank == Items.CHERRY_PLANKS) return Items.CHERRY_LOG;
        if (plank == Items.MANGROVE_PLANKS) return Items.MANGROVE_LOG;
        if (plank == Items.BAMBOO_PLANKS) return Items.BAMBOO_BLOCK;
        if (plank == Items.CRIMSON_PLANKS) return Items.CRIMSON_STEM;
        if (plank == Items.WARPED_PLANKS) return Items.WARPED_STEM;
        return null;
    }
    
    /**
     * Check if we have a specific item (not tag) in the station
     */
    private boolean hasItemsInStationSpecific(Object itemOrTag, int count) {
        if (stationPos == null || !(level() instanceof ServerLevel serverLevel)) {
            return false;
        }
        
        if (!(serverLevel.getBlockEntity(stationPos) instanceof StationBlockEntity station)) {
            return false;
        }
        
        return hasItemsInContainer(station, itemOrTag, count);
    }
    
    /**
     * Check if we have the required inputs for a recipe in inventory
     */
    private boolean hasInputsForRecipe(CraftingRecipe recipe) {
        if (!hasItemsInInventory(recipe.input1, recipe.input1Count)) {
            return false;
        }
        if (recipe.input2 != null && !hasItemsInInventory(recipe.input2, recipe.input2Count)) {
            return false;
        }
        return true;
    }
    
    /**
     * Check if the required inputs for a recipe are available in the station
     */
    private boolean hasInputsInStation(CraftingRecipe recipe) {
        if (stationPos == null || !(level() instanceof ServerLevel serverLevel)) {
            return false;
        }
        
        if (!(serverLevel.getBlockEntity(stationPos) instanceof StationBlockEntity station)) {
            return false;
        }
        
        if (!hasItemsInContainer(station, recipe.input1, recipe.input1Count)) {
            return false;
        }
        if (recipe.input2 != null && !hasItemsInContainer(station, recipe.input2, recipe.input2Count)) {
            return false;
        }
        return true;
    }
    
    /**
     * Check if a container has enough of a specific item or tag
     */
    private boolean hasItemsInContainer(StationBlockEntity station, Object itemOrTag, int count) {
        int found = 0;
        for (int i = 0; i < station.getContainerSize(); i++) {
            ItemStack stack = station.getItem(i);
            if (stack.isEmpty()) continue;
            
            if (itemOrTag instanceof Item) {
                if (stack.is((Item) itemOrTag)) {
                    found += stack.getCount();
                }
            } else if (itemOrTag instanceof net.minecraft.tags.TagKey) {
                if (stack.is((net.minecraft.tags.TagKey<Item>) itemOrTag)) {
                    found += stack.getCount();
                }
            }
            
            if (found >= count) return true;
        }
        return false;
    }
    
    /**
     * Fetch materials from station and add to inventory
     */
    private void fetchMaterialsFromStation(Object itemOrTag, int count) {
        if (stationPos == null || !(level() instanceof ServerLevel serverLevel)) {
            return;
        }
        
        if (!(serverLevel.getBlockEntity(stationPos) instanceof StationBlockEntity station)) {
            return;
        }
        
        int remaining = count;
        for (int i = 0; i < station.getContainerSize() && remaining > 0; i++) {
            ItemStack stack = station.getItem(i);
            if (stack.isEmpty()) continue;
            
            boolean matches = false;
            if (itemOrTag instanceof Item) {
                matches = stack.is((Item) itemOrTag);
            } else if (itemOrTag instanceof net.minecraft.tags.TagKey) {
                matches = stack.is((net.minecraft.tags.TagKey<Item>) itemOrTag);
            }
            
            if (matches) {
                int toTake = Math.min(remaining, stack.getCount());
                ItemStack taken = station.removeItem(i, toTake);
                addToInventory(taken);
                remaining -= toTake;
                System.out.println("[BuilderEntity] Fetched " + toTake + "x " + taken.getDisplayName().getString() + " from station");
            }
        }
    }
    
    /**
     * Check if we have enough of a specific item or tag in inventory
     */
    private boolean hasItemsInInventory(Object itemOrTag, int count) {
        int found = 0;
        for (ItemStack stack : inventory) {
            if (stack.isEmpty()) continue;
            
            if (itemOrTag instanceof Item) {
                if (stack.is((Item) itemOrTag)) {
                    found += stack.getCount();
                }
            } else if (itemOrTag instanceof net.minecraft.tags.TagKey) {
                if (stack.is((net.minecraft.tags.TagKey<Item>) itemOrTag)) {
                    found += stack.getCount();
                }
            }
            
            if (found >= count) return true;
        }
        return false;
    }
    
    /**
     * Remove items from inventory for crafting
     */
    private void removeItemsForCrafting(Object itemOrTag, int count) {
        int remaining = count;
        for (int i = 0; i < inventory.size() && remaining > 0; i++) {
            ItemStack stack = inventory.get(i);
            if (stack.isEmpty()) continue;
            
            boolean matches = false;
            if (itemOrTag instanceof Item) {
                matches = stack.is((Item) itemOrTag);
            } else if (itemOrTag instanceof net.minecraft.tags.TagKey) {
                matches = stack.is((net.minecraft.tags.TagKey<Item>) itemOrTag);
            }
            
            if (matches) {
                int toRemove = Math.min(remaining, stack.getCount());
                stack.shrink(toRemove);
                remaining -= toRemove;
                
                if (stack.isEmpty()) {
                    inventory.set(i, ItemStack.EMPTY);
                }
            }
        }
    }
    
    /**
     * Add crafted items to inventory
     */
    private void addCraftedItems(Object itemOrTag, int count, Item neededItem) {
        // Determine which item to create
        Item itemToAdd = null;
        if (itemOrTag instanceof Item) {
            itemToAdd = (Item) itemOrTag;
        } else if (itemOrTag instanceof net.minecraft.tags.TagKey) {
            // For tags, check if the needed item matches the tag - if yes, use it!
            net.minecraft.tags.TagKey<Item> tag = (net.minecraft.tags.TagKey<Item>) itemOrTag;
            if (neededItem != null) {
                ItemStack testStack = new ItemStack(neededItem);
                if (testStack.is(tag)) {
                    // The needed item matches this tag, so craft exactly what we need!
                    itemToAdd = neededItem;
                    System.out.println("[BuilderEntity] Crafting exact match: " + neededItem.getDescriptionId() + " for tag " + tag.location());
                } else {
                    // Fallback to defaults
                    itemToAdd = getDefaultItemForTag(tag);
                    System.out.println("[BuilderEntity] Needed item " + neededItem.getDescriptionId() + " not in tag " + tag.location() + ", defaulting to " + (itemToAdd != null ? itemToAdd.getDescriptionId() : "null"));
                }
            } else {
                // No needed item specified, use default
                itemToAdd = getDefaultItemForTag(tag);
                System.out.println("[BuilderEntity] Crafting default for tag " + tag.location() + ": " + (itemToAdd != null ? itemToAdd.getDescriptionId() : "null"));
            }
        }
        
        if (itemToAdd != null) {
            ItemStack craftedStack = new ItemStack(itemToAdd, count);
            System.out.println("[BuilderEntity] Adding crafted items to inventory: " + craftedStack.getDisplayName().getString() + " x" + count);
            addToInventory(craftedStack);
        }
    }
    
    /**
     * Get default item for a tag (for intermediate crafting steps)
     */
    private Item getDefaultItemForTag(net.minecraft.tags.TagKey<Item> tag) {
        if (tag == ItemTags.PLANKS) {
            return getPreferredItemForTag(tag);
        } else if (tag == ItemTags.WOODEN_FENCES) {
            return getPreferredItemForTag(tag);
        } else if (tag == ItemTags.WOODEN_STAIRS) {
            return Items.OAK_STAIRS;
        } else if (tag == ItemTags.WOODEN_SLABS) {
            return Items.OAK_SLAB;
        } else if (tag == ItemTags.WOODEN_DOORS) {
            return Items.OAK_DOOR;
        } else if (tag == ItemTags.WOODEN_TRAPDOORS) {
            return Items.OAK_TRAPDOOR;
        } else if (tag == ItemTags.WOODEN_PRESSURE_PLATES) {
            return Items.OAK_PRESSURE_PLATE;
        } else if (tag == ItemTags.WOODEN_BUTTONS) {
            return Items.OAK_BUTTON;
        } else if (tag == ItemTags.FENCE_GATES) {
            return Items.OAK_FENCE_GATE;
        } else if (tag == ItemTags.SIGNS) {
            return Items.OAK_SIGN;
        } else if (tag == ItemTags.WOOL_CARPETS) {
            return Items.WHITE_CARPET;
        }
        return null;
    }

    /**
     * Choose a preferred concrete item for a tag based on materials already present
     * to reduce mixing variants. Falls back to oak if nothing is present.
     */
    private Item getPreferredItemForTag(net.minecraft.tags.TagKey<Item> tag) {
        // Prefer a variant that we already have in inventory or station
        if (tag == ItemTags.PLANKS) {
            Item[] variants = new Item[] {
                Items.OAK_PLANKS, Items.BIRCH_PLANKS, Items.SPRUCE_PLANKS,
                Items.JUNGLE_PLANKS, Items.ACACIA_PLANKS, Items.DARK_OAK_PLANKS,
                Items.CHERRY_PLANKS, Items.MANGROVE_PLANKS, Items.BAMBOO_PLANKS,
                Items.CRIMSON_PLANKS, Items.WARPED_PLANKS
            };
            for (Item variant : variants) {
                if (hasItemsInInventory(variant, 1) || hasItemsInStationSpecific(variant, 1)) {
                    return variant;
                }
            }
            return Items.OAK_PLANKS;
        } else if (tag == ItemTags.WOODEN_FENCES) {
            // Derive preferred fence from preferred plank type
            Item plank = getPreferredItemForTag(ItemTags.PLANKS);
            if (plank == Items.OAK_PLANKS) return Items.OAK_FENCE;
            if (plank == Items.BIRCH_PLANKS) return Items.BIRCH_FENCE;
            if (plank == Items.SPRUCE_PLANKS) return Items.SPRUCE_FENCE;
            if (plank == Items.JUNGLE_PLANKS) return Items.JUNGLE_FENCE;
            if (plank == Items.ACACIA_PLANKS) return Items.ACACIA_FENCE;
            if (plank == Items.DARK_OAK_PLANKS) return Items.DARK_OAK_FENCE;
            if (plank == Items.CHERRY_PLANKS) return Items.CHERRY_FENCE;
            if (plank == Items.MANGROVE_PLANKS) return Items.MANGROVE_FENCE;
            if (plank == Items.BAMBOO_PLANKS) return Items.BAMBOO_FENCE;
            if (plank == Items.CRIMSON_PLANKS) return Items.CRIMSON_FENCE;
            if (plank == Items.WARPED_PLANKS) return Items.WARPED_FENCE;
            return Items.OAK_FENCE;
        }
        // Fallback: try to pick any item currently present matching the tag
        net.minecraft.core.Registry<Item> itemRegistry = level().registryAccess().registryOrThrow(net.minecraft.core.registries.Registries.ITEM);
        java.util.Optional<net.minecraft.core.HolderSet.Named<Item>> tagHolders = itemRegistry.getTag(tag);
        if (tagHolders.isPresent()) {
            for (net.minecraft.core.Holder<Item> holder : tagHolders.get()) {
                Item itm = holder.value();
                if (hasItemsInInventory(itm, 1) || hasItemsInStationSpecific(itm, 1)) {
                    return itm;
                }
            }
        }
        return null;
    }

    /**
     * Estimate how many batches to craft based on remaining steps and inventory.
     * Returns -1 if unknown (craft as much as possible).
     */
    private int getNeededOutputBatches(Block neededBlock, int outputCount) {
        if (currentTask == null || outputCount <= 0) {
            return -1;
        }
        // Count remaining steps requiring a compatible block
        int remainingNeeded = 0;
        List<com.nothomealone.structure.BuildTask.BuildStep> steps = currentTask.getAllSteps();
        int startIdx = currentTask.getCurrentStepIndex();
        for (int i = startIdx; i < steps.size(); i++) {
            Block stepBlock = steps.get(i).state.getBlock();
            if (areBlocksCompatible(stepBlock, neededBlock)) {
                remainingNeeded++;
            }
        }
        // Count compatible items already in inventory
        int invCount = countCompatibleInInventory(neededBlock);
        int toCraft = Math.max(0, remainingNeeded - invCount);
        if (toCraft == 0) return 0;
        return (int) Math.ceil((double) toCraft / (double) outputCount);
    }

    /**
     * Count how many blocks in inventory are compatible with the needed block.
     */
    private int countCompatibleInInventory(Block neededBlock) {
        int total = 0;
        for (ItemStack stack : inventory) {
            if (stack.isEmpty()) continue;
            Block stackBlock = Block.byItem(stack.getItem());
            if (stackBlock != Blocks.AIR && (stackBlock == neededBlock || areBlocksCompatible(stackBlock, neededBlock))) {
                total += stack.getCount();
            }
        }
        return total;
    }

    /**
     * Breaks a block using the best available tool from inventory.
     * Damages the tool appropriately and removes it if broken.
     * @return true if a tool was used, false if no suitable tool found
     */
    private boolean breakBlockWithTool(ServerLevel level, BlockPos pos, BlockState state) {
        // First, try to find the CORRECT tool for drops (pickaxe for stone, axe for wood, shovel for dirt, etc.)
        ItemStack bestTool = ItemStack.EMPTY;
        int bestSlot = -1;
        float bestSpeed = 0.0f;
        
        // Priority 1: Find a tool that is correct for this block AND fast
        for (int i = 0; i < inventory.size(); i++) {
            ItemStack stack = inventory.get(i);
            if (!stack.isEmpty() && stack.isDamageableItem()) {
                float speed = stack.getDestroySpeed(state);
                boolean isCorrect = stack.isCorrectToolForDrops(state);
                
                // Prioritize correct tools
                if (isCorrect && speed > bestSpeed) {
                    bestSpeed = speed;
                    bestTool = stack;
                    bestSlot = i;
                }
            }
        }
        
        // Priority 2: If no correct tool found, use any tool that can break it faster than hand
        if (bestSlot == -1) {
            for (int i = 0; i < inventory.size(); i++) {
                ItemStack stack = inventory.get(i);
                if (!stack.isEmpty() && stack.isDamageableItem()) {
                    float speed = stack.getDestroySpeed(state);
                    if (speed > 1.0f && speed > bestSpeed) { // Faster than hand (1.0)
                        bestSpeed = speed;
                        bestTool = stack;
                        bestSlot = i;
                    }
                }
            }
        }
        
        // If we found a tool, use it
        if (bestSlot != -1 && !bestTool.isEmpty()) {
            final int finalSlot = bestSlot;  // Make final for lambda
            final String toolName = bestTool.getDisplayName().getString();  // Store name before breaking
            final boolean isCorrectTool = bestTool.isCorrectToolForDrops(state);
            
            // Reset tool request flag since we have tools
            toolsRequested = false;
            
            System.out.println("[BuilderEntity] Using tool: " + toolName + " (speed: " + bestSpeed + ", correct: " + isCorrectTool + ")");
            
            // Get the drops from breaking this block with the tool
            List<ItemStack> drops = Block.getDrops(state, level, pos, level.getBlockEntity(pos), this, bestTool);
            
            // Add drops directly to builder's inventory
            for (ItemStack drop : drops) {
                addToInventory(drop);
                System.out.println("[BuilderEntity] Collected: " + drop.getCount() + "x " + drop.getDisplayName().getString());
            }
            
            // Break the block without drops (we already collected them)
            level.destroyBlock(pos, false);
            
            // Damage the tool
            bestTool.hurtAndBreak(1, this, (entity) -> {
                // Tool broke - remove it from inventory
                System.out.println("[BuilderEntity] Tool broke: " + toolName);
                inventory.set(finalSlot, ItemStack.EMPTY);
            });
            
            // If tool didn't break, it's still in inventory (already damaged)
            if (bestTool.isEmpty()) {
                inventory.set(finalSlot, ItemStack.EMPTY);
            }
            
            return true;
        }
        
        // No tool found in inventory - check if tools are available in station before alerting player
        if (!toolsRequested && !hasToolsInStation()) {
            System.out.println("[BuilderEntity] No suitable tool found in inventory OR station");
            Component message = Component.literal("[Builder] I need tools to break blocks! Please add tools to my station.");
            level.players().forEach(player -> {
                if (player.distanceToSqr(this) < 4096) { // Within 64 blocks
                    player.sendSystemMessage(message);
                }
            });
            toolsRequested = true;
        } else if (!toolsRequested) {
            System.out.println("[BuilderEntity] No tool in inventory, but tools available in station - ReplaceToolsGoal will handle");
        }
        return false;
    }
    
    /**
     * Check if there are any damageable tools in the station
     */
    private boolean hasToolsInStation() {
        if (stationPos == null || !(level() instanceof ServerLevel serverLevel)) {
            return false;
        }
        
        if (!(serverLevel.getBlockEntity(stationPos) instanceof StationBlockEntity station)) {
            return false;
        }
        
        for (int i = 0; i < station.getContainerSize(); i++) {
            ItemStack stack = station.getItem(i);
            if (!stack.isEmpty() && stack.isDamageableItem() && stack.getDamageValue() < stack.getMaxDamage()) {
                return true; // Found a usable tool
            }
        }
        
        return false;
    }
    
    /**
     * Checks if builder has the specified block in inventory.
     */
    private boolean hasBlockInInventory(Block block) {
        System.out.println("[BuilderEntity] Checking inventory for: " + block.getName().getString());
        for (ItemStack stack : inventory) {
            if (stack.isEmpty()) continue;
            Block stackBlock = Block.byItem(stack.getItem());
            boolean compatible = areBlocksCompatible(stackBlock, block);
            System.out.println("[BuilderEntity]   Found: " + stackBlock.getName().getString() + ", compatible: " + compatible);
            // Check exact match or compatible blocks (e.g. oak fence = birch fence)
            if (stackBlock == block || compatible) {
                System.out.println("[BuilderEntity]   -> Match found!");
                return true;
            }
        }
        System.out.println("[BuilderEntity]   -> No match found");
        return false;
    }

    /**
     * Returns a compatible block from inventory if present, else null.
     */
    private Block getCompatibleInventoryBlock(Block required) {
        for (ItemStack stack : inventory) {
            if (stack.isEmpty()) continue;
            Block stackBlock = Block.byItem(stack.getItem());
            if (stackBlock == required || areBlocksCompatible(stackBlock, required)) {
                return stackBlock;
            }
        }
        return null;
    }

    /**
     * Removes one block from builder's inventory (or compatible block).
     */
    private void removeBlockFromInventory(Block block) {
        for (int i = 0; i < inventory.size(); i++) {
            ItemStack stack = inventory.get(i);
            if (stack.isEmpty()) continue;
            Block stackBlock = Block.byItem(stack.getItem());
            // Remove exact match or compatible block
            if (stackBlock == block || areBlocksCompatible(stackBlock, block)) {
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
    public InteractionResult mobInteract(Player player, InteractionHand hand) {
        if (!this.level().isClientSide && player instanceof ServerPlayer serverPlayer) {
            // Open chest-like GUI showing builder's inventory
            serverPlayer.openMenu(new SimpleMenuProvider(
                (id, playerInventory, p) -> ChestMenu.threeRows(id, playerInventory, this),
                Component.literal("Builder Inventory")
            ));
            return InteractionResult.SUCCESS;
        }
        return InteractionResult.sidedSuccess(this.level().isClientSide);
    }
    
    // Implement Container interface methods for chest GUI
    @Override
    public int getContainerSize() {
        return inventory.size();
    }
    
    @Override
    public boolean isEmpty() {
        for (ItemStack stack : inventory) {
            if (!stack.isEmpty()) return false;
        }
        return true;
    }
    
    @Override
    public ItemStack getItem(int slot) {
        return inventory.get(slot);
    }
    
    @Override
    public ItemStack removeItem(int slot, int amount) {
        return net.minecraft.world.ContainerHelper.removeItem(inventory, slot, amount);
    }
    
    @Override
    public ItemStack removeItemNoUpdate(int slot) {
        return net.minecraft.world.ContainerHelper.takeItem(inventory, slot);
    }
    
    @Override
    public void setItem(int slot, ItemStack stack) {
        inventory.set(slot, stack);
    }
    
    @Override
    public void setChanged() {
        // Builder inventory changed
    }
    
    @Override
    public boolean stillValid(Player player) {
        return this.isAlive() && player.distanceToSqr(this) < 64.0;
    }
    
    @Override
    public void clearContent() {
        inventory.clear();
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
    // === Batch crafting helpers (must be at class level, after all other methods) ===
    /**
     * Compute the maximum number of batches that can be crafted from available inventory and station inputs.
     */
    private int getMaxCraftableBatches(CraftingRecipe recipe) {
        int input1Avail = getTotalAvailableForInput(recipe.input1);
        int input2Avail = recipe.input2 != null ? getTotalAvailableForInput(recipe.input2) : Integer.MAX_VALUE;
        int maxBatches = input1Avail / recipe.input1Count;
        if (recipe.input2 != null) {
            maxBatches = Math.min(maxBatches, input2Avail / recipe.input2Count);
        }
        return maxBatches;
    }

    /**
     * Get total available count for an input (inventory + station).
     */
    private int getTotalAvailableForInput(Object itemOrTag) {
        int total = 0;
        // Inventory
        for (ItemStack stack : inventory) {
            if (stack.isEmpty()) continue;
            if (itemOrTag instanceof Item && stack.is((Item)itemOrTag)) total += stack.getCount();
            else if (itemOrTag instanceof net.minecraft.tags.TagKey && stack.is((net.minecraft.tags.TagKey<Item>)itemOrTag)) total += stack.getCount();
        }
        // Station
        if (stationPos != null && (level() instanceof ServerLevel serverLevel)) {
            if (serverLevel.getBlockEntity(stationPos) instanceof StationBlockEntity station) {
                for (int i = 0; i < station.getContainerSize(); i++) {
                    ItemStack stack = station.getItem(i);
                    if (stack.isEmpty()) continue;
                    if (itemOrTag instanceof Item && stack.is((Item)itemOrTag)) total += stack.getCount();
                    else if (itemOrTag instanceof net.minecraft.tags.TagKey && stack.is((net.minecraft.tags.TagKey<Item>)itemOrTag)) total += stack.getCount();
                }
            }
        }
        return total;
    }
}
