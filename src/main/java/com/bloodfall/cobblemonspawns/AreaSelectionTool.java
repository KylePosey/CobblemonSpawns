package com.bloodfall.cobblemonspawns;

import net.minecraft.client.item.TooltipContext;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Hand;
import net.minecraft.util.TypedActionResult;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.RaycastContext;
import net.minecraft.world.World;
import net.minecraft.util.Formatting;

import java.util.List;
import java.util.UUID;

import org.joml.Vector3d;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Custom item for selecting two points to define an area.
 */
public class AreaSelectionTool extends Item {

    private static final Logger LOGGER = LoggerFactory.getLogger(CobblemonSpawns.MOD_ID);

    public AreaSelectionTool(Settings settings) {
        super(settings);
    }

    @Override
    public TypedActionResult<ItemStack> use(World world, PlayerEntity player, Hand hand) {
        ItemStack stack = player.getStackInHand(hand);
        LOGGER.info("AreaSelectionTool used by player: {}", player.getName().getString());

        // Only proceed on the server side
        if (!world.isClient) {
            // Perform a raycast to find the block being clicked
            BlockHitResult hitResult = performRaycast(world, player, RaycastContext.FluidHandling.NONE);

            if (hitResult.getType() != BlockHitResult.Type.BLOCK) {
                player.sendMessage(Text.literal("No block targeted. Please aim at a block."), false);
                LOGGER.warn("Player {} did not target a block.", player.getName().getString());
                return TypedActionResult.pass(stack);
            }

            BlockPos pos = hitResult.getBlockPos();
            LOGGER.info("Player {} targeted block at {}", player.getName().getString(), pos);
            UUID playerId = player.getUuid();

            // Determine if this is the first or second point
            if (!SelectionManager.hasCompleteFirstPoint(playerId)) {
                // Set first point
                SelectionManager.setFirstPoint(playerId, pos);
                player.sendMessage(Text.literal("First point set at " + pos).formatted(Formatting.GREEN), false);
                LOGGER.info("Player {} set first point at {}", playerId, pos);
            } else {
                // Set second point
                SelectionManager.setSecondPoint(playerId, pos);
                player.sendMessage(Text.literal("Second point set at " + pos).formatted(Formatting.GREEN), false);
                player.sendMessage(Text.literal("Use the command /area add <name> to create the area.").formatted(Formatting.YELLOW), false);
                LOGGER.info("Player {} set second point at {}", playerId, pos);
            }

            return TypedActionResult.success(stack, world.isClient);
        }

        return TypedActionResult.pass(stack);
    }

    /**
     * Performs a raycast to determine which block the player is targeting.
     *
     * @param world The current world.
     * @param player The player performing the action.
     * @param fluidHandling Determines how fluids are handled during the raycast.
     * @return The result of the raycast.
     */
    private BlockHitResult performRaycast(World world, PlayerEntity player, RaycastContext.FluidHandling fluidHandling) {
        Vec3d start = player.getCameraPosVec(1.0F);
        Vec3d direction = player.getRotationVec(1.0F).multiply(5.0D); // 5 blocks reach
        Vec3d end = start.add(direction);

        RaycastContext context = new RaycastContext(start, end, RaycastContext.ShapeType.OUTLINE, fluidHandling, player);
        return world.raycast(context);
    }

    @Override
    public void appendTooltip(ItemStack stack, World world, List<Text> tooltip, TooltipContext context) {
        tooltip.add(Text.literal("Right-click a block to select two points and define an area.").formatted(Formatting.GRAY));
        tooltip.add(Text.literal("After selecting two points, use /area add <name> to create the area.").formatted(Formatting.GRAY));
    }
}
