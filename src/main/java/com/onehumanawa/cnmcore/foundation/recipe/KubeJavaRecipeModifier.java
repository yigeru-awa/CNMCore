package com.onehumanawa.cnmcore.foundation.recipe;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Predicate;

import com.google.gson.JsonObject;

/**
 * ====================================================================
 * KubeJava Recipe Modifier - modpack recipe configuration entry point
 * ====================================================================
 *
 * <h2>Overview</h2>
 * Every recipe change of the modpack is declared here, inside
 * {@link #init()}. The declaration style follows three steps:
 *
 * <ol>
 *   <li><b>Select</b> recipes with a filter factory ({@code byId}, {@code byMod},
 *       {@code byType}, {@code all}, {@code byCustom})</li>
 *   <li><b>Declare</b> changes on the returned {@link RecipeFilter}
 *       ({@code replaceInput}, {@code replaceOutput}, {@code modify}, {@code remove})</li>
 *   <li>Changes are <b>applied automatically</b> on server start and after
 *       {@code /reload}, and synced to clients (crafting tables, recipe books,
 *       JEI) without any further code</li>
 * </ol>
 *
 * <h2>Filter factories</h2>
 * <table border="1">
 *   <caption>available selectors</caption>
 *   <tr><th>factory</th><th>matches</th></tr>
 *   <tr><td>{@code all()}</td><td>every recipe</td></tr>
 *   <tr><td>{@code byId(id)}</td><td>the single recipe with this id</td></tr>
 *   <tr><td>{@code byId(id, true)}</td><td>every recipe <i>except</i> this id (blacklist)</td></tr>
 *   <tr><td>{@code byIds(ids...)}</td><td>any of the given ids</td></tr>
 *   <tr><td>{@code byMod(modId)}</td><td>every recipe of a mod namespace</td></tr>
 *   <tr><td>{@code byMod(modId, true)}</td><td>every recipe <i>not</i> from this mod</td></tr>
 *   <tr><td>{@code byMods(modIds...)}</td><td>any of the given namespaces</td></tr>
 *   <tr><td>{@code byType(typeId)}</td><td>every recipe of a type, e.g. {@code "create:mixing"}</td></tr>
 *   <tr><td>{@code byType(typeId, true)}</td><td>every recipe <i>not</i> of this type</td></tr>
 *   <tr><td>{@code byTypes(typeIds...)}</td><td>any of the given types</td></tr>
 *   <tr><td>{@code byCustom(predicate)}</td><td>arbitrary {@link Predicate} on
 *       {@link RecipeFilter.RecipeInfo} (id / modId / typeId)</td></tr>
 * </table>
 *
 * <h2>Actions</h2>
 * <ul>
 *   <li>{@link RecipeFilter#replaceInput(String, String)} - swap an ingredient
 *       item in all matched recipes</li>
 *   <li>{@link RecipeFilter#replaceOutput(String, String)} - swap a result item
 *       in all matched recipes</li>
 *   <li>{@link RecipeFilter#modify(Consumer)} - edit the raw recipe JSON
 *       directly, any field</li>
 *   <li>{@link RecipeFilter#remove()} - delete all matched recipes</li>
 * </ul>
 *
 * <h2>Examples</h2>
 * <pre>{@code
 * // 1. Remove a single recipe
 * byId("create:mechanical_crafting/crushing_wheel").remove();
 *
 * // 2. Global input replacement: every recipe (vanilla + mods)
 * replaceInput("minecraft:iron_ingot", "create:iron_sheet");
 *
 * // 3. Scoped output replacement: only Create's mixing recipes
 * byType("create:mixing").replaceOutput("minecraft:gold_ingot", "create:brass_ingot");
 *
 * // 4. Per-recipe JSON edit
 * byId("create:mixing/tea").modify(json -> {
 *     json.addProperty("processingTime", 300);
 *     json.getAsJsonArray("results").get(0).getAsJsonObject().addProperty("count", 2);
 * });
 *
 * // 5. Blacklist: apply to everything except Create recipes
 * byMod("create", true).replaceInput("minecraft:cobblestone", "create:limestone");
 *
 * // 6. Custom predicate: recipes whose id path contains "dye"
 * byCustom(info -> info.id().getPath().contains("dye"))
 *         .modify(json -> json.addProperty("category", "misc"));
 * }</pre>
 *
 * <h2>Rule merging</h2>
 * A recipe may match several filters; all their replacements are merged
 * (later registrations win on conflicting item ids) and all JSON transformers
 * run in registration order. If any matching filter called {@code remove()},
 * the recipe is deleted regardless of other changes.
 */
public final class KubeJavaRecipeModifier {

    private KubeJavaRecipeModifier() {
    }

    /**
     * Configuration entry point, invoked once when
     * {@link RecipeModificationHandler} loads. Declare all recipe changes here.
     */
    public static void init() {
        byId("create:mechanical_crafting/crushing_wheel").remove();
        // replaceInput("minecraft:iron_ingot", "create:iron_sheet");
        // replaceOutput("minecraft:chest", "create:cardboard_package");
        // removeRecipe("minecraft:chest");
        // byMod("create").replaceInput("minecraft:iron_ingot", "create:iron_sheet");
        // byType("create:mixing").modify(json -> json.addProperty("processingTime", 200));
    }

    // ------------------------------------------------------------------
    // Global helpers (equivalent to all().xxx(), provided for convenience)
    // ------------------------------------------------------------------

    /**
     * Replaces an input item in <b>every</b> recipe of the modpack, inside
     * {@code ingredient}/{@code ingredients} entries.
     *
     * @param oldItem item id to replace, e.g. {@code "minecraft:iron_ingot"}
     * @param newItem replacement item id
     */
    public static void replaceInput(String oldItem, String newItem) {
        all().replaceInput(oldItem, newItem);
    }

    /**
     * Replaces an output item in <b>every</b> recipe of the modpack, inside
     * {@code result}/{@code results} entries. Counts and chances are preserved.
     *
     * @param oldItem item id to replace
     * @param newItem replacement item id
     */
    public static void replaceOutput(String oldItem, String newItem) {
        all().replaceOutput(oldItem, newItem);
    }

    /**
     * Shortcut for removing a single recipe.
     *
     * @param recipeId recipe id in "modid:path" form
     */
    public static void removeRecipe(String recipeId) {
        byId(recipeId).remove();
    }

    // ------------------------------------------------------------------
    // Filter factories
    // ------------------------------------------------------------------

    /**
     * Matches every loaded recipe.
     *
     * @return a filter applying to all recipes
     */
    public static RecipeFilter all() {
        return new RecipeFilter(info -> true);
    }

    /**
     * Matches the single recipe with the given id.
     *
     * @param id recipe id in "modid:path" form, e.g. {@code "create:mixing/tea"}
     * @return a filter for this recipe
     */
    public static RecipeFilter byId(String id) {
        return byId(id, false);
    }

    /**
     * Matches a single recipe, or - in blacklist mode - every recipe except it.
     *
     * @param id        recipe id in "modid:path" form
     * @param blacklist if {@code true}, matches every recipe whose id is <i>not</i> the given id
     * @return the filter
     */
    public static RecipeFilter byId(String id, boolean blacklist) {
        var target = RecipeModificationHandler.parseId(id);
        Predicate<RecipeFilter.RecipeInfo> predicate = info -> info.id().equals(target);
        return new RecipeFilter(blacklist ? predicate.negate() : predicate);
    }

    /**
     * Matches any recipe whose id is in the given list.
     *
     * @param ids recipe ids in "modid:path" form
     * @return the filter
     */
    public static RecipeFilter byIds(String... ids) {
        Set<String> targets = new HashSet<>(Arrays.asList(ids));
        return new RecipeFilter(info -> targets.contains(info.id().toString()));
    }

    /**
     * Matches every recipe registered under the given mod's namespace.
     *
     * @param modId namespace, e.g. {@code "create"}
     * @return the filter
     */
    public static RecipeFilter byMod(String modId) {
        return byMod(modId, false);
    }

    /**
     * Matches a mod's recipes, or - in blacklist mode - every recipe from
     * other mods and vanilla.
     *
     * @param modId     namespace, e.g. {@code "create"}
     * @param blacklist if {@code true}, matches every recipe whose namespace is <i>not</i> the given mod
     * @return the filter
     */
    public static RecipeFilter byMod(String modId, boolean blacklist) {
        Predicate<RecipeFilter.RecipeInfo> predicate = info -> info.modId().equals(modId);
        return new RecipeFilter(blacklist ? predicate.negate() : predicate);
    }

    /**
     * Matches every recipe of any of the given mods.
     *
     * @param modIds namespaces
     * @return the filter
     */
    public static RecipeFilter byMods(String... modIds) {
        Set<String> targets = new HashSet<>(Arrays.asList(modIds));
        return new RecipeFilter(info -> targets.contains(info.modId()));
    }

    /**
     * Matches every recipe of the given type.
     *
     * @param typeId registered recipe type id, e.g. {@code "create:mixing"},
     *               {@code "minecraft:crafting_shaped"}
     * @return the filter
     */
    public static RecipeFilter byType(String typeId) {
        return byType(typeId, false);
    }

    /**
     * Matches a recipe type, or - in blacklist mode - every recipe of other types.
     *
     * @param typeId    registered recipe type id
     * @param blacklist if {@code true}, matches every recipe whose type is <i>not</i> the given type
     * @return the filter
     */
    public static RecipeFilter byType(String typeId, boolean blacklist) {
        var target = RecipeModificationHandler.parseId(typeId);
        Predicate<RecipeFilter.RecipeInfo> predicate = info -> target.equals(info.typeId());
        return new RecipeFilter(blacklist ? predicate.negate() : predicate);
    }

    /**
     * Matches every recipe of any of the given types.
     *
     * @param typeIds registered recipe type ids
     * @return the filter
     */
    public static RecipeFilter byTypes(String... typeIds) {
        Set<String> targets = new HashSet<>(Arrays.asList(typeIds));
        return new RecipeFilter(info -> info.typeId() != null && targets.contains(info.typeId().toString()));
    }

    /**
     * Matches recipes through an arbitrary predicate, enabling selectors the
     * built-in factories do not cover (id path patterns, type groups, ...).
     *
     * @param predicate test applied to each {@link RecipeFilter.RecipeInfo}
     * @return the filter
     */
    public static RecipeFilter byCustom(Predicate<RecipeFilter.RecipeInfo> predicate) {
        return new RecipeFilter(predicate);
    }
}
