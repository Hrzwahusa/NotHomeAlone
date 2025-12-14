package com.nothomealone.structure;

import com.nothomealone.block.entity.StationBlockEntity;
import com.nothomealone.entity.custom.BuilderEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;

/**
 * Manages the building process when a new Builder Station is placed.
 */
public class BuilderManager {
    
    /**
     * Initializes the building process for a newly placed Builder Station.
     * Loads the structure file and assigns it to the builder.
     */
    public static void initializeBuildTask(Level level, BlockPos stationPos, String structureLevel) {
        if (!(level instanceof ServerLevel serverLevel)) return;
        
        BlockEntity blockEntity = level.getBlockEntity(stationPos);
        if (!(blockEntity instanceof StationBlockEntity stationEntity)) return;
        
        // Check if structure already built
        if (stationEntity.isStructureBuilt()) return;
        
        // Load the structure file (path is relative to data/nothomealone/structures/)
        ResourceLocation structureLocation = new ResourceLocation("nothomealone", 
            "builder/builder_level_" + structureLevel);
        
        System.out.println("[BuilderManager] Initializing build task for level " + structureLevel);
        System.out.println("[BuilderManager] Looking for structure: " + structureLocation);
        
        try {
            // Analyze structure and create build task
            BuildTask task = StructureAnalyzer.analyzeStructure(serverLevel, structureLocation, stationPos);
            
            if (task != null) {
                System.out.println("[BuilderManager] Build task created with " + task.getTotalSteps() + " steps");
                // Find or create builder entity (try multiple times as entity might spawn later)
                assignTaskToBuilder(serverLevel, stationPos, task, 0);
            } else {
                System.out.println("[BuilderManager] Failed to create build task - structure might not exist");
            }
        } catch (Exception e) {
            // Structure file might not exist yet
            System.err.println("[BuilderManager] Could not load structure for builder station: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * Tries to assign task to builder, retrying if builder not found yet.
     */
    private static void assignTaskToBuilder(ServerLevel level, BlockPos stationPos, BuildTask task, int retryCount) {
        BuilderEntity builder = findBuilderForStation(level, stationPos);
        if (builder != null) {
            System.out.println("[BuilderManager] Assigning build task to builder");
            builder.setBuildTask(task);
        } else if (retryCount < 20) {
            // Builder might not have spawned yet, retry in 10 ticks (0.5 seconds)
            level.getServer().tell(new net.minecraft.server.TickTask(
                level.getServer().getTickCount() + 10,
                () -> assignTaskToBuilder(level, stationPos, task, retryCount + 1)
            ));
        } else {
            System.err.println("[BuilderManager] Could not find builder after 20 retries");
        }
    }
    
    /**
     * Finds the builder entity associated with a station.
     */
    private static BuilderEntity findBuilderForStation(ServerLevel level, BlockPos stationPos) {
        System.out.println("[BuilderManager] Searching for builder at station: " + stationPos);
        int builderCount = 0;
        for (Entity entity : level.getAllEntities()) {
            if (entity instanceof BuilderEntity builder) {
                builderCount++;
                BlockPos builderStation = builder.getStationPos();
                System.out.println("[BuilderManager] Found builder #" + builderCount + " with station: " + builderStation);
                if (builderStation != null && builderStation.equals(stationPos)) {
                    System.out.println("[BuilderManager] Match! Returning this builder");
                    return builder;
                }
            }
        }
        System.out.println("[BuilderManager] No matching builder found (checked " + builderCount + " builders)");
        return null;
    }
    
    /**
     * Marks the structure as built for a station.
     */
    public static void markStructureBuilt(Level level, BlockPos stationPos) {
        BlockEntity blockEntity = level.getBlockEntity(stationPos);
        if (blockEntity instanceof StationBlockEntity stationEntity) {
            stationEntity.setStructureBuilt(true);
        }
    }
}
