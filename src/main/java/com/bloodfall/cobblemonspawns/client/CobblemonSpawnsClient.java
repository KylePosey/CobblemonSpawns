package com.bloodfall.cobblemonspawns.client;

import com.bloodfall.cobblemonspawns.*;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;
import net.fabricmc.fabric.api.client.screenhandler.v1.ScreenRegistry;
import net.fabricmc.fabric.api.network.PacketRegistry;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.server.MinecraftServer;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;

import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class CobblemonSpawnsClient implements ClientModInitializer {

    public static final Identifier REQUEST_GUI_PACKET_PACKET_ID = new Identifier(CobblemonSpawns.MOD_ID, "request_gui_packet");

    private static KeyBinding guiKeyBinding;
    private static KeyBinding debugViewKeyBinding;

    @Override
    public void onInitializeClient()
    {
        guiKeyBinding = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.cobblemonspawns.gui",
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_I,
                "category.cobblemonspawns.areas"
        ));

        debugViewKeyBinding = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.cobblemonspawns.debugview",
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_N,
                "category.cobblemonspawns.areas"
        ));

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            while (guiKeyBinding.wasPressed()) {
                assert client.player != null;
                //client.player.sendMessage(Text.literal("Key GUI was pressed!"), false);
                PacketByteBuf buf = PacketByteBufs.create();
                ClientPlayNetworking.send(REQUEST_GUI_PACKET_PACKET_ID, buf);
            }
        });

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            while (debugViewKeyBinding.wasPressed()) {
                assert client.player != null;
                //client.player.sendMessage(Text.literal("Key Debug View was pressed!"), false);
                PacketByteBuf buf = PacketByteBufs.create();
                ClientPlayNetworking.send(CobblemonSpawns.KEYBIND_TOGGLE_DEBUG_VIEW_PACKET_ID, buf);
            }
        });

        ClientPlayNetworking.registerGlobalReceiver(CobblemonSpawns.OPEN_AREA_GUI_PACKET_ID, (client, handler, buf, responseSender) -> {
            // Deserialize areas
            List<Area> areas = deserializeAreas(buf);

            // Open the GUI on the client thread
            client.execute(() -> {
                client.setScreen(new AreaManagementScreen(areas));
            });
        });

        ClientPlayNetworking.registerGlobalReceiver(CobblemonSpawns.START_BOUNDING_BOX_PACKET_ID, (client, handler, buf, responseSender) -> {
            BlockPos minPos = buf.readBlockPos();
            BlockPos maxPos = buf.readBlockPos();
            client.execute(() -> {
                BoundingBoxRenderer.addBoundingBox(minPos, maxPos);
            });
        });

        ClientPlayNetworking.registerGlobalReceiver(CobblemonSpawns.STOP_BOUNDING_BOX_PACKET_ID, (client, handler, buf, responseSender) -> {
            client.execute(BoundingBoxRenderer::clearBoundingBoxes);
        });

        ClientPlayNetworking.registerGlobalReceiver(CobblemonSpawns.FLOATING_TEXT_PACKET_ID, (client, handler, buf, responseSender) -> {
            List<FloatingText> texts = deserializeFloatingTexts(buf);
            client.execute(() -> {
                FloatingTextRenderer.setFloatingTexts(texts);
            });
        });

        ClientPlayNetworking.registerGlobalReceiver(CobblemonSpawns.CLEAR_FLOATING_TEXT_PACKET_ID, (client, handler, buf, responseSender) -> {
            client.execute(FloatingTextRenderer::clearFloatingTexts);
        });

        WorldRenderEvents.BEFORE_DEBUG_RENDER.register(FloatingTextRenderer::render);

        WorldRenderEvents.BEFORE_DEBUG_RENDER.register(BoundingBoxRenderer::render);
    }

    private void sendToggleDebugViewPacket() {
        PacketByteBuf buf = PacketByteBufs.create();
        // You can send the current state or any required data
        ClientPlayNetworking.send(CobblemonSpawns.KEYBIND_TOGGLE_DEBUG_VIEW_PACKET_ID, buf);
    }

    private List<Area> deserializeAreas(PacketByteBuf buf) {
        int areaCount = buf.readInt();
        List<Area> areas = new ArrayList<>();
        for (int i = 0; i < areaCount; i++) {
            UUID id = buf.readUuid();
            String name = buf.readString(32767);
            BlockPos minPos = buf.readBlockPos();
            BlockPos maxPos = buf.readBlockPos();
            Area area = new Area(id, name, minPos, maxPos);
            areas.add(area);
        }
        return areas;
    }

    private List<FloatingText> deserializeFloatingTexts(PacketByteBuf buf) {
        int areaCount = buf.readInt();
        List<FloatingText> texts = new ArrayList<>();

        for (int i = 0; i < areaCount; i++) {
            String areaName = buf.readString(32767);
            BlockPos center = buf.readBlockPos();
            int spawnConfigCount = buf.readInt();

            StringBuilder sb = new StringBuilder();
            sb.append(areaName);

            for (int j = 0; j < spawnConfigCount; j++) {
                String pokemonName = buf.readString(32767);
                int minLevel = buf.readInt();
                int maxLevel = buf.readInt();
                double spawnRate = buf.readDouble();

                sb.append("\n").append(pokemonName)
                        .append(" (").append(minLevel).append("-").append(maxLevel).append(")")
                        .append(" | Rate: ").append(String.format("%.2f", spawnRate * 100)).append("%");
            }

            texts.add(new FloatingText(sb.toString(), center));
        }

        return texts;
    }
}
