package com.onehumanawa.cnmcore.content.redprint;

import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/**
 * Redprint tool - select an area and remove all blocks within it.
 */
public class RedprintItem extends Item {

    public RedprintItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);

        if (level.isClientSide) {
            // Delegate to handler with fresh raycast
            RedprintHandler.getInstance().onItemUse(player);
        }

        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        return RedprintHandler.getInstance().isSelecting();
    }
}