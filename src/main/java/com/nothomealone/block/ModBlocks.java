package com.nothomealone.block;

import com.nothomealone.NotHomeAlone;
import com.nothomealone.block.custom.*;
import com.nothomealone.item.ModItems;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

import java.util.function.Supplier;

public class ModBlocks {
    public static final DeferredRegister<Block> BLOCKS =
            DeferredRegister.create(ForgeRegistries.BLOCKS, NotHomeAlone.MOD_ID);

    // Station Blocks
    public static final RegistryObject<Block> BUILDER_STATION = registerBlock("builder_station",
            BuilderStationBlock::new);
    
    public static final RegistryObject<Block> LUMBERJACK_STATION = registerBlock("lumberjack_station",
            LumberjackStationBlock::new);
    
    public static final RegistryObject<Block> HUNTER_STATION = registerBlock("hunter_station",
            HunterStationBlock::new);
    
    public static final RegistryObject<Block> FARMER_STATION = registerBlock("farmer_station",
            FarmerStationBlock::new);
    
    public static final RegistryObject<Block> MINER_STATION = registerBlock("miner_station",
            MinerStationBlock::new);
    
    public static final RegistryObject<Block> FISHER_STATION = registerBlock("fisher_station",
            FisherStationBlock::new);
    
    public static final RegistryObject<Block> STORAGE_STATION = registerBlock("storage_station",
            StorageStationBlock::new);
    
    public static final RegistryObject<Block> BLACKSMITH_STATION = registerBlock("blacksmith_station",
            BlacksmithStationBlock::new);

    private static <T extends Block> RegistryObject<T> registerBlock(String name, Supplier<T> block) {
        RegistryObject<T> toReturn = BLOCKS.register(name, block);
        registerBlockItem(name, toReturn);
        return toReturn;
    }

    private static <T extends Block> void registerBlockItem(String name, RegistryObject<T> block) {
        ModItems.ITEMS.register(name, () -> new BlockItem(block.get(), new Item.Properties()));
    }

    public static void register(IEventBus eventBus) {
        BLOCKS.register(eventBus);
    }
}
