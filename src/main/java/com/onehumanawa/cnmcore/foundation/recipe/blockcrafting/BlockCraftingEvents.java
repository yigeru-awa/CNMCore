package com.onehumanawa.cnmcore.foundation.recipe.blockcrafting;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Optional;

/**
 * NeoForge integration for right-click crafting.
 * No commands, only Java-registered recipes.
 */
public final class BlockCraftingEvents {
    private static final Logger LOGGER = LoggerFactory.getLogger("BlockCrafting/Events");
    private static volatile boolean debugLogging = false;
    private static volatile boolean failureFeedback = true;

    private BlockCraftingEvents() {}

    public static void register() {
        NeoForge.EVENT_BUS.addListener(BlockCraftingEvents::onRightClickBlock);
    }

    public static void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        if (!(event.getEntity() instanceof ServerPlayer player) || event.getLevel().isClientSide()) return;
        if (event.getHand() != net.minecraft.world.InteractionHand.MAIN_HAND) return;

        var level = player.serverLevel();
        var input = event.getItemStack();
        Optional<BlockCraftingRecipe.Match> match = BlockCraftingRegistry.findMatch(level, event.getPos(), input);

        if (match.isPresent()) {
            BlockCraftingRecipe recipe = match.get().recipe();
            int rotation = match.get().rotation();

            NeoForge.EVENT_BUS.post(new BlockCraftingCompleteEvent(
                    level, event.getPos(), player, recipe.id(), input.copy(), rotation
            ));

            if (recipe.craft(level, event.getPos(), player, input, rotation)) {
                event.setCanceled(true);
                event.setCancellationResult(net.minecraft.world.InteractionResult.SUCCESS);
                if (!recipe.feedback().isBlank()) {
                    player.displayClientMessage(Component.translatable(recipe.feedback()), true);
                }
                LOGGER.info("Player {} completed block crafting {} (rotation={})",
                        player.getGameProfile().getName(), recipe.id(), rotation);
            }
            return;
        }

        if (debugLogging) {
            LOGGER.info("No matching block crafting recipe: player={}, pos={}, input={}",
                    player.getGameProfile().getName(), event.getPos(), input.getHoverName().getString());
        }
        if (failureFeedback && !BlockCraftingRegistry.candidates(input).isEmpty()) {
            player.displayClientMessage(Component.translatable("blockcrafting.feedback.failure"), true);
        }
    }

    public static boolean debugLogging() { return debugLogging; }
    public static boolean failureFeedback() { return failureFeedback; }
    public static void setDebugLogging(boolean enabled) { debugLogging = enabled; }
    public static void setFailureFeedback(boolean enabled) { failureFeedback = enabled; }
}