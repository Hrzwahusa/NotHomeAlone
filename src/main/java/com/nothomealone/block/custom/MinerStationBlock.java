package com.nothomealone.block.custom;

import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;

public class MinerStationBlock extends BaseStationBlock {
    public MinerStationBlock() {
        super(BlockBehaviour.Properties.of()
                .mapColor(MapColor.STONE)
                .strength(3.0f)
                .sound(SoundType.STONE)
                .noOcclusion());
    }

    @Override
    public String getWorkerType() {
        return "miner";
    }

    @Override
    public int getWorkRadius() {
        return 6; // Starts mining under claim, expands downward/outward as needed
    }
}
