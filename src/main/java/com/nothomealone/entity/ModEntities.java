package com.nothomealone.entity;

import com.nothomealone.NotHomeAlone;
import com.nothomealone.entity.custom.*;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class ModEntities {
    public static final DeferredRegister<EntityType<?>> ENTITY_TYPES =
            DeferredRegister.create(ForgeRegistries.ENTITY_TYPES, NotHomeAlone.MOD_ID);

    public static final RegistryObject<EntityType<LumberjackEntity>> LUMBERJACK =
            ENTITY_TYPES.register("lumberjack", () -> EntityType.Builder.of(LumberjackEntity::new, MobCategory.CREATURE)
                    .sized(0.6f, 1.95f)
                    .build("lumberjack"));

    public static final RegistryObject<EntityType<HunterEntity>> HUNTER =
            ENTITY_TYPES.register("hunter", () -> EntityType.Builder.of(HunterEntity::new, MobCategory.CREATURE)
                    .sized(0.6f, 1.95f)
                    .build("hunter"));

    public static final RegistryObject<EntityType<FarmerEntity>> FARMER =
            ENTITY_TYPES.register("farmer", () -> EntityType.Builder.of(FarmerEntity::new, MobCategory.CREATURE)
                    .sized(0.6f, 1.95f)
                    .build("farmer"));

    public static final RegistryObject<EntityType<BuilderEntity>> BUILDER =
            ENTITY_TYPES.register("builder", () -> EntityType.Builder.of(BuilderEntity::new, MobCategory.CREATURE)
                    .sized(0.6f, 1.95f)
                    .build("builder"));

    public static final RegistryObject<EntityType<MinerEntity>> MINER =
            ENTITY_TYPES.register("miner", () -> EntityType.Builder.of(MinerEntity::new, MobCategory.CREATURE)
                    .sized(0.6f, 1.95f)
                    .build("miner"));

    public static final RegistryObject<EntityType<FisherEntity>> FISHER =
            ENTITY_TYPES.register("fisher", () -> EntityType.Builder.of(FisherEntity::new, MobCategory.CREATURE)
                    .sized(0.6f, 1.95f)
                    .build("fisher"));

    public static void register(IEventBus eventBus) {
        ENTITY_TYPES.register(eventBus);
    }
}
