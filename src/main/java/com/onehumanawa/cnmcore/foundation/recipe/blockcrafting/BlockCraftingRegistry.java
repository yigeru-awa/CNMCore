package com.onehumanawa.cnmcore.foundation.recipe.blockcrafting;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Java-only registry for BlockCrafting recipes.
 * Use {@link KubeJavaBlockCrafting} to register recipes.
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

    public static boolean remove(String id) {
        ResourceLocation parsed = ResourceLocation.tryParse(id);
        return parsed != null && remove(parsed);
    }

    public static Collection<BlockCraftingRecipe> all() {
        List<BlockCraftingRecipe> sorted = new ArrayList<>(RECIPES.values());
        sorted.sort(Comparator.comparing(recipe -> recipe.id().toString()));
        return List.copyOf(sorted);
    }

    public static Optional<BlockCraftingRecipe.Match> findMatch(ServerLevel level, BlockPos center, ItemStack input) {
        for (BlockCraftingRecipe recipe : all()) {
            int rotation = recipe.matchingRotation(level, center, input);
            if (rotation >= 0) return Optional.of(new BlockCraftingRecipe.Match(recipe, rotation));
        }
        return Optional.empty();
    }

    public static boolean canCraft(ServerLevel level, BlockPos center, ItemStack input) {
        return findMatch(level, center, input).isPresent();
    }

    public static Collection<BlockCraftingRecipe> candidates(ItemStack input) {
        return all().stream().filter(recipe -> recipe.matchesInput(input)).toList();
    }
}