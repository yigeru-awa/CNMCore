package com.onehumanawa.cnmcore.foundation.recipe.blockcrafting;

import com.onehumanawa.cnmcore.CNMCore;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Runtime registry for block crafting recipes.
 * Stores recipes in memory, resolved at match time.
 */
public final class BlockCraftingRegistry {

    private static final Map<ResourceLocation, BlockCraftingRecipe> RECIPES = new ConcurrentHashMap<>();

    private BlockCraftingRegistry() {}

    public static void register(BlockCraftingRecipe recipe) {
        if (recipe == null || recipe.id() == null) {
            CNMCore.LOGGER.warn("[BlockCrafting] Ignored registration of an invalid recipe");
            return;
        }
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

    /**
     * A matched recipe together with the rotation that matched,
     * so callers don't need to scan rotations a second time.
     */
    public record Match(BlockCraftingRecipe recipe, int rotation) {}

    public static Optional<Match> findMatch(ServerLevel level, BlockPos center, ItemStack input) {
        for (BlockCraftingRecipe recipe : RECIPES.values()) {
            var item = BuiltInRegistries.ITEM.get(ResourceLocation.tryParse(recipe.itemInputId()));
            if (item == null || !input.is(item)) continue;
            int rotation = recipe.matchingRotation(level, center);
            if (rotation >= 0) return Optional.of(new Match(recipe, rotation));
        }
        return Optional.empty();
    }
}