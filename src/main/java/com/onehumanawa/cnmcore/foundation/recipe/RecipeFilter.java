package com.onehumanawa.cnmcore.foundation.recipe;

import com.google.gson.JsonObject;
import net.minecraft.resources.ResourceLocation;

import java.util.*;
import java.util.function.Consumer;
import java.util.function.Predicate;

/**
 * Enterprise-grade Recipe Filter with fluent API for recipe manipulation.
 * <p>
 * This class implements the Chain of Responsibility and Specification patterns
 * for composable recipe filtering and transformation. Instances are created
 * through factory methods in {@link KubeJavaRecipeModifier}.
 * </p>
 *
 * <h2>Usage Examples:</h2>
 * <pre>{@code
 * // Filter all Create recipes and replace inputs
 * RecipeFilter.byMod("create")
 *         .replaceInput("minecraft:iron_ingot", "create:iron_sheet")
 *         .execute();
 *
 * // Filter specific recipe and modify JSON
 * RecipeFilter.byId("create:mixing/tea")
 *         .replaceOutput("create:builders_tea", "create:tea")
 *         .modify(json -> json.addProperty("processingTime", 200))
 *         .execute();
 *
 * // Remove multiple recipes
 * RecipeFilter.byIds("recipe1", "recipe2", "recipe3")
 *         .remove();
 * }</pre>
 *
 * @author OneHumanAwa
 * @version 2.0
 * @since 1.0
 */
@SuppressWarnings({"unused", "UnusedReturnValue"})
public final class RecipeFilter {

    /**
     * Recipe metadata record providing context for filtering decisions.
     *
     * @param id     Full recipe identifier (e.g., "create:mixing/tea")
     * @param modId  Namespace/mod ID from the recipe ID (e.g., "create")
     * @param typeId Registered recipe type identifier (e.g., "create:mixing"),
     *               may be {@code null} if the recipe type is not registered
     */
    public record RecipeInfo(ResourceLocation id, String modId, ResourceLocation typeId) {

        /**
         * Convenience constructor for creating RecipeInfo from a ResourceLocation.
         *
         * @param id The recipe ID
         */
        public RecipeInfo(ResourceLocation id) {
            this(id, id.getNamespace(), null);
        }

        /**
         * Convenience constructor for creating RecipeInfo with type.
         *
         * @param id     The recipe ID
         * @param typeId The recipe type ID
         */
        public RecipeInfo(ResourceLocation id, ResourceLocation typeId) {
            this(id, id.getNamespace(), typeId);
        }
    }

    // ================================================================
    // Filter State
    // ================================================================

    private final Predicate<RecipeInfo> predicate;
    private final Map<String, String> inputReplacements = new LinkedHashMap<>();
    private final Map<String, String> outputReplacements = new LinkedHashMap<>();
    private final List<Consumer<JsonObject>> transformers = new ArrayList<>();
    private final List<Consumer<JsonObject>> preTransformers = new ArrayList<>();
    private final List<Consumer<JsonObject>> postTransformers = new ArrayList<>();
    private boolean shouldRemove = false;
    private boolean executed = false;

    // ================================================================
    // Package-Private Constructor
    // ================================================================

    /**
     * Package-private constructor. Use factory methods in KubeJavaRecipeModifier.
     *
     * @param predicate The filtering predicate
     */
    RecipeFilter(Predicate<RecipeInfo> predicate) {
        this.predicate = Objects.requireNonNull(predicate, "Predicate cannot be null");
        // Auto-register with handler for lazy execution
        RecipeModificationHandler.register(this);
    }

    // ================================================================
    // Factory Methods (Delegated from KubeJavaRecipeModifier)
    // ================================================================

    /**
     * Creates a filter that matches all recipes.
     *
     * @return A new RecipeFilter instance
     */
    public static RecipeFilter all() {
        return new RecipeFilter(info -> true);
    }

    /**
     * Creates a filter that matches a specific recipe by ID.
     *
     * @param id Recipe identifier
     * @return A new RecipeFilter instance
     */
    public static RecipeFilter byId(String id) {
        return byId(id, false);
    }

    /**
     * Creates a filter that matches a specific recipe by ID with blacklist option.
     *
     * @param id        Recipe identifier
     * @param blacklist If true, matches all recipes except the specified one
     * @return A new RecipeFilter instance
     */
    public static RecipeFilter byId(String id, boolean blacklist) {
        ResourceLocation target = RecipeModificationHandler.parseId(id);
        Predicate<RecipeInfo> predicate = info -> info.id().equals(target);
        return new RecipeFilter(blacklist ? predicate.negate() : predicate);
    }

    /**
     * Creates a filter that matches multiple recipe IDs.
     *
     * @param ids Recipe identifiers
     * @return A new RecipeFilter instance
     */
    public static RecipeFilter byIds(String... ids) {
        Set<ResourceLocation> targets = new HashSet<>();
        for (String id : ids) {
            targets.add(RecipeModificationHandler.parseId(id));
        }
        return new RecipeFilter(info -> targets.contains(info.id()));
    }

    /**
     * Creates a filter that matches recipes from a specific mod.
     *
     * @param modId Mod identifier
     * @return A new RecipeFilter instance
     */
    public static RecipeFilter byMod(String modId) {
        return byMod(modId, false);
    }

    /**
     * Creates a filter that matches recipes from a specific mod with blacklist option.
     *
     * @param modId     Mod identifier
     * @param blacklist If true, matches all recipes except those from the specified mod
     * @return A new RecipeFilter instance
     */
    public static RecipeFilter byMod(String modId, boolean blacklist) {
        Predicate<RecipeInfo> predicate = info -> info.modId().equals(modId);
        return new RecipeFilter(blacklist ? predicate.negate() : predicate);
    }

    /**
     * Creates a filter that matches recipes from multiple mods.
     *
     * @param modIds Mod identifiers
     * @return A new RecipeFilter instance
     */
    public static RecipeFilter byMods(String... modIds) {
        Set<String> targets = new HashSet<>(Arrays.asList(modIds));
        return new RecipeFilter(info -> targets.contains(info.modId()));
    }

    /**
     * Creates a filter that matches recipes of a specific type.
     *
     * @param typeId Recipe type identifier
     * @return A new RecipeFilter instance
     */
    public static RecipeFilter byType(String typeId) {
        return byType(typeId, false);
    }

    /**
     * Creates a filter that matches recipes of a specific type with blacklist option.
     *
     * @param typeId    Recipe type identifier
     * @param blacklist If true, matches all recipes except those of the specified type
     * @return A new RecipeFilter instance
     */
    public static RecipeFilter byType(String typeId, boolean blacklist) {
        ResourceLocation target = RecipeModificationHandler.parseId(typeId);
        Predicate<RecipeInfo> predicate = info -> target.equals(info.typeId());
        return new RecipeFilter(blacklist ? predicate.negate() : predicate);
    }

    /**
     * Creates a filter that matches recipes of multiple types.
     *
     * @param typeIds Recipe type identifiers
     * @return A new RecipeFilter instance
     */
    public static RecipeFilter byTypes(String... typeIds) {
        Set<ResourceLocation> targets = new HashSet<>();
        for (String typeId : typeIds) {
            targets.add(RecipeModificationHandler.parseId(typeId));
        }
        return new RecipeFilter(info -> info.typeId() != null && targets.contains(info.typeId()));
    }

    /**
     * Creates a filter with a custom predicate.
     *
     * @param predicate The filtering predicate
     * @return A new RecipeFilter instance
     */
    public static RecipeFilter byCustom(Predicate<RecipeInfo> predicate) {
        return new RecipeFilter(predicate);
    }

    // ================================================================
    // Fluent API Methods
    // ================================================================

    /**
     * Adds an input replacement for recipes matched by this filter.
     * <p>
     * Applies to {@code ingredient}/{@code ingredients} entries.
     * If multiple filters replace the same item, the last registered wins.
     * </p>
     *
     * @param oldItem The item ID to replace (e.g., "minecraft:iron_ingot")
     * @param newItem The replacement item ID
     * @return This filter instance for method chaining
     */
    public RecipeFilter replaceInput(String oldItem, String newItem) {
        ensureNotExecuted();
        inputReplacements.put(oldItem, newItem);
        return this;
    }

    /**
     * Adds an output replacement for recipes matched by this filter.
     * <p>
     * Applies to {@code result}/{@code results} entries.
     * Counts and chances are preserved.
     * </p>
     *
     * @param oldItem The item ID to replace
     * @param newItem The replacement item ID
     * @return This filter instance for method chaining
     */
    public RecipeFilter replaceOutput(String oldItem, String newItem) {
        ensureNotExecuted();
        outputReplacements.put(oldItem, newItem);
        return this;
    }

    /**
     * Registers a JSON transformer for recipes matched by this filter.
     * <p>
     * The transformer receives the raw recipe JSON before re-parsing.
     * Can modify any field (e.g., {@code processingTime}, {@code results}[n].{@code count}).
     * </p>
     *
     * @param transformer JSON editor (must not replace the object itself)
     * @return This filter instance for method chaining
     */
    public RecipeFilter modify(Consumer<JsonObject> transformer) {
        ensureNotExecuted();
        transformers.add(Objects.requireNonNull(transformer, "Transformer cannot be null"));
        return this;
    }

    /**
     * Registers a pre-transformer that runs before standard transformations.
     *
     * @param transformer JSON editor
     * @return This filter instance for method chaining
     */
    public RecipeFilter preModify(Consumer<JsonObject> transformer) {
        ensureNotExecuted();
        preTransformers.add(Objects.requireNonNull(transformer, "Transformer cannot be null"));
        return this;
    }

    /**
     * Registers a post-transformer that runs after standard transformations.
     *
     * @param transformer JSON editor
     * @return This filter instance for method chaining
     */
    public RecipeFilter postModify(Consumer<JsonObject> transformer) {
        ensureNotExecuted();
        postTransformers.add(Objects.requireNonNull(transformer, "Transformer cannot be null"));
        return this;
    }

    /**
     * Marks all recipes matched by this filter for removal.
     * <p>
     * This is a terminal operation. The filter cannot be modified after this call.
     * </p>
     */
    public void remove() {
        ensureNotExecuted();
        this.shouldRemove = true;
        this.executed = true;
        // Notify handler that this filter is complete
        RecipeModificationHandler.complete(this);
    }

    /**
     * Executes all registered transformations for recipes matched by this filter.
     * <p>
     * This is a terminal operation. The filter cannot be modified after this call.
     * </p>
     */
    public void execute() {
        ensureNotExecuted();
        this.executed = true;
        // The actual execution is performed by RecipeModificationHandler
        // This just marks the filter as ready for processing
    }

    // ================================================================
    // Internal Methods (Package-Private)
    // ================================================================

    /**
     * Tests if a recipe matches this filter.
     *
     * @param info The recipe info to test
     * @return {@code true} if the recipe matches
     */
    boolean matches(RecipeInfo info) {
        return predicate.test(info);
    }

    /**
     * Checks if this filter should remove matched recipes.
     *
     * @return {@code true} if matched recipes should be removed
     */
    boolean shouldRemove() {
        return shouldRemove;
    }

    /**
     * Checks if this filter has been executed.
     *
     * @return {@code true} if the filter has been executed
     */
    boolean isExecuted() {
        return executed;
    }

    /**
     * Collects all transformations from this filter.
     *
     * @param inputs           Map to collect input replacements
     * @param outputs          Map to collect output replacements
     * @param transformerList  List to collect transformers
     */
    void collect(Map<String, String> inputs, Map<String, String> outputs,
                 List<Consumer<JsonObject>> transformerList) {
        if (!shouldRemove) {
            inputs.putAll(inputReplacements);
            outputs.putAll(outputReplacements);
            transformerList.addAll(preTransformers);
            transformerList.addAll(transformers);
            transformerList.addAll(postTransformers);
        }
    }

    /**
     * Gets the collection of input replacements.
     *
     * @return An unmodifiable view of the input replacements map
     */
    public Map<String, String> getInputReplacements() {
        return Collections.unmodifiableMap(inputReplacements);
    }

    /**
     * Gets the collection of output replacements.
     *
     * @return An unmodifiable view of the output replacements map
     */
    public Map<String, String> getOutputReplacements() {
        return Collections.unmodifiableMap(outputReplacements);
    }

    /**
     * Gets the count of registered transformers.
     *
     * @return The number of transformers
     */
    public int getTransformerCount() {
        return preTransformers.size() + transformers.size() + postTransformers.size();
    }

    // ================================================================
    // Builder Pattern for Complex Filters
    // ================================================================

    /**
     * Creates a new filter builder for complex filter construction.
     *
     * @return A new FilterBuilder instance
     */
    public static FilterBuilder builder() {
        return new FilterBuilder();
    }

    /**
     * Filter Builder - enables construction of complex filters with multiple conditions.
     */
    public static final class FilterBuilder {
        private final List<Predicate<RecipeInfo>> predicates = new ArrayList<>();
        private final List<String> includeIds = new ArrayList<>();
        private final List<String> excludeIds = new ArrayList<>();
        private final List<String> includeMods = new ArrayList<>();
        private final List<String> excludeMods = new ArrayList<>();
        private final List<String> includeTypes = new ArrayList<>();
        private final List<String> excludeTypes = new ArrayList<>();

        /**
         * Includes a specific recipe ID.
         *
         * @param id The recipe ID to include
         * @return This builder instance
         */
        public FilterBuilder includeId(String id) {
            includeIds.add(id);
            return this;
        }

        /**
         * Excludes a specific recipe ID.
         *
         * @param id The recipe ID to exclude
         * @return This builder instance
         */
        public FilterBuilder excludeId(String id) {
            excludeIds.add(id);
            return this;
        }

        /**
         * Includes recipes from a specific mod.
         *
         * @param modId The mod ID to include
         * @return This builder instance
         */
        public FilterBuilder includeMod(String modId) {
            includeMods.add(modId);
            return this;
        }

        /**
         * Excludes recipes from a specific mod.
         *
         * @param modId The mod ID to exclude
         * @return This builder instance
         */
        public FilterBuilder excludeMod(String modId) {
            excludeMods.add(modId);
            return this;
        }

        /**
         * Includes recipes of a specific type.
         *
         * @param typeId The recipe type to include
         * @return This builder instance
         */
        public FilterBuilder includeType(String typeId) {
            includeTypes.add(typeId);
            return this;
        }

        /**
         * Excludes recipes of a specific type.
         *
         * @param typeId The recipe type to exclude
         * @return This builder instance
         */
        public FilterBuilder excludeType(String typeId) {
            excludeTypes.add(typeId);
            return this;
        }

        /**
         * Adds a custom predicate condition.
         *
         * @param predicate The predicate to add
         * @return This builder instance
         */
        public FilterBuilder withPredicate(Predicate<RecipeInfo> predicate) {
            predicates.add(predicate);
            return this;
        }

        /**
         * Builds the filter from the configured conditions.
         * <p>
         * All conditions are combined using logical AND.
         * </p>
         *
         * @return A new RecipeFilter instance
         */
        public RecipeFilter build() {
            List<Predicate<RecipeInfo>> allPredicates = new ArrayList<>();

            // ID inclusion/exclusion
            if (!includeIds.isEmpty()) {
                Set<ResourceLocation> includeSet = new HashSet<>();
                for (String id : includeIds) {
                    includeSet.add(RecipeModificationHandler.parseId(id));
                }
                allPredicates.add(info -> includeSet.contains(info.id()));
            }

            if (!excludeIds.isEmpty()) {
                Set<ResourceLocation> excludeSet = new HashSet<>();
                for (String id : excludeIds) {
                    excludeSet.add(RecipeModificationHandler.parseId(id));
                }
                allPredicates.add(info -> !excludeSet.contains(info.id()));
            }

            // Mod inclusion/exclusion
            if (!includeMods.isEmpty()) {
                Set<String> includeSet = new HashSet<>(includeMods);
                allPredicates.add(info -> includeSet.contains(info.modId()));
            }

            if (!excludeMods.isEmpty()) {
                Set<String> excludeSet = new HashSet<>(excludeMods);
                allPredicates.add(info -> !excludeSet.contains(info.modId()));
            }

            // Type inclusion/exclusion
            if (!includeTypes.isEmpty()) {
                Set<ResourceLocation> includeSet = new HashSet<>();
                for (String typeId : includeTypes) {
                    includeSet.add(RecipeModificationHandler.parseId(typeId));
                }
                allPredicates.add(info -> info.typeId() != null && includeSet.contains(info.typeId()));
            }

            if (!excludeTypes.isEmpty()) {
                Set<ResourceLocation> excludeSet = new HashSet<>();
                for (String typeId : excludeTypes) {
                    excludeSet.add(RecipeModificationHandler.parseId(typeId));
                }
                allPredicates.add(info -> info.typeId() == null || !excludeSet.contains(info.typeId()));
            }

            // Add custom predicates
            allPredicates.addAll(predicates);

            // Combine all predicates with AND
            Predicate<RecipeInfo> finalPredicate = allPredicates.stream()
                    .reduce(Predicate::and)
                    .orElse(info -> true);

            return new RecipeFilter(finalPredicate);
        }
    }

    // ================================================================
    // Validation Methods
    // ================================================================

    /**
     * Ensures the filter hasn't been executed yet.
     *
     * @throws IllegalStateException if the filter has already been executed
     */
    private void ensureNotExecuted() {
        if (executed) {
            throw new IllegalStateException(
                    "This RecipeFilter has already been executed and cannot be modified. " +
                            "Create a new filter for additional changes."
            );
        }
    }

    // ================================================================
    // Object Overrides
    // ================================================================

    @Override
    public String toString() {
        return String.format("RecipeFilter{inputReplacements=%d, outputReplacements=%d, transformers=%d, remove=%s}",
                inputReplacements.size(), outputReplacements.size(), getTransformerCount(), shouldRemove);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (!(obj instanceof RecipeFilter that)) return false;
        return shouldRemove == that.shouldRemove &&
                Objects.equals(inputReplacements, that.inputReplacements) &&
                Objects.equals(outputReplacements, that.outputReplacements);
    }

    @Override
    public int hashCode() {
        return Objects.hash(inputReplacements, outputReplacements, shouldRemove);
    }
}