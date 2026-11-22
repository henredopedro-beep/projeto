package com.codespace.marvelzombie;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import org.joml.Matrix4f;

public class SlingRingPortalRenderer extends EntityRenderer<SlingRingPortal> {
    public SlingRingPortalRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public void render(SlingRingPortal portal, float yaw, float partialTick,
                       PoseStack poseStack, MultiBufferSource buffer, int packedLight) {
        float time = portal.tickCount + partialTick;
        poseStack.pushPose();
        poseStack.translate(0.0D, 1.38D, 0.0D);
        poseStack.mulPose(Axis.YP.rotationDegrees(-portal.getPortalYaw()));

        VertexConsumer vertices = buffer.getBuffer(RenderType.lightning());
        Matrix4f pose = poseStack.last().pose();

        drawPortalFace(vertices, pose, time, -1.0F);
        poseStack.pushPose();
        poseStack.scale(-1.0F, 1.0F, 1.0F);
        drawPortalFace(vertices, poseStack.last().pose(), time, 1.0F);
        poseStack.popPose();
        poseStack.popPose();
        super.render(portal, yaw, partialTick, poseStack, buffer, LightTexture.FULL_BRIGHT);
    }

    private static void drawPortalFace(VertexConsumer vertices, Matrix4f pose,
                                       float time, float side) {
        drawDome(vertices, pose, side);
        drawBand(vertices, pose, 1.07F, 1.07F, 0.075F, 0.0F, time * 0.012F,
                255, 120, 35, 205, side);
        drawBand(vertices, pose, 0.91F, 0.91F, 0.025F, -0.018F, -time * 0.018F,
                255, 219, 112, 225, side);
    }

    private static void drawDome(VertexConsumer vertices, Matrix4f pose, float side) {
        int angularSegments = 48;
        int radialSegments = 8;
        float radiusX = 1.075F;
        float radiusY = 1.075F;
        float depth = 0.12F;

        for (int radial = 0; radial < radialSegments; radial++) {
            float innerRadius = radial / (float) radialSegments;
            float outerRadius = (radial + 1) / (float) radialSegments;
            int alpha = 24 + Math.round(14.0F * (1.0F - outerRadius));
            for (int segment = 0; segment < angularSegments; segment++) {
                float firstAngle = (float) (Math.PI * 2.0D * segment / angularSegments);
                float secondAngle = (float) (Math.PI * 2.0D * (segment + 1) / angularSegments);
                quad(vertices, pose,
                        innerRadius * radiusX * (float) Math.cos(firstAngle),
                        innerRadius * radiusY * (float) Math.sin(firstAngle),
                        side * domeDepth(innerRadius, depth),
                        outerRadius * radiusX * (float) Math.cos(firstAngle),
                        outerRadius * radiusY * (float) Math.sin(firstAngle),
                        side * domeDepth(outerRadius, depth),
                        outerRadius * radiusX * (float) Math.cos(secondAngle),
                        outerRadius * radiusY * (float) Math.sin(secondAngle),
                        side * domeDepth(outerRadius, depth),
                        innerRadius * radiusX * (float) Math.cos(secondAngle),
                        innerRadius * radiusY * (float) Math.sin(secondAngle),
                        side * domeDepth(innerRadius, depth),
                        255, 130, 42, alpha);
            }
        }
    }

    private static float domeDepth(float radius, float depth) {
        return 0.02F + depth * (float) Math.sqrt(Math.max(0.0F, 1.0F - radius * radius));
    }

    private static void drawBand(VertexConsumer vertices, Matrix4f pose,
                                 float radiusX, float radiusY, float width, float z,
                                 float rotation, int red, int green, int blue, int alpha,
                                 float side) {
        int segments = 72;
        for (int i = 0; i < segments; i++) {
            float firstAngle = (float) (Math.PI * 2.0D * i / segments) + rotation;
            float secondAngle = (float) (Math.PI * 2.0D * (i + 1) / segments) + rotation;
            float wobbleA = 1.0F + 0.025F * (float) Math.sin(firstAngle * 7.0F + rotation * 4.0F);
            float wobbleB = 1.0F + 0.025F * (float) Math.sin(secondAngle * 7.0F + rotation * 4.0F);
            float innerX1 = (radiusX - width) * wobbleA * (float) Math.cos(firstAngle);
            float innerY1 = (radiusY - width) * wobbleA * (float) Math.sin(firstAngle);
            float outerX1 = (radiusX + width) * wobbleA * (float) Math.cos(firstAngle);
            float outerY1 = (radiusY + width) * wobbleA * (float) Math.sin(firstAngle);
            float innerX2 = (radiusX - width) * wobbleB * (float) Math.cos(secondAngle);
            float innerY2 = (radiusY - width) * wobbleB * (float) Math.sin(secondAngle);
            float outerX2 = (radiusX + width) * wobbleB * (float) Math.cos(secondAngle);
            float outerY2 = (radiusY + width) * wobbleB * (float) Math.sin(secondAngle);

            quad(vertices, pose,
                    innerX1, innerY1, z * side, outerX1, outerY1, z * side,
                    outerX2, outerY2, z * side, innerX2, innerY2, z * side,
                    red, green, blue, alpha);
        }
    }

    private static void quad(VertexConsumer vertices, Matrix4f pose,
                             float x1, float y1, float z1,
                             float x2, float y2, float z2,
                             float x3, float y3, float z3,
                             float x4, float y4, float z4,
                             int red, int green, int blue, int alpha) {
        vertex(vertices, pose, x1, y1, z1, red, green, blue, alpha);
        vertex(vertices, pose, x2, y2, z2, red, green, blue, alpha);
        vertex(vertices, pose, x3, y3, z3, red, green, blue, alpha);
        vertex(vertices, pose, x4, y4, z4, red, green, blue, alpha);
    }

    private static void vertex(VertexConsumer vertices, Matrix4f pose, float x, float y, float z,
                               int red, int green, int blue, int alpha) {
        vertices.vertex(pose, x, y, z).color(red, green, blue, alpha).endVertex();
    }

    @Override
    public ResourceLocation getTextureLocation(SlingRingPortal portal) {
        return new ResourceLocation("minecraft", "textures/item/clock_00.png");
    }
}
