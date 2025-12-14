package com.nothomealone.item;

import com.nothomealone.NotHomeAlone;
import com.nothomealone.block.ModBlocks;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;

public class ModCreativeTabs {
    public static final DeferredRegister<CreativeModeTab> CREATIVE_MODE_TABS =
            DeferredRegister.create(Registries.CREATIVE_MODE_TAB, NotHomeAlone.MOD_ID);

    public static final RegistryObject<CreativeModeTab> NOT_HOME_ALONE_TAB = CREATIVE_MODE_TABS.register("nothomealone_tab",
            () -> CreativeModeTab.builder()
                    .icon(() -> new ItemStack(ModBlocks.BUILDER_STATION.get()))
                    .title(Component.translatable("creativetab.nothomealone_tab"))
                    .displayItems((parameters, output) -> {
                        // Add all station blocks
                        output.accept(ModBlocks.BUILDER_STATION.get());
                        output.accept(ModBlocks.LUMBERJACK_STATION.get());
                        output.accept(ModBlocks.HUNTER_STATION.get());
                        output.accept(ModBlocks.FARMER_STATION.get());
                        output.accept(ModBlocks.MINER_STATION.get());
                        output.accept(ModBlocks.FISHER_STATION.get());
                        output.accept(ModBlocks.STORAGE_STATION.get());
                        output.accept(ModBlocks.BLACKSMITH_STATION.get());
                    })
                    .build());

    public static void register(IEventBus eventBus) {
        CREATIVE_MODE_TABS.register(eventBus);
    }
}
