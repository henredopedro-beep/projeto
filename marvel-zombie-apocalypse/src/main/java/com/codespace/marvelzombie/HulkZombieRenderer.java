package com.codespace.marvelzombie;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.ZombieRenderer;

public class HulkZombieRenderer extends ZombieRenderer {
    public HulkZombieRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    protected void scale(net.minecraft.world.entity.monster.Zombie entity,
                         PoseStack poseStack, float partialTick) {
        super.scale(entity, poseStack, partialTick);
        if (entity instanceof HulkZombie hulk && hulk.isHulk()) {
            poseStack.scale(1.75F, 1.8F, 1.75F);
        }
    }
}
