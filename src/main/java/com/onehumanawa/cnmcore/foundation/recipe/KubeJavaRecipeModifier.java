package com.onehumanawa.cnmcore.foundation.recipe;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Predicate;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import com.onehumanawa.cnmcore.foundation.data.recipe.KubeJavaDatagenSupport;

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
 * <h2>Recipe addition</h2>
 * {@code addRecipe} injects raw JSON (any registered type), {@code addShaped}/
 * {@code addShapeless} cover the crafting table, {@code addSmelting}/
 * {@code addBlasting}/{@code addSmoking}/{@code addCampfireCooking}/
 * {@code addStonecutting} cover vanilla heat and cutting recipes,
 * {@code addCreateMixing}/{@code addCreateProcessing}/{@code addMechanicalCrafting}
 * cover Create machines, and {@code addBrewing} covers the brewing stand.
 * Added recipes persist across {@code /reload} and sync to clients; an added
 * recipe whose id already exists overrides the original.
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
@SuppressWarnings({"unused"})
public final class KubeJavaRecipeModifier {

    private KubeJavaRecipeModifier() {
    }

    /**
     * Configuration entry point, invoked once when
     * {@link RecipeModificationHandler} loads. Declare all recipe changes here.
     */
    public static void init() {
        byIds(
                "create_connected:crafting/kinetics/kinetic_battery",
                "create:sequenced_assembly/precision_mechanism",
                "create:mixing/andesite_alloy",
                "create:mixing/andesite_alloy_from_zinc",
                "create:crafting/materials/andesite_alloy",
                "create:crafting/materials/andesite_alloy_from_zinc"
        ).remove();
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
    // Recipe addition (backed by KubeJavaDatagenSupport)
    // ------------------------------------------------------------------

    /**
     * Adds a fully custom recipe with raw JSON. The JSON must contain a valid
     * {@code "type"} field; any registered recipe type of the modpack works,
     * vanilla or modded. Recipes are injected on server start and
     * {@code /reload}, and synced to clients automatically.
     *
     * @param id   recipe id; without a namespace it defaults to {@code cnmcore}
     * @param json full recipe JSON object
     */
    public static void addRecipe(String id, JsonObject json) {
        KubeJavaDatagenSupport.addRecipe(id, json);
    }

    /**
     * Adds a fully custom recipe with raw JSON text.
     *
     * @param id       recipe id
     * @param jsonText recipe JSON, e.g. {@code "{\"type\":\"create:mixing\",...}"}
     * @see #addRecipe(String, JsonObject)
     */
    public static void addRecipe(String id, String jsonText) {
        addRecipe(id, JsonParser.parseString(jsonText).getAsJsonObject());
    }

    /**
     * Adds a 3x3 crafting table recipe with a pattern.
     *
     * @param id      recipe id
     * @param pattern rows, e.g. {@code {"AAA", " B ", " B "}}; space = empty slot
     * @param key     single-letter symbol to ingredient spec, e.g.
     *                {@code Map.of("A", "minecraft:iron_ingot", "B", "#minecraft:planks")}
     * @param result  result item id
     */
    public static void addShaped(String id, String[] pattern, Map<String, String> key, String result) {
        addShaped(id, pattern, key, result, 1);
    }

    /**
     * Adds a 3x3 crafting table recipe with a pattern and output count.
     *
     * @see #addShaped(String, String[], Map, String)
     */
    public static void addShaped(String id, String[] pattern, Map<String, String> key, String result, int count) {
        JsonObject json = new JsonObject();
        json.addProperty("type", "minecraft:crafting_shaped");
        JsonArray patternArray = new JsonArray();
        for (String row : pattern)
            patternArray.add(row);
        json.add("pattern", patternArray);
        JsonObject keyObject = new JsonObject();
        key.forEach((symbol, spec) -> keyObject.add(symbol, ingredient(spec)));
        json.add("key", keyObject);
        json.add("result", itemStack(result, count));
        addRecipe(id, json);
    }

    /**
     * Adds a shapeless crafting table recipe.
     *
     * @param id          recipe id
     * @param ingredients ingredient specs (item ids or {@code "#modid:tag"})
     * @param result      result item id
     */
    public static void addShapeless(String id, String[] ingredients, String result) {
        addShapeless(id, ingredients, result, 1);
    }

    /**
     * Adds a shapeless crafting table recipe with output count.
     *
     * @see #addShapeless(String, String[], String)
     */
    public static void addShapeless(String id, String[] ingredients, String result, int count) {
        JsonObject json = new JsonObject();
        json.addProperty("type", "minecraft:crafting_shapeless");
        JsonArray array = new JsonArray();
        for (String spec : ingredients)
            array.add(ingredient(spec));
        json.add("ingredients", array);
        json.add("result", itemStack(result, count));
        addRecipe(id, json);
    }

    /**
     * Adds a furnace recipe (200 ticks, 0.35 XP by default).
     *
     * @param id     recipe id
     * @param input  ingredient spec
     * @param result result item id
     */
    public static void addSmelting(String id, String input, String result) {
        addSmelting(id, input, result, 0.35F, 200);
    }

    /**
     * Adds a furnace recipe with explicit experience and cooking time.
     *
     * @see #addSmelting(String, String, String)
     */
    public static void addSmelting(String id, String input, String result, float experience, int cookingTime) {
        addCooking("minecraft:smelting", id, input, result, experience, cookingTime);
    }

    /** Adds a blast furnace recipe (100 ticks, 1.0 XP by default). */
    public static void addBlasting(String id, String input, String result) {
        addBlasting(id, input, result, 1.0F, 100);
    }

    /** Adds a blast furnace recipe with explicit experience and cooking time. */
    public static void addBlasting(String id, String input, String result, float experience, int cookingTime) {
        addCooking("minecraft:blasting", id, input, result, experience, cookingTime);
    }

    /** Adds a smoker recipe (100 ticks, 0.35 XP by default). */
    public static void addSmoking(String id, String input, String result) {
        addSmoking(id, input, result, 0.35F, 100);
    }

    /** Adds a smoker recipe with explicit experience and cooking time. */
    public static void addSmoking(String id, String input, String result, float experience, int cookingTime) {
        addCooking("minecraft:smoking", id, input, result, experience, cookingTime);
    }

    /** Adds a campfire cooking recipe (600 ticks, 0.35 XP by default). */
    public static void addCampfireCooking(String id, String input, String result) {
        addCampfireCooking(id, input, result, 0.35F, 600);
    }

    /** Adds a campfire cooking recipe with explicit experience and cooking time. */
    public static void addCampfireCooking(String id, String input, String result, float experience, int cookingTime) {
        addCooking("minecraft:campfire_cooking", id, input, result, experience, cookingTime);
    }

    /** Adds a stonecutter recipe with output count 1. */
    public static void addStonecutting(String id, String input, String result) {
        addStonecutting(id, input, result, 1);
    }

    /**
     * Adds a stonecutter recipe.
     *
     * @param count output count, e.g. 2 for slabs from stone
     */
    public static void addStonecutting(String id, String input, String result, int count) {
        JsonObject json = new JsonObject();
        json.addProperty("type", "minecraft:stonecutting");
        json.add("ingredient", ingredient(input));
        json.add("result", itemStack(result, count));
        addRecipe(id, json);
    }

    /**
     * Adds a Create mechanical crafting recipe (shaped, any size up to 9x9).
     *
     * @param id      recipe id
     * @param pattern rows, e.g. {@code {"AAA", " B ", " B "}}
     * @param key     single-letter symbol to ingredient spec
     * @param result  result item id
     */
    public static void addMechanicalCrafting(String id, String[] pattern, Map<String, String> key, String result) {
        addMechanicalCrafting(id, pattern, key, result, 1);
    }

    /**
     * Adds a Create mechanical crafting recipe with output count.
     *
     * @see #addMechanicalCrafting(String, String[], Map, String)
     */
    public static void addMechanicalCrafting(String id, String[] pattern, Map<String, String> key, String result,
                                             int count) {
        JsonObject json = new JsonObject();
        json.addProperty("type", "create:mechanical_crafting");
        JsonArray patternArray = new JsonArray();
        for (String row : pattern)
            patternArray.add(row);
        json.add("pattern", patternArray);
        JsonObject keyObject = new JsonObject();
        key.forEach((symbol, spec) -> keyObject.add(symbol, ingredient(spec)));
        json.add("key", keyObject);
        json.add("result", itemStack(result, count));
        addRecipe(id, json);
    }

    /**
     * Adds a Create mixing recipe.
     *
     * @param id      recipe id
     * @param inputs  ingredient specs
     * @param outputs result item ids, count 1 each (use {@link #addRecipe} for
     *                counts, chances or fluid results)
     */
    public static void addCreateMixing(String id, String[] inputs, String[] outputs) {
        addCreateMixing(id, inputs, outputs, -1, null);
    }

    /**
     * Adds a Create mixing recipe with processing time and heat requirement.
     *
     * @param processingTime ticks, pass -1 for Create's default (100)
     * @param heatRequirement {@code "none"}, {@code "heated"} or {@code "superheated"}
     * @see #addCreateMixing(String, String[], String[])
     */
    public static void addCreateMixing(String id, String[] inputs, String[] outputs, int processingTime,
                                       String heatRequirement) {
        addCreateProcessing("create:mixing", id, inputs, outputs, processingTime, heatRequirement);
    }

    /**
     * Adds a Create bulk processing recipe of any type:
     * {@code create:milling}, {@code create:crushing}, {@code create:splashing}
     * (washing), {@code create:haunting}, {@code create:compacting},
     * {@code create:pressing}, ...
     *
     * @param type recipe type id, e.g. {@code "create:milling"}
     * @param id   recipe id
     * @param inputs  ingredient specs
     * @param outputs result item ids, count 1 each
     */
    public static void addCreateProcessing(String type, String id, String[] inputs, String[] outputs) {
        addCreateProcessing(type, id, inputs, outputs, -1, null);
    }

    /**
     * Adds a Create bulk processing recipe with processing time and heat.
     *
     * @see #addCreateProcessing(String, String, String[], String[])
     */
    public static void addCreateProcessing(String type, String id, String[] inputs, String[] outputs,
                                           int processingTime, String heatRequirement) {
        JsonObject json = new JsonObject();
        json.addProperty("type", type);
        JsonArray ingredients = new JsonArray();
        for (String spec : inputs)
            ingredients.add(ingredient(spec));
        json.add("ingredients", ingredients);
        JsonArray results = new JsonArray();
        for (String spec : outputs) {
            JsonObject result = new JsonObject();
            result.addProperty("item", spec);
            results.add(result);
        }
        json.add("results", results);
        if (processingTime >= 0)
            json.addProperty("processingTime", processingTime);
        if (heatRequirement != null)
            json.addProperty("heatRequirement", heatRequirement);
        addRecipe(id, json);
    }

    /**
     * Adds a brewing stand recipe (item-based, NeoForge extension; potion
     * output is the input's potion applied to the output container item).
     * Applied on both client and server.
     *
     * @param input      the bottle/potion being modified
     * @param ingredient the reagent item in the top slot
     * @param output     the resulting item
     */
    public static void addBrewing(String input, String ingredient, String output) {
        KubeJavaDatagenSupport.addBrewing(input, ingredient, output);
    }

    // ------------------------------------------------------------------
    // Item specs with data components
    // ------------------------------------------------------------------

    /**
     * Creates an item spec carrying data components, usable anywhere a plain
     * item id is accepted: as the replacement target of
     * {@link #replaceInput}/{@link #replaceOutput}, and as ingredient/result
     * parameters of the {@code add*} builders (including shaped {@code key}
     * values). Tag specs ({@code "#modid:tag"}) do not support components.
     * <p>
     * On inputs the spec becomes a NeoForge component ingredient (partial
     * match); on results it becomes a stack with a {@code components} field.
     *
     * @param id             item id, e.g. {@code "minecraft:diamond_sword"}
     * @param dataComponent  JSON text of the component map, e.g.
     *                       {@code "{\"minecraft:enchantments\":{\"levels\":{\"minecraft:sharpness\":5}}}"}
     * @return encoded item spec, or the plain id when {@code dataComponent}
     *         is {@code null} or blank
     */
    public static String itemOf(String id, String dataComponent) {
        return ItemSpec.of(id, dataComponent);
    }

    // ------------------------------------------------------------------
    // JSON building helpers
    // ------------------------------------------------------------------

    /** Builds a generic cooking recipe JSON (furnace / blast furnace / smoker / campfire). */
    private static void addCooking(String type, String id, String input, String result, float experience,
                                   int cookingTime) {
        JsonObject json = new JsonObject();
        json.addProperty("type", type);
        json.add("ingredient", ingredient(input));
        json.add("result", itemStack(result, 1));
        json.addProperty("experience", experience);
        json.addProperty("cookingtime", cookingTime);
        addRecipe(id, json);
    }

    /** Builds an ingredient JSON from a spec: {@code "#modid:tag"}, an item id, or an encoded spec from {@link #itemOf(String, String)}. */
    private static JsonObject ingredient(String spec) {
        if (spec.startsWith("#")) {
            JsonObject json = new JsonObject();
            json.addProperty("tag", spec.substring(1));
            return json;
        }
        return ItemSpec.ingredientJson(spec);
    }

    /** Builds an item stack JSON from a plain id or encoded spec: {@code {"id": ..., "count": n, "components": {...}}}. */
    private static JsonObject itemStack(String item, int count) {
        return ItemSpec.stackJson(item, count);
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
