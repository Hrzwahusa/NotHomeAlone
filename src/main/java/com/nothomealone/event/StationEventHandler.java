package com.nothomealone.event;

import com.nothomealone.NotHomeAlone;
import com.nothomealone.block.custom.BaseStationBlock;
import com.nothomealone.block.entity.StationBlockEntity;
import com.nothomealone.territory.Territory;
import com.nothomealone.territory.TerritoryManager;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.event.level.BlockEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.UUID;

@Mod.EventBusSubscriber(modid = NotHomeAlone.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class StationEventHandler {

    /**
     * Handles station block placement.
     * Claims territory and prevents placement in already claimed areas.
     */
    @SubscribeEvent
    public static void onBlockPlace(BlockEvent.EntityPlaceEvent event) {
        if (!(event.getEntity() instanceof Player player)) {
            return;
        }

        if (!(event.getPlacedBlock().getBlock() instanceof BaseStationBlock stationBlock)) {
            return;
        }

        Level level = (Level) event.getLevel();
        BlockPos pos = event.getPos();

        int territoryRadius = stationBlock.getTerritoryRadius();
        int workRadius = stationBlock.getWorkRadius();

        // Check if this position is in an already claimed territory
        if (TerritoryManager.isAreaClaimed(level, 
                pos.offset(-territoryRadius, -territoryRadius, -territoryRadius),
                pos.offset(territoryRadius, territoryRadius, territoryRadius))) {
            
            // Cancel placement and return item to player
            event.setCanceled(true);
            
            if (!level.isClientSide) {
                player.displayClientMessage(
                    Component.literal("§cCannot place station here! This area overlaps with an existing territory."),
                    true
                );
                
                // Return the block to the player's inventory
                ItemStack blockItem = new ItemStack(stationBlock);
                if (!player.getInventory().add(blockItem)) {
                    player.drop(blockItem, false);
                }
            }
            return;
        }

        // Claim territory
        if (!level.isClientSide) {
            Territory territory = TerritoryManager.claimTerritory(level, pos, territoryRadius, workRadius);
            
            if (territory != null) {
                // Store territory info in block entity
                BlockEntity blockEntity = level.getBlockEntity(pos);
                if (blockEntity instanceof StationBlockEntity stationEntity) {
                    stationEntity.setTerritory(territory.min(), territory.max(), territoryRadius, workRadius);
                }
                
                int territorySize = territoryRadius * 2;
                int workArea = workRadius * 2;
                
                player.displayClientMessage(
                    Component.literal("§aTerritory claimed! Claim: " + territorySize + "x" + territorySize + 
                                     ", Work area: " + workArea + "x" + workArea),
                    true
                );

                System.out.println("[NotHomeAlone] Attempting to spawn " + stationBlock.getWorkerType() + " at " + pos);
                
                // Spawn NPC worker
                boolean spawned = spawnWorker(level, pos, stationBlock.getWorkerType(), workRadius);
                
                if (spawned) {
                    System.out.println("[NotHomeAlone] Successfully spawned " + stationBlock.getWorkerType());
                    player.displayClientMessage(
                        Component.literal("§e" + stationBlock.getWorkerType() + " spawned!"),
                        false
                    );
                } else {
                    System.out.println("[NotHomeAlone] Failed to spawn " + stationBlock.getWorkerType());
                    player.displayClientMessage(
                        Component.literal("§cFailed to spawn " + stationBlock.getWorkerType()),
                        false
                    );
                }
                
                // If Builder station, initialize build task
                System.out.println("[StationEventHandler] Checking if builder station: " + stationBlock.getWorkerType());
                if ("builder".equalsIgnoreCase(stationBlock.getWorkerType())) {
                    int stationLevel = event.getPlacedBlock().getValue(BaseStationBlock.LEVEL);
                    System.out.println("[StationEventHandler] IS BUILDER! Calling initializeBuildTask with level " + stationLevel);
                    com.nothomealone.structure.BuilderManager.initializeBuildTask(level, pos, String.valueOf(stationLevel));
                    // Register in task manager for automatic reassignment
                    BuilderTaskManager.registerStation(pos, String.valueOf(stationLevel));
                } else {
                    System.out.println("[StationEventHandler] NOT a builder station");
                }
            }
        }
    }
    
    /**
     * Spawns a worker NPC for the station.
     * @return true if spawn was successful
     */
    private static boolean spawnWorker(Level level, BlockPos stationPos, String workerType, int workRadius) {
        System.out.println("[NotHomeAlone] spawnWorker called: " + workerType + " at " + stationPos + " (isClientSide: " + level.isClientSide + ")");
        
        // First, cleanup all workers without a station
        cleanupOrphanedWorkers(level);
        
        // Check if a worker already exists for this station
        if (findWorkerForStation(level, stationPos) != null) {
            System.out.println("[NotHomeAlone] Worker already exists for this station, skipping spawn");
            return true; // Consider it successful since a worker exists
        }
        // Import entity types based on worker type (case-insensitive)
        net.minecraft.world.entity.EntityType<?> entityType = switch (workerType.toLowerCase()) {
            case "builder" -> com.nothomealone.entity.ModEntities.BUILDER.get();
            case "lumberjack" -> com.nothomealone.entity.ModEntities.LUMBERJACK.get();
            case "miner" -> com.nothomealone.entity.ModEntities.MINER.get();
            case "hunter" -> com.nothomealone.entity.ModEntities.HUNTER.get();
            case "farmer" -> com.nothomealone.entity.ModEntities.FARMER.get();
            case "fisher" -> com.nothomealone.entity.ModEntities.FISHER.get();
            case "storage" -> com.nothomealone.entity.ModEntities.STORAGE.get();
            case "blacksmith" -> com.nothomealone.entity.ModEntities.BLACKSMITH.get();
            default -> null;
        };
        
        System.out.println("[NotHomeAlone] EntityType: " + entityType);
        
        if (entityType == null) {
            System.out.println("[NotHomeAlone] EntityType is null for: " + workerType);
            return false;
        }
        
        try {
            net.minecraft.world.entity.Entity entity = entityType.create(level);
            System.out.println("[NotHomeAlone] Created entity: " + entity);
            
            if (entity instanceof com.nothomealone.entity.custom.WorkerEntity worker) {
                // Position worker next to station
                worker.moveTo(stationPos.getX() + 0.5, stationPos.getY() + 1, stationPos.getZ() + 0.5, 0, 0);
                worker.setHomeStation(stationPos, workRadius);
                
                System.out.println("[NotHomeAlone] Adding entity to world at: " + worker.position());
                boolean added = level.addFreshEntity(worker);
                System.out.println("[NotHomeAlone] Entity added: " + added);
                
                // Store worker ID and UUID in station and give tools to builder
                BlockEntity blockEntity = level.getBlockEntity(stationPos);
                if (blockEntity instanceof StationBlockEntity stationEntity) {
                    stationEntity.setWorkerEntityId(worker.getId());
                    stationEntity.setWorkerUUID(worker.getUUID());
                    System.out.println("[NotHomeAlone] Stored worker ID: " + worker.getId() + " UUID: " + worker.getUUID());
                    
                    // Give crafting tools to builder
                    if (worker instanceof com.nothomealone.entity.custom.BuilderEntity builder) {
                        builder.addToInventory(new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.WOODEN_PICKAXE));
                        builder.addToInventory(new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.WOODEN_AXE));
                        builder.addToInventory(new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.WOODEN_SHOVEL));
                        System.out.println("[NotHomeAlone] Gave tools to builder");
                    }
                }
                
                return added;
            } else {
                System.out.println("[NotHomeAlone] Entity is not a WorkerEntity: " + entity.getClass().getName());
            }
        } catch (Exception e) {
            System.err.println("[NotHomeAlone] Error spawning entity: " + e.getMessage());
            e.printStackTrace();
        }
        
        return false;
    }
    
    /**
     * Removes all workers that don't have a station assigned.
     */
    private static void cleanupOrphanedWorkers(Level level) {
        java.util.List<com.nothomealone.entity.custom.WorkerEntity> workers = 
            level.getEntitiesOfClass(com.nothomealone.entity.custom.WorkerEntity.class, 
                new net.minecraft.world.phys.AABB(-30000000, -64, -30000000, 30000000, 320, 30000000));
        
        int removed = 0;
        for (com.nothomealone.entity.custom.WorkerEntity worker : workers) {
            if (worker.getStationPos() == null) {
                System.out.println("[NotHomeAlone] Removing orphaned worker #" + worker.getId() + " at " + worker.position());
                worker.discard();
                removed++;
            }
        }
        
        if (removed > 0) {
            System.out.println("[NotHomeAlone] Cleaned up " + removed + " orphaned workers");
        }
    }
    
    /**
     * Finds a worker that is assigned to the given station.
     */
    private static com.nothomealone.entity.custom.WorkerEntity findWorkerForStation(Level level, BlockPos stationPos) {
        java.util.List<com.nothomealone.entity.custom.WorkerEntity> workers = 
            level.getEntitiesOfClass(com.nothomealone.entity.custom.WorkerEntity.class, 
                new net.minecraft.world.phys.AABB(-30000000, -64, -30000000, 30000000, 320, 30000000));
        
        for (com.nothomealone.entity.custom.WorkerEntity worker : workers) {
            BlockPos workerStation = worker.getStationPos();
            if (workerStation != null && workerStation.equals(stationPos)) {
                System.out.println("[NotHomeAlone] Found existing worker #" + worker.getId() + " for station at " + stationPos);
                return worker;
            }
        }
        
        return null;
    }

    /**
     * Handles station block destruction.
     * Releases territory when station is broken.
     */
    @SubscribeEvent
    public static void onBlockBreak(BlockEvent.BreakEvent event) {
        if (!(event.getState().getBlock() instanceof BaseStationBlock stationBlock)) {
            return;
        }

        Level level = (Level) event.getLevel();
        BlockPos pos = event.getPos();

        if (!level.isClientSide) {
            TerritoryManager.releaseTerritory(level, pos);
            
            // Remove worker entity if station has one
            BlockEntity blockEntity = level.getBlockEntity(pos);
            if (blockEntity instanceof StationBlockEntity stationEntity) {
                UUID workerUUID = stationEntity.getWorkerUUID();
                if (workerUUID != null && level instanceof ServerLevel serverLevel) {
                    net.minecraft.world.entity.Entity entity = serverLevel.getEntity(workerUUID);
                    if (entity instanceof com.nothomealone.entity.custom.WorkerEntity worker) {
                        worker.discard();
                        System.out.println("[NotHomeAlone] Removed worker with UUID: " + workerUUID);
                    }
                }
            }
            
            // Unregister builder station from task manager
            if ("builder".equalsIgnoreCase(stationBlock.getWorkerType())) {
                BuilderTaskManager.unregisterStation(pos);
            }
            
            Player player = event.getPlayer();
            if (player != null) {
                player.displayClientMessage(
                    Component.literal("§6Territory released."),
                    true
                );
            }
        }
    }
}
