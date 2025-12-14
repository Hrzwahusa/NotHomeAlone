package com.nothomealone.block.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.Container;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

/**
 * BlockEntity for worker stations.
 * Stores territory information, upgrade status, NPC reference, and crafting materials.
 */
public class StationBlockEntity extends BlockEntity implements Container, MenuProvider {
    private BlockPos territoryMin;
    private BlockPos territoryMax;
    private int territoryRadius;
    private int workRadius;
    private int workerEntityId = -1; // Legacy, use workerUUID instead
    private UUID workerUUID = null;
    private boolean structureBuilt = false;
    
    // Stores the crafting materials used to create this station
    // Builder can extract these materials when building the structure
    // Size 54 = double chest (6 rows x 9 columns)
    private NonNullList<ItemStack> craftingMaterials = NonNullList.withSize(54, ItemStack.EMPTY);

    public StationBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.STATION_BLOCK_ENTITY.get(), pos, state);
    }

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        
        if (territoryMin != null) {
            tag.putLong("TerritoryMin", territoryMin.asLong());
        }
        if (territoryMax != null) {
            tag.putLong("TerritoryMax", territoryMax.asLong());
        }
        tag.putInt("TerritoryRadius", territoryRadius);
        tag.putInt("WorkRadius", workRadius);
        tag.putInt("WorkerEntityId", workerEntityId);
        if (workerUUID != null) {
            tag.putUUID("WorkerUUID", workerUUID);
        }
        tag.putBoolean("StructureBuilt", structureBuilt);
        ContainerHelper.saveAllItems(tag, craftingMaterials);
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        
        if (tag.contains("TerritoryMin")) {
            territoryMin = BlockPos.of(tag.getLong("TerritoryMin"));
        }
        if (tag.contains("TerritoryMax")) {
            territoryMax = BlockPos.of(tag.getLong("TerritoryMax"));
        }
        territoryRadius = tag.getInt("TerritoryRadius");
        workRadius = tag.getInt("WorkRadius");
        workerEntityId = tag.getInt("WorkerEntityId");
        if (tag.hasUUID("WorkerUUID")) {
            workerUUID = tag.getUUID("WorkerUUID");
        }
        structureBuilt = tag.getBoolean("StructureBuilt");
        ContainerHelper.loadAllItems(tag, craftingMaterials);
    }

    public void setTerritory(BlockPos min, BlockPos max, int territoryRadius, int workRadius) {
        this.territoryMin = min;
        this.territoryMax = max;
        this.territoryRadius = territoryRadius;
        this.workRadius = workRadius;
        setChanged();
    }

    public BlockPos getTerritoryMin() {
        return territoryMin;
    }

    public BlockPos getTerritoryMax() {
        return territoryMax;
    }

    public void setWorkerEntityId(int entityId) {
        this.workerEntityId = entityId;
        setChanged();
    }

    public int getWorkerEntityId() {
        return workerEntityId;
    }

    public void setWorkerUUID(UUID uuid) {
        this.workerUUID = uuid;
        setChanged();
    }

    public UUID getWorkerUUID() {
        return workerUUID;
    }

    public void setStructureBuilt(boolean built) {
        this.structureBuilt = built;
        setChanged();
    }

    public boolean isStructureBuilt() {
        return structureBuilt;
    }

    public int getTerritoryRadius() {
        return territoryRadius;
    }

    public int getWorkRadius() {
        return workRadius;
    }

    /**
     * Gets the crafting materials stored in this station.
     * Builder can use these materials to construct the station structure.
     */
    public NonNullList<ItemStack> getCraftingMaterials() {
        return craftingMaterials;
    }

    /**
     * Sets the crafting materials for this station.
     * Called when the station is crafted and placed.
     */
    public void setCraftingMaterials(NonNullList<ItemStack> materials) {
        this.craftingMaterials = materials;
        setChanged();
    }

    /**
     * Checks if the builder has already extracted materials.
     */
    public boolean hasMaterials() {
        for (ItemStack stack : craftingMaterials) {
            if (!stack.isEmpty()) {
                return true;
            }
        }
        return false;
    }

    /**
     * Clears all crafting materials (called after builder uses them).
     */
    public void clearMaterials() {
        craftingMaterials.clear();
        for (int i = 0; i < craftingMaterials.size(); i++) {
            craftingMaterials.set(i, ItemStack.EMPTY);
        }
        setChanged();
    }

    // Container implementation
    @Override
    public int getContainerSize() {
        return craftingMaterials.size();
    }

    @Override
    public boolean isEmpty() {
        return !hasMaterials();
    }

    @Override
    public ItemStack getItem(int slot) {
        return craftingMaterials.get(slot);
    }

    @Override
    public ItemStack removeItem(int slot, int amount) {
        ItemStack result = ContainerHelper.removeItem(craftingMaterials, slot, amount);
        if (!result.isEmpty()) {
            setChanged();
        }
        return result;
    }

    @Override
    public ItemStack removeItemNoUpdate(int slot) {
        return ContainerHelper.takeItem(craftingMaterials, slot);
    }

    @Override
    public void setItem(int slot, ItemStack stack) {
        craftingMaterials.set(slot, stack);
        if (stack.getCount() > getMaxStackSize()) {
            stack.setCount(getMaxStackSize());
        }
        setChanged();
    }

    @Override
    public boolean stillValid(Player player) {
        return Container.stillValidBlockEntity(this, player);
    }

    @Override
    public void clearContent() {
        craftingMaterials.clear();
    }

    // MenuProvider implementation
    @Override
    public Component getDisplayName() {
        return Component.translatable("container.nothomealone.station");
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
        // GENERIC_9x6 = 6 rows (double chest)
        return new ChestMenu(MenuType.GENERIC_9x6, containerId, playerInventory, this, 6);
    }
}
