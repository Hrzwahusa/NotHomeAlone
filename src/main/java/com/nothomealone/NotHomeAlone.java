package com.nothomealone;

import com.mojang.logging.LogUtils;
import com.nothomealone.entity.ModEntities;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.common.MinecraftForge;
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

        // Register entities
        ModEntities.register(modEventBus);

        modEventBus.addListener(this::commonSetup);

        MinecraftForge.EVENT_BUS.register(this);
    }

    private void commonSetup(final FMLCommonSetupEvent event) {
        LOGGER.info("NotHomeAlone common setup complete!");
    }

    @SubscribeEvent
    public void onServerStarting(ServerStartingEvent event) {
        LOGGER.info("NotHomeAlone server is starting!");
    }

    @Mod.EventBusSubscriber(modid = MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
    public static class ClientModEvents {
        @SubscribeEvent
        public static void onClientSetup(FMLClientSetupEvent event) {
            LOGGER.info("NotHomeAlone client setup complete!");
        }
    }
}
