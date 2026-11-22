package com.codespace.marvelzombie;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;

public class ThrownCaptainShieldRenderer extends EntityRenderer<ThrownCaptainShield> {
    private final ItemRenderer itemRenderer;

    public ThrownCaptainShieldRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.itemRenderer = context.getItemRenderer();
    }

    @Override
    public void render(ThrownCaptainShield entity, float yaw, float partialTicks,
                       PoseStack poseStack, MultiBufferSource buffer, int light) {
        poseStack.pushPose();
        poseStack.mulPose(Axis.YP.rotationDegrees(entity.tickCount + partialTicks * 20.0F));
        poseStack.mulPose(Axis.ZP.rotationDegrees(90.0F));
        poseStack.scale(0.9F, 0.9F, 0.9F);
        ItemStack shield = entity.getItem();
        if (shield.isEmpty()) {
            shield = MarvelZombieMod.CAPTAIN_SHIELD.get().getDefaultInstance();
        }
        itemRenderer.renderStatic(
                shield,
                ItemDisplayContext.GROUND,
                light,
                0,
                poseStack,
                buffer,
                entity.level(),
                0);
        poseStack.popPose();
        super.render(entity, yaw, partialTicks, poseStack, buffer, light);
    }

    @Override
    public ResourceLocation getTextureLocation(ThrownCaptainShield entity) {
        return new ResourceLocation("minecraft", "textures/item/shield.png");
    }
}
