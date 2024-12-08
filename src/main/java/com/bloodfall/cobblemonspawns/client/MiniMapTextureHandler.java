package com.bloodfall.cobblemonspawns.client;

import com.bloodfall.cobblemonspawns.CobblemonSpawns;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.client.texture.NativeImageBackedTexture;
import net.minecraft.util.Identifier;
import java.awt.image.BufferedImage;

public class MiniMapTextureHandler {

    private final Identifier miniMapTextureId;
    private final NativeImage nativeImage;
    private final NativeImageBackedTexture nativeImageBackedTexture;

    public MiniMapTextureHandler() {
        this.nativeImage = new NativeImage(150, 150, false);
        this.nativeImageBackedTexture = new NativeImageBackedTexture(nativeImage);
        this.miniMapTextureId = MinecraftClient.getInstance().getTextureManager().registerDynamicTexture("cobblemonspawns_mini_map", nativeImageBackedTexture);
    }

    public void updateMiniMapTexture(BufferedImage bufferedImage) {
        if (bufferedImage == null) return;

        CobblemonSpawns.LOGGER.info("Updating MiniMapTexture");

        // Ensure the image size matches the native image
        if (bufferedImage.getWidth() != nativeImage.getWidth() ||
                bufferedImage.getHeight() != nativeImage.getHeight()) {
            CobblemonSpawns.LOGGER.warn("MiniMap image size does not match DynamicTexture size.");
            return;
        }

        // Update the native image pixel by pixel
        for (int y = 0; y < bufferedImage.getHeight(); y++) {
            for (int x = 0; x < bufferedImage.getWidth(); x++) {
                int rgb = bufferedImage.getRGB(x, y);
                int alpha = (rgb >> 24) & 0xFF;
                int red = (rgb >> 16) & 0xFF;
                int green = (rgb >> 8) & 0xFF;
                int blue = rgb & 0xFF;
                int argb = (alpha << 24) | (red << 16) | (green << 8) | blue;
                nativeImage.setColor(x, y, argb);
            }
        }

        MinecraftClient.getInstance().execute(nativeImageBackedTexture::upload);
    }

    public Identifier getMiniMapTextureId() {
        return miniMapTextureId;
    }
}
