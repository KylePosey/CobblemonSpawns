package com.bloodfall.cobblemonspawns.client;

// In BoundingBoxRenderer.java
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.client.MinecraftClient;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.util.math.Vec3d;
import net.minecraft.client.render.Camera;

import java.util.ArrayList;
import java.util.List;

public class BoundingBoxRenderer {
    private static final List<Box> boundingBoxes = new ArrayList<>();

    public static void addBoundingBox(BlockPos minPos, BlockPos maxPos) {
        boundingBoxes.add(new Box(minPos, maxPos));
    }

    public static void clearBoundingBoxes() {
        boundingBoxes.clear();
    }

    public static void render(WorldRenderContext context) {
        MatrixStack matrices = context.matrixStack();
        Camera camera = context.camera();
        Vec3d camPos = camera.getPos();

        matrices.push();
        matrices.translate(-camPos.x, -camPos.y, -camPos.z);

        VertexConsumerProvider.Immediate vertexConsumers = MinecraftClient.getInstance().getBufferBuilders().getEntityVertexConsumers();
        VertexConsumer vertexConsumer = vertexConsumers.getBuffer(RenderLayer.getLines());

        for (Box box : boundingBoxes) {
            drawBoxLines(matrices, vertexConsumer, box, 1.0F, 1.0F, 1.0F, 1.0F);
        }

        matrices.pop();
        vertexConsumers.draw();
    }

    private static void drawBoxLines(MatrixStack matrices, VertexConsumer vertexConsumer, Box box, float red, float green, float blue, float alpha) {
        Vec3d[] corners = new Vec3d[]{
                new Vec3d(box.minX, box.minY, box.minZ),
                new Vec3d(box.maxX, box.minY, box.minZ),
                new Vec3d(box.maxX, box.minY, box.maxZ),
                new Vec3d(box.minX, box.minY, box.maxZ),
                new Vec3d(box.minX, box.maxY, box.minZ),
                new Vec3d(box.maxX, box.maxY, box.minZ),
                new Vec3d(box.maxX, box.maxY, box.maxZ),
                new Vec3d(box.minX, box.maxY, box.maxZ)
        };

        int[][] edges = new int[][]{
                {0,1},{1,2},{2,3},{3,0}, // Bottom face edges
                {4,5},{5,6},{6,7},{7,4}, // Top face edges
                {0,4},{1,5},{2,6},{3,7}  // Side edges
        };

        for (int[] edge : edges) {
            Vec3d start = corners[edge[0]];
            Vec3d end = corners[edge[1]];
            vertexConsumer.vertex(matrices.peek().getPositionMatrix(), (float) start.x, (float) start.y, (float) start.z)
                    .color(red, green, blue, alpha).normal(0, 1, 0).next();
            vertexConsumer.vertex(matrices.peek().getPositionMatrix(), (float) end.x, (float) end.y, (float) end.z)
                    .color(red, green, blue, alpha).normal(0, 1, 0).next();
        }
    }
}
