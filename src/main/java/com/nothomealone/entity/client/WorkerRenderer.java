package com.nothomealone.entity.client;

import com.nothomealone.NotHomeAlone;
import com.nothomealone.entity.custom.WorkerEntity;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.HumanoidMobRenderer;
import net.minecraft.resources.ResourceLocation;

public class WorkerRenderer extends HumanoidMobRenderer<WorkerEntity, HumanoidModel<WorkerEntity>> {
    private static final ResourceLocation TEXTURE = new ResourceLocation(NotHomeAlone.MOD_ID, "textures/entity/worker.png");

    public WorkerRenderer(EntityRendererProvider.Context context) {
        super(context, new HumanoidModel<>(context.bakeLayer(ModelLayers.PLAYER)), 0.5f);
    }

    @Override
    public ResourceLocation getTextureLocation(WorkerEntity entity) {
        return TEXTURE;
    }
}
