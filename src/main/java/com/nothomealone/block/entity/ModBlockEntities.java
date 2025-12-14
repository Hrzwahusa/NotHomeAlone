package com.nothomealone.block.entity;

import com.nothomealone.NotHomeAlone;
import com.nothomealone.block.ModBlocks;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class ModBlockEntities {
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES =
            DeferredRegister.create(ForgeRegistries.BLOCK_ENTITY_TYPES, NotHomeAlone.MOD_ID);

    @SuppressWarnings("null")
    public static final RegistryObject<BlockEntityType<StationBlockEntity>> STATION_BLOCK_ENTITY =
            BLOCK_ENTITIES.register("station_block_entity", () ->
                    BlockEntityType.Builder.of(StationBlockEntity::new,
                            ModBlocks.BUILDER_STATION.get(),
                            ModBlocks.LUMBERJACK_STATION.get(),
                            ModBlocks.HUNTER_STATION.get(),
                            ModBlocks.FARMER_STATION.get(),
                            ModBlocks.MINER_STATION.get(),
                            ModBlocks.FISHER_STATION.get(),
                            ModBlocks.STORAGE_STATION.get(),
                            ModBlocks.BLACKSMITH_STATION.get()
                    ).build(null));

    public static void register(IEventBus eventBus) {
        BLOCK_ENTITIES.register(eventBus);
    }
}
