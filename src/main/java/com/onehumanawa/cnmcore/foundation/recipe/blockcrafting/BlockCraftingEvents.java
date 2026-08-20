package com.onehumanawa.cnmcore.foundation.recipe.blockcrafting;

import com.onehumanawa.cnmcore.CNMCore;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;

import java.util.Optional;

/**
 * NeoForge integration: right-click block triggers recipe matching.
 */
public final class BlockCraftingEvents {

    private BlockCraftingEvents() {}

    public static void register() {
        CNMCore.LOGGER.info("[BlockCrafting] Events registered!");
        NeoForge.EVENT_BUS.addListener(BlockCraftingEvents::onRightClickBlock);
    }

    private static void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        CNMCore.LOGGER.info("[BlockCrafting] RightClickBlock event fired!");
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        if (event.getLevel().isClientSide()) return;
        if (event.getHand() != net.minecraft.world.InteractionHand.MAIN_HAND) return;

        CNMCore.LOGGER.info("[BlockCrafting] Passed checks, finding match...");

        var level = player.serverLevel();
        var pos = event.getPos();
        var input = event.getItemStack();

        CNMCore.LOGGER.info("[BlockCrafting] center={}, input={}, block={}",
                pos, input.getHoverName().getString(),
                level.getBlockState(pos).getBlock().getDescriptionId());

        Optional<BlockCraftingRecipe> match = BlockCraftingRegistry.findMatch(level, pos, input);

        if (match.isPresent()) {
            CNMCore.LOGGER.info("[BlockCrafting] Match found!");
            BlockCraftingRecipe recipe = match.get();
            int rotation = recipe.matchingRotation(level, pos);

            if (recipe.craft(level, pos, player, input, rotation)) {
                event.setCanceled(true);
                event.setCancellationResult(net.minecraft.world.InteractionResult.SUCCESS);

                if (!recipe.feedback().isBlank()) {
                    // player.displayClientMessage(Component.translatable(recipe.feedback()), true);
                }

                CNMCore.LOGGER.info("[BlockCrafting] Player {} crafted {}", player.getName().getString(), recipe.id());
            }
        } else {
            CNMCore.LOGGER.info("[BlockCrafting] No match found.");
        }
    }
}