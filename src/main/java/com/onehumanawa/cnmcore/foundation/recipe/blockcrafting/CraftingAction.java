package com.onehumanawa.cnmcore.foundation.recipe.blockcrafting;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

/**
 * A server-side action executed after block crafting completes.
 * Custom recipes can use it for side effects such as spawning entities, modifying blocks, or granting rewards.
 */
@FunctionalInterface
public interface CraftingAction {
    void execute(ServerLevel level, BlockPos center, ServerPlayer player);
}