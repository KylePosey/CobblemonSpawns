package com.bloodfall.cobblemonspawns.client;

import com.bloodfall.cobblemonspawns.Area;
import com.bloodfall.cobblemonspawns.AreaCommands;
import com.bloodfall.cobblemonspawns.CobblemonSpawns;
import io.wispforest.owo.ui.base.BaseOwoScreen;
import io.wispforest.owo.ui.component.*;
import io.wispforest.owo.ui.container.Containers;
import io.wispforest.owo.ui.container.FlowLayout;
import io.wispforest.owo.ui.core.*;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class AreaManagementScreen extends BaseOwoScreen<FlowLayout> {

    private final List<Area> areas;
    private Area selectedArea;

    // Components for displaying and editing area details
    private Component nameComponent;
    private Component minPosComponent;
    private Component maxPosComponent;

    private FlowLayout areaListLayout;
    private FlowLayout detailsLayout;

    private boolean isEditing = false;

    public AreaManagementScreen(List<Area> areas) {
        super(Text.literal("Area Management"));
        this.areas = areas;
    }

    @Override
    protected @NotNull OwoUIAdapter<FlowLayout> createAdapter() {
        return OwoUIAdapter.create(this, Containers::verticalFlow);
    }

    @Override
    protected void build(FlowLayout rootComponent) {
        rootComponent
                .surface(Surface.VANILLA_TRANSLUCENT)
                .padding(Insets.of(10))
                .horizontalAlignment(HorizontalAlignment.CENTER)
                .verticalAlignment(VerticalAlignment.TOP);

        areaListLayout = Containers.verticalFlow(Sizing.fill(100), Sizing.content());
        areaListLayout.padding(Insets.of(5));

        updateAreaList();

        detailsLayout = Containers.verticalFlow(Sizing.fill(20), Sizing.content());
        detailsLayout.gap(5);

        // Initially, no area is selected
        refreshDetailsLayout();

        // Add components to the root layout
        rootComponent.child(areaListLayout);
        rootComponent.child(Components.box(Sizing.fill(100), Sizing.fixed(2))); // Divider
        rootComponent.child(detailsLayout);
        Containers.verticalScroll(Sizing.content(), Sizing.fill(20), areaListLayout);
    }

    private void updateAreaList() {
        areaListLayout.clearChildren();

        for (Area area : areas) {
            ButtonComponent areaButton = (ButtonComponent) Components.button(Text.literal(area.getName()), button -> {
                selectArea(area);
            }).sizing(Sizing.fill(50));
            areaButton.margins(Insets.of(2)).horizontalSizing(Sizing.fixed(120)).verticalSizing(Sizing.fixed(20));;

            areaListLayout.child(areaButton);
        }
    }

    private void selectArea(Area area) {
        this.selectedArea = area;
        this.isEditing = false;

        refreshDetailsLayout();
    }

    private void refreshDetailsLayout() {
        detailsLayout.clearChildren();

        if (selectedArea == null) {
            // No area selected
            return;
        }

        if (isEditing) {
            // Editing mode: replace labels with text boxes

            // Name field
            nameComponent = Components.textBox(Sizing.fixed(120), selectedArea.getName());

            // Min Pos field
            minPosComponent = Components.textBox(Sizing.fixed(120), formatBlockPos(selectedArea.getMinPos()));

            // Max Pos field
            maxPosComponent = Components.textBox(Sizing.fixed(120), formatBlockPos(selectedArea.getMaxPos()));

            detailsLayout.child(Components.label(Text.literal("Name:")));
            detailsLayout.child(nameComponent);
            detailsLayout.child(Components.label(Text.literal("Min Pos (x, y, z):")));
            detailsLayout.child(minPosComponent);
            detailsLayout.child(Components.label(Text.literal("Max Pos (x, y, z):")));
            detailsLayout.child(maxPosComponent);

            // Save and Cancel buttons
            FlowLayout buttonLayout = Containers.horizontalFlow(Sizing.content(), Sizing.content());
            buttonLayout.gap(5);

            ButtonComponent saveButton = (ButtonComponent) Components.button(Text.literal("Save"), button -> {
                saveArea();
            }).horizontalSizing(Sizing.fixed(120)).verticalSizing(Sizing.fixed(20));

            ButtonComponent cancelButton = (ButtonComponent) Components.button(Text.literal("Cancel"), button -> {
                cancelEdit();
            }).horizontalSizing(Sizing.fixed(120)).verticalSizing(Sizing.fixed(20));

            buttonLayout.child(saveButton);
            buttonLayout.child(cancelButton);

            detailsLayout.child(buttonLayout);

        } else {
            // Viewing mode: show labels

            nameComponent = Components.label(Text.literal("Name: " + selectedArea.getName()));
            minPosComponent = Components.label(Text.literal("Min Pos: " + formatBlockPos(selectedArea.getMinPos())));
            maxPosComponent = Components.label(Text.literal("Max Pos: " + formatBlockPos(selectedArea.getMaxPos())));

            nameComponent.horizontalSizing(Sizing.fixed(120)).verticalSizing(Sizing.fixed(20));
            minPosComponent.horizontalSizing(Sizing.fixed(120)).verticalSizing(Sizing.fixed(20));
            maxPosComponent.horizontalSizing(Sizing.fixed(120)).verticalSizing(Sizing.fixed(20));

            detailsLayout.child(nameComponent);
            detailsLayout.child(minPosComponent);
            detailsLayout.child(maxPosComponent);

            // Edit and Delete buttons
            FlowLayout buttonLayout = Containers.horizontalFlow(Sizing.content(), Sizing.content());
            buttonLayout.gap(5);

            ButtonComponent editButton = (ButtonComponent) Components.button(Text.literal("Edit"), button -> {
                editArea();
            }).horizontalSizing(Sizing.fixed(120)).verticalSizing(Sizing.fixed(20));

            ButtonComponent deleteButton = (ButtonComponent) Components.button(Text.literal("Delete"), button -> {
                deleteArea(selectedArea);
            }).horizontalSizing(Sizing.fixed(120)).verticalSizing(Sizing.fixed(20));

            buttonLayout.child(editButton);
            buttonLayout.child(deleteButton);

            detailsLayout.child(buttonLayout);
        }
    }

    private void editArea() {
        this.isEditing = true;
        refreshDetailsLayout();
    }

    private void cancelEdit() {
        this.isEditing = false;
        refreshDetailsLayout();
    }

    private void refreshDebugView() {
        PacketByteBuf buf = PacketByteBufs.create();
        // If you need to send additional data, write it to buf here
        // For example, you might send the area ID or coordinates

        ClientPlayNetworking.send(CobblemonSpawns.REFRESH_DEBUG_VIEW_PACKET, buf);
    }

    private void saveArea() {
        if (selectedArea != null) {
            // Get values from input fields
            String newName = ((TextBoxComponent) nameComponent).getText().trim();
            String minPosText = ((TextBoxComponent) minPosComponent).getText().trim();
            String maxPosText = ((TextBoxComponent) maxPosComponent).getText().trim();

            // Validate inputs
            if (newName.isEmpty()) {
                showError("Name cannot be empty.");
                return;
            }

            // Check for duplicate names
            for (Area area : areas) {
                if (!area.getId().equals(selectedArea.getId()) && area.getName().equalsIgnoreCase(newName)) {
                    showError("An area with this name already exists.");
                    return;
                }
            }

            BlockPos newMinPos = parseBlockPos(minPosText);
            BlockPos newMaxPos = parseBlockPos(maxPosText);

            if (newMinPos == null || newMaxPos == null) {
                showError("Invalid position format. Use 'x, y, z'.");
                return;
            }

            //TODO: Check if this is necessary
            //if (newMinPos.getX() > newMaxPos.getX() || newMinPos.getY() > newMaxPos.getY() || newMinPos.getZ() > newMaxPos.getZ()) {
                //showError("Min position must be less than or equal to max position.");
                //return;
            //}

            // Update the selectedArea object
            selectedArea.setName(newName);
            selectedArea.setMinPos(newMinPos);
            selectedArea.setMaxPos(newMaxPos);

            // Send updated area to server
            PacketByteBuf buf = PacketByteBufs.create();
            buf.writeUuid(selectedArea.getId());
            buf.writeString(selectedArea.getName());
            buf.writeBlockPos(selectedArea.getMinPos());
            buf.writeBlockPos(selectedArea.getMaxPos());

            ClientPlayNetworking.send(new Identifier("cobblemonspawns", "update_area"), buf);

            isEditing = false;
            refreshDetailsLayout();
            updateAreaList();

            refreshDebugView();
        }
    }

    private BlockPos parseBlockPos(String text) {
        String[] parts = text.split(",");
        if (parts.length != 3) return null;

        try {
            int x = Integer.parseInt(parts[0].trim());
            int y = Integer.parseInt(parts[1].trim());
            int z = Integer.parseInt(parts[2].trim());
            return new BlockPos(x, y, z);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private String formatBlockPos(BlockPos pos) {
        return pos.getX() + ", " + pos.getY() + ", " + pos.getZ();
    }

    private void showError(String message) {
        MinecraftClient.getInstance().player.sendMessage(Text.literal(message), false);
    }

    private void deleteArea(Area area) {
        if (area != null) {
            // Send a packet to the server to delete the area
            PacketByteBuf buf = PacketByteBufs.create();
            buf.writeUuid(area.getId());
            ClientPlayNetworking.send(new Identifier("cobblemonspawns", "delete_area"), buf);

            // Remove the area from the list and update the UI
            areas.remove(area);
            updateAreaList();
            selectedArea = null;

            // Clear details
            isEditing = false;
            refreshDetailsLayout();
        }
    }
}
