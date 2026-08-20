package com.onehumanawa.cnmcore.foundation.recipe.blockcrafting;

import com.onehumanawa.cnmcore.CNMCore;
import com.simibubi.create.content.kinetics.deployer.DeployerFakePlayer;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;

import java.util.Optional;

/**
 * NeoForge integration for block crafting.
 * Handles right-click interactions and Deployer (mechanical arm) compatibility.
 * A failing recipe can never crash the interaction: exceptions are caught
 * and logged, the event is left untouched.
 */
public final class BlockCraftingEvents {

    private BlockCraftingEvents() {}

    public static void register() {
        NeoForge.EVENT_BUS.addListener(BlockCraftingEvents::onRightClickBlock);
    }

    private static void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        if (event.getLevel().isClientSide()) return;
        if (event.getHand() != InteractionHand.MAIN_HAND) return;

        try {
            handleInteraction(event, player);
        } catch (Exception e) {
            CNMCore.LOGGER.error("[BlockCrafting] Failed to process interaction at {}:", event.getPos(), e);
        }
    }

    private static void handleInteraction(PlayerInteractEvent.RightClickBlock event, ServerPlayer player) {
        var level = player.serverLevel();
        var pos = event.getPos();
        var input = event.getItemStack();

        Optional<BlockCraftingRegistry.Match> match = BlockCraftingRegistry.findMatch(level, pos, input);
        if (match.isEmpty()) return;

        BlockCraftingRecipe recipe = match.get().recipe();
        boolean isDeployer = player instanceof DeployerFakePlayer;

        if (recipe.craft(level, pos, player, input, match.get().rotation(), isDeployer)) {
            event.setCanceled(true);
            event.setCancellationResult(InteractionResult.SUCCESS);

            if (recipe.feedback() != null && !recipe.feedback().isBlank()) {
                player.displayClientMessage(Component.translatable(recipe.feedback()), true);
            }

            // If triggered by a Deployer, insert results directly into its inventory via addItem
            if (isDeployer) {
                for (String resultId : recipe.resultIds()) {
                    ResourceLocation id = ResourceLocation.tryParse(resultId);
                    if (id == null) continue;
                    var item = BuiltInRegistries.ITEM.get(id);
                    if (item == null || item == Items.AIR) continue;
                    ItemStack result = new ItemStack(item, 1);
                    if (!player.addItem(result)) {
                        Block.popResource(level, pos, result);
                    }
                }
            }
        }
    }
}