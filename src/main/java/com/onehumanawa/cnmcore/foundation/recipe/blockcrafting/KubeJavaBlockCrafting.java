package com.onehumanawa.cnmcore.foundation.recipe.blockcrafting;

import com.onehumanawa.cnmcore.CNMCore;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

/**
 * KubeJava entry point for BlockCrafting recipes.
 * <p>
 * Usage:
 * <pre>{@code
 * blockCrafting("example:craft")
 *     .pattern("ABA", " B ", "ABA")
 *     .where('A', Blocks.STONE)
 *     .where('B', Blocks.IRON_BLOCK)
 *     .center('B', Blocks.IRON_BLOCK)
 *     .input(Items.IRON_INGOT)
 *     .result(new ItemStack(Items.DIAMOND, 1))
 *     .register();
 * }</pre>
 */
@SuppressWarnings("unused")
public final class KubeJavaBlockCrafting {

    private KubeJavaBlockCrafting() {}

    public static Builder blockCrafting(String id) {
        return new Builder(ResourceLocation.parse(id));
    }

    public static Builder blockCrafting(ResourceLocation id) {
        return new Builder(id);
    }

    public static final class Builder {
        private final ResourceLocation id;
        private final List<String[]> layers = new ArrayList<>();
        private final java.util.HashMap<Character, Predicate<Block>> symbolBlocks = new java.util.HashMap<>();
        private char centerSymbol;
        private Predicate<ItemStack> inputPredicate;
        private final List<ItemStack> results = new ArrayList<>();
        private final List<CraftingAction> actions = new ArrayList<>();
        private String feedback = "";

        private Builder(ResourceLocation id) {
            this.id = id;
        }

        public Builder pattern(String... rows) {
            layers.add(rows);
            return this;
        }

        public Builder where(char symbol, Block block) {
            symbolBlocks.put(symbol, state -> state == block);
            return this;
        }

        public Builder where(char symbol, Predicate<Block> condition) {
            symbolBlocks.put(symbol, condition);
            return this;
        }

        public Builder center(char symbol, Block block) {
            this.centerSymbol = symbol;
            symbolBlocks.put(symbol, state -> state == block);
            return this;
        }

        public Builder center(char symbol, Predicate<Block> condition) {
            this.centerSymbol = symbol;
            symbolBlocks.put(symbol, condition);
            return this;
        }

        public Builder input(Item item) {
            this.inputPredicate = stack -> stack.is(item);
            return this;
        }

        public Builder input(Predicate<ItemStack> predicate) {
            this.inputPredicate = predicate;
            return this;
        }

        public Builder result(ItemStack... stacks) {
            for (ItemStack stack : stacks) results.add(stack.copy());
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

        public void register() {
            BlockCraftingRecipe.Builder builder = BlockCraftingRecipe.builder(id);

            for (String[] rows : layers) {
                builder.pattern(rows);
            }

            for (var entry : symbolBlocks.entrySet()) {
                builder.where(entry.getKey(), state -> entry.getValue().test(state.getBlock()));
            }

            builder.center(centerSymbol, state -> {
                Predicate<Block> cond = symbolBlocks.get(centerSymbol);
                return cond != null && cond.test(state.getBlock());
            });

            if (inputPredicate != null) {
                builder.craftingItem(inputPredicate);
            }

            for (ItemStack stack : results) {
                builder.resultItem(stack);
            }

            for (CraftingAction action : actions) {
                builder.resultAction(action);
            }

            if (!feedback.isEmpty()) {
                builder.feedback(feedback);
            }

            BlockCraftingRecipe recipe = builder.build();
            BlockCraftingRegistry.register(recipe);
            CNMCore.LOGGER.info("[BlockCrafting] Registered recipe: {}", id);
        }
    }
}