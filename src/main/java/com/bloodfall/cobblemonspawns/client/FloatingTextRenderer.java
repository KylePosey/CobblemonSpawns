package com.bloodfall.cobblemonspawns.client;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.BlockPos;
import net.minecraft.client.render.Camera;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;
import org.joml.Quaternionf;

import java.util.ArrayList;
import java.util.List;

public class FloatingTextRenderer {
    private static final List<FloatingText> floatingTexts = new ArrayList<>();

    public static void setFloatingTexts(List<FloatingText> texts) {
        floatingTexts.clear();
        floatingTexts.addAll(texts);
    }

    public static void clearFloatingTexts() {
        floatingTexts.clear();
    }

    public static void render(WorldRenderContext context) {
        if (floatingTexts.isEmpty()) return;

        MatrixStack matrices = context.matrixStack();
        Camera camera = context.camera();
        double camX = camera.getPos().x;
        double camY = camera.getPos().y;
        double camZ = camera.getPos().z;

        matrices.push();
        matrices.translate(-camX, -camY, -camZ);

        for (FloatingText text : floatingTexts) {
            BlockPos pos = text.getPosition();
            double x = pos.getX();
            double y = pos.getY();
            double z = pos.getZ();

            double distanceSquared = (x - camera.getPos().x) * (x - camera.getPos().x)
                                    + (y - camera.getPos().y) * (y - camera.getPos().y)
                                    + (z - camera.getPos().z) * (z - camera.getPos().z);
            double MAX_RENDER_DISTANCE_SQUARED = 100 * 100;

            if (distanceSquared > MAX_RENDER_DISTANCE_SQUARED) {
                continue;
            }

            double dx = camX - x;
            double dy = camY - y;
            double dz = camZ - z;

            double distance = Math.sqrt(dx * dx + dy * dy + dz * dz);

            if (distance == 0) distance = 0.0001;

            float yaw = (float) Math.toDegrees(Math.atan2(dz, dx)) - 90.0F;
            float pitch = (float) Math.toDegrees(Math.asin(dy / distance));

            matrices.push();
            matrices.translate(x, y, z);

            //float yaw = camera.getYaw();
            //float pitch = camera.getPitch();

            Quaternionf yawRotation = new Quaternionf().rotateY((float) Math.toRadians(-yaw));
            Quaternionf  pitchRotation = new Quaternionf().rotateX((float) Math.toRadians(-pitch));

            matrices.multiply(yawRotation);
            matrices.multiply(pitchRotation);

            matrices.scale(0.02F, -0.02F, 0.02F);

            TextRenderer textRenderer = MinecraftClient.getInstance().textRenderer;

            String[] lines = text.getText().split("\n");
            int lineHeight = textRenderer.fontHeight;

            boolean setShadow = false;

            // Iterate through each line and render it
            for (int i = 0; i < lines.length; i++) {
                String line = lines[i];


                setShadow = i == 0;

                // Calculate text width to center it
                float textWidth = textRenderer.getWidth(line);

                // Y offset for each line to stack them vertically
                float yOffset = i * lineHeight;

                // Draw the text centered horizontally and stacked vertically
                textRenderer.draw(
                        line,
                        -textWidth / 2.0f, -lineHeight / 2.0f + yOffset, 0xFFFFFF,
                        setShadow,
                        matrices.peek().getPositionMatrix(),
                        MinecraftClient.getInstance().getBufferBuilders().getEntityVertexConsumers(),
                        TextRenderer.TextLayerType.NORMAL,
                        0,
                        15728880
                );
            }

            matrices.pop();
        }

        matrices.pop();
    }
}
