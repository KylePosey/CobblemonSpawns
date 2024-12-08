package com.bloodfall.cobblemonspawns.client;

import net.minecraft.client.texture.DynamicTexture;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.util.Identifier;

import java.io.IOException;
import java.nio.file.Path;

public class CustomDynamicTexture implements DynamicTexture {

   public CustomDynamicTexture(NativeImage image) {
       super();
   }

    @Override
    public void save(Identifier id, Path path) throws IOException {
    }
}
