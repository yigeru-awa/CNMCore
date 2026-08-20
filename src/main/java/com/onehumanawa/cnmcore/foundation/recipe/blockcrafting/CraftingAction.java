package com.onehumanawa.cnmcore.foundation.recipe.blockcrafting;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

@FunctionalInterface
public interface CraftingAction {
    void execute(ServerLevel level, BlockPos center, ServerPlayer player);
}