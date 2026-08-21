package com.onehumanawa.cnmcore.foundation.recipe;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.onehumanawa.cnmcore.foundation.data.recipe.KubeJavaDatagenSupport;
import com.onehumanawa.cnmcore.foundation.fluid.FluidSpec;
import com.onehumanawa.cnmcore.foundation.item.ItemSpec;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Predicate;

/**
 * KubeJava Recipe Modifier - modpack recipe configuration entry point.
 * <p>
 * Supports:
 * <ul>
 *   <li>Item ingredients: {@link #itemOf(String)}</li>
 *   <li>Item tags: {@link #itemTagOf(String)}</li>
 *   <li>Fluid ingredients: {@link #fluidOf(String, int)}</li>
 * </ul>
 * <p>
 * Recipe types:
 * <ul>
 *   <li>Vanilla: Shaped, Shapeless, Smelting, Blasting, Smoking, Campfire, Stonecutting</li>
 *   <li>Create: Mechanical Crafting, Mixing, Milling, Crushing, Splashing, Haunting, Compacting, Pressing, Filling, Emptying</li>
 * </ul>
 */
@SuppressWarnings("unused")
public final class KubeJavaRecipeModifier {

    private KubeJavaRecipeModifier() {}

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
                "create:crafting/materials/andesite_alloy_from_zinc",
                "create:sequenced_assembly/sturdy_sheet",
                "create_connected:sequenced_assembly/control_chip",
                "vintageimprovements:sequenced_assembly/recipe_card",
                "vintageimprovements:sequenced_assembly/redstone_module"
        ).remove();
        addShapeless(
                "crafting_shapeless/redprint",
                new String[]{"create:schematic_and_quill", "minecraft:red_dye"},
                "cnmcore:redprint"
        );
    }

    // ============================
    // Fluid reference
    // ============================

    /**
     * Creates a fluid reference for recipes.
     *
     * @param id     fluid id (e.g. "minecraft:water", "create:chocolate")
     * @param amount amount in millibuckets
     * @return a FluidSpec
     */
    public static FluidSpec fluidOf(String id, int amount) {
        return FluidSpec.of(id, amount);
    }

    // ============================
    // Item reference
    // ============================

    public static String itemOf(String id) {
        return ItemSpec.of(id, null);
    }

    public static String itemOf(String id, String dataComponents) {
        return ItemSpec.of(id, dataComponents);
    }

    public static String itemTagOf(String tagId) {
        return "#" + (tagId.contains(":") ? tagId : "minecraft:" + tagId);
    }

    // ============================
    // Create Mixing (with fluid support)
    // ============================

    public static void addCreateMixing(String id, String[] itemInputs, FluidSpec[] fluidInputs,
                                       String[] itemOutputs, FluidSpec[] fluidOutputs) {
        addCreateMixing(id, itemInputs, fluidInputs, itemOutputs, fluidOutputs, -1, null);
    }

    public static void addCreateMixing(String id, String[] itemInputs, FluidSpec[] fluidInputs,
                                       String[] itemOutputs, FluidSpec[] fluidOutputs,
                                       int processingTime, String heatRequirement) {
        JsonObject json = new JsonObject();
        json.addProperty("type", "create:mixing");

        JsonArray ingredients = new JsonArray();
        for (String spec : itemInputs) {
            if (spec != null && !spec.isEmpty()) {
                ingredients.add(ingredient(spec));
            }
        }
        for (FluidSpec fluid : fluidInputs) {
            if (fluid != null && !fluid.isEmpty()) {
                ingredients.add(fluid.toJson());
            }
        }
        json.add("ingredients", ingredients);

        JsonArray results = new JsonArray();
        for (String spec : itemOutputs) {
            if (spec != null && !spec.isEmpty()) {
                results.add(itemStack(spec, 1));
            }
        }
        for (FluidSpec fluid : fluidOutputs) {
            if (fluid != null && !fluid.isEmpty()) {
                results.add(fluid.toJson());
            }
        }
        json.add("results", results);

        if (processingTime >= 0) json.addProperty("processingTime", processingTime);
        if (heatRequirement != null && !heatRequirement.isEmpty()) {
            json.addProperty("heatRequirement", heatRequirement);
        }

        addRecipe(id, json);
    }

    // Overloaded: no fluids
    public static void addCreateMixing(String id, String[] itemInputs, String[] itemOutputs) {
        addCreateMixing(id, itemInputs, new FluidSpec[0], itemOutputs, new FluidSpec[0]);
    }

    public static void addCreateMixing(String id, String[] itemInputs, String[] itemOutputs,
                                       int processingTime, String heatRequirement) {
        addCreateMixing(id, itemInputs, new FluidSpec[0], itemOutputs, new FluidSpec[0],
                processingTime, heatRequirement);
    }

    // ============================
    // Create Processing (generic, with fluid support)
    // ============================

    public static void addCreateProcessing(String type, String id, String[] itemInputs, FluidSpec[] fluidInputs,
                                           String[] itemOutputs, FluidSpec[] fluidOutputs) {
        addCreateProcessing(type, id, itemInputs, fluidInputs, itemOutputs, fluidOutputs, -1, null);
    }

    public static void addCreateProcessing(String type, String id, String[] itemInputs, FluidSpec[] fluidInputs,
                                           String[] itemOutputs, FluidSpec[] fluidOutputs,
                                           int processingTime, String heatRequirement) {
        JsonObject json = new JsonObject();
        json.addProperty("type", type);

        JsonArray ingredients = new JsonArray();
        for (String spec : itemInputs) {
            if (spec != null && !spec.isEmpty()) {
                ingredients.add(ingredient(spec));
            }
        }
        for (FluidSpec fluid : fluidInputs) {
            if (fluid != null && !fluid.isEmpty()) {
                ingredients.add(fluid.toJson());
            }
        }
        json.add("ingredients", ingredients);

        JsonArray results = new JsonArray();
        for (String spec : itemOutputs) {
            if (spec != null && !spec.isEmpty()) {
                JsonObject result = new JsonObject();
                result.addProperty("item", spec);
                results.add(result);
            }
        }
        for (FluidSpec fluid : fluidOutputs) {
            if (fluid != null && !fluid.isEmpty()) {
                results.add(fluid.toJson());
            }
        }
        json.add("results", results);

        if (processingTime >= 0) json.addProperty("processingTime", processingTime);
        if (heatRequirement != null && !heatRequirement.isEmpty()) {
            json.addProperty("heatRequirement", heatRequirement);
        }

        addRecipe(id, json);
    }

    // Overloaded: no fluids
    public static void addCreateProcessing(String type, String id, String[] itemInputs, String[] itemOutputs) {
        addCreateProcessing(type, id, itemInputs, new FluidSpec[0], itemOutputs, new FluidSpec[0]);
    }

    // ============================
    // Create Filling (item + fluid -> item)
    // ============================

    public static void addCreateFilling(String id, String itemInput, FluidSpec fluidInput, String itemOutput) {
        JsonObject json = new JsonObject();
        json.addProperty("type", "create:filling");
        json.add("ingredients", ingredient(itemInput));
        json.add("fluid", fluidInput.toJson());
        json.add("result", itemStack(itemOutput, 1));
        addRecipe(id, json);
    }

    // ============================
    // Create Emptying (item -> item + fluid)
    // ============================

    public static void addCreateEmptying(String id, String itemInput, String itemOutput, FluidSpec fluidOutput) {
        JsonObject json = new JsonObject();
        json.addProperty("type", "create:emptying");
        json.add("ingredients", ingredient(itemInput));
        json.add("result", itemStack(itemOutput, 1));
        json.add("fluid", fluidOutput.toJson());
        addRecipe(id, json);
    }

    // ============================
    // Create Compacting (items + fluids -> items + fluids)
    // ============================

    public static void addCreateCompacting(String id, String[] itemInputs, FluidSpec[] fluidInputs,
                                           String[] itemOutputs, FluidSpec[] fluidOutputs) {
        addCreateProcessing("create:compacting", id, itemInputs, fluidInputs, itemOutputs, fluidOutputs);
    }

    // Overloaded: no fluids
    public static void addCreateCompacting(String id, String[] itemInputs, String[] itemOutputs) {
        addCreateProcessing("create:compacting", id, itemInputs, new FluidSpec[0], itemOutputs, new FluidSpec[0]);
    }

    // ============================
    // Create Pressing (item -> item)
    // ============================

    public static void addCreatePressing(String id, String itemInput, String itemOutput) {
        JsonObject json = new JsonObject();
        json.addProperty("type", "create:pressing");
        json.add("ingredients", ingredient(itemInput));
        json.add("result", itemStack(itemOutput, 1));
        addRecipe(id, json);
    }

    // ============================
    // Create Milling (item -> item)
    // ============================

    public static void addCreateMilling(String id, String itemInput, String itemOutput) {
        addCreateProcessing("create:milling", id, new String[]{itemInput}, new FluidSpec[0],
                new String[]{itemOutput}, new FluidSpec[0]);
    }

    // ============================
    // Create Crushing (item -> item(s))
    // ============================

    public static void addCreateCrushing(String id, String itemInput, String[] itemOutputs) {
        addCreateProcessing("create:crushing", id, new String[]{itemInput}, new FluidSpec[0],
                itemOutputs, new FluidSpec[0]);
    }

    // ============================
    // Create Splashing / Washing (item -> item(s))
    // ============================

    public static void addCreateSplashing(String id, String itemInput, String[] itemOutputs) {
        addCreateProcessing("create:splashing", id, new String[]{itemInput}, new FluidSpec[0],
                itemOutputs, new FluidSpec[0]);
    }

    // ============================
    // Create Haunting (item -> item)
    // ============================

    public static void addCreateHaunting(String id, String itemInput, String itemOutput) {
        addCreateProcessing("create:haunting", id, new String[]{itemInput}, new FluidSpec[0],
                new String[]{itemOutput}, new FluidSpec[0]);
    }

    // ============================
    // Vanilla Crafting
    // ============================

    public static void addShaped(String id, String[] pattern, Map<String, String> key, String result) {
        addShaped(id, pattern, key, result, 1);
    }

    public static void addShaped(String id, String[] pattern, Map<String, String> key, String result, int count) {
        JsonObject json = new JsonObject();
        json.addProperty("type", "minecraft:crafting_shaped");
        JsonArray patternArray = new JsonArray();
        for (String row : pattern) patternArray.add(row);
        json.add("pattern", patternArray);
        JsonObject keyObject = new JsonObject();
        key.forEach((symbol, spec) -> keyObject.add(symbol, ingredient(spec)));
        json.add("key", keyObject);
        json.add("result", itemStack(result, count));
        addRecipe(id, json);
    }

    public static void addShapeless(String id, String[] ingredients, String result) {
        addShapeless(id, ingredients, result, 1);
    }

    public static void addShapeless(String id, String[] ingredients, String result, int count) {
        JsonObject json = new JsonObject();
        json.addProperty("type", "minecraft:crafting_shapeless");
        JsonArray array = new JsonArray();
        for (String spec : ingredients) {
            if (spec != null && !spec.isEmpty()) {
                array.add(ingredient(spec));
            }
        }
        json.add("ingredients", array);
        json.add("result", itemStack(result, count));
        addRecipe(id, json);
    }

    // ============================
    // Vanilla Cooking
    // ============================

    public static void addSmelting(String id, String input, String result) {
        addSmelting(id, input, result, 0.35F, 200);
    }

    public static void addSmelting(String id, String input, String result, float experience, int cookingTime) {
        addCooking("minecraft:smelting", id, input, result, experience, cookingTime);
    }

    public static void addBlasting(String id, String input, String result) {
        addBlasting(id, input, result, 1.0F, 100);
    }

    public static void addBlasting(String id, String input, String result, float experience, int cookingTime) {
        addCooking("minecraft:blasting", id, input, result, experience, cookingTime);
    }

    public static void addSmoking(String id, String input, String result) {
        addSmoking(id, input, result, 0.35F, 100);
    }

    public static void addSmoking(String id, String input, String result, float experience, int cookingTime) {
        addCooking("minecraft:smoking", id, input, result, experience, cookingTime);
    }

    public static void addCampfireCooking(String id, String input, String result) {
        addCampfireCooking(id, input, result, 0.35F, 600);
    }

    public static void addCampfireCooking(String id, String input, String result, float experience, int cookingTime) {
        addCooking("minecraft:campfire_cooking", id, input, result, experience, cookingTime);
    }

    private static void addCooking(String type, String id, String input, String result, float experience, int cookingTime) {
        JsonObject json = new JsonObject();
        json.addProperty("type", type);
        json.add("ingredient", ingredient(input));
        json.add("result", itemStack(result, 1));
        json.addProperty("experience", experience);
        json.addProperty("cookingtime", cookingTime);
        addRecipe(id, json);
    }

    // ============================
    // Vanilla Stonecutting
    // ============================

    public static void addStonecutting(String id, String input, String result) {
        addStonecutting(id, input, result, 1);
    }

    public static void addStonecutting(String id, String input, String result, int count) {
        JsonObject json = new JsonObject();
        json.addProperty("type", "minecraft:stonecutting");
        json.add("ingredient", ingredient(input));
        json.add("result", itemStack(result, count));
        addRecipe(id, json);
    }

    // ============================
    // Create Mechanical Crafting
    // ============================

    public static void addMechanicalCrafting(String id, String[] pattern, Map<String, String> key, String result) {
        addMechanicalCrafting(id, pattern, key, result, 1);
    }

    public static void addMechanicalCrafting(String id, String[] pattern, Map<String, String> key, String result, int count) {
        JsonObject json = new JsonObject();
        json.addProperty("type", "create:mechanical_crafting");
        JsonArray patternArray = new JsonArray();
        for (String row : pattern) patternArray.add(row);
        json.add("pattern", patternArray);
        JsonObject keyObject = new JsonObject();
        key.forEach((symbol, spec) -> keyObject.add(symbol, ingredient(spec)));
        json.add("key", keyObject);
        json.add("result", itemStack(result, count));
        addRecipe(id, json);
    }

    // ============================
    // Recipe addition (core)
    // ============================

    public static void addRecipe(String id, JsonObject json) {
        KubeJavaDatagenSupport.addRecipe(id, json);
    }

    public static void addRecipe(String id, String jsonText) {
        addRecipe(id, JsonParser.parseString(jsonText).getAsJsonObject());
    }

    // ============================
    // JSON builders (internal)
    // ============================

    private static JsonObject ingredient(String spec) {
        if (spec.startsWith("#")) {
            JsonObject json = new JsonObject();
            json.addProperty("tag", spec.substring(1));
            return json;
        }
        return ItemSpec.ingredientJson(spec);
    }

    private static JsonObject itemStack(String item, int count) {
        return ItemSpec.stackJson(item, count);
    }

    // ============================
    // Recipe replacement / removal
    // ============================

    public static void replaceInput(String oldItem, String newItem) {
        all().replaceInput(oldItem, newItem);
    }

    public static void replaceOutput(String oldItem, String newItem) {
        all().replaceOutput(oldItem, newItem);
    }

    public static void removeRecipe(String recipeId) {
        byId(recipeId).remove();
    }

    // ============================
    // Filter factories
    // ============================

    public static RecipeFilter all() {
        return new RecipeFilter(info -> true);
    }

    public static RecipeFilter byId(String id) {
        return byId(id, false);
    }

    public static RecipeFilter byId(String id, boolean blacklist) {
        var target = RecipeModificationHandler.parseId(id);
        Predicate<RecipeFilter.RecipeInfo> predicate = info -> info.id().equals(target);
        return new RecipeFilter(blacklist ? predicate.negate() : predicate);
    }

    public static RecipeFilter byIds(String... ids) {
        Set<String> targets = new HashSet<>(Arrays.asList(ids));
        return new RecipeFilter(info -> targets.contains(info.id().toString()));
    }

    public static RecipeFilter byMod(String modId) {
        return byMod(modId, false);
    }

    public static RecipeFilter byMod(String modId, boolean blacklist) {
        Predicate<RecipeFilter.RecipeInfo> predicate = info -> info.modId().equals(modId);
        return new RecipeFilter(blacklist ? predicate.negate() : predicate);
    }

    public static RecipeFilter byMods(String... modIds) {
        Set<String> targets = new HashSet<>(Arrays.asList(modIds));
        return new RecipeFilter(info -> targets.contains(info.modId()));
    }

    public static RecipeFilter byType(String typeId) {
        return byType(typeId, false);
    }

    public static RecipeFilter byType(String typeId, boolean blacklist) {
        var target = RecipeModificationHandler.parseId(typeId);
        Predicate<RecipeFilter.RecipeInfo> predicate = info -> target.equals(info.typeId());
        return new RecipeFilter(blacklist ? predicate.negate() : predicate);
    }

    public static RecipeFilter byTypes(String... typeIds) {
        Set<String> targets = new HashSet<>(Arrays.asList(typeIds));
        return new RecipeFilter(info -> info.typeId() != null && targets.contains(info.typeId().toString()));
    }

    public static RecipeFilter byCustom(Predicate<RecipeFilter.RecipeInfo> predicate) {
        return new RecipeFilter(predicate);
    }
}