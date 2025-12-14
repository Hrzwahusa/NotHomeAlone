package com.nothomealone.structure;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;

import java.util.Optional;

/**
 * Analyzes NBT structures and creates BuildTasks for the Builder NPC.
 */
public class StructureAnalyzer {
    
    /**
     * Analyzes a structure and creates a BuildTask.
     * Special handling:
     * - Ender Chests mark the station position (not built)
     * - Dirt blocks under important blocks (priority)
     * - Crafting tables (high priority)
     * - Clears obstacles before building
     * 
     * @param level Server level
     * @param structurePath Path to structure
     * @param stationPos Position of the station block (where Ender Chest is in structure)
     * @return BuildTask or null if structure not found
     */
    public static BuildTask analyzeStructure(ServerLevel level, ResourceLocation structurePath, BlockPos stationPos) {
        Optional<StructureTemplate> templateOpt = StructureLoader.getTemplate(level, structurePath);
        
        if (templateOpt.isEmpty()) {
            return null;
        }
        
        StructureTemplate template = templateOpt.get();
        BuildTask task = new BuildTask(structurePath.toString(), stationPos);
        
        // Get structure size
        net.minecraft.core.Vec3i sizeVec = template.getSize();
        BlockPos size = new BlockPos(sizeVec.getX(), sizeVec.getY(), sizeVec.getZ());
        
        // Calculate offset (structure 0,0,0 to station position)
        // Ender Chest marks station block position, structure should start 1 block BELOW
        // and centered horizontally
        BlockPos offset = stationPos.offset(-size.getX() / 2, -1, -size.getZ() / 2);
        
        System.out.println("[StructureAnalyzer] Structure size: " + size);
        System.out.println("[StructureAnalyzer] Station position: " + stationPos);
        System.out.println("[StructureAnalyzer] Offset (structure starts 1 block below station): " + offset);
        
        // Use a custom processor to intercept blocks WITHOUT placing them
        final java.util.List<StructureTemplate.StructureBlockInfo> capturedBlocks = new java.util.ArrayList<>();
        final BlockPos finalOffset = offset; // Make offset final for lambda
        
        net.minecraft.world.level.levelgen.structure.templatesystem.StructureProcessor captureProcessor = 
            new net.minecraft.world.level.levelgen.structure.templatesystem.StructureProcessor() {
                @Override
                public StructureTemplate.StructureBlockInfo processBlock(
                    net.minecraft.world.level.LevelReader levelReader,
                    BlockPos jigsawPos,
                    BlockPos structurePos,
                    StructureTemplate.StructureBlockInfo localInfo,
                    StructureTemplate.StructureBlockInfo worldInfo,
                    net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings settings
                ) {
                    // Capture the block info with processor's world position (already correct!)
                    if (!worldInfo.state().isAir()) {
                        BlockState state = worldInfo.state();
                        
                        // Replace grass blocks with dirt blocks for easier building
                        if (state.is(Blocks.GRASS_BLOCK)) {
                            state = Blocks.DIRT.defaultBlockState();
                        }
                        
                        capturedBlocks.add(new StructureTemplate.StructureBlockInfo(
                            worldInfo.pos(),  // Use processor's position directly
                            state,
                            worldInfo.nbt()
                        ));
                    }
                    // Return null to prevent actual placement
                    return null;
                }
                
                @Override
                protected net.minecraft.world.level.levelgen.structure.templatesystem.StructureProcessorType<?> getType() {
                    return net.minecraft.world.level.levelgen.structure.templatesystem.StructureProcessorType.BLOCK_IGNORE;
                }
            };
        
        net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings settings = 
            new net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings();
        settings.setIgnoreEntities(true);
        settings.addProcessor(captureProcessor);
        
        // Process structure without placing it
        template.placeInWorld(
            level,
            offset,
            offset,
            settings,
            level.getRandom(),
            2  // flags
        );
        
        java.util.List<StructureTemplate.StructureBlockInfo> blocks = capturedBlocks;
        
        System.out.println("[StructureAnalyzer] Total blocks in structure: " + blocks.size());
        
        int airCount = 0;
        int enderChestCount = 0;
        int validBlockCount = 0;
        
        // Iterate through all blocks in structure
        for (StructureTemplate.StructureBlockInfo blockInfo : blocks) {
            BlockPos worldPos = blockInfo.pos();  // Already world position from processor
            BlockState state = blockInfo.state();
            
            // Skip Ender Chests (they mark the station position)
            if (state.is(Blocks.ENDER_CHEST)) {
                enderChestCount++;
                continue;
            }
            
            // Skip air blocks
            if (state.isAir()) {
                airCount++;
                continue;
            }
            
            validBlockCount++;
            if (validBlockCount <= 5) {
                System.out.println("[StructureAnalyzer] Found block: " + state.getBlock().getName().getString() + " at " + worldPos);
            }
            
            // Determine priority
            int priority = 0;
            
            // Crafting table = highest priority (need it to craft)
            if (state.is(Blocks.CRAFTING_TABLE)) {
                priority = 100;
                
                // Also add dirt block below crafting table with high priority
                BlockPos below = worldPos.below();
                task.addStep(below, Blocks.DIRT.defaultBlockState(), 99);
            }
            // Dirt foundation = medium priority
            else if (state.is(Blocks.DIRT)) {
                priority = 10;
            }
            // Walls, fences, etc = normal priority
            else {
                priority = 5;
            }
            
            task.addStep(worldPos, state, priority);
        }
        
        System.out.println("[StructureAnalyzer] Stats: " + validBlockCount + " valid blocks, " + airCount + " air blocks, " + enderChestCount + " ender chests");
        
        // Sort steps by priority
        task.sortSteps();
        
        return task;
    }
    
    /**
     * Checks if a block should be cleared before building.
     */
    public static boolean shouldClearBlock(BlockState state) {
        // Don't clear air, liquids, or already-correct blocks
        if (state.isAir()) return false;
        
        // Clear grass, flowers, saplings, etc
        return !state.getFluidState().isEmpty() || 
               state.is(Blocks.GRASS) || 
               state.is(Blocks.TALL_GRASS) ||
               state.is(Blocks.FERN) ||
               state.is(Blocks.DEAD_BUSH);
    }
}
