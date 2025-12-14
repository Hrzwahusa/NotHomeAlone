package com.nothomealone;

import com.mojang.logging.LogUtils;
import com.nothomealone.block.ModBlocks;
import com.nothomealone.block.entity.ModBlockEntities;
import com.nothomealone.entity.ModEntities;
import com.nothomealone.entity.custom.*;
import com.nothomealone.entity.client.WorkerRenderer;
import com.nothomealone.item.ModItems;
import net.minecraft.client.renderer.entity.EntityRenderers;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.EntityAttributeCreationEvent;
import net.minecraftforge.event.server.ServerStartingEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.slf4j.Logger;

@Mod(NotHomeAlone.MOD_ID)
public class NotHomeAlone {
    public static final String MOD_ID = "nothomealone";
    private static final Logger LOGGER = LogUtils.getLogger();

    public NotHomeAlone() {
        IEventBus modEventBus = FMLJavaModLoadingContext.get().getModEventBus();

        // Register items, blocks, entities, and block entities
        ModItems.register(modEventBus);
        ModBlocks.register(modEventBus);
        ModEntities.register(modEventBus);
        ModBlockEntities.register(modEventBus);
        com.nothomealone.item.ModCreativeTabs.register(modEventBus);

        modEventBus.addListener(this::commonSetup);
        modEventBus.addListener(this::entityAttributeCreation);

        MinecraftForge.EVENT_BUS.register(this);
    }

    private void commonSetup(final FMLCommonSetupEvent event) {
        LOGGER.info("NotHomeAlone common setup complete!");
    }

    private void entityAttributeCreation(EntityAttributeCreationEvent event) {
        event.put(ModEntities.BUILDER.get(), BuilderEntity.createAttributes().build());
        event.put(ModEntities.LUMBERJACK.get(), LumberjackEntity.createAttributes().build());
        event.put(ModEntities.HUNTER.get(), HunterEntity.createAttributes().build());
        event.put(ModEntities.FARMER.get(), FarmerEntity.createAttributes().build());
        event.put(ModEntities.MINER.get(), MinerEntity.createAttributes().build());
        event.put(ModEntities.FISHER.get(), FisherEntity.createAttributes().build());
        event.put(ModEntities.STORAGE.get(), StorageEntity.createAttributes().build());
        event.put(ModEntities.BLACKSMITH.get(), BlacksmithEntity.createAttributes().build());
    }

    @SubscribeEvent
    public void onServerStarting(ServerStartingEvent event) {
        LOGGER.info("NotHomeAlone server is starting!");
    }

    @Mod.EventBusSubscriber(modid = MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
    public static class ClientModEvents {
        @SubscribeEvent
        public static void onClientSetup(FMLClientSetupEvent event) {
            event.enqueueWork(() -> {
                // Register entity renderers - use WorkerRenderer for all workers
                EntityRenderers.register(ModEntities.BUILDER.get(), WorkerRenderer::new);
                EntityRenderers.register(ModEntities.LUMBERJACK.get(), WorkerRenderer::new);
                EntityRenderers.register(ModEntities.HUNTER.get(), WorkerRenderer::new);
                EntityRenderers.register(ModEntities.FARMER.get(), WorkerRenderer::new);
                EntityRenderers.register(ModEntities.MINER.get(), WorkerRenderer::new);
                EntityRenderers.register(ModEntities.FISHER.get(), WorkerRenderer::new);
                EntityRenderers.register(ModEntities.STORAGE.get(), WorkerRenderer::new);
                EntityRenderers.register(ModEntities.BLACKSMITH.get(), WorkerRenderer::new);
            });
            LOGGER.info("NotHomeAlone client setup complete!");
        }
    }
}
