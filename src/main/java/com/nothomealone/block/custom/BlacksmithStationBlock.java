package com.nothomealone.block.custom;

import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;

public class BlacksmithStationBlock extends BaseStationBlock {
    public BlacksmithStationBlock() {
        super(BlockBehaviour.Properties.of()
                .mapColor(MapColor.METAL)
                .strength(3.5f)
                .sound(SoundType.METAL)
                .noOcclusion());
    }

    @Override
    public String getWorkerType() {
        return "blacksmith";
    }

    @Override
    public int getWorkRadius() {
        return 6; // Stays exclusively within claim area
    }
}
