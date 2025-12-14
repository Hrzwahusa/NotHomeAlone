package com.nothomealone.block.custom;

import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;

public class FarmerStationBlock extends BaseStationBlock {
    public FarmerStationBlock() {
        super(BlockBehaviour.Properties.of()
                .mapColor(MapColor.WOOD)
                .strength(2.5f)
                .sound(SoundType.WOOD)
                .noOcclusion());
    }

    @Override
    public String getWorkerType() {
        return "farmer";
    }

    @Override
    public int getTerritoryRadius() {
        return 12; // 24x24 farm area
    }

    @Override
    public int getWorkRadius() {
        return 12; // Works only within farm territory
    }
}
