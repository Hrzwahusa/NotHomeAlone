package com.nothomealone.block.custom;

import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;

public class FisherStationBlock extends BaseStationBlock {
    public FisherStationBlock() {
        super(BlockBehaviour.Properties.of()
                .mapColor(MapColor.WOOD)
                .strength(2.5f)
                .sound(SoundType.WOOD)
                .noOcclusion());
    }

    @Override
    public String getWorkerType() {
        return "fisher";
    }

    @Override
    public int getWorkRadius() {
        return 32; // Moderate area around water sources
    }
}
