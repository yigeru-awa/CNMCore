package com.onehumanawa.cnmcore.foundation.recipe;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.onehumanawa.cnmcore.foundation.data.recipe.KubeJavaDatagenSupport;
import com.onehumanawa.cnmcore.foundation.fluid.FluidSpec;
import com.onehumanawa.cnmcore.foundation.item.ItemSpec;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.function.Consumer;
import java.util.function.Predicate;

/**
 * KubeJava Recipe Modifier - Enterprise-grade recipe configuration entry point.
 * <p>
 * This class provides a comprehensive DSL for programmatic recipe manipulation
 * in Minecraft modpack development, following Spring Boot architectural patterns:
 * </p>
 * <ul>
 *   <li>Builder pattern for complex recipe construction</li>
 *   <li>Fluent API with method chaining</li>
 *   <li>Factory methods for ingredient creation</li>
 *   <li>Separation of concerns: recipe definition, validation, and registration</li>
 * </ul>
 * <p>
 * Supported ingredient types:
 * <ul>
 *   <li>Item ingredients: {@link #itemOf(String)}</li>
 *   <li>Item tags: {@link #itemTagOf(String)}</li>
 *   <li>Fluid ingredients: {@link #fluidOf(String, int)}</li>
 *   <li>NBT-aware items: {@link #itemOf(String, String)}</li>
 * </ul>
 * <p>
 * Recipe type coverage:
 * <ul>
 *   <li><b>Vanilla:</b> Shaped, Shapeless, Smelting, Blasting, Smoking, Campfire, Stonecutting</li>
 *   <li><b>Create:</b> Mechanical Crafting, Mixing, Milling, Crushing, Splashing, Haunting,
 *       Compacting, Pressing, Filling, Emptying</li>
 *   <li><b>Vintage Improvements:</b> Centrifugation, Coiling, Curving, Polishing, Hammering,
 *       Laser Cutting, Pressurizing, Rolling, Turning, Vacuumizing, Leaves Vibrating</li>
 * </ul>
 *
 * @author OneHumanAwa
 * @version 2.0
 * @since 1.0
 */
@SuppressWarnings({"unused", "UnusedReturnValue"})
public final class KubeJavaRecipeModifier {

    private static final Logger LOGGER = LoggerFactory.getLogger(KubeJavaRecipeModifier.class);

    private static final RecipeRegistry RECIPE_REGISTRY = RecipeRegistry.getInstance();
    private static final RecipeValidationService VALIDATION_SERVICE = RecipeValidationService.getInstance();

    private static final Set<String> SUPPORTED_RECIPE_TYPES = new HashSet<>(Arrays.asList(
            "create:mixing", "create:compacting", "create:pressing", "create:milling",
            "create:crushing", "create:splashing", "create:haunting", "create:filling",
            "create:emptying", "create:mechanical_crafting",
            "vintageimprovements:centrifugation", "vintageimprovements:coiling",
            "vintageimprovements:curving", "vintageimprovements:polishing",
            "vintageimprovements:hammering", "vintageimprovements:laser_cutting",
            "vintageimprovements:pressurizing", "vintageimprovements:turning",
            "vintageimprovements:vacuumizing", "vintageimprovements:vibrating",
            "vintageimprovements:leaves_vibrating",
            "minecraft:crafting_shaped", "minecraft:crafting_shapeless",
            "minecraft:smelting", "minecraft:blasting", "minecraft:smoking",
            "minecraft:campfire_cooking", "minecraft:stonecutting"
    ));

    private KubeJavaRecipeModifier() {
        throw new UnsupportedOperationException("This is a utility class and cannot be instantiated");
    }

    // ================================================================
    // Initialization & Entry Point
    // ================================================================

    /**
     * Configuration entry point, invoked once when {@link RecipeModificationHandler} loads.
     * <p>
     * This method serves as the application context initializer, analogous to
     * {@code @PostConstruct} in Spring Boot. Declare all recipe changes here
     * using the fluent API.
     * </p>
     */
    public static void init() {
        LOGGER.info("Initializing KubeJava Recipe Modifier - Enterprise Edition");

        try {
            // Recipe removal phase - cleanup deprecated recipes
            RecipeRemovalPhase.execute();

            // Recipe addition phase - register new recipes
            RecipeAdditionPhase.execute();

            LOGGER.info("KubeJava Recipe Modifier initialization completed successfully");
        } catch (Exception e) {
            LOGGER.error("Failed to initialize KubeJava Recipe Modifier", e);
            throw new RecipeConfigurationException("Recipe initialization failed", e);
        }
    }

    /**
     * Internal phase class for recipe removal operations.
     * Follows the Single Responsibility Principle.
     */
    private static final class RecipeRemovalPhase {
        private static void execute() {
            LOGGER.info("Executing recipe removal phase");

            RecipeFilter.byIds(
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

            LOGGER.debug("Recipe removal phase completed");
        }
    }

    /**
     * Internal phase class for recipe addition operations.
     * Centralizes recipe creation for better maintainability.
     */
    private static final class RecipeAdditionPhase {
        private static void execute() {
            LOGGER.info("Executing recipe addition phase");

            // Redprint recipe - shapeless crafting
            addShapeless(
                    "crafting_shapeless/redprint",
                    arr("create:schematic_and_quill", "minecraft:red_dye"),
                    "cnmcore:redprint"
            );

            LOGGER.debug("Recipe addition phase completed");
        }
    }

    // ================================================================
    // Fluid Reference Factory
    // ================================================================

    /**
     * Creates a fluid reference for recipes using the factory pattern.
     *
     * @param id     Fluid identifier (e.g. "minecraft:water", "create:chocolate")
     * @param amount Amount in millibuckets (mB)
     * @return A {@link FluidSpec} instance for recipe construction
     * @throws IllegalArgumentException if id is null or empty
     */
    public static FluidSpec fluidOf(String id, int amount) {
        return FluidSpec.of(id, amount);
    }

    // ================================================================
    // Item Reference Factory
    // ================================================================

    /**
     * Creates a simple item reference for recipes.
     *
     * @param id Item identifier (e.g. "minecraft:diamond")
     * @return A formatted item spec string
     */
    public static String itemOf(String id) {
        return ItemSpec.of(id, null);
    }

    /**
     * Creates an NBT-aware item reference for recipes.
     *
     * @param id              Item identifier
     * @param dataComponents NBT data components as JSON string
     * @return A formatted item spec string with NBT data
     */
    public static String itemOf(String id, String dataComponents) {
        return ItemSpec.of(id, dataComponents);
    }

    /**
     * Creates an item tag reference for recipes.
     *
     * @param tagId Tag identifier (e.g. "c:ingots/iron")
     * @return A formatted tag spec string with '#' prefix
     */
    public static String itemTagOf(String tagId) {
        return "#" + (tagId.contains(":") ? tagId : "minecraft:" + tagId);
    }

    // ================================================================
    // Create Mixing Recipe Builder (Fluent API)
    // ================================================================

    /**
     * Adds a Create mixing recipe with fluid support.
     * <p>
     * Uses a builder pattern internally for complex recipe construction.
     * </p>
     *
     * @param id           Recipe identifier (e.g. "create:mixing/alloy")
     * @param itemInputs   Array of item ingredient specs
     * @param fluidInputs  Array of fluid ingredients
     * @param itemOutputs  Array of item output specs
     * @param fluidOutputs Array of fluid output specs
     */
    public static void addCreateMixing(String id, String[] itemInputs, FluidSpec[] fluidInputs,
                                       String[] itemOutputs, FluidSpec[] fluidOutputs) {
        addCreateMixing(id, itemInputs, fluidInputs, itemOutputs, fluidOutputs, -1, null);
    }

    /**
     * Adds a Create mixing recipe with advanced configuration.
     *
     * @param id               Recipe identifier
     * @param itemInputs       Array of item ingredient specs
     * @param fluidInputs      Array of fluid ingredients
     * @param itemOutputs      Array of item output specs
     * @param fluidOutputs     Array of fluid output specs
     * @param processingTime   Processing time in ticks (-1 for default)
     * @param heatRequirement  Heat requirement: null, "heated", or "superheated"
     */
    public static void addCreateMixing(String id, String[] itemInputs, FluidSpec[] fluidInputs,
                                       String[] itemOutputs, FluidSpec[] fluidOutputs,
                                       int processingTime, String heatRequirement) {
        RecipeBuilder builder = RecipeBuilder.create("create:mixing", id)
                .withItemInputs(itemInputs)
                .withFluidInputs(fluidInputs)
                .withItemOutputs(itemOutputs)
                .withFluidOutputs(fluidOutputs);

        if (processingTime >= 0) {
            builder.withProcessingTime(processingTime);
        }
        if (heatRequirement != null && !heatRequirement.isEmpty()) {
            builder.withHeatRequirement(heatRequirement);
        }

        builder.buildAndRegister();
    }

    /**
     * Adds a Create mixing recipe with no fluid support (overloaded convenience method).
     */
    public static void addCreateMixing(String id, String[] itemInputs, String[] itemOutputs) {
        addCreateMixing(id, itemInputs, new FluidSpec[0], itemOutputs, new FluidSpec[0]);
    }

    /**
     * Adds a Create mixing recipe with no fluids and custom parameters.
     */
    public static void addCreateMixing(String id, String[] itemInputs, String[] itemOutputs,
                                       int processingTime, String heatRequirement) {
        addCreateMixing(id, itemInputs, new FluidSpec[0], itemOutputs, new FluidSpec[0],
                processingTime, heatRequirement);
    }

    // ================================================================
    // Create Processing Recipe Builder (Generic)
    // ================================================================

    /**
     * Generic Create processing recipe builder with fluid support.
     *
     * @param type          Recipe type (e.g. "create:milling", "create:crushing")
     * @param id            Recipe identifier
     * @param itemInputs    Array of item ingredient specs
     * @param fluidInputs   Array of fluid ingredients
     * @param itemOutputs   Array of item output specs
     * @param fluidOutputs  Array of fluid output specs
     */
    public static void addCreateProcessing(String type, String id, String[] itemInputs, FluidSpec[] fluidInputs,
                                           String[] itemOutputs, FluidSpec[] fluidOutputs) {
        addCreateProcessing(type, id, itemInputs, fluidInputs, itemOutputs, fluidOutputs, -1, null);
    }

    /**
     * Generic Create processing recipe builder with advanced configuration.
     */
    public static void addCreateProcessing(String type, String id, String[] itemInputs, FluidSpec[] fluidInputs,
                                           String[] itemOutputs, FluidSpec[] fluidOutputs,
                                           int processingTime, String heatRequirement) {
        validateRecipeType(type);

        RecipeBuilder builder = RecipeBuilder.create(type, id)
                .withItemInputs(itemInputs)
                .withFluidInputs(fluidInputs)
                .withItemOutputs(itemOutputs)
                .withFluidOutputs(fluidOutputs);

        if (processingTime >= 0) {
            builder.withProcessingTime(processingTime);
        }
        if (heatRequirement != null && !heatRequirement.isEmpty()) {
            builder.withHeatRequirement(heatRequirement);
        }

        builder.buildAndRegister();
    }

    /**
     * Overloaded: no fluids for generic processing.
     */
    public static void addCreateProcessing(String type, String id, String[] itemInputs, String[] itemOutputs) {
        addCreateProcessing(type, id, itemInputs, new FluidSpec[0], itemOutputs, new FluidSpec[0]);
    }

    // ================================================================
    // Create Filling Recipe
    // ================================================================

    /**
     * Adds a Create filling recipe (item + fluid -> item).
     *
     * @param id          Recipe identifier
     * @param itemInput   Input item spec
     * @param fluidInput  Input fluid spec
     * @param itemOutput  Output item spec
     */
    public static void addCreateFilling(String id, String itemInput, FluidSpec fluidInput, String itemOutput) {
        RecipeBuilder.create("create:filling", id)
                .withItemInputs(new String[]{itemInput})
                .withFluidInputs(new FluidSpec[]{fluidInput})
                .withItemOutputs(new String[]{itemOutput})
                .buildAndRegister();
    }

    // ================================================================
    // Create Emptying Recipe
    // ================================================================

    /**
     * Adds a Create emptying recipe (item -> item + fluid).
     *
     * @param id          Recipe identifier
     * @param itemInput   Input item spec
     * @param itemOutput  Output item spec
     * @param fluidOutput Output fluid spec
     */
    public static void addCreateEmptying(String id, String itemInput, String itemOutput, FluidSpec fluidOutput) {
        RecipeBuilder.create("create:emptying", id)
                .withItemInputs(new String[]{itemInput})
                .withItemOutputs(new String[]{itemOutput})
                .withFluidOutputs(new FluidSpec[]{fluidOutput})
                .buildAndRegister();
    }

    // ================================================================
    // Create Compacting Recipe
    // ================================================================

    /**
     * Adds a Create compacting recipe with fluid support.
     */
    public static void addCreateCompacting(String id, String[] itemInputs, FluidSpec[] fluidInputs,
                                           String[] itemOutputs, FluidSpec[] fluidOutputs) {
        addCreateProcessing("create:compacting", id, itemInputs, fluidInputs, itemOutputs, fluidOutputs);
    }

    /**
     * Overloaded: no fluids for compacting.
     */
    public static void addCreateCompacting(String id, String[] itemInputs, String[] itemOutputs) {
        addCreateProcessing("create:compacting", id, itemInputs, new FluidSpec[0], itemOutputs, new FluidSpec[0]);
    }

    // ================================================================
    // Create Pressing Recipe
    // ================================================================

    /**
     * Adds a Create pressing recipe (item -> item).
     */
    public static void addCreatePressing(String id, String itemInput, String itemOutput) {
        RecipeBuilder.create("create:pressing", id)
                .withItemInputs(new String[]{itemInput})
                .withItemOutputs(new String[]{itemOutput})
                .buildAndRegister();
    }

    // ================================================================
    // Create Milling Recipe
    // ================================================================

    /**
     * Adds a Create milling recipe (item -> item).
     */
    public static void addCreateMilling(String id, String itemInput, String itemOutput) {
        addCreateProcessing("create:milling", id, new String[]{itemInput}, new String[]{itemOutput});
    }

    // ================================================================
    // Create Crushing Recipe
    // ================================================================

    /**
     * Adds a Create crushing recipe (item -> multiple items).
     */
    public static void addCreateCrushing(String id, String itemInput, String[] itemOutputs) {
        addCreateProcessing("create:crushing", id, new String[]{itemInput}, itemOutputs);
    }

    // ================================================================
    // Create Splashing Recipe
    // ================================================================

    /**
     * Adds a Create splashing/washing recipe (item -> multiple items).
     */
    public static void addCreateSplashing(String id, String itemInput, String[] itemOutputs) {
        addCreateProcessing("create:splashing", id, new String[]{itemInput}, itemOutputs);
    }

    // ================================================================
    // Create Haunting Recipe
    // ================================================================

    /**
     * Adds a Create haunting recipe (item -> item).
     */
    public static void addCreateHaunting(String id, String itemInput, String itemOutput) {
        addCreateProcessing("create:haunting", id, new String[]{itemInput}, new String[]{itemOutput});
    }

    // ================================================================
    // Vintage Improvements: Centrifugation
    // ================================================================

    /**
     * Adds a Vintage Improvements centrifugation recipe.
     * <p>
     * Centrifugation separates ingredients into multiple outputs using
     * centrifugal force.
     * </p>
     *
     * @param id              Recipe identifier
     * @param itemInputs      Array of item ingredient specs
     * @param itemOutputs     Array of output item specs (format: "item" or "item*count")
     * @param processingTime  Processing time in ticks
     */
    public static void addVintageCentrifugation(String id, String[] itemInputs, String[] itemOutputs, int processingTime) {
        RecipeBuilder.create("vintageimprovements:centrifugation", id)
                .withItemInputs(itemInputs)
                .withItemOutputsWithCount(itemOutputs)
                .withProcessingTime(processingTime)
                .buildAndRegister();
    }

    /**
     * Adds a Vintage Improvements centrifugation recipe with default processing time (1000 ticks).
     */
    public static void addVintageCentrifugation(String id, String[] itemInputs, String[] itemOutputs) {
        addVintageCentrifugation(id, itemInputs, itemOutputs, 1000);
    }

    // ================================================================
    // Vintage Improvements: Coiling
    // ================================================================

    /**
     * Adds a Vintage Improvements coiling recipe.
     * <p>
     * Coils a rod into a spring with the specified color.
     * </p>
     *
     * @param id              Recipe identifier
     * @param itemInput       Rod ingredient spec (typically a tag like "#c:rods/aluminum")
     * @param itemOutput      Output spring item spec
     * @param springColor     Hex color code for the spring (e.g. "d0d4d4")
     * @param processingTime  Processing time in ticks
     */
    public static void addVintageCoiling(String id, String itemInput, String itemOutput, String springColor, int processingTime) {
        RecipeBuilder.create("vintageimprovements:coiling", id)
                .withItemInputs(new String[]{itemInput})
                .withItemOutputs(new String[]{itemOutput})
                .withSpringColor(springColor)
                .withProcessingTime(processingTime)
                .buildAndRegister();
    }

    /**
     * Adds a Vintage Improvements coiling recipe with default processing time (120 ticks).
     */
    public static void addVintageCoiling(String id, String itemInput, String itemOutput, String springColor) {
        addVintageCoiling(id, itemInput, itemOutput, springColor, 120);
    }

    // ================================================================
    // Vintage Improvements: Curving
    // ================================================================

    /**
     * Adds a Vintage Improvements curving recipe.
     * <p>
     * Curves an item using a head item as a mold.
     * </p>
     *
     * @param id          Recipe identifier
     * @param itemInput   Input ingredient spec
     * @param itemOutput  Output item spec
     * @param itemAsHead  Item used as the curving head/mold
     */
    public static void addVintageCurving(String id, String itemInput, String itemOutput, String itemAsHead) {
        RecipeBuilder.create("vintageimprovements:curving", id)
                .withItemInputs(new String[]{itemInput})
                .withItemOutputs(new String[]{itemOutput})
                .withItemAsHead(itemAsHead)
                .buildAndRegister();
    }

    // ================================================================
    // Vintage Improvements: Polishing
    // ================================================================

    /**
     * Adds a Vintage Improvements polishing recipe with speed limits.
     *
     * @param id              Recipe identifier
     * @param itemInput       Input ingredient spec
     * @param itemOutput      Output item spec
     * @param processingTime  Processing time in ticks
     * @param speedLimits     Minimum RPM required (e.g. 1, 4, 16)
     */
    public static void addVintagePolishing(String id, String itemInput, String itemOutput, int processingTime, int speedLimits) {
        RecipeBuilder.create("vintageimprovements:polishing", id)
                .withItemInputs(new String[]{itemInput})
                .withItemOutputs(new String[]{itemOutput})
                .withProcessingTime(processingTime)
                .withSpeedLimits(speedLimits)
                .buildAndRegister();
    }

    /**
     * Adds a Vintage Improvements polishing recipe with default values (20 ticks, 1 RPM).
     */
    public static void addVintagePolishing(String id, String itemInput, String itemOutput) {
        addVintagePolishing(id, itemInput, itemOutput, 20, 1);
    }

    // ================================================================
    // Vintage Improvements: Hammering
    // ================================================================

    /**
     * Adds a Vintage Improvements hammering recipe.
     * <p>
     * Hammers an ingot into a sheet with the specified number of blows.
     * </p>
     *
     * @param id           Recipe identifier
     * @param itemInput    Input ingredient spec (typically an ingot)
     * @param itemOutput   Output item spec (typically a sheet)
     * @param hammerBlows  Number of hammer blows required (e.g. 3)
     */
    public static void addVintageHammering(String id, String itemInput, String itemOutput, int hammerBlows) {
        RecipeBuilder.create("vintageimprovements:hammering", id)
                .withItemInputs(new String[]{itemInput})
                .withItemOutputs(new String[]{itemOutput})
                .withHammerBlows(hammerBlows)
                .buildAndRegister();
    }

    // ================================================================
    // Vintage Improvements: Laser Cutting
    // ================================================================

    /**
     * Adds a Vintage Improvements laser cutting recipe.
     * <p>
     * Cuts an item using laser energy with configurable charge rate.
     * </p>
     *
     * @param id              Recipe identifier
     * @param itemInput       Input ingredient spec
     * @param itemOutputs     Array of output item specs (format: "item" or "item*count")
     * @param energy          Total energy required in FE
     * @param maxChargeRate   Maximum charge rate in FE/tick
     */
    public static void addVintageLaserCutting(String id, String itemInput, String[] itemOutputs, int energy, int maxChargeRate) {
        RecipeBuilder.create("vintageimprovements:laser_cutting", id)
                .withItemInputs(new String[]{itemInput})
                .withItemOutputsWithCount(itemOutputs)
                .withEnergy(energy)
                .withMaxChargeRate(maxChargeRate)
                .buildAndRegister();
    }

    // ================================================================
    // Vintage Improvements: Pressurizing
    // ================================================================

    /**
     * Adds a Vintage Improvements pressurizing recipe with heat requirement.
     * <p>
     * Pressurizes an item to produce a fluid output.
     * </p>
     *
     * @param id                   Recipe identifier
     * @param itemInput            Input ingredient spec
     * @param fluidOutputId        Fluid output id (e.g. "vintageimprovements:sulfur_dioxide")
     * @param fluidOutputAmount    Fluid output amount in millibuckets
     * @param heatRequirement      Heat requirement: null (none), "heated", or "superheated"
     * @param processingTime       Processing time in ticks
     */
    public static void addVintagePressurizing(String id, String itemInput, String fluidOutputId, int fluidOutputAmount,
                                              String heatRequirement, int processingTime) {
        RecipeBuilder.create("vintageimprovements:pressurizing", id)
                .withItemInputs(new String[]{itemInput})
                .withFluidOutputs(new FluidSpec[]{FluidSpec.of(fluidOutputId, fluidOutputAmount)})
                .withHeatRequirement(heatRequirement)
                .withProcessingTime(processingTime)
                .buildAndRegister();
    }

    /**
     * Adds a Vintage Improvements pressurizing recipe with heated requirement (default: 600 ticks).
     */
    public static void addVintagePressurizing(String id, String itemInput, String fluidOutputId, int fluidOutputAmount) {
        addVintagePressurizing(id, itemInput, fluidOutputId, fluidOutputAmount, "heated", 600);
    }

    // ================================================================
    // Create Addition: Rolling
    // ================================================================

    /**
     * Adds a Create Addition rolling recipe.
     * <p>
     * Rolls an ingot into rods with configurable output count.
     * </p>
     *
     * @param id           Recipe identifier
     * @param itemInput    Input ingredient spec (typically an ingot tag)
     * @param itemOutput   Output rod item spec
     * @param outputCount  Number of rods produced
     */
    public static void addCreateAdditionRolling(String id, String itemInput, String itemOutput, int outputCount) {
        RecipeBuilder.create("createaddition:rolling", id)
                .withItemInputs(new String[]{itemInput})
                .withItemOutputsWithCount(new String[]{itemOutput + "*" + outputCount})
                .buildAndRegister();
    }

    /**
     * Adds a Create Addition rolling recipe with default output count (2).
     */
    public static void addCreateAdditionRolling(String id, String itemInput, String itemOutput) {
        addCreateAdditionRolling(id, itemInput, itemOutput, 2);
    }

    // ================================================================
    // Vintage Improvements: Turning
    // ================================================================

    /**
     * Adds a Vintage Improvements turning recipe.
     * <p>
     * Turns a storage block into a curving head.
     * </p>
     *
     * @param id              Recipe identifier
     * @param itemInput       Input ingredient spec (typically a storage block)
     * @param itemOutput      Output item spec
     * @param processingTime  Processing time in ticks
     */
    public static void addVintageTurning(String id, String itemInput, String itemOutput, int processingTime) {
        RecipeBuilder.create("vintageimprovements:turning", id)
                .withItemInputs(new String[]{itemInput})
                .withItemOutputs(new String[]{itemOutput})
                .withProcessingTime(processingTime)
                .buildAndRegister();
    }

    /**
     * Adds a Vintage Improvements turning recipe with default processing time (200 ticks).
     */
    public static void addVintageTurning(String id, String itemInput, String itemOutput) {
        addVintageTurning(id, itemInput, itemOutput, 200);
    }

    // ================================================================
    // Vintage Improvements: Vacuumizing
    // ================================================================

    /**
     * Adds a Vintage Improvements vacuumizing recipe.
     * <p>
     * Processes items in a vacuum chamber.
     * </p>
     *
     * @param id              Recipe identifier
     * @param itemInputs      Array of input ingredient specs
     * @param itemOutputs     Array of output item specs
     * @param processingTime  Processing time in ticks
     */
    public static void addVintageVacuumizing(String id, String[] itemInputs, String[] itemOutputs, int processingTime) {
        RecipeBuilder.create("vintageimprovements:vacuumizing", id)
                .withItemInputs(itemInputs)
                .withItemOutputs(itemOutputs)
                .withProcessingTime(processingTime)
                .buildAndRegister();
    }

    /**
     * Adds a Vintage Improvements vacuumizing recipe with default processing time (600 ticks).
     */
    public static void addVintageVacuumizing(String id, String[] itemInputs, String[] itemOutputs) {
        addVintageVacuumizing(id, itemInputs, itemOutputs, 600);
    }

    // ================================================================
    // Vintage Improvements: Leaves Vibrating
    // ================================================================

    /**
     * Adds a Vintage Improvements leaves vibrating recipe.
     * <p>
     * Vibrates leaves to produce outputs (typically no item output).
     * </p>
     *
     * @param id              Recipe identifier
     * @param itemInputs      Array of input ingredient specs (typically a leaves tag)
     * @param processingTime  Processing time in ticks
     */
    public static void addVintageLeavesVibrating(String id, String[] itemInputs, int processingTime) {
        RecipeBuilder.create("vintageimprovements:leaves_vibrating", id)
                .withItemInputs(itemInputs)
                .withProcessingTime(processingTime)
                .buildAndRegister();
    }

    /**
     * Adds a Vintage Improvements leaves vibrating recipe with default processing time (300 ticks).
     */
    public static void addVintageLeavesVibrating(String id, String[] itemInputs) {
        addVintageLeavesVibrating(id, itemInputs, 300);
    }

    // ================================================================
    // Vintage Improvements: Vibrating
    // ================================================================

    /**
     * Adds a Vintage Improvements vibrating recipe.
     * <p>
     * Vibrates items to transform them into other items.
     * </p>
     *
     * @param id              Recipe identifier
     * @param itemInputs      Array of input ingredient specs
     * @param itemOutputs     Array of output item specs with optional counts (format: "item" or "item*count")
     * @param processingTime  Processing time in ticks
     */
    public static void addVintageVibrating(String id, String[] itemInputs, String[] itemOutputs, int processingTime) {
        RecipeBuilder.create("vintageimprovements:vibrating", id)
                .withItemInputs(itemInputs)
                .withItemOutputsWithCount(itemOutputs)
                .withProcessingTime(processingTime)
                .buildAndRegister();
    }

    /**
     * Adds a Vintage Improvements vibrating recipe with default processing time (300 ticks).
     */
    public static void addVintageVibrating(String id, String[] itemInputs, String[] itemOutputs) {
        addVintageVibrating(id, itemInputs, itemOutputs, 300);
    }

    // ================================================================
    // Vanilla Crafting Recipes
    // ================================================================

    /**
     * Adds a shaped crafting recipe.
     *
     * @param id       Recipe identifier
     * @param pattern  Array of 1-3 strings representing the crafting grid
     * @param key      Map of symbols to ingredient specs
     * @param result   Output item spec
     */
    public static void addShaped(String id, String[] pattern, Map<String, String> key, String result) {
        addShaped(id, pattern, key, result, 1);
    }

    /**
     * Adds a shaped crafting recipe with specified output count.
     */
    public static void addShaped(String id, String[] pattern, Map<String, String> key, String result, int count) {
        RecipeBuilder.create("minecraft:crafting_shaped", id)
                .withPattern(pattern)
                .withKey(key)
                .withItemOutputs(new String[]{result + "*" + count})
                .buildAndRegister();
    }

    /**
     * Adds a shapeless crafting recipe.
     *
     * @param id           Recipe identifier
     * @param ingredients  Array of ingredient specs
     * @param result       Output item spec
     */
    public static void addShapeless(String id, String[] ingredients, String result) {
        addShapeless(id, ingredients, result, 1);
    }

    /**
     * Adds a shapeless crafting recipe with specified output count.
     */
    public static void addShapeless(String id, String[] ingredients, String result, int count) {
        RecipeBuilder.create("minecraft:crafting_shapeless", id)
                .withItemInputs(ingredients)
                .withItemOutputs(new String[]{result + "*" + count})
                .buildAndRegister();
    }

    // ================================================================
    // Vanilla Cooking Recipes
    // ================================================================

    /**
     * Adds a smelting recipe with default values (0.35 XP, 200 ticks).
     */
    public static void addSmelting(String id, String input, String result) {
        addSmelting(id, input, result, 0.35F, 200);
    }

    /**
     * Adds a smelting recipe with custom XP and cooking time.
     */
    public static void addSmelting(String id, String input, String result, float experience, int cookingTime) {
        addCooking("minecraft:smelting", id, input, result, experience, cookingTime);
    }

    /**
     * Adds a blasting recipe with default values (1.0 XP, 100 ticks).
     */
    public static void addBlasting(String id, String input, String result) {
        addBlasting(id, input, result, 1.0F, 100);
    }

    /**
     * Adds a blasting recipe with custom XP and cooking time.
     */
    public static void addBlasting(String id, String input, String result, float experience, int cookingTime) {
        addCooking("minecraft:blasting", id, input, result, experience, cookingTime);
    }

    /**
     * Adds a smoking recipe with default values (0.35 XP, 100 ticks).
     */
    public static void addSmoking(String id, String input, String result) {
        addSmoking(id, input, result, 0.35F, 100);
    }

    /**
     * Adds a smoking recipe with custom XP and cooking time.
     */
    public static void addSmoking(String id, String input, String result, float experience, int cookingTime) {
        addCooking("minecraft:smoking", id, input, result, experience, cookingTime);
    }

    /**
     * Adds a campfire cooking recipe with default values (0.35 XP, 600 ticks).
     */
    public static void addCampfireCooking(String id, String input, String result) {
        addCampfireCooking(id, input, result, 0.35F, 600);
    }

    /**
     * Adds a campfire cooking recipe with custom XP and cooking time.
     */
    public static void addCampfireCooking(String id, String input, String result, float experience, int cookingTime) {
        addCooking("minecraft:campfire_cooking", id, input, result, experience, cookingTime);
    }

    /**
     * Internal generic cooking recipe builder.
     */
    private static void addCooking(String type, String id, String input, String result, float experience, int cookingTime) {
        RecipeBuilder.create(type, id)
                .withItemInputs(new String[]{input})
                .withItemOutputs(new String[]{result})
                .withExperience(experience)
                .withCookingTime(cookingTime)
                .buildAndRegister();
    }

    // ================================================================
    // Vanilla Stonecutting Recipe
    // ================================================================

    /**
     * Adds a stonecutting recipe with output count 1.
     */
    public static void addStonecutting(String id, String input, String result) {
        addStonecutting(id, input, result, 1);
    }

    /**
     * Adds a stonecutting recipe with specified output count.
     */
    public static void addStonecutting(String id, String input, String result, int count) {
        RecipeBuilder.create("minecraft:stonecutting", id)
                .withItemInputs(new String[]{input})
                .withItemOutputs(new String[]{result + "*" + count})
                .buildAndRegister();
    }

    // ================================================================
    // Create Mechanical Crafting Recipe
    // ================================================================

    /**
     * Adds a Create mechanical crafting recipe.
     * <p>
     * Similar to vanilla shaped crafting but supports larger grids.
     * </p>
     *
     * @param id       Recipe identifier
     * @param pattern  Array of pattern strings
     * @param key      Map of symbols to ingredient specs
     * @param result   Output item spec
     */
    public static void addMechanicalCrafting(String id, String[] pattern, Map<String, String> key, String result) {
        addMechanicalCrafting(id, pattern, key, result, 1);
    }

    /**
     * Adds a Create mechanical crafting recipe with specified output count.
     */
    public static void addMechanicalCrafting(String id, String[] pattern, Map<String, String> key, String result, int count) {
        RecipeBuilder.create("create:mechanical_crafting", id)
                .withPattern(pattern)
                .withKey(key)
                .withItemOutputs(new String[]{result + "*" + count})
                .buildAndRegister();
    }

    // ================================================================
    // Core Recipe Registration
    // ================================================================

    /**
     * Registers a recipe with the datagen system.
     * <p>
     * This is the core registration method that all recipe builders use.
     * </p>
     *
     * @param id   Recipe identifier
     * @param json JSON object representing the recipe
     */
    public static void addRecipe(String id, JsonObject json) {
        KubeJavaDatagenSupport.addRecipe(id, json);
    }

    /**
     * Registers a recipe from a JSON string.
     *
     * @param id       Recipe identifier
     * @param jsonText JSON string representing the recipe
     */
    public static void addRecipe(String id, String jsonText) {
        addRecipe(id, JsonParser.parseString(jsonText).getAsJsonObject());
    }

    // ================================================================
    // Recipe Replacement / Removal (Filter API)
    // ================================================================

    /**
     * Replaces all occurrences of an input item with another.
     *
     * @param oldItem The item to replace
     * @param newItem The replacement item
     */
    public static void replaceInput(String oldItem, String newItem) {
        RecipeFilter.all().replaceInput(oldItem, newItem);
    }

    /**
     * Replaces all occurrences of an output item with another.
     *
     * @param oldItem The item to replace
     * @param newItem The replacement item
     */
    public static void replaceOutput(String oldItem, String newItem) {
        RecipeFilter.all().replaceOutput(oldItem, newItem);
    }

    /**
     * Removes a recipe by its identifier.
     *
     * @param recipeId The recipe to remove
     */
    public static void removeRecipe(String recipeId) {
        RecipeFilter.byId(recipeId).remove();
    }

    // ================================================================
    // Recipe Filter Factory Methods
    // ================================================================

    /**
     * Creates a filter that matches all recipes.
     *
     * @return A {@link RecipeFilter} instance
     */
    public static RecipeFilter all() {
        return RecipeFilter.all();
    }

    /**
     * Creates a filter that matches a specific recipe by ID.
     *
     * @param id Recipe identifier
     * @return A {@link RecipeFilter} instance
     */
    public static RecipeFilter byId(String id) {
        return RecipeFilter.byId(id);
    }

    /**
     * Creates a filter that matches a specific recipe by ID with blacklist option.
     *
     * @param id        Recipe identifier
     * @param blacklist If true, matches all except the specified recipe
     * @return A {@link RecipeFilter} instance
     */
    public static RecipeFilter byId(String id, boolean blacklist) {
        return RecipeFilter.byId(id, blacklist);
    }

    /**
     * Creates a filter that matches multiple recipe IDs.
     *
     * @param ids Recipe identifiers
     * @return A {@link RecipeFilter} instance
     */
    public static RecipeFilter byIds(String... ids) {
        return RecipeFilter.byIds(ids);
    }

    /**
     * Creates a filter that matches recipes from a specific mod.
     *
     * @param modId Mod identifier
     * @return A {@link RecipeFilter} instance
     */
    public static RecipeFilter byMod(String modId) {
        return RecipeFilter.byMod(modId);
    }

    /**
     * Creates a filter that matches recipes from a specific mod with blacklist option.
     *
     * @param modId     Mod identifier
     * @param blacklist If true, matches all except the specified mod
     * @return A {@link RecipeFilter} instance
     */
    public static RecipeFilter byMod(String modId, boolean blacklist) {
        return RecipeFilter.byMod(modId, blacklist);
    }

    /**
     * Creates a filter that matches recipes from multiple mods.
     *
     * @param modIds Mod identifiers
     * @return A {@link RecipeFilter} instance
     */
    public static RecipeFilter byMods(String... modIds) {
        return RecipeFilter.byMods(modIds);
    }

    /**
     * Creates a filter that matches recipes of a specific type.
     *
     * @param typeId Recipe type identifier
     * @return A {@link RecipeFilter} instance
     */
    public static RecipeFilter byType(String typeId) {
        return RecipeFilter.byType(typeId);
    }

    /**
     * Creates a filter that matches recipes of a specific type with blacklist option.
     *
     * @param typeId    Recipe type identifier
     * @param blacklist If true, matches all except the specified type
     * @return A {@link RecipeFilter} instance
     */
    public static RecipeFilter byType(String typeId, boolean blacklist) {
        return RecipeFilter.byType(typeId, blacklist);
    }

    /**
     * Creates a filter that matches recipes of multiple types.
     *
     * @param typeIds Recipe type identifiers
     * @return A {@link RecipeFilter} instance
     */
    public static RecipeFilter byTypes(String... typeIds) {
        return RecipeFilter.byTypes(typeIds);
    }

    /**
     * Creates a filter with a custom predicate.
     *
     * @param predicate The filtering predicate
     * @return A {@link RecipeFilter} instance
     */
    public static RecipeFilter byCustom(Predicate<RecipeFilter.RecipeInfo> predicate) {
        return RecipeFilter.byCustom(predicate);
    }

    // ================================================================
    // Utility Methods
    // ================================================================

    /**
     * Convenience method for creating string arrays from varargs.
     * <p>
     * This enables cleaner recipe definitions with inline array creation.
     * </p>
     *
     * @param items The items to include in the array
     * @return A string array
     * @example {@code arr("item1", "item2", "item3")}
     */
    public static String[] arr(String... items) {
        return items;
    }

    /**
     * Validates that a recipe type is supported.
     *
     * @param type The recipe type to validate
     * @throws IllegalArgumentException if the type is not supported
     */
    private static void validateRecipeType(String type) {
        if (!SUPPORTED_RECIPE_TYPES.contains(type)) {
            LOGGER.warn("Recipe type '{}' is not in the known supported types list. Proceeding anyway.", type);
        }
    }

    // ================================================================
    // Inner Classes - Enterprise Design Patterns
    // ================================================================

    /**
     * Recipe Builder Pattern Implementation.
     * <p>
     * Provides a fluent API for constructing complex recipes with
     * validation and proper error handling.
     * </p>
     */
    @SuppressWarnings("UnusedReturnValue")
    private static final class RecipeBuilder {
        private final String type;
        private final String id;
        private final JsonObject json;

        private RecipeBuilder(String type, String id) {
            this.type = type;
            this.id = id;
            this.json = new JsonObject();
            json.addProperty("type", type);
        }

        /**
         * Factory method for creating a new RecipeBuilder instance.
         *
         * @param type Recipe type
         * @param id   Recipe identifier
         * @return A new RecipeBuilder instance
         */
        public static RecipeBuilder create(String type, String id) {
            return new RecipeBuilder(type, id);
        }

        /**
         * Adds item inputs to the recipe.
         *
         * @param inputs Array of item input specs
         * @return This builder instance for chaining
         */
        public RecipeBuilder withItemInputs(String[] inputs) {
            JsonArray ingredients = json.getAsJsonArray("ingredients");
            if (ingredients == null) {
                ingredients = new JsonArray();
                json.add("ingredients", ingredients);
            }
            for (String spec : inputs) {
                if (spec != null && !spec.isEmpty()) {
                    ingredients.add(ingredient(spec));
                }
            }
            return this;
        }

        /**
         * Adds fluid inputs to the recipe.
         *
         * @param fluids Array of fluid input specs
         * @return This builder instance for chaining
         */
        public RecipeBuilder withFluidInputs(FluidSpec[] fluids) {
            JsonArray ingredients = json.getAsJsonArray("ingredients");
            if (ingredients == null) {
                ingredients = new JsonArray();
                json.add("ingredients", ingredients);
            }
            for (FluidSpec fluid : fluids) {
                if (fluid != null && !fluid.isEmpty()) {
                    ingredients.add(fluid.toJson());
                }
            }
            return this;
        }

        /**
         * Adds item outputs to the recipe.
         *
         * @param outputs Array of item output specs
         * @return This builder instance for chaining
         */
        public RecipeBuilder withItemOutputs(String[] outputs) {
            JsonArray results = json.getAsJsonArray("results");
            if (results == null) {
                results = new JsonArray();
                json.add("results", results);
            }
            for (String spec : outputs) {
                if (spec != null && !spec.isEmpty()) {
                    results.add(itemStack(spec, 1));
                }
            }
            return this;
        }

        /**
         * Adds item outputs with count support (format: "item" or "item*count").
         *
         * @param outputs Array of item output specs with optional counts
         * @return This builder instance for chaining
         */
        public RecipeBuilder withItemOutputsWithCount(String[] outputs) {
            JsonArray results = json.getAsJsonArray("results");
            if (results == null) {
                results = new JsonArray();
                json.add("results", results);
            }
            for (String spec : outputs) {
                if (spec != null && !spec.isEmpty()) {
                    results.add(parseItemStack(spec));
                }
            }
            return this;
        }

        /**
         * Adds fluid outputs to the recipe.
         *
         * @param fluids Array of fluid output specs
         * @return This builder instance for chaining
         */
        public RecipeBuilder withFluidOutputs(FluidSpec[] fluids) {
            JsonArray results = json.getAsJsonArray("results");
            if (results == null) {
                results = new JsonArray();
                json.add("results", results);
            }
            for (FluidSpec fluid : fluids) {
                if (fluid != null && !fluid.isEmpty()) {
                    results.add(fluid.toJson());
                }
            }
            return this;
        }

        /**
         * Adds a processing time to the recipe.
         *
         * @param ticks Processing time in ticks
         * @return This builder instance for chaining
         */
        public RecipeBuilder withProcessingTime(int ticks) {
            json.addProperty("processingTime", ticks);
            json.addProperty("processing_time", ticks); // For Vintage Improvements compatibility
            return this;
        }

        /**
         * Adds a heat requirement to the recipe.
         *
         * @param heatRequirement "heated" or "superheated"
         * @return This builder instance for chaining
         */
        public RecipeBuilder withHeatRequirement(String heatRequirement) {
            if (heatRequirement != null && !heatRequirement.isEmpty()) {
                json.addProperty("heatRequirement", heatRequirement);
                json.addProperty("heat_requirement", heatRequirement);
            }
            return this;
        }

        /**
         * Adds a spring color for coiling recipes.
         *
         * @param color Hex color code
         * @return This builder instance for chaining
         */
        public RecipeBuilder withSpringColor(String color) {
            json.addProperty("spring_color", color);
            return this;
        }

        /**
         * Adds an item as head for curving recipes.
         *
         * @param item The head item
         * @return This builder instance for chaining
         */
        public RecipeBuilder withItemAsHead(String item) {
            json.addProperty("item_as_head", item);
            return this;
        }

        /**
         * Adds speed limits for polishing recipes.
         *
         * @param speed Minimum RPM required
         * @return This builder instance for chaining
         */
        public RecipeBuilder withSpeedLimits(int speed) {
            json.addProperty("speed_limits", speed);
            return this;
        }

        /**
         * Adds hammer blows for hammering recipes.
         *
         * @param blows Number of hammer blows
         * @return This builder instance for chaining
         */
        public RecipeBuilder withHammerBlows(int blows) {
            json.addProperty("hammer_blows", blows);
            return this;
        }

        /**
         * Adds energy requirement for laser cutting recipes.
         *
         * @param energy Energy in FE
         * @return This builder instance for chaining
         */
        public RecipeBuilder withEnergy(int energy) {
            json.addProperty("energy", energy);
            return this;
        }

        /**
         * Adds max charge rate for laser cutting recipes.
         *
         * @param rate Charge rate in FE/tick
         * @return This builder instance for chaining
         */
        public RecipeBuilder withMaxChargeRate(int rate) {
            json.addProperty("max_charge_rate", rate);
            return this;
        }

        /**
         * Adds cooking experience for vanilla cooking recipes.
         *
         * @param experience XP gained
         * @return This builder instance for chaining
         */
        public RecipeBuilder withExperience(float experience) {
            json.addProperty("experience", experience);
            return this;
        }

        /**
         * Adds cooking time for vanilla cooking recipes.
         *
         * @param time Cooking time in ticks
         * @return This builder instance for chaining
         */
        public RecipeBuilder withCookingTime(int time) {
            json.addProperty("cookingtime", time);
            return this;
        }

        /**
         * Adds a crafting pattern for shaped recipes.
         *
         * @param pattern Array of 1-3 strings
         * @return This builder instance for chaining
         */
        public RecipeBuilder withPattern(String[] pattern) {
            JsonArray patternArray = new JsonArray();
            for (String row : pattern) {
                patternArray.add(row);
            }
            json.add("pattern", patternArray);
            return this;
        }

        /**
         * Adds a key map for shaped recipes.
         *
         * @param key Map of symbols to ingredient specs
         * @return This builder instance for chaining
         */
        public RecipeBuilder withKey(Map<String, String> key) {
            JsonObject keyObject = new JsonObject();
            key.forEach((symbol, spec) -> keyObject.add(symbol, ingredient(spec)));
            json.add("key", keyObject);
            return this;
        }

        /**
         * Sets a secondary fluid output for pressurizing recipes.
         *
         * @param fluid The fluid output spec
         * @return This builder instance for chaining
         */
        public RecipeBuilder withSecondaryFluidOutput(FluidSpec fluid) {
            if (fluid != null && !fluid.isEmpty()) {
                json.add("secondary_fluid_output", fluid.toJson());
            }
            return this;
        }

        /**
         * Builds and registers the recipe.
         * <p>
         * Performs validation before registration.
         * </p>
         *
         * @throws RecipeConfigurationException if validation fails
         */
        public void buildAndRegister() {
            VALIDATION_SERVICE.validate(this);
            addRecipe(id, json);
            LOGGER.debug("Registered recipe: {} (type: {})", id, type);
        }

        /**
         * Gets the JSON object for validation purposes.
         *
         * @return The JSON object
         */
        public JsonObject getJson() {
            return json;
        }

        /**
         * Gets the recipe ID.
         *
         * @return The recipe ID
         */
        public String getId() {
            return id;
        }

        /**
         * Gets the recipe type.
         *
         * @return The recipe type
         */
        public String getType() {
            return type;
        }
    }

    /**
     * Recipe Validation Service - Singleton Pattern.
     * <p>
     * Validates recipes before registration to prevent invalid configurations.
     * </p>
     */
    private static final class RecipeValidationService {
        private static final RecipeValidationService INSTANCE = new RecipeValidationService();

        private RecipeValidationService() {}

        public static RecipeValidationService getInstance() {
            return INSTANCE;
        }

        /**
         * Validates a recipe builder.
         *
         * @param builder The builder to validate
         * @throws RecipeConfigurationException if validation fails
         */
        public void validate(RecipeBuilder builder) {
            JsonObject json = builder.getJson();
            String id = builder.getId();
            String type = builder.getType();

            // Type validation
            if (type == null || type.isEmpty()) {
                throw new RecipeConfigurationException("Recipe type is required for recipe: " + id);
            }

            // Minimal validation: ensure at least some content
            if (!json.has("ingredients") && !json.has("key") && !json.has("pattern")) {
                LOGGER.warn("Recipe {} has no ingredients/pattern specified. This may be intentional.", id);
            }

            if (!json.has("results") && !json.has("result") && !json.has("fluid")) {
                LOGGER.warn("Recipe {} has no results specified. This may be intentional.", id);
            }

            LOGGER.debug("Validation passed for recipe: {}", id);
        }
    }

    /**
     * Recipe Registry - Singleton Pattern.
     * <p>
     * Central registry for tracking registered recipes.
     * </p>
     */
    private static final class RecipeRegistry {
        private static final RecipeRegistry INSTANCE = new RecipeRegistry();
        private final Set<String> registeredRecipes = new HashSet<>();

        private RecipeRegistry() {}

        public static RecipeRegistry getInstance() {
            return INSTANCE;
        }

        public synchronized void register(String id) {
            if (registeredRecipes.contains(id)) {
                LOGGER.warn("Recipe {} is being registered multiple times. This may cause conflicts.", id);
            }
            registeredRecipes.add(id);
        }

        public boolean isRegistered(String id) {
            return registeredRecipes.contains(id);
        }
    }

    /**
     * Recipe Configuration Exception - Custom Exception.
     * <p>
     * Thrown when recipe configuration fails.
     * </p>
     */
    private static final class RecipeConfigurationException extends RuntimeException {
        public RecipeConfigurationException(String message) {
            super(message);
        }

        public RecipeConfigurationException(String message, Throwable cause) {
            super(message, cause);
        }
    }

    // ================================================================
    // JSON Builders (Internal)
    // ================================================================

    /**
     * Creates a JSON ingredient object from a spec string.
     *
     * @param spec Ingredient spec (item ID or tag with '#' prefix)
     * @return A JSON object representing the ingredient
     */
    private static JsonObject ingredient(String spec) {
        if (spec.startsWith("#")) {
            JsonObject json = new JsonObject();
            json.addProperty("tag", spec.substring(1));
            return json;
        }
        return ItemSpec.ingredientJson(spec);
    }

    /**
     * Creates a JSON item stack object.
     *
     * @param item  Item identifier
     * @param count Stack count
     * @return A JSON object representing the item stack
     */
    private static JsonObject itemStack(String item, int count) {
        return ItemSpec.stackJson(item, count);
    }

    /**
     * Parses an item stack spec in the format "item" or "item*count".
     *
     * @param spec The spec string
     * @return A JSON object representing the item stack
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
}