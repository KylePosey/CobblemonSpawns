package com.bloodfall.cobblemonspawns.client;

import com.bloodfall.cobblemonspawns.CobblemonSpawns;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.MapColor;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.color.world.BiomeColors;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.decoration.Brightness;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.Heightmap;
import net.minecraft.world.chunk.Chunk;

import java.awt.image.BufferedImage;

public class MiniMapGenerator {
    public BufferedImage generateMiniMapImage() {
        MinecraftClient client = MinecraftClient.getInstance();
        ClientWorld world = client.world;
        if (world == null) return null;

        PlayerEntity player = client.player;
        if (player == null) return null;

        BlockPos playerPos = player.getBlockPos();
        int size = 150;
        int halfSize = size / 2;
        BlockPos startPos = new BlockPos(playerPos.getX() - halfSize, playerPos.getY(), playerPos.getZ() - halfSize);
        //BlockPos startPos = new BlockPos(playerPos.getX() - size, playerPos.getY(), playerPos.getZ() - size);

        BufferedImage miniMap = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);

        float slopeIntensity = 0.075f;
        float dirLightIntensity = 0.075f;

        for (int x = 0; x < size; x++) {
            for (int z = 0; z < size; z++) {
                BlockPos currentPos = startPos.add(x, 0, z);
                int topY = world.getTopY(Heightmap.Type.MOTION_BLOCKING , currentPos.getX(), currentPos.getZ());
                BlockPos b = new BlockPos(currentPos.getX(), topY - 1, currentPos.getZ());
                BlockState state = world.getBlockState(b);
                Block block = state.getBlock();

                int color;
                if (block == Blocks.GRASS_BLOCK) {
                    color = (world.getColor(b, BiomeColors.GRASS_COLOR) | 0xFF000000);
                } else if (block == Blocks.OAK_LEAVES || block == Blocks.ACACIA_LEAVES || block == Blocks.DARK_OAK_LEAVES || block == Blocks.BIRCH_LEAVES || block == Blocks.JUNGLE_LEAVES || block == Blocks.SPRUCE_LEAVES || block == Blocks.CHERRY_LEAVES) {
                    color = (world.getColor(b, BiomeColors.FOLIAGE_COLOR) | 0xFF000000);
                } else if (block == Blocks.WATER) {
                    color = invertColor(world.getColor(b, BiomeColors.WATER_COLOR) | 0xFF000000);
                }else if (block == Blocks.LAVA)
                {
                   color = invertColor((state.getBlock().getDefaultMapColor().color) | 0xFF000000);
                }else if (block == Blocks.ICE || block == Blocks.PACKED_ICE || block == Blocks.FROSTED_ICE || block == Blocks.BLUE_ICE)
                {
                    color = -11;
                }
                else if (block == Blocks.SNOW || block == Blocks.SNOW_BLOCK || block == Blocks.POWDER_SNOW)
                {
                    color = -17;
                }else {
                    //color = invertColor(state.getBlock().getDefaultMapColor().color);
                    color = invertColor(state.getBlock().getDefaultMapColor().color);
                }

                //color = state.getBlock().getDefaultMapColor().color + (state.getLuminance());

                int sumHeights = 0;
                int samples = 0;

                for (int nx = -1; nx <= 1; nx++) {
                    for (int nz = -1; nz <= 1; nz++) {
                        if (nx == 0 && nz == 0) continue;
                        int sampleX = x + nx;
                        int sampleZ = z + nz;
                        if (sampleX >= 0 && sampleX < size && sampleZ >= 0 && sampleZ < size) {
                            BlockPos neighborPos = findTopBlock(startPos.add(sampleX, 0, sampleZ), world);
                            sumHeights += neighborPos.getY();
                            samples++;
                        }
                    }
                }

                //float avgNeighborHeight = (float)sumHeights / samples;
                float avgNeighborHeight = samples > 0 ? (float) sumHeights / samples : b.getY();
                float heightDiff = b.getY() - avgNeighborHeight;
                float slopeFactor = 1.0f + (heightDiff * slopeIntensity);
                //slopeFactor = Math.max(0.8f, Math.min(slopeFactor, 1.2f));
                slopeFactor = clamp(slopeFactor, 0.8f, 1.2f);

                int nwX = x - 1;
                int nwZ = z - 1;
                float dirFactor = 1.0f;

                if (nwX >= 0 && nwZ >= 0 && nwX < size && nwZ < size) {
                    BlockPos nwPos = findTopBlock(startPos.add(nwX, 0, nwZ), world);
                    int nwHeight = nwPos.getY();
                    int currentHeight = b.getY();
                    int heightDifference = currentHeight - nwHeight;

                    // If current is higher than NW, brighten; if lower, darken
                    dirFactor = 1.0f + (heightDifference * dirLightIntensity);
                    // Slight clamp to avoid extremes
                    dirFactor = clamp(dirFactor, 0.8f, 1.2f);
                }

                float finalFactor = slopeFactor * dirFactor;

                int r = (color >> 16) & 0xFF;
                int g = (color >> 8) & 0xFF;
                int bl = color & 0xFF;

                r = clampColor((int)(r * finalFactor));
                g = clampColor((int)(g * finalFactor));
                bl = clampColor((int)(bl * finalFactor));

                color = (0xFF << 24) | (r << 16) | (g << 8) | bl;
                miniMap.setRGB(x, z, color);
            }
        }

        return miniMap;
    }

    public int invertColor(int argb) {
        int alpha = (argb >> 24) & 0xFF;
        int red = 255 - ((argb >> 16) & 0xFF);
        int green = 255 - ((argb >> 8) & 0xFF);
        int blue = 255 - (argb & 0xFF);
        return (alpha << 24) | (red << 16) | (green << 8) | blue;
    }

    private static float clamp(float value, float min, float max) {
        return value < min ? min : (Math.min(value, max));
    }

    private static int clampColor(int c) {
        return c < 0 ? 0 : (Math.min(c, 255));
    }

    private BlockPos findTopBlock(BlockPos pos, ClientWorld world) {
        int maxY = world.getTopY();
        for (int y = maxY - 1; y >= world.getBottomY(); y--) {
            BlockPos currentPos = new BlockPos(pos.getX(), y, pos.getZ());
            if (!world.getBlockState(currentPos).isAir()) {
                return currentPos;
            }
        }
        return pos; // Default to the original position if no block found
    }
}
