package com.codespace.marvelzombie;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.model.ZombieModel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.ZombieRenderer;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.level.ClipContext;
import org.joml.Matrix4f;

public class ThorBeamLayer extends RenderLayer<Zombie, ZombieModel<Zombie>> {
    public ThorBeamLayer(ZombieRenderer renderer) {
        super(renderer);
    }

    @Override
    public void render(PoseStack poseStack, MultiBufferSource buffer, int packedLight,
                       Zombie entity, float limbSwing, float limbSwingAmount,
                       float partialTick, float ageInTicks, float netHeadYaw, float headPitch) {
        if (!(entity instanceof ThorZombie thor) || !thor.isBeamFiring()) {
            return;
        }

        Vec3 start = thor.getEyePosition(partialTick);
        Vec3 end = start.add(thor.getLookAngle().scale(28.0D));
        HitResult blockHit = thor.level().clip(new ClipContext(start, end,
                ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, thor));
        double distance = blockHit.getType() == HitResult.Type.MISS
                ? 28.0D : start.distanceTo(blockHit.getLocation());
        float length = (float) Math.min(28.0D, distance);
        if (length <= 0.01F) {
            return;
        }

        poseStack.pushPose();
        getParentModel().getHead().translateAndRotate(poseStack);
        poseStack.translate(0.0D, 0.0D, -0.45D);
        VertexConsumer vertices = buffer.getBuffer(RenderType.lightning());
        Matrix4f pose = poseStack.last().pose();
        drawLightning(vertices, pose, length);
        poseStack.popPose();
    }

    private static void drawLightning(VertexConsumer vertices, Matrix4f pose, float length) {
        int segments = 18;
        float previousX = 0.0F;
        float previousY = 0.0F;
        for (int i = 1; i <= segments; i++) {
            float zStart = -length * (i - 1) / (float) segments;
            float zEnd = -length * i / (float) segments;
            float nextX = i == segments ? 0.0F : (((i * 37) % 13) - 6) * 0.018F;
            float nextY = i == segments ? 0.0F : ((((i * 53) % 11) - 5) * 0.018F);
            drawSegment(vertices, pose, previousX, previousY, zStart,
                    nextX, nextY, zEnd, 0.075F, 45, 155, 255, 150);
            drawSegment(vertices, pose, previousX, previousY, zStart,
                    nextX, nextY, zEnd, 0.028F, 230, 248, 255, 255);
            previousX = nextX;
            previousY = nextY;
        }
    }

    private static void drawSegment(VertexConsumer vertices, Matrix4f pose,
                                    float x1, float y1, float z1,
                                    float x2, float y2, float z2,
                                    float width, int red, int green, int blue, int alpha) {
        vertex(vertices, pose, x1 - width, y1, z1, red, green, blue, alpha);
        vertex(vertices, pose, x1 + width, y1, z1, red, green, blue, alpha);
        vertex(vertices, pose, x2 + width, y2, z2, red, green, blue, alpha);
        vertex(vertices, pose, x2 - width, y2, z2, red, green, blue, alpha);
        vertex(vertices, pose, x1, y1 - width, z1, red, green, blue, alpha);
        vertex(vertices, pose, x1, y1 + width, z1, red, green, blue, alpha);
        vertex(vertices, pose, x2, y2 + width, z2, red, green, blue, alpha);
        vertex(vertices, pose, x2, y2 - width, z2, red, green, blue, alpha);
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
