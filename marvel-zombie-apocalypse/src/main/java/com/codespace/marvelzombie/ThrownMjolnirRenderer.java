package com.codespace.marvelzombie;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;

public class ThrownMjolnirRenderer extends EntityRenderer<ThrownMjolnir> {
    private final ItemRenderer itemRenderer;

    public ThrownMjolnirRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.itemRenderer = context.getItemRenderer();
    }

    @Override
    public void render(ThrownMjolnir entity, float yaw, float partialTicks,
                       PoseStack poseStack, MultiBufferSource buffer, int light) {
        poseStack.pushPose();
        poseStack.mulPose(Axis.YP.rotationDegrees(entity.tickCount + partialTicks * 20.0F));
        poseStack.mulPose(Axis.ZP.rotationDegrees(135.0F));
        poseStack.scale(1.1F, 1.1F, 1.1F);

        ItemStack mjolnir = entity.getItem();
        if (mjolnir.isEmpty()) {
            mjolnir = MarvelZombieMod.MJOLNIR.get().getDefaultInstance();
        }
        itemRenderer.renderStatic(
                mjolnir,
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
    public ResourceLocation getTextureLocation(ThrownMjolnir entity) {
        return new ResourceLocation(MarvelZombieMod.MOD_ID, "textures/item/mjolnir.png");
    }
}
