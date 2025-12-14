package com.nothomealone.block.custom;

import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;

public class BuilderStationBlock extends BaseStationBlock {
    public BuilderStationBlock() {
        super(BlockBehaviour.Properties.of()
                .mapColor(MapColor.WOOD)
                .strength(2.5f)
                .sound(SoundType.WOOD)
                .noOcclusion());
    }

    @Override
    public String getWorkerType() {
        return "builder";
    }

    @Override
    public int getWorkRadius() {
        return 128; // Must be able to reach all other stations in settlement
    }
}
