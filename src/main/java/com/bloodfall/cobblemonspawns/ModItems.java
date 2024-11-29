package com.bloodfall.cobblemonspawns;

import net.fabricmc.fabric.api.item.v1.FabricItemSettings;
import net.fabricmc.fabric.api.itemgroup.v1.FabricItemGroupEntries;
import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents;
import net.minecraft.item.Item;
import net.minecraft.item.ItemGroups;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.util.Identifier;
import com.bloodfall.cobblemonspawns.CobblemonSpawns;

public class ModItems {
    public static final Item AREA_SELECTION_TOOL = registerItem("area_selection_tool", new Item(new FabricItemSettings()));

    private static void addToToolGroup(FabricItemGroupEntries entries)
    {
        entries.add(AREA_SELECTION_TOOL);
    }

    private static Item registerItem(String name, Item item)
    {
        return Registry.register(Registries.ITEM, new Identifier(CobblemonSpawns.MOD_ID, name), item);
    }

    public static void registerModItems()
    {
        CobblemonSpawns.LOGGER.info("Registering mod items for " + CobblemonSpawns.MOD_ID);

        ItemGroupEvents.modifyEntriesEvent(ItemGroups.TOOLS).register(ModItems::addToToolGroup);
    }
}
