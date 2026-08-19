package com.onehumanawa.cnmcore.foundation.recipe.blockcrafting;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

import java.util.*;

/**
 * A block crafting recipe registered by Java code.
 * Each string in pattern represents an X layer; its characters represent Y rows and Z positions.
 * Spaces ignore positions, and the structure is tested in all four rotations around the Y axis.
 */
@SuppressWarnings("unused")
public final class BlockCraftingRecipe {
    public record PatternEntry(Vec3i offset, java.util.function.Predicate<BlockState> condition, char symbol) {}
    public record Match(BlockCraftingRecipe recipe, int rotation) {}

    private final net.minecraft.resources.ResourceLocation id;
    private final List<PatternEntry> pattern;
    private final java.util.function.Predicate<ItemStack> input;
    private final List<ItemStack> results;
    private final List<CraftingAction> actions;
    private final String feedback;
    private final java.util.function.Predicate<BlockState> centerCondition;

    private BlockCraftingRecipe(net.minecraft.resources.ResourceLocation id, List<PatternEntry> pattern,
                                java.util.function.Predicate<ItemStack> input, List<ItemStack> results,
                                List<CraftingAction> actions, String feedback,
                                java.util.function.Predicate<BlockState> centerCondition) {
        this.id = id;
        this.pattern = List.copyOf(pattern);
        this.input = input;
        this.results = results.stream().map(ItemStack::copy).toList();
        this.actions = List.copyOf(actions);
        this.feedback = feedback;
        this.centerCondition = centerCondition;
    }

    public net.minecraft.resources.ResourceLocation id() { return id; }
    public List<PatternEntry> pattern() { return pattern; }
    public List<ItemStack> results() { return results.stream().map(ItemStack::copy).toList(); }
    public String feedback() { return feedback; }
    public boolean matchesInput(ItemStack stack) { return !stack.isEmpty() && input.test(stack); }
    public boolean matchesCenter(BlockState state) { return centerCondition.test(state); }

    public int matchingRotation(ServerLevel level, BlockPos center, ItemStack stack) {
        if (!matchesInput(stack)) return -1;
        for (int rotation = 0; rotation < 4; rotation++) {
            boolean matches = true;
            for (PatternEntry entry : pattern) {
                Vec3i offset = rotate(entry.offset(), rotation);
                if (!entry.condition().test(level.getBlockState(center.offset(offset)))) {
                    matches = false;
                    break;
                }
            }
            if (matches) return rotation;
        }
        return -1;
    }

    public boolean matches(ServerLevel level, BlockPos center, ItemStack stack) {
        return matchingRotation(level, center, stack) >= 0;
    }

    public boolean craft(ServerLevel level, BlockPos center, ServerPlayer player, ItemStack inputStack, int rotation) {
        if (rotation < 0 || rotation > 3 || !matchesInput(inputStack)) return false;
        for (PatternEntry entry : pattern) {
            level.destroyBlock(center.offset(rotate(entry.offset(), rotation)), false);
        }
        if (!player.getAbilities().instabuild) inputStack.shrink(1);
        for (ItemStack result : results) {
            if (!result.isEmpty()) Block.popResource(level, center, result.copy());
        }
        for (CraftingAction action : actions) action.execute(level, center, player);
        return true;
    }

    public static Vec3i rotate(Vec3i offset, int rotation) {
        int x = offset.getX();
        int z = offset.getZ();
        return switch (rotation & 3) {
            case 0 -> new Vec3i(x, offset.getY(), z);
            case 1 -> new Vec3i(-z, offset.getY(), x);
            case 2 -> new Vec3i(-x, offset.getY(), -z);
            default -> new Vec3i(z, offset.getY(), -x);
        };
    }

    public static Builder builder(net.minecraft.resources.ResourceLocation id) {
        return new Builder(id);
    }

    public static final class Builder {
        private final net.minecraft.resources.ResourceLocation id;
        private final List<List<String>> layers = new ArrayList<>();
        private final Map<Character, java.util.function.Predicate<BlockState>> symbols = new HashMap<>();
        private char centerSymbol = 0;
        private java.util.function.Predicate<BlockState> centerCondition;
        private java.util.function.Predicate<ItemStack> input = stack -> false;
        private final List<ItemStack> results = new ArrayList<>();
        private final List<CraftingAction> actions = new ArrayList<>();
        private String feedback = "";

        private Builder(net.minecraft.resources.ResourceLocation id) {
            this.id = Objects.requireNonNull(id, "id");
        }

        public Builder pattern(String... rows) {
            if (rows.length == 0) throw new IllegalArgumentException("A pattern layer cannot be empty");
            layers.add(List.of(rows));
            return this;
        }

        public Builder center(char symbol, Block block) {
            return center(symbol, state -> state.is(block));
        }

        public Builder center(char symbol, java.util.function.Predicate<BlockState> condition) {
            centerSymbol = symbol;
            centerCondition = Objects.requireNonNull(condition, "condition");
            symbols.put(symbol, condition);
            return this;
        }

        public Builder where(char symbol, Block block) {
            return where(symbol, state -> state.is(block));
        }

        public Builder where(char symbol, java.util.function.Predicate<BlockState> condition) {
            symbols.put(symbol, Objects.requireNonNull(condition, "condition"));
            return this;
        }

        public Builder craftingItem(Item item) {
            return craftingItem(stack -> stack.is(item));
        }

        public Builder craftingItem(java.util.function.Predicate<ItemStack> condition) {
            input = Objects.requireNonNull(condition, "condition");
            return this;
        }

        public Builder resultItem(ItemStack... stacks) {
            for (ItemStack stack : stacks) results.add(stack.copy());
            return this;
        }

        public Builder resultAction(CraftingAction action) {
            actions.add(Objects.requireNonNull(action, "action"));
            return this;
        }

        public Builder feedback(String message) {
            feedback = message == null ? "" : message;
            return this;
        }

        public BlockCraftingRecipe build() {
            if (layers.isEmpty()) throw new IllegalStateException("Block crafting recipe is missing pattern: " + id);
            if (centerSymbol == 0 || centerCondition == null) {
                throw new IllegalStateException("Block crafting recipe is missing center: " + id);
            }
            if (results.isEmpty() && actions.isEmpty()) {
                throw new IllegalStateException("A block crafting recipe needs at least one result or action: " + id);
            }
            List<PatternEntry> entries = new ArrayList<>();
            Vec3i center = null;
            for (int x = 0; x < layers.size(); x++) {
                List<String> rows = layers.get(x);
                for (int y = 0; y < rows.size(); y++) {
                    String row = rows.get(y);
                    for (int z = 0; z < row.length(); z++) {
                        char symbol = row.charAt(z);
                        if (symbol == ' ') continue;
                        java.util.function.Predicate<BlockState> condition = symbols.get(symbol);
                        if (condition == null) throw new IllegalStateException("Undefined block symbol " + symbol + ": " + id);
                        Vec3i offset = new Vec3i(x, y, -z);
                        if (symbol == centerSymbol) center = offset;
                        entries.add(new PatternEntry(offset, condition, symbol));
                    }
                }
            }
            if (center == null) throw new IllegalStateException("Pattern contains no center symbol: " + id);
            int cx = center.getX(), cy = center.getY(), cz = center.getZ();
            List<PatternEntry> normalized = entries.stream()
                    .map(entry -> new PatternEntry(new Vec3i(entry.offset().getX() - cx,
                            entry.offset().getY() - cy, entry.offset().getZ() - cz),
                            entry.condition(), entry.symbol()))
                    .toList();
            return new BlockCraftingRecipe(id, normalized, input, results, actions, feedback, centerCondition);
        }
    }
}