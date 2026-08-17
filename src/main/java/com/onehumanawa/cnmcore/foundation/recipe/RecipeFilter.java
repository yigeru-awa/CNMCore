package com.onehumanawa.cnmcore.foundation.recipe;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

import com.google.gson.JsonObject;

/**
 * A recipe filter bound to a set of modifications. Instances are created
 * through the factory methods of {@link KubeJavaRecipeModifier} (for example
 * {@code byMod("create")}) and are automatically registered with
 * {@link RecipeModificationHandler}.
 *
 * <pre>{@code
 * // All Create recipes: swap iron ingot inputs for iron sheets
 * KubeJavaRecipeModifier.byMod("create")
 *         .replaceInput("minecraft:iron_ingot", "create:iron_sheet");
 *
 * // A single recipe: slower mixing, different output
 * KubeJavaRecipeModifier.byId("create:mixing/tea")
 *         .replaceOutput("create: builders_tea", "create:builders_tea")
 *         .modify(json -> json.addProperty("processingTime", 200));
 * }</pre>
 */
public final class RecipeFilter {

    /**
     * Matching context describing a single recipe.
     *
     * @param id     full recipe id, e.g. {@code create:mixing/tea}
     * @param modId  namespace of the recipe id, e.g. {@code create}
     * @param typeId registered id of the recipe type, e.g. {@code create:mixing},
     *               or {@code null} if the type is not registered
     */
    public record RecipeInfo(net.minecraft.resources.ResourceLocation id, String modId,
                             net.minecraft.resources.ResourceLocation typeId) {
    }

    private final java.util.function.Predicate<RecipeInfo> predicate;
    private final Map<String, String> inputReplacements = new HashMap<>();
    private final Map<String, String> outputReplacements = new HashMap<>();
    private final List<Consumer<JsonObject>> transformers = new ArrayList<>();
    private boolean remove;

    RecipeFilter(java.util.function.Predicate<RecipeInfo> predicate) {
        this.predicate = predicate;
        RecipeModificationHandler.register(this);
    }

    /**
     * Replaces an input item in every recipe matched by this filter.
     * <p>
     * Applies inside {@code ingredient}/{@code ingredients} entries. If several
     * matching filters replace the same item, the last registered one wins.
     *
     * @param oldItem item id to replace, e.g. {@code "minecraft:iron_ingot"}
     * @param newItem replacement item id
     * @return this filter, for chaining
     */
    public RecipeFilter replaceInput(String oldItem, String newItem) {
        inputReplacements.put(oldItem, newItem);
        return this;
    }

    /**
     * Replaces an output item in every recipe matched by this filter.
     * <p>
     * Applies inside {@code result}/{@code results} entries. Counts and
     * chances are preserved. If several matching filters replace the same
     * item, the last registered one wins.
     *
     * @param oldItem item id to replace
     * @param newItem replacement item id
     * @return this filter, for chaining
     */
    public RecipeFilter replaceOutput(String oldItem, String newItem) {
        outputReplacements.put(oldItem, newItem);
        return this;
    }

    /**
     * Registers an arbitrary JSON transformer for every recipe matched by
     * this filter. The transformer receives the raw recipe JSON before
     * re-parsing, so any field (e.g. {@code processingTime}, {@code heatCondition},
     * {@code results}[n].{@code count}) can be edited directly.
     *
     * @param transformer JSON editor, must not replace the object itself
     * @return this filter, for chaining
     */
    public RecipeFilter modify(Consumer<JsonObject> transformer) {
        transformers.add(transformer);
        return this;
    }

    /**
     * Removes every recipe matched by this filter. Terminal action, cannot be
     * chained.
     */
    public void remove() {
        this.remove = true;
    }

    boolean matches(RecipeInfo info) {
        return predicate.test(info);
    }

    boolean shouldRemove() {
        return remove;
    }

    void collect(Map<String, String> inputs, Map<String, String> outputs,
                 List<Consumer<JsonObject>> transformerList) {
        inputs.putAll(inputReplacements);
        outputs.putAll(outputReplacements);
        transformerList.addAll(transformers);
    }
}
