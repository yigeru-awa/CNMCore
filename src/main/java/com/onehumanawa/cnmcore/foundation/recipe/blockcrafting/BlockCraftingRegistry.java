package com.onehumanawa.cnmcore.foundation.recipe.blockcrafting;

import com.onehumanawa.cnmcore.CNMCore;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Runtime registry for block crafting recipes.
 * Stores recipes in memory, resolved at match time.
 */
public final class BlockCraftingRegistry {

    private static final Map<ResourceLocation, BlockCraftingRecipe> RECIPES = new ConcurrentHashMap<>();

    private BlockCraftingRegistry() {}

    public static void register(BlockCraftingRecipe recipe) {
        RECIPES.put(recipe.id(), recipe);
    }

    public static Optional<BlockCraftingRecipe> get(ResourceLocation id) {
        return Optional.ofNullable(RECIPES.get(id));
    }

    public static Optional<BlockCraftingRecipe> get(String id) {
        ResourceLocation parsed = ResourceLocation.tryParse(id);
        return parsed == null ? Optional.empty() : get(parsed);
    }

    public static boolean remove(ResourceLocation id) {
        return RECIPES.remove(id) != null;
    }

    public static Collection<BlockCraftingRecipe> all() {
        return List.copyOf(RECIPES.values());
    }

    public static Optional<BlockCraftingRecipe> findMatch(ServerLevel level, BlockPos center, ItemStack input) {
        for (BlockCraftingRecipe recipe : all()) {
            boolean matches = recipe.matches(level, center, input);
            if (matches) return Optional.of(recipe);
        }
        return Optional.empty();
    }
}