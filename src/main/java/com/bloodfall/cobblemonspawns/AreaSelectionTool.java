package com.bloodfall.cobblemonspawns;

import com.bloodfall.cobblemonspawns.SelectionManager;
import net.minecraft.client.item.TooltipContext;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Hand;
import net.minecraft.util.TypedActionResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import java.util.List;
import java.util.UUID;

/**
 * Custom item for selecting two points to define an area.
 */
public class AreaSelectionTool extends Item {

    public AreaSelectionTool(Settings settings) {
        super(settings);
    }

    @Override
    public TypedActionResult<ItemStack> use(World world, PlayerEntity player, Hand hand) {
        ItemStack stack = player.getStackInHand(hand);

        // Only proceed on the server side
        if (!world.isClient) {
            BlockPos pos = player.getBlockPos();
            UUID playerId = player.getUuid();

            // Determine if this is the first or second point
            if (!SelectionManager.hasCompleteSelection(playerId)) {
                // Set first point
                SelectionManager.setFirstPoint(playerId, pos);
                player.sendMessage(Text.literal("First point set at " + pos).formatted(Formatting.GREEN), false);
            } else {
                // Set second point
                SelectionManager.setSecondPoint(playerId, pos);
                player.sendMessage(Text.literal("Second point set at " + pos).formatted(Formatting.GREEN), false);
                player.sendMessage(Text.literal("Use the command /area add <name> to create the area.").formatted(Formatting.YELLOW), false);
            }

            return TypedActionResult.success(stack, world.isClient);
        }

        return TypedActionResult.pass(stack);
    }

    @Override
    public void appendTooltip(ItemStack stack, World world, List<Text> tooltip, TooltipContext context) {
        tooltip.add(Text.literal("Right-click to select two blocks and define an area.").formatted(Formatting.GRAY));
        tooltip.add(Text.literal("After selecting, use /area add <name> to create the area.").formatted(Formatting.GRAY));
    }
}
