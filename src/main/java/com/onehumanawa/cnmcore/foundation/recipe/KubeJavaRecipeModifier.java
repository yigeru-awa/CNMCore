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
 *   <li>Vintage Improvements: Centrifugation, Coiling, Curving, Polishing, Hammering, Laser Cutting, Pressurizing, Rolling, Turning, Vacuumizing, Leaves Vibrating</li>
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
    // Vintage Improvements: Centrifugation
    // ============================

    /**
     * Adds a Vintage Improvements centrifugation recipe.
     * Centrifugation separates ingredients into multiple outputs.
     *
     * @param id              recipe id (e.g. "vintageimprovements:centrifugation/example")
     * @param itemInputs      array of item ingredient specs
     * @param itemOutputs     array of output item specs with optional counts (format: "item" or "item*count")
     * @param processingTime  processing time in ticks
     */
    public static void addVintageCentrifugation(String id, String[] itemInputs, String[] itemOutputs, int processingTime) {
        JsonObject json = new JsonObject();
        json.addProperty("type", "vintageimprovements:centrifugation");

        JsonArray ingredients = new JsonArray();
        for (String spec : itemInputs) {
            if (spec != null && !spec.isEmpty()) {
                ingredients.add(ingredient(spec));
            }
        }
        json.add("ingredients", ingredients);

        JsonArray results = new JsonArray();
        for (String spec : itemOutputs) {
            if (spec != null && !spec.isEmpty()) {
                results.add(parseItemStack(spec));
            }
        }
        json.add("results", results);

        json.addProperty("processing_time", processingTime);
        addRecipe(id, json);
    }

    /**
     * Adds a Vintage Improvements centrifugation recipe with default processing time (1000 ticks).
     */
    public static void addVintageCentrifugation(String id, String[] itemInputs, String[] itemOutputs) {
        addVintageCentrifugation(id, itemInputs, itemOutputs, 1000);
    }

    // ============================
    // Vintage Improvements: Coiling
    // ============================

    /**
     * Adds a Vintage Improvements coiling recipe.
     * Coils a rod into a spring.
     *
     * @param id              recipe id
     * @param itemInput       rod ingredient spec (typically a tag like "#c:rods/aluminum")
     * @param itemOutput      output spring item spec
     * @param springColor     hex color code for the spring (e.g. "d0d4d4")
     * @param processingTime  processing time in ticks
     */
    public static void addVintageCoiling(String id, String itemInput, String itemOutput, String springColor, int processingTime) {
        JsonObject json = new JsonObject();
        json.addProperty("type", "vintageimprovements:coiling");
        json.addProperty("spring_color", springColor);

        JsonArray ingredients = new JsonArray();
        ingredients.add(ingredient(itemInput));
        json.add("ingredients", ingredients);

        JsonArray results = new JsonArray();
        results.add(itemStack(itemOutput, 1));
        json.add("results", results);

        json.addProperty("processing_time", processingTime);
        addRecipe(id, json);
    }

    /**
     * Adds a Vintage Improvements coiling recipe with default processing time (120 ticks).
     */
    public static void addVintageCoiling(String id, String itemInput, String itemOutput, String springColor) {
        addVintageCoiling(id, itemInput, itemOutput, springColor, 120);
    }

    // ============================
    // Vintage Improvements: Curving
    // ============================

    /**
     * Adds a Vintage Improvements curving recipe.
     * Curves an item using a head item as a mold.
     *
     * @param id            recipe id
     * @param itemInput     input ingredient spec
     * @param itemOutput    output item spec
     * @param itemAsHead    item used as the curving head/mold
     */
    public static void addVintageCurving(String id, String itemInput, String itemOutput, String itemAsHead) {
        JsonObject json = new JsonObject();
        json.addProperty("type", "vintageimprovements:curving");
        json.addProperty("item_as_head", itemAsHead);

        JsonArray ingredients = new JsonArray();
        ingredients.add(ingredient(itemInput));
        json.add("ingredients", ingredients);

        JsonArray results = new JsonArray();
        results.add(itemStack(itemOutput, 1));
        json.add("results", results);

        addRecipe(id, json);
    }

    // ============================
    // Vintage Improvements: Polishing
    // ============================

    /**
     * Adds a Vintage Improvements polishing recipe.
     * Polishes an item with speed limits.
     *
     * @param id              recipe id
     * @param itemInput       input ingredient spec
     * @param itemOutput      output item spec
     * @param processingTime  processing time in ticks
     * @param speedLimits     minimum RPM required (e.g. 1, 4, 16)
     */
    public static void addVintagePolishing(String id, String itemInput, String itemOutput, int processingTime, int speedLimits) {
        JsonObject json = new JsonObject();
        json.addProperty("type", "vintageimprovements:polishing");

        JsonArray ingredients = new JsonArray();
        ingredients.add(ingredient(itemInput));
        json.add("ingredients", ingredients);

        JsonArray results = new JsonArray();
        results.add(itemStack(itemOutput, 1));
        json.add("results", results);

        json.addProperty("processing_time", processingTime);
        json.addProperty("speed_limits", speedLimits);
        addRecipe(id, json);
    }

    /**
     * Adds a Vintage Improvements polishing recipe with default processing time (20 ticks) and speed limit (1 RPM).
     */
    public static void addVintagePolishing(String id, String itemInput, String itemOutput) {
        addVintagePolishing(id, itemInput, itemOutput, 20, 1);
    }

    // ============================
    // Vintage Improvements: Hammering
    // ============================

    /**
     * Adds a Vintage Improvements hammering recipe.
     * Hammers an ingot into a sheet.
     *
     * @param id              recipe id
     * @param itemInput       input ingredient spec (typically an ingot)
     * @param itemOutput      output item spec (typically a sheet)
     * @param hammerBlows     number of hammer blows required (e.g. 3)
     */
    public static void addVintageHammering(String id, String itemInput, String itemOutput, int hammerBlows) {
        JsonObject json = new JsonObject();
        json.addProperty("type", "vintageimprovements:hammering");
        json.addProperty("hammer_blows", hammerBlows);

        JsonArray ingredients = new JsonArray();
        ingredients.add(ingredient(itemInput));
        json.add("ingredients", ingredients);

        JsonArray results = new JsonArray();
        results.add(itemStack(itemOutput, 1));
        json.add("results", results);

        addRecipe(id, json);
    }

    // ============================
    // Vintage Improvements: Laser Cutting
    // ============================

    /**
     * Adds a Vintage Improvements laser cutting recipe.
     * Cuts an item with laser energy.
     *
     * @param id               recipe id
     * @param itemInput        input ingredient spec
     * @param itemOutputs      array of output item specs with optional counts
     * @param energy           total energy required in FE
     * @param maxChargeRate    maximum charge rate in FE/tick
     */
    public static void addVintageLaserCutting(String id, String itemInput, String[] itemOutputs, int energy, int maxChargeRate) {
        JsonObject json = new JsonObject();
        json.addProperty("type", "vintageimprovements:laser_cutting");

        JsonArray ingredients = new JsonArray();
        ingredients.add(ingredient(itemInput));
        json.add("ingredients", ingredients);

        JsonArray results = new JsonArray();
        for (String spec : itemOutputs) {
            if (spec != null && !spec.isEmpty()) {
                results.add(parseItemStack(spec));
            }
        }
        json.add("results", results);

        json.addProperty("energy", energy);
        json.addProperty("max_charge_rate", maxChargeRate);
        addRecipe(id, json);
    }

    // ============================
    // Vintage Improvements: Pressurizing
    // ============================

    /**
     * Adds a Vintage Improvements pressurizing recipe.
     * Pressurizes an item to produce a fluid output.
     *
     * @param id                     recipe id
     * @param itemInput              input ingredient spec
     * @param fluidOutputId          fluid output id (e.g. "vintageimprovements:sulfur_dioxide")
     * @param fluidOutputAmount      fluid output amount in millibuckets
     * @param heatRequirement        heat requirement: null for none, "heated" or "superheated"
     * @param processingTime         processing time in ticks
     */
    public static void addVintagePressurizing(String id, String itemInput, String fluidOutputId, int fluidOutputAmount,
                                              String heatRequirement, int processingTime) {
        JsonObject json = new JsonObject();
        json.addProperty("type", "vintageimprovements:pressurizing");

        JsonArray ingredients = new JsonArray();
        ingredients.add(ingredient(itemInput));
        json.add("ingredients", ingredients);

        json.add("results", new JsonArray());

        if (heatRequirement != null && !heatRequirement.isEmpty()) {
            json.addProperty("heat_requirement", heatRequirement);
        }

        json.addProperty("processing_time", processingTime);

        JsonObject fluidOutput = new JsonObject();
        fluidOutput.addProperty("id", fluidOutputId);
        fluidOutput.addProperty("amount", fluidOutputAmount);
        json.add("secondary_fluid_output", fluidOutput);

        addRecipe(id, json);
    }

    /**
     * Adds a Vintage Improvements pressurizing recipe with heated requirement.
     */
    public static void addVintagePressurizing(String id, String itemInput, String fluidOutputId, int fluidOutputAmount) {
        addVintagePressurizing(id, itemInput, fluidOutputId, fluidOutputAmount, "heated", 600);
    }

    // ============================
    // Vintage Improvements: Rolling (Create Addition)
    // ============================

    /**
     * Adds a Create Addition rolling recipe.
     * Rolls an ingot into rods.
     *
     * @param id              recipe id
     * @param itemInput       input ingredient spec (typically an ingot tag)
     * @param itemOutput      output rod item spec
     * @param outputCount     number of rods produced
     */
    public static void addCreateAdditionRolling(String id, String itemInput, String itemOutput, int outputCount) {
        JsonObject json = new JsonObject();
        json.addProperty("type", "createaddition:rolling");

        JsonArray ingredients = new JsonArray();
        ingredients.add(ingredient(itemInput));
        json.add("ingredients", ingredients);

        JsonArray results = new JsonArray();
        results.add(itemStack(itemOutput, outputCount));
        json.add("results", results);

        addRecipe(id, json);
    }

    /**
     * Adds a Create Addition rolling recipe with output count 2.
     */
    public static void addCreateAdditionRolling(String id, String itemInput, String itemOutput) {
        addCreateAdditionRolling(id, itemInput, itemOutput, 2);
    }

    // ============================
    // Vintage Improvements: Turning
    // ============================

    /**
     * Adds a Vintage Improvements turning recipe.
     * Turns a storage block into a curving head.
     *
     * @param id              recipe id
     * @param itemInput       input ingredient spec (typically a storage block)
     * @param itemOutput      output item spec
     * @param processingTime  processing time in ticks
     */
    public static void addVintageTurning(String id, String itemInput, String itemOutput, int processingTime) {
        JsonObject json = new JsonObject();
        json.addProperty("type", "vintageimprovements:turning");

        JsonArray ingredients = new JsonArray();
        ingredients.add(ingredient(itemInput));
        json.add("ingredients", ingredients);

        JsonArray results = new JsonArray();
        results.add(itemStack(itemOutput, 1));
        json.add("results", results);

        json.addProperty("processing_time", processingTime);
        addRecipe(id, json);
    }

    /**
     * Adds a Vintage Improvements turning recipe with default processing time (200 ticks).
     */
    public static void addVintageTurning(String id, String itemInput, String itemOutput) {
        addVintageTurning(id, itemInput, itemOutput, 200);
    }

    // ============================
    // Vintage Improvements: Vacuumizing
    // ============================

    /**
     * Adds a Vintage Improvements vacuumizing recipe.
     * Processes items in a vacuum chamber.
     *
     * @param id              recipe id
     * @param itemInputs      array of input ingredient specs
     * @param itemOutputs     array of output item specs
     * @param processingTime  processing time in ticks
     */
    public static void addVintageVacuumizing(String id, String[] itemInputs, String[] itemOutputs, int processingTime) {
        JsonObject json = new JsonObject();
        json.addProperty("type", "vintageimprovements:vacuumizing");

        JsonArray ingredients = new JsonArray();
        for (String spec : itemInputs) {
            if (spec != null && !spec.isEmpty()) {
                ingredients.add(ingredient(spec));
            }
        }
        json.add("ingredients", ingredients);

        JsonArray results = new JsonArray();
        for (String spec : itemOutputs) {
            if (spec != null && !spec.isEmpty()) {
                results.add(itemStack(spec, 1));
            }
        }
        json.add("results", results);

        json.addProperty("processing_time", processingTime);
        addRecipe(id, json);
    }

    /**
     * Adds a Vintage Improvements vacuumizing recipe with default processing time (600 ticks).
     */
    public static void addVintageVacuumizing(String id, String[] itemInputs, String[] itemOutputs) {
        addVintageVacuumizing(id, itemInputs, itemOutputs, 600);
    }

    // ============================
    // Vintage Improvements: Leaves Vibrating
    // ============================

    /**
     * Adds a Vintage Improvements leaves vibrating recipe.
     * Vibrates leaves to produce outputs (typically no item output, just drops).
     *
     * @param id              recipe id
     * @param itemInputs      array of input ingredient specs (typically a leaves tag)
     * @param processingTime  processing time in ticks
     */
    public static void addVintageLeavesVibrating(String id, String[] itemInputs, int processingTime) {
        JsonObject json = new JsonObject();
        json.addProperty("type", "vintageimprovements:leaves_vibrating");

        JsonArray ingredients = new JsonArray();
        for (String spec : itemInputs) {
            if (spec != null && !spec.isEmpty()) {
                ingredients.add(ingredient(spec));
            }
        }
        json.add("ingredients", ingredients);

        json.add("results", new JsonArray());
        json.addProperty("processing_time", processingTime);
        addRecipe(id, json);
    }

    /**
     * Adds a Vintage Improvements leaves vibrating recipe with default processing time (300 ticks).
     */
    public static void addVintageLeavesVibrating(String id, String[] itemInputs) {
        addVintageLeavesVibrating(id, itemInputs, 300);
    }

    // ============================
    // Vintage Improvements: Vibrating
    // ============================

    /**
     * Adds a Vintage Improvements vibrating recipe.
     * Vibrates items to transform them into other items.
     *
     * @param id              recipe id
     * @param itemInputs      array of input ingredient specs
     * @param itemOutputs     array of output item specs with optional counts (format: "item" or "item*count")
     * @param processingTime  processing time in ticks
     */
    public static void addVintageVibrating(String id, String[] itemInputs, String[] itemOutputs, int processingTime) {
        JsonObject json = new JsonObject();
        json.addProperty("type", "vintageimprovements:vibrating");

        JsonArray ingredients = new JsonArray();
        for (String spec : itemInputs) {
            if (spec != null && !spec.isEmpty()) {
                ingredients.add(ingredient(spec));
            }
        }
        json.add("ingredients", ingredients);

        JsonArray results = new JsonArray();
        for (String spec : itemOutputs) {
            if (spec != null && !spec.isEmpty()) {
                results.add(parseItemStack(spec));
            }
        }
        json.add("results", results);

        json.addProperty("processing_time", processingTime);
        addRecipe(id, json);
    }

    /**
     * Adds a Vintage Improvements vibrating recipe with default processing time (300 ticks).
     */
    public static void addVintageVibrating(String id, String[] itemInputs, String[] itemOutputs) {
        addVintageVibrating(id, itemInputs, itemOutputs, 300);
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

    /**
     * Parses an item stack spec in the format "item" or "item*count".
     */
    private static JsonObject parseItemStack(String spec) {
        if (spec.contains("*")) {
            String[] parts = spec.split("\\*");
            String item = parts[0];
            int count = Integer.parseInt(parts[1]);
            return itemStack(item, count);
        }
        return itemStack(spec, 1);
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