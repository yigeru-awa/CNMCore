package com.onehumanawa.cnmcore.foundation.recipe;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonPrimitive;
import com.mojang.serialization.JsonOps;
import org.jetbrains.annotations.NotNull;

import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.RegistryOps;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimplePreparableReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeManager;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.AddReloadListenerEvent;

import com.onehumanawa.cnmcore.CNMCore;
import com.onehumanawa.cnmcore.foundation.data.recipe.KubeJavaDatagenSupport;
import com.onehumanawa.cnmcore.foundation.item.ItemSpec;

/**
 * Engine for modpack-wide recipe control. Declarative configuration lives in
 * {@link KubeJavaRecipeModifier}; filters are created through {@link RecipeFilter}.
 * <p>
 * Runs as a server reload listener placed after the vanilla RecipeManager, so
 * all changes apply both on initial server start and after {@code /reload}.
 * Modifications work at the JSON level: the original recipe file is read from
 * the resource manager, edited, then re-parsed through {@link Recipe#CODEC}.
 * This works for every recipe type, including Create processing recipes.
 * The updated recipe set is synced to clients automatically.
 */
@SuppressWarnings("unused")
@EventBusSubscriber(modid = CNMCore.ID)
public class RecipeModificationHandler {

    /** All registered filters; recipe changes are declared against them. */
    private static final List<RecipeFilter> FILTERS = new ArrayList<>();

    // Runtime references for the public query API
    private static RecipeManager recipeManager;
    private static ResourceManager resourceManager;

    static {
        // Load the modpack's recipe configuration
        KubeJavaRecipeModifier.init();
    }

    static void register(RecipeFilter filter) {
        FILTERS.add(filter);
    }

    // ------------------------------------------------------------------
    // Public query API
    // ------------------------------------------------------------------

    /** Looks up a currently loaded recipe by its "modid:path" id. */
    public static Optional<RecipeHolder<?>> getRecipeById(String recipeId) {
        return recipeManager == null ? Optional.empty() : recipeManager.byKey(parseId(recipeId));
    }

    /**
     * Returns the raw recipe JSON as loaded from the resource packs, before any
     * modifications made by this handler.
     */
    public static Optional<JsonObject> getRecipeJson(String recipeId) {
        if (resourceManager == null)
            return Optional.empty();
        return readRecipeJson(parseId(recipeId), resourceManager);
    }

    // ------------------------------------------------------------------
    // Reload handling
    // ------------------------------------------------------------------

    @SubscribeEvent
    public static void onAddReloadListeners(AddReloadListenerEvent event) {
        event.addListener(new SimplePreparableReloadListener<Void>() {
            @Override
            protected @NotNull Void prepare(@NotNull ResourceManager resourceManager, @NotNull ProfilerFiller profiler) {
                return null;
            }

            @Override
            protected void apply(@NotNull Void data, @NotNull ResourceManager resourceManager, @NotNull ProfilerFiller profiler) {
                profiler.push("cnmcore:modify_recipes");
                processRecipes(event.getServerResources().getRecipeManager(), resourceManager,
                        event.getRegistryAccess());
                profiler.pop();
            }
        });
    }

    private static void processRecipes(RecipeManager manager, ResourceManager resources,
                                       HolderLookup.Provider registries) {
        recipeManager = manager;
        resourceManager = resources;
        Map<ResourceLocation, JsonObject> added = KubeJavaDatagenSupport.getAddedRecipes();
        if (FILTERS.isEmpty() && added.isEmpty())
            return;

        RegistryOps<JsonElement> ops = registries.createSerializationContext(JsonOps.INSTANCE);
        var typeLookup = registries.lookupOrThrow(Registries.RECIPE_TYPE);
        Map<net.minecraft.world.item.crafting.RecipeType<?>, ResourceLocation> typeIds = new HashMap<>();
        typeLookup.listElements().forEach(holder -> typeIds.put(holder.value(), holder.key().location()));

        Map<ResourceLocation, RecipeHolder<?>> result = new LinkedHashMap<>();
        int removedCount = 0;
        int modifiedCount = 0;

        for (RecipeHolder<?> holder : manager.getRecipes()) {
            ResourceLocation typeId = typeIds.get(holder.value().getType());
            RecipeFilter.RecipeInfo info = new RecipeFilter.RecipeInfo(holder.id(), holder.id().getNamespace(), typeId);

            List<RecipeFilter> matched = new ArrayList<>();
            for (RecipeFilter filter : FILTERS)
                if (filter.matches(info))
                    matched.add(filter);

            boolean removed = false;
            for (RecipeFilter filter : matched) {
                if (filter.shouldRemove()) {
                    removed = true;
                    break;
                }
            }

            if (removed) {
                removedCount++;
                CNMCore.LOGGER.info("[Recipe] Removed recipe {}", holder.id());
                continue;
            }

            if (matched.isEmpty()) {
                result.put(holder.id(), holder);
                continue;
            }

            Map<String, String> inputs = new HashMap<>();
            Map<String, String> outputs = new HashMap<>();
            List<Consumer<JsonObject>> transformers = new ArrayList<>();
            for (RecipeFilter filter : matched)
                filter.collect(inputs, outputs, transformers);

            RecipeHolder<?> modified = tryModify(holder, resources, ops, inputs, outputs, transformers);
            if (modified != holder) {
                modifiedCount++;
                CNMCore.LOGGER.debug("[Recipe] Modified recipe {}", holder.id());
            }
            result.put(modified.id(), modified);
        }

        int addedCount = 0;
        for (Map.Entry<ResourceLocation, JsonObject> entry : added.entrySet()) {
            Optional<net.minecraft.world.item.crafting.Recipe<?>> recipe = Recipe.CODEC.parse(ops, entry.getValue())
                    .result();
            if (recipe.isEmpty()) {
                CNMCore.LOGGER.warn("[Recipe] Failed to parse added recipe {}", entry.getKey());
                continue;
            }
            result.put(entry.getKey(), new RecipeHolder<>(entry.getKey(), recipe.get()));
            addedCount++;
            CNMCore.LOGGER.info("[Recipe] Added recipe {}", entry.getKey());
        }

        manager.replaceRecipes(result.values());
        CNMCore.LOGGER.info("[Recipe] Processing done: {} removed, {} modified, {} added",
                removedCount, modifiedCount, addedCount);
    }

    private static RecipeHolder<?> tryModify(RecipeHolder<?> holder, ResourceManager resources,
                                             RegistryOps<JsonElement> ops,
                                             Map<String, String> inputReplacements,
                                             Map<String, String> outputReplacements,
                                             List<Consumer<JsonObject>> transformers) {
        if (inputReplacements.isEmpty() && outputReplacements.isEmpty() && transformers.isEmpty())
            return holder;

        Optional<JsonObject> jsonOpt = readRecipeJson(holder.id(), resources);
        if (jsonOpt.isEmpty()) {
            CNMCore.LOGGER.warn("[Recipe] Could not read JSON of {}, leaving it unchanged", holder.id());
            return holder;
        }
        JsonObject json = jsonOpt.get();

        boolean changed = false;
        if (!inputReplacements.isEmpty())
            changed |= replaceScoped(json, INPUT_SCOPES, inputReplacements, true);
        if (!outputReplacements.isEmpty())
            changed |= replaceScoped(json, OUTPUT_SCOPES, outputReplacements, false);
        for (Consumer<JsonObject> transformer : transformers) {
            transformer.accept(json);
            changed = true;
        }
        if (!changed)
            return holder;

        Optional<RecipeHolder<?>> reparsed = Recipe.CODEC.parse(ops, json)
                .result()
                .map(recipe -> (RecipeHolder<?>) new RecipeHolder<>(holder.id(), recipe));
        if (reparsed.isEmpty()) {
            CNMCore.LOGGER.warn("[Recipe] Failed to re-parse modified JSON of {}, keeping original", holder.id());
            return holder;
        }
        return reparsed.get();
    }

    // ------------------------------------------------------------------
    // JSON helpers
    // ------------------------------------------------------------------

    private static final Set<String> INPUT_SCOPES = Set.of("ingredient", "ingredients");
    private static final Set<String> OUTPUT_SCOPES = Set.of("result", "results");

    /**
     * Replaces item ids inside a scope subtree. {@code ingredientContext} is
     * {@code true} for inputs and {@code false} for result stacks: plain ids
     * are set as-is, while specs with components ({@link ItemSpec}) become a
     * NeoForge component ingredient on inputs and a {@code components} field
     * on result stacks.
     */
    private static boolean replaceScoped(JsonObject root, Set<String> scopes, Map<String, String> replacements,
                                         boolean ingredientContext) {
        boolean changed = false;
        for (Map.Entry<String, JsonElement> entry : root.entrySet()) {
            if (scopes.contains(entry.getKey())) {
                changed |= replaceItemIds(entry.getValue(), replacements, ingredientContext);
            } else {
                changed |= replaceScopedDeep(entry.getValue(), scopes, replacements, ingredientContext);
            }
        }
        return changed;
    }

    private static boolean replaceScopedDeep(JsonElement element, Set<String> scopes,
                                             Map<String, String> replacements, boolean ingredientContext) {
        boolean changed = false;
        if (element.isJsonObject()) {
            changed |= replaceScoped(element.getAsJsonObject(), scopes, replacements, ingredientContext);
        } else if (element.isJsonArray()) {
            for (JsonElement e : element.getAsJsonArray())
                changed |= replaceScopedDeep(e, scopes, replacements, ingredientContext);
        }
        return changed;
    }

    /**
     * Replaces item ids inside an ingredient or result subtree. Handles both
     * single-item objects ({@code {"item": "..."}} / {@code {"id": "..."}})
     * and plain string arrays. Replacement values may be encoded specs from
     * {@link ItemSpec#of(String, String)} carrying data components.
     */
    private static boolean replaceItemIds(JsonElement element, Map<String, String> replacements,
                                          boolean ingredientContext) {
        boolean changed = false;
        if (element.isJsonArray()) {
            var array = element.getAsJsonArray();
            for (int i = 0; i < array.size(); i++) {
                JsonElement e = array.get(i);
                if (e.isJsonPrimitive()) {
                    String mapped = replacements.get(e.getAsString());
                    if (mapped != null) {
                        ItemSpec.Decoded decoded = ItemSpec.decode(mapped);
                        if (decoded.components() == null)
                            array.set(i, new JsonPrimitive(decoded.id()));
                        else if (ingredientContext)
                            array.set(i, ItemSpec.ingredientJson(mapped));
                        else
                            array.set(i, ItemSpec.stackJson(mapped, 1));
                        changed = true;
                    }
                } else {
                    changed |= replaceItemIds(e, replacements, ingredientContext);
                }
            }
        } else if (element.isJsonObject()) {
            JsonObject obj = element.getAsJsonObject();
            for (String field : List.of("item", "id")) {
                JsonElement value = obj.get(field);
                if (value != null && value.isJsonPrimitive()) {
                    String mapped = replacements.get(value.getAsString());
                    if (mapped != null) {
                        ItemSpec.Decoded decoded = ItemSpec.decode(mapped);
                        if (decoded.components() == null) {
                            obj.addProperty(field, decoded.id());
                        } else if (ingredientContext) {
                            // replace the whole ingredient entry with a component ingredient
                            JsonObject replacement = ItemSpec.ingredientJson(mapped);
                            obj.entrySet().clear();
                            for (Map.Entry<String, JsonElement> e : replacement.entrySet())
                                obj.add(e.getKey(), e.getValue());
                        } else {
                            obj.addProperty(field, decoded.id());
                            obj.add("components", decoded.components());
                        }
                        changed = true;
                    }
                }
            }
            for (Map.Entry<String, JsonElement> entry : obj.entrySet())
                changed |= replaceItemIds(entry.getValue(), replacements, ingredientContext);
        }
        return changed;
    }

    private static Optional<JsonObject> readRecipeJson(ResourceLocation id, ResourceManager resources) {
        try {
            Optional<Resource> resource = resources.getResource(
                    ResourceLocation.fromNamespaceAndPath(id.getNamespace(), "recipe/" + id.getPath()));
            if (resource.isEmpty())
                return Optional.empty();
            try (var reader = resource.get().openAsReader()) {
                JsonElement element = JsonParser.parseReader(reader);
                return element.isJsonObject() ? Optional.of(element.getAsJsonObject()) : Optional.empty();
            }
        } catch (Exception e) {
            CNMCore.LOGGER.warn("[Recipe] Error reading JSON of {}", id, e);
            return Optional.empty();
        }
    }

    static ResourceLocation parseId(String s) {
        String[] parts = s.split(":", 2);
        return parts.length == 2
                ? ResourceLocation.fromNamespaceAndPath(parts[0], parts[1])
                : ResourceLocation.withDefaultNamespace(s);
    }
}