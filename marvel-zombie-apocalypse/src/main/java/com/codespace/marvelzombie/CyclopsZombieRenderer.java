package com.codespace.marvelzombie;

import net.minecraft.client.model.ZombieModel;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.resources.ResourceLocation;

public class CyclopsZombieRenderer extends MobRenderer<CyclopsZombie, ZombieModel<CyclopsZombie>> {
    private static final ResourceLocation TEXTURE =
            new ResourceLocation("minecraft", "textures/entity/zombie/zombie.png");

    public CyclopsZombieRenderer(EntityRendererProvider.Context context) {
        super(context, new ZombieModel<>(context.bakeLayer(ModelLayers.ZOMBIE)), 0.65F);
        addLayer(new CyclopsEyeLayer(this, context.getItemRenderer()));
        addLayer(new CyclopsBeamLayer(this));
    }

    @Override
    protected void scale(CyclopsZombie entity, com.mojang.blaze3d.vertex.PoseStack poseStack,
                         float partialTickTime) {
        poseStack.scale(1.15F, 1.2F, 1.15F);
    }

    @Override
    public ResourceLocation getTextureLocation(CyclopsZombie entity) {
        return TEXTURE;
    }
}
