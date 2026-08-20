package com.onehumanawa.cnmcore.foundation.recipe.blockcrafting;

import com.onehumanawa.cnmcore.CNMCore;
import com.onehumanawa.cnmcore.foundation.registry.KubeJavaRegistryHandler;
import net.minecraft.core.Vec3i;
import net.minecraft.resources.ResourceLocation;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * KubeJava entry point for BlockCrafting recipes.
 * <p>
 * Pattern layers are stacked along Y axis (bottom to top).
 * Each layer is a String[] where each String is a row (Z direction),
 * and each character is a column (X direction).
 * <p>
 * Example: 3-layer vertical pillar:
 * <pre>{@code
 * blockCrafting("cnmcore:andesite_pillar")
 *     .pattern("I")   // layer 0 (y=0): iron block
 *     .pattern("A")   // layer 1 (y=1): andesite (center)
 *     .pattern("Z")   // layer 2 (y=2): zinc block
 *     .where('I', "minecraft:iron_block")
 *     .where('A', "minecraft:andesite")
 *     .where('Z', "create:zinc_block")
 *     .center('A')
 *     .input("create:wrench")
 *     .keepPattern()          // keep iron & zinc blocks
 *     .consumeCenter(true)
 *     .result("create:andesite_alloy", 4)
 *     .action(CraftingActions.sound("minecraft:block.anvil_land"))
 *     .action(CraftingActions.particles("minecraft:happy_villager", 10))
 *     .feedback("cnmcore.blockcrafting.success", "Block crafted successfully", "成功进行方块合成")
 *     .register();
 * }</pre>
 * <p>
 */
public final class KubeJavaBlockCrafting {

    private KubeJavaBlockCrafting() {}

    public static void init() {
        // ============================================================
        // Andesite Alloy Recipe
        // Structure (bottom to top): Iron Block -> Andesite -> Zinc Block
        // Right-click Andesite with Wrench, only Andesite is consumed
        // ============================================================
        blockCrafting("cnmcore:andesite_alloy_craft")
                .pattern("I")   // layer 0 (y=0): iron block
                .pattern("A")   // layer 1 (y=1): andesite (center)
                .pattern("Z")   // layer 2 (y=2): zinc block
                .where('I', "minecraft:iron_block")
                .where('A', "minecraft:andesite")
                .where('Z', "create:zinc_block")
                .center('A')
                .input("create:wrench")
                .keepPattern()
                .consumeCenter(true)
                .keepInput()
                .result("create:andesite_alloy", 4)
                .action(CraftingActions.sound("minecraft:block.anvil_land"))
                .action(CraftingActions.particles("minecraft:happy_villager", 10))
                .feedback("cnmcore.blockcrafting.success", "Block crafted successfully", "成功进行方块合成")
                .register();
    }

    /**
     * Starts a block crafting recipe declaration for the given id.
     * Invalid ids are logged when the builder is created and the recipe
     * is skipped on {@link Builder#register()}.
     */
    public static Builder blockCrafting(String id) {
        return new Builder(id);
    }

    public static final class Builder {
        private final ResourceLocation id;
        private final List<String[]> layers = new ArrayList<>();
        private final Map<Character, String> symbols = new LinkedHashMap<>();
        private char centerSymbol;
        private String inputId;
        private final List<String> resultIds = new ArrayList<>();
        private final List<CraftingAction> actions = new ArrayList<>();
        private String feedback = "";
        private boolean consumePattern = true;
        private boolean consumeCenter = true;
        private boolean consumeInput = true;

        private Builder(String id) {
            ResourceLocation parsed = id == null ? null : ResourceLocation.tryParse(id);
            if (parsed == null) {
                CNMCore.LOGGER.error("[BlockCrafting] Invalid recipe id '{}' - expected \"modid:path\", recipe skipped", id);
            }
            this.id = parsed;
        }

        /**
         * Adds one layer. Layers are stacked along Y axis (bottom to top).
         * Each string is a row (Z direction), each character is a column (X direction).
         * Space characters are ignored.
         */
        public Builder pattern(String... rows) {
            if (rows == null || rows.length == 0) {
                CNMCore.LOGGER.warn("[BlockCrafting] Empty pattern layer ignored for {}", describe());
                return this;
            }
            layers.add(rows);
            return this;
        }

        public Builder where(char symbol, String blockId) {
            if (blockId == null || blockId.isBlank()) {
                CNMCore.LOGGER.warn("[BlockCrafting] Blank block id for symbol '{}' ignored in {}", symbol, describe());
                return this;
            }
            symbols.put(symbol, blockId);
            return this;
        }

        public Builder center(char symbol) {
            this.centerSymbol = symbol;
            return this;
        }

        public Builder input(String itemId) {
            this.inputId = itemId;
            return this;
        }

        public Builder result(String... itemIds) {
            if (itemIds != null) {
                resultIds.addAll(Arrays.asList(itemIds));
            }
            return this;
        }

        /**
         * Adds the given item {@code count} times to the results.
         */
        public Builder result(String itemId, int count) {
            for (int i = 0; i < Math.max(0, count); i++) {
                resultIds.add(itemId);
            }
            return this;
        }

        public Builder action(CraftingAction action) {
            if (action != null) {
                actions.add(action);
            }
            return this;
        }

        /**
         * Sets the action bar feedback shown after crafting, with translations
         * registered automatically (consumed by {@code ModLangProvider}).
         *
         * @param key translation key, e.g. {@code "cnmcore.blockcrafting.success"}
         * @param en  english translation
         * @param zh  chinese translation
         */
        public Builder feedback(String key, String en, String zh) {
            if (key == null || key.isBlank()) return this;
            this.feedback = key;
            KubeJavaRegistryHandler.getLangCollector().add(key, en, zh);
            return this;
        }

        public Builder feedback(String message) {
            this.feedback = message;
            return this;
        }

        public Builder consumePattern(boolean consume) {
            this.consumePattern = consume;
            return this;
        }

        public Builder consumeCenter(boolean consume) {
            this.consumeCenter = consume;
            return this;
        }

        public Builder consumeInput(boolean consume) {
            this.consumeInput = consume;
            return this;
        }

        /** Convenience overload of {@code consumePattern(false)}. */
        public Builder keepPattern() {
            return consumePattern(false);
        }

        /** Convenience overload of {@code consumeCenter(false)}. */
        public Builder keepCenter() {
            return consumeCenter(false);
        }

        /** Convenience overload of {@code consumeInput(false)}. */
        public Builder keepInput() {
            return consumeInput(false);
        }

        /** Consumes the whole structure and the input item. */
        public Builder consumeAll() {
            return consumePattern(true).consumeCenter(true).consumeInput(true);
        }

        /** Keeps the whole structure and the input item. */
        public Builder keepAll() {
            return keepPattern().keepCenter().keepInput();
        }

        /**
         * Registers the recipe. Any invalid declaration is logged and skipped
         * instead of throwing.
         *
         * @return whether the recipe was registered successfully
         */
        public boolean register() {
            if (id == null) return false; // already logged at creation

            if (layers.isEmpty()) {
                CNMCore.LOGGER.error("[BlockCrafting] Recipe {} has no pattern layers, skipped", id);
                return false;
            }
            if (centerSymbol == 0) {
                CNMCore.LOGGER.error("[BlockCrafting] Recipe {} has no center symbol (call center(...)), skipped", id);
                return false;
            }

            List<BlockCraftingRecipe.PatternEntry> entries = new ArrayList<>();
            Vec3i centerOffset = null;

            // layers: index 0 = y=0 (bottom), index 1 = y=1, etc.
            for (int y = 0; y < layers.size(); y++) {
                String[] rows = layers.get(y);
                // rows: each string is a row along Z direction
                for (int z = 0; z < rows.length; z++) {
                    String row = rows[z];
                    if (row == null) continue;
                    // row: each character is a column along X direction
                    for (int x = 0; x < row.length(); x++) {
                        char symbol = row.charAt(x);
                        if (symbol == ' ') continue;

                        String blockId = symbols.get(symbol);
                        if (blockId == null) {
                            continue;
                        }

                        // offset: (x, y, -z) so that Z axis goes into the screen
                        Vec3i offset = new Vec3i(x, y, -z);
                        entries.add(new BlockCraftingRecipe.PatternEntry(offset, blockId, symbol));

                        if (symbol == centerSymbol) {
                            centerOffset = offset;
                        }
                    }
                }
            }

            if (entries.isEmpty()) {
                CNMCore.LOGGER.error("[BlockCrafting] Recipe {} has no pattern blocks (check where(...) mappings), skipped", id);
                return false;
            }
            if (centerOffset == null) {
                CNMCore.LOGGER.error("[BlockCrafting] Center symbol '{}' not found in pattern of {}, skipped", centerSymbol, id);
                return false;
            }

            // Normalize all offsets so center is at (0, 0, 0)
            int cx = centerOffset.getX();
            int cy = centerOffset.getY();
            int cz = centerOffset.getZ();

            List<BlockCraftingRecipe.PatternEntry> normalized = new ArrayList<>();
            for (BlockCraftingRecipe.PatternEntry entry : entries) {
                Vec3i original = entry.offset();
                Vec3i normalizedOffset = new Vec3i(
                        original.getX() - cx,
                        original.getY() - cy,
                        original.getZ() - cz
                );
                normalized.add(new BlockCraftingRecipe.PatternEntry(
                        normalizedOffset,
                        entry.blockId(),
                        entry.symbol()
                ));
            }

            BlockCraftingRecipe recipe = new BlockCraftingRecipe(
                    id,
                    normalized,
                    centerSymbol,
                    inputId,
                    resultIds,
                    actions,
                    feedback,
                    consumePattern,
                    consumeCenter,
                    consumeInput
            );

            BlockCraftingRegistry.register(recipe);
            CNMCore.LOGGER.info("[BlockCrafting] Registered recipe: {}", id);
            return true;
        }

        private String describe() {
            return id == null ? "<invalid id>" : id.toString();
        }
    }
}
