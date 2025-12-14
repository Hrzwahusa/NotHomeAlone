package com.nothomealone.block.custom;

import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;

public class HunterStationBlock extends BaseStationBlock {
    public HunterStationBlock() {
        super(BlockBehaviour.Properties.of()
                .mapColor(MapColor.WOOD)
                .strength(2.5f)
                .sound(SoundType.WOOD)
                .noOcclusion());
    }

    @Override
    public String getWorkerType() {
        return "hunter";
    }

    @Override
    public int getWorkRadius() {
        return 96; // Very large area - animals can be far away
    }
}
