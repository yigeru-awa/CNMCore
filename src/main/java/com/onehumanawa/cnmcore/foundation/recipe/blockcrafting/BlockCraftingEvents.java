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

        var level = player.serverLevel();
        var pos = event.getPos();
        var input = event.getItemStack();

        Optional<BlockCraftingRecipe> match = BlockCraftingRegistry.findMatch(level, pos, input);
        if (match.isEmpty()) return;

        BlockCraftingRecipe recipe = match.get();
        int rotation = recipe.matchingRotation(level, pos);
        boolean isDeployer = player instanceof DeployerFakePlayer;

        if (recipe.craft(level, pos, player, input, rotation, isDeployer)) {
            event.setCanceled(true);
            event.setCancellationResult(InteractionResult.SUCCESS);

            if (!recipe.feedback().isBlank()) {
                player.displayClientMessage(Component.translatable(recipe.feedback()), true);
            }

            // If triggered by a Deployer, insert results directly into its inventory via addItem
            if (isDeployer) {
                for (String resultId : recipe.resultIds()) {
                    var item = BuiltInRegistries.ITEM.get(ResourceLocation.parse(resultId));
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