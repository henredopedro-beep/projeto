package com.codespace.marvelzombie;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

public class CyclopsEyeLayer extends RenderLayer<CyclopsZombie, net.minecraft.client.model.ZombieModel<CyclopsZombie>> {
    private final ItemRenderer itemRenderer;

    public CyclopsEyeLayer(MobRenderer<CyclopsZombie, net.minecraft.client.model.ZombieModel<CyclopsZombie>> renderer,
                           ItemRenderer itemRenderer) {
        super(renderer);
        this.itemRenderer = itemRenderer;
    }

    @Override
    public void render(PoseStack poseStack, MultiBufferSource buffer, int packedLight,
                       CyclopsZombie entity, float limbSwing, float limbSwingAmount,
                       float partialTick, float ageInTicks, float netHeadYaw, float headPitch) {
        poseStack.pushPose();
        getParentModel().getHead().translateAndRotate(poseStack);
        poseStack.translate(0.0D, 0.0D, -0.43D);
        poseStack.scale(1.35F, 1.35F, 1.35F);
        itemRenderer.renderStatic(new ItemStack(Items.ENDER_EYE), ItemDisplayContext.HEAD,
                packedLight, OverlayTexture.NO_OVERLAY, poseStack, buffer, entity.level(), entity.getId());
        poseStack.popPose();
    }
}
