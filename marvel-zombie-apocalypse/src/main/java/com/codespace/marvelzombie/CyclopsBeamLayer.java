package com.codespace.marvelzombie;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.world.phys.Vec3;
import net.threetag.palladium.power.ability.AbilityInstance;
import net.threetag.palladium.power.ability.EnergyBeamAbility;
import org.joml.Matrix4f;

public class CyclopsBeamLayer extends RenderLayer<CyclopsZombie, net.minecraft.client.model.ZombieModel<CyclopsZombie>> {
    public CyclopsBeamLayer(MobRenderer<CyclopsZombie, net.minecraft.client.model.ZombieModel<CyclopsZombie>> renderer) {
        super(renderer);
    }

    @Override
    public void render(PoseStack poseStack, MultiBufferSource buffer, int packedLight,
                       CyclopsZombie entity, float limbSwing, float limbSwingAmount,
                       float partialTick, float ageInTicks, float netHeadYaw, float headPitch) {
        if (!entity.isBeamFiring()) {
            return;
        }

        AbilityInstance ability = CyclopsPalladium.BEAM_ABILITY.getEntry(entity);
        if (ability == null) {
            return;
        }

        EnergyBeamAbility.updateTargetPos(entity, ability, partialTick);
        Vec3 target = ability.getProperty(EnergyBeamAbility.TARGET);
        double distance = entity.getEyePosition(partialTick).distanceTo(target);
        float value = ability.getProperty(EnergyBeamAbility.VALUE);
        float length = (float) Math.min(32.0D, distance) * Math.max(0.0F, Math.min(1.0F, value));
        if (length <= 0.01F) {
            return;
        }

        poseStack.pushPose();
        getParentModel().getHead().translateAndRotate(poseStack);
        poseStack.translate(0.0D, 0.0D, -0.45D);
        VertexConsumer vertices = buffer.getBuffer(RenderType.lightning());
        Matrix4f pose = poseStack.last().pose();
        drawBeam(vertices, pose, length, 0.18F, 255, 20, 36, 80);
        drawBeam(vertices, pose, length, 0.085F, 255, 28, 44, 210);
        drawBeam(vertices, pose, length, 0.028F, 255, 242, 245, 255);
        poseStack.popPose();
    }

    private static void drawBeam(VertexConsumer vertices, Matrix4f pose, float length, float width,
                                 int red, int green, int blue, int alpha) {
        vertex(vertices, pose, -width, 0.0F, 0.0F, red, green, blue, alpha);
        vertex(vertices, pose, width, 0.0F, 0.0F, red, green, blue, alpha);
        vertex(vertices, pose, width, 0.0F, -length, red, green, blue, alpha);
        vertex(vertices, pose, -width, 0.0F, -length, red, green, blue, alpha);

        vertex(vertices, pose, 0.0F, -width, 0.0F, red, green, blue, alpha);
        vertex(vertices, pose, 0.0F, width, 0.0F, red, green, blue, alpha);
        vertex(vertices, pose, 0.0F, width, -length, red, green, blue, alpha);
        vertex(vertices, pose, 0.0F, -width, -length, red, green, blue, alpha);
    }

    private static void vertex(VertexConsumer vertices, Matrix4f pose, float x, float y, float z,
                               int red, int green, int blue, int alpha) {
        vertices.vertex(pose, x, y, z).color(red, green, blue, alpha).endVertex();
    }
}
