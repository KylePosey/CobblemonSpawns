package com.bloodfall.cobblemonspawns.client;

import net.minecraft.util.math.BlockPos;

public class FloatingText {
    private final String text;
    private final BlockPos position;

    public FloatingText(String text, BlockPos position) {
        this.text = text;
        this.position = position;
    }

    public String getText() {
        return text;
    }

    public BlockPos getPosition() {
        return position;
    }
}
