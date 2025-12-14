package com.nothomealone.block.custom;

import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;

public class LumberjackStationBlock extends BaseStationBlock {
    public LumberjackStationBlock() {
        super(BlockBehaviour.Properties.of()
                .mapColor(MapColor.WOOD)
                .strength(2.5f)
                .sound(SoundType.WOOD)
                .noOcclusion());
    }

    @Override
    public String getWorkerType() {
        return "lumberjack";
    }

    @Override
    public int getWorkRadius() {
        return 48; // Larger area to find and chop trees
    }
}
