package com.onehumanawa.cnmcore.foundation.data.recipe;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import com.google.gson.JsonObject;

import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.alchemy.PotionBrewing;
import net.minecraft.world.item.crafting.Ingredient;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.brewing.RegisterBrewingRecipesEvent;

import com.onehumanawa.cnmcore.CNMCore;
import com.onehumanawa.cnmcore.foundation.recipe.RecipeModificationHandler;

/**
 * Backend for the {@code add*} methods of the recipe configuration front-end
 * {@code KubeJavaRecipeModifier}. Stores recipes to inject and applies them
 * through {@link RecipeModificationHandler} on every (re)load.
 * <p>
 * Potion brewing is not JSON-driven on 1.21.1, so brewing recipes are applied
 * separately through NeoForge's {@link RegisterBrewingRecipesEvent}, which
 * fires on both client and server.
 */
@EventBusSubscriber(modid = CNMCore.ID)
public final class KubeJavaDatagenSupport {

    /** Recipes to add, keyed by recipe id; later additions with the same id override. */
    private static final Map<ResourceLocation, JsonObject> RECIPES = new LinkedHashMap<>();

    /** Potion brewing recipes to register. */
    private static final List<BrewingEntry> BREWING = new ArrayList<>();

    /**
     * A brewing stand recipe: brew {@code output} from a bottle-type
     * {@code input} and a reagent {@code ingredient}.
     */
    public record BrewingEntry(String input, String ingredient, String output) {
    }

    private KubeJavaDatagenSupport() {
    }

    /**
     * Registers a recipe to be injected into the {@code RecipeManager}.
     *
     * @param id   recipe id; without a namespace it defaults to {@code cnmcore}
     * @param json full recipe JSON, must contain a valid {@code "type"} field
     */
    public static void addRecipe(String id, JsonObject json) {
        RECIPES.put(parse(id), json);
    }

    /** All recipes registered for injection; keyed by recipe id. */
    public static Map<ResourceLocation, JsonObject> getAddedRecipes() {
        return RECIPES;
    }

    /**
     * Registers a brewing stand recipe. Item specs are item ids, or
     * {@code "#modid:tag"} for tags.
     *
     * @param input     the bottle/potion being modified
     * @param ingredient the reagent item added in the top slot
     * @param output    the resulting item
     */
    public static void addBrewing(String input, String ingredient, String output) {
        BREWING.add(new BrewingEntry(input, ingredient, output));
    }

    // ------------------------------------------------------------------
    // Brewing integration
    // ------------------------------------------------------------------

    @SubscribeEvent
    public static void onRegisterBrewing(RegisterBrewingRecipesEvent event) {
        if (BREWING.isEmpty())
            return;

        HolderLookup<Item> items = event.getRegistryAccess().lookupOrThrow(Registries.ITEM);
        PotionBrewing.Builder builder = event.getBuilder();

        for (BrewingEntry entry : BREWING) {
            Ingredient input = toIngredient(entry.input(), items);
            Ingredient reagent = toIngredient(entry.ingredient(), items);
            Optional<Item> output = items.get(ResourceKey.create(Registries.ITEM, parse(entry.output())))
                    .map(Holder::value);

            if (input.isEmpty() || reagent.isEmpty() || output.isEmpty()) {
                CNMCore.LOGGER.warn("[Recipe] Skipping brewing recipe {} -> {}: unresolvable item or empty ingredient",
                        entry.ingredient(), entry.output());
                continue;
            }

            builder.addRecipe(input, reagent, new ItemStack(output.get()));
            CNMCore.LOGGER.info("[Recipe] Added brewing recipe {} + {} -> {}",
                    entry.input(), entry.ingredient(), entry.output());
        }
    }

    private static Ingredient toIngredient(String spec, HolderLookup<Item> items) {
        if (spec.startsWith("#")) {
            return Ingredient.of(TagKey.create(Registries.ITEM, ResourceLocation.parse(spec.substring(1))));
        }
        Item item = items.get(ResourceKey.create(Registries.ITEM, parse(spec)))
                .map(Holder::value)
                .orElse(null);
        return item == null ? Ingredient.EMPTY : Ingredient.of(item);
    }

    private static ResourceLocation parse(String id) {
        return id.indexOf(':') >= 0
                ? ResourceLocation.parse(id)
                : ResourceLocation.fromNamespaceAndPath(CNMCore.ID, id);
    }
}