package com.nothomealone.block.custom;

import com.nothomealone.block.entity.StationBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.NonNullList;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Containers;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Base class for all worker station blocks.
 * Handles territory claiming, upgrading, and NPC spawning.
 */
public abstract class BaseStationBlock extends BaseEntityBlock {
    public static final IntegerProperty LEVEL = IntegerProperty.create("level", 1, 3);
    
    protected BaseStationBlock(Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any().setValue(LEVEL, 1));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(LEVEL);
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new StationBlockEntity(pos, state);
    }

    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, 
                                 InteractionHand hand, BlockHitResult hit) {
        if (!level.isClientSide && hand == InteractionHand.MAIN_HAND) {
            BlockEntity blockEntity = level.getBlockEntity(pos);
            if (blockEntity instanceof StationBlockEntity stationEntity) {
                // Open the station GUI for inventory/upgrade management
                player.openMenu(stationEntity);
                return InteractionResult.SUCCESS;
            }
        }
        return InteractionResult.SUCCESS;
    }

    /**
     * Returns the worker type for this station.
     */
    public abstract String getWorkerType();

    /**
     * Returns the territory radius (claim area where no other stations can be placed).
     * Default: 6 blocks (12x12 area)
     */
    public int getTerritoryRadius() {
        return 6;
    }

    /**
     * Returns the work radius (area where the NPC can work).
     * Default: 32 blocks - much larger than territory
     */
    public int getWorkRadius() {
        return 32;
    }

    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state, @Nullable LivingEntity placer, ItemStack stack) {
        super.setPlacedBy(level, pos, state, placer, stack);
        
        if (!level.isClientSide) {
            BlockEntity blockEntity = level.getBlockEntity(pos);
            if (blockEntity instanceof StationBlockEntity stationEntity) {
                // Load crafting recipe materials and put them in the station's inventory
                loadCraftingMaterials(level, stationEntity);
            }
        }
    }
    
    /**
     * Loads the crafting materials from the recipe and stores them in the station's inventory.
     * Excludes tools (items with durability/craftingRemainingItem).
     */
    private void loadCraftingMaterials(Level level, StationBlockEntity stationEntity) {
        // Find the recipe for this station block
        List<CraftingRecipe> recipes = level.getRecipeManager()
            .getAllRecipesFor(RecipeType.CRAFTING);
        
        for (CraftingRecipe recipe : recipes) {
            ItemStack result = recipe.getResultItem(level.registryAccess());
            if (result.getItem() == this.asItem()) {
                // Found the recipe for this station
                // Count non-tool ingredients
                Map<Item, Integer> ingredientCounts = new HashMap<>();
                
                for (var ingredient : recipe.getIngredients()) {
                    ItemStack[] matchingItems = ingredient.getItems();
                    if (matchingItems.length > 0) {
                        ItemStack item = matchingItems[0];
                        
                        // Skip tools (items with durability or crafting remaining items like buckets)
                        if (!item.isDamageableItem() && !item.hasCraftingRemainingItem()) {
                            Item itemType = item.getItem();
                            ingredientCounts.put(itemType, ingredientCounts.getOrDefault(itemType, 0) + 1);
                        }
                    }
                }
                
                // Put the counted items into the station's inventory
                int slot = 0;
                for (Map.Entry<Item, Integer> entry : ingredientCounts.entrySet()) {
                    if (slot < stationEntity.getContainerSize()) {
                        ItemStack stack = new ItemStack(entry.getKey(), entry.getValue());
                        stationEntity.setItem(slot++, stack);
                    }
                }
                
                System.out.println("[BaseStationBlock] Loaded " + ingredientCounts.size() + " different material types into station inventory");
                break;
            }
        }
    }

    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean isMoving) {
        if (!state.is(newState.getBlock())) {
            BlockEntity blockEntity = level.getBlockEntity(pos);
            if (blockEntity instanceof StationBlockEntity stationEntity) {
                // Drop all items from the station's inventory
                Containers.dropContents(level, pos, stationEntity);
                
                // Remove the worker NPC when station is destroyed
                if (!level.isClientSide && level instanceof net.minecraft.server.level.ServerLevel serverLevel) {
                    int workerEntityId = stationEntity.getWorkerEntityId();
                    if (workerEntityId != -1) {
                        net.minecraft.world.entity.Entity entity = serverLevel.getEntity(workerEntityId);
                        if (entity != null) {
                            entity.discard();
                        }
                    }
                }
            }
            super.onRemove(state, level, pos, newState, isMoving);
        }
    }

    @Override
    public List<ItemStack> getDrops(BlockState state, LootParams.Builder builder) {
        // Drop the station block itself with current level preserved
        ItemStack stack = new ItemStack(this);
        return Collections.singletonList(stack);
    }
}
