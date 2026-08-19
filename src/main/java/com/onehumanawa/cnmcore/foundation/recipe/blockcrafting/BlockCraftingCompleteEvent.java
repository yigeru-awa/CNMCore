package com.onehumanawa.cnmcore.foundation.recipe.blockcrafting;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.Event;

/**
 * Public event posted after the structure matches and before blocks are consumed.
 */
public final class BlockCraftingCompleteEvent extends Event {
    private final ServerLevel level;
    private final BlockPos center;
    private final ServerPlayer player;
    private final ResourceLocation recipeId;
    private final ItemStack input;
    private final int rotation;

    public BlockCraftingCompleteEvent(ServerLevel level, BlockPos center, ServerPlayer player,
                                      ResourceLocation recipeId, ItemStack input, int rotation) {
        this.level = level;
        this.center = center;
        this.player = player;
        this.recipeId = recipeId;
        this.input = input;
        this.rotation = rotation;
    }

    public ServerLevel level() { return level; }
    public BlockPos center() { return center; }
    public ServerPlayer player() { return player; }
    public ResourceLocation recipeId() { return recipeId; }
    public ItemStack input() { return input; }
    public int rotation() { return rotation; }
}