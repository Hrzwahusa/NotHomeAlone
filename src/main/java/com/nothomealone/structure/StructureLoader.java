package com.nothomealone.structure;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;

import java.util.Optional;

/**
 * Handles loading and placing structure templates (NBT files).
 */
public class StructureLoader {
    
    /**
     * Loads a structure template from resources.
     * @param level The server level
     * @param structurePath Path like "nothomealone:builder/builder_level_1"
     * @return Optional containing the template, or empty if not found
     */
    public static Optional<StructureTemplate> loadStructure(ServerLevel level, ResourceLocation structurePath) {
        return level.getStructureManager().get(structurePath);
    }
    
    /**
     * Places a structure in the world at the given position.
     * @param level The server level
     * @param structurePath Path to the structure
     * @param pos Position to place the structure (relative to structure's 0,0,0)
     * @param rotation Rotation to apply
     * @param mirror Mirror to apply
     * @return true if successfully placed
     */
    public static boolean placeStructure(ServerLevel level, ResourceLocation structurePath, 
                                        BlockPos pos, Rotation rotation, Mirror mirror) {
        Optional<StructureTemplate> templateOpt = loadStructure(level, structurePath);
        
        if (templateOpt.isEmpty()) {
            return false;
        }
        
        StructureTemplate template = templateOpt.get();
        StructurePlaceSettings settings = new StructurePlaceSettings()
                .setRotation(rotation)
                .setMirror(mirror)
                .setIgnoreEntities(true);
        
        template.placeInWorld(level, pos, pos, settings, level.random, 2);
        return true;
    }
    
    /**
     * Gets the structure template without placing it.
     * Useful for analyzing the structure before building.
     */
    public static Optional<StructureTemplate> getTemplate(ServerLevel level, ResourceLocation structurePath) {
        return level.getStructureManager().get(structurePath);
    }
}
