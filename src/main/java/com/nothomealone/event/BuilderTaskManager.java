package com.nothomealone.event;

import com.nothomealone.NotHomeAlone;
import com.nothomealone.block.entity.StationBlockEntity;
import com.nothomealone.entity.custom.BuilderEntity;
import com.nothomealone.structure.BuildTask;
import com.nothomealone.structure.StructureAnalyzer;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.level.LevelEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Manages builder task assignment and reassignment.
 * Ensures builders always have their tasks even after chunk reload.
 */
@Mod.EventBusSubscriber(modid = NotHomeAlone.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class BuilderTaskManager {
    
    // Track which stations have active build tasks (station pos -> structure level)
    private static final Map<BlockPos, String> activeStations = new HashMap<>();
    private static int tickCounter = 0;
    
    /**
     * Register a station as having an active build task.
     */
    public static void registerStation(BlockPos stationPos, String structureLevel) {
        activeStations.put(stationPos, structureLevel);
        System.out.println("[BuilderTaskManager] Registered station at " + stationPos + " with structure level " + structureLevel);
    }
    
    /**
     * Unregister a station (when structure is complete or station destroyed).
     */
    public static void unregisterStation(BlockPos stationPos) {
        activeStations.remove(stationPos);
        System.out.println("[BuilderTaskManager] Unregistered station at " + stationPos);
    }
    
    /**
     * When world loads, scan for all builder stations and register them.
     * This ensures tasks are reassigned after server restart.
     */
    @SubscribeEvent
    public static void onWorldLoad(LevelEvent.Load event) {
        if (!(event.getLevel() instanceof ServerLevel serverLevel)) return;
        
        System.out.println("[BuilderTaskManager] Scanning world for builder stations...");
        
        // Scan all loaded chunks for builder stations and restore tasks
        serverLevel.getAllEntities().forEach(entity -> {
            if (entity instanceof BuilderEntity builder) {
                BlockPos stationPos = builder.getStationPos();
                if (stationPos != null) {
                    BlockEntity blockEntity = serverLevel.getBlockEntity(stationPos);
                    if (blockEntity instanceof StationBlockEntity stationEntity && !stationEntity.isStructureBuilt()) {
                        // Register this station - assume level 1 for now
                        // TODO: Store structure level in StationBlockEntity NBT
                        registerStation(stationPos, "1");
                        System.out.println("[BuilderTaskManager] Found builder station at " + stationPos);
                        
                        // If builder has no task, reassign it immediately with saved progress
                        if (builder.getCurrentTask() == null) {
                            // Read saved progress from builder's NBT (already loaded in readAdditionalSaveData)
                            int savedProgress = builder.getSavedTaskProgress();
                            System.out.println("[BuilderTaskManager] Restoring task for builder #" + builder.getId() + " with progress: " + savedProgress);
                            reassignTask(serverLevel, stationPos, "1", builder, savedProgress);
                        }
                    }
                }
            }
        });
    }
    
    /**
     * Every 5 seconds, check all builders and reassign tasks if needed.
     */
    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        
        tickCounter++;
        if (tickCounter < 100) return; // Check every 5 seconds (100 ticks)
        tickCounter = 0;
        
        // Check all registered stations
        for (Map.Entry<BlockPos, String> entry : activeStations.entrySet()) {
            BlockPos stationPos = entry.getKey();
            String structureLevel = entry.getValue();
            
            for (ServerLevel level : event.getServer().getAllLevels()) {
                // Check if station still exists
                BlockEntity blockEntity = level.getBlockEntity(stationPos);
                if (!(blockEntity instanceof StationBlockEntity stationEntity)) {
                    continue;
                }
                
                // Check if structure already built
                if (stationEntity.isStructureBuilt()) {
                    unregisterStation(stationPos);
                    continue;
                }
                
                // Find builder for this station
                BuilderEntity builder = findBuilderForStation(level, stationPos);
                if (builder == null) {
                    System.out.println("[BuilderTaskManager] No builder found for station at " + stationPos);
                    continue;
                }
                
                // Check if builder has a task (only reassign if truly lost, not if completed)
                if (builder.getCurrentTask() == null) {
                    // Try to restore saved progress from NBT
                    int savedProgress = builder.getSavedTaskProgress();
                    System.out.println("[BuilderTaskManager] Builder #" + builder.getId() + " lost task, reassigning with saved progress: " + savedProgress);
                    reassignTask(level, stationPos, structureLevel, builder, savedProgress);
                } else if (builder.getCurrentTask().isCompleted()) {
                    System.out.println("[BuilderTaskManager] Builder #" + builder.getId() + " completed task!");
                    stationEntity.setStructureBuilt(true);
                    unregisterStation(stationPos);
                }
            }
        }
    }
    
    /**
     * Reassigns a build task to a builder.
     * @param savedProgress If > 0, restores the task to that progress
     */
    private static void reassignTask(ServerLevel level, BlockPos stationPos, String structureLevel, BuilderEntity builder, int savedProgress) {
        ResourceLocation structureLocation = new ResourceLocation("nothomealone", 
            "builder/builder_level_" + structureLevel);
        
        try {
            BuildTask task = StructureAnalyzer.analyzeStructure(level, structureLocation, stationPos);
            if (task != null) {
                // Restore progress if we have saved data
                if (savedProgress > 0) {
                    task.setCurrentStepIndex(savedProgress);
                    System.out.println("[BuilderTaskManager] Restored task progress: " + savedProgress + "/" + task.getTotalSteps());
                }
                
                builder.setBuildTask(task);
                System.out.println("[BuilderTaskManager] Reassigned task with " + task.getTotalSteps() + " steps to builder #" + builder.getId() + " (progress: " + task.getCompletedSteps() + ")");
            }
        } catch (Exception e) {
            System.err.println("[BuilderTaskManager] Failed to reassign task: " + e.getMessage());
        }
    }
    
    /**
     * Finds the builder entity for a station.
     */
    private static BuilderEntity findBuilderForStation(ServerLevel level, BlockPos stationPos) {
        for (BuilderEntity builder : level.getEntitiesOfClass(BuilderEntity.class, 
                new net.minecraft.world.phys.AABB(-30000000, -64, -30000000, 30000000, 320, 30000000))) {
            BlockPos builderStation = builder.getStationPos();
            if (builderStation != null && builderStation.equals(stationPos)) {
                return builder;
            }
        }
        return null;
    }
}
