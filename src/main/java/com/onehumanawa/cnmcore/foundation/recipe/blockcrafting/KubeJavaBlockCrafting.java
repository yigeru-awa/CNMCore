package com.onehumanawa.cnmcore.foundation.recipe.blockcrafting;

import com.onehumanawa.cnmcore.CNMCore;
import net.minecraft.core.Vec3i;
import net.minecraft.resources.ResourceLocation;

import java.util.*;

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
 *     .consumePattern(false)
 *     .consumeCenter(true)
 *     .result("create:andesite_alloy")
 *     .register();
 * }</pre>
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
                .consumePattern(false)
                .consumeCenter(true)
                .consumeInput(false)
                .result("create:andesite_alloy", 4)
                .feedback("cnmcore.blockcrafting.success")
                .register();
    }

    public static Builder blockCrafting(String id) {
        return new Builder(ResourceLocation.parse(id));
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

        private Builder(ResourceLocation id) {
            this.id = id;
        }

        /**
         * Adds one layer. Layers are stacked along Y axis (bottom to top).
         * Each string is a row (Z direction), each character is a column (X direction).
         * Space characters are ignored.
         */
        public Builder pattern(String... rows) {
            if (rows.length == 0) {
                throw new IllegalArgumentException("Pattern layer cannot be empty");
            }
            layers.add(rows);
            return this;
        }

        public Builder where(char symbol, String blockId) {
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
            resultIds.addAll(Arrays.asList(itemIds));
            return this;
        }

        public Builder result(String itemId, int count) {
            for (int i = 0; i < count; i++) {
                resultIds.add(itemId);
            }
            return this;
        }

        public Builder action(CraftingAction action) {
            actions.add(action);
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

        public void register() {
            List<BlockCraftingRecipe.PatternEntry> entries = new ArrayList<>();
            Vec3i centerOffset = null;

            // layers: index 0 = y=0 (bottom), index 1 = y=1, etc.
            for (int y = 0; y < layers.size(); y++) {
                String[] rows = layers.get(y);
                // rows: each string is a row along Z direction
                for (int z = 0; z < rows.length; z++) {
                    String row = rows[z];
                    // row: each character is a column along X direction
                    for (int x = 0; x < row.length(); x++) {
                        char symbol = row.charAt(x);
                        if (symbol == ' ') continue;

                        String blockId = symbols.get(symbol);
                        if (blockId == null) {
                            CNMCore.LOGGER.warn("[BlockCrafting] Undefined symbol '{}' in recipe {}, skipping", symbol, id);
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

            if (centerOffset == null) {
                throw new IllegalStateException("Center symbol '" + centerSymbol + "' not found in pattern: " + id);
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
                    symbols.get(centerSymbol),
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
        }
    }
}