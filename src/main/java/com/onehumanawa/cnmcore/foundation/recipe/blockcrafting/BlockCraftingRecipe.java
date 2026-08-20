package com.onehumanawa.cnmcore.foundation.recipe.blockcrafting;

import com.onehumanawa.cnmcore.CNMCore;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

import java.util.List;

/**
 * Runtime block crafting recipe.
 * Stores block/item IDs as strings; they are resolved at match time.
 */
public class BlockCraftingRecipe {

    private final ResourceLocation id;
    private final List<PatternEntry> pattern;
    private final char centerSymbol;
    private final String itemInputId;
    private final List<String> resultIds;
    private final List<CraftingAction> actions;
    private final String feedback;
    private final boolean consumePattern;
    private final boolean consumeCenter;
    private final boolean consumeInput;

    public BlockCraftingRecipe(
            ResourceLocation id,
            List<PatternEntry> pattern,
            char centerSymbol,
            String itemInputId,
            List<String> resultIds,
            List<CraftingAction> actions,
            String feedback,
            boolean consumePattern,
            boolean consumeCenter,
            boolean consumeInput
    ) {
        this.id = id;
        this.pattern = List.copyOf(pattern);
        this.centerSymbol = centerSymbol;
        this.itemInputId = itemInputId;
        this.resultIds = List.copyOf(resultIds);
        this.actions = List.copyOf(actions);
        this.feedback = feedback;
        this.consumePattern = consumePattern;
        this.consumeCenter = consumeCenter;
        this.consumeInput = consumeInput;
    }

    public ResourceLocation id() { return id; }
    public List<PatternEntry> pattern() { return pattern; }
    public char centerSymbol() { return centerSymbol; }
    public String itemInputId() { return itemInputId; }
    public List<String> resultIds() { return resultIds; }
    public List<CraftingAction> actions() { return actions; }
    public String feedback() { return feedback; }
    public boolean consumePattern() { return consumePattern; }
    public boolean consumeCenter() { return consumeCenter; }
    public boolean consumeInput() { return consumeInput; }

    public record PatternEntry(Vec3i offset, String blockId, char symbol) {}

    /**
     * Whether the given pattern entry represents the center block of the structure,
     * matched by pattern symbol.
     */
    public boolean isCenter(PatternEntry entry) {
        return centerSymbol != 0 && entry.symbol() == centerSymbol;
    }

    public int matchingRotation(ServerLevel level, BlockPos center) {
        for (int rotation = 0; rotation < 4; rotation++) {
            boolean matches = true;
            for (PatternEntry entry : pattern) {
                Vec3i offset = rotate(entry.offset(), rotation);
                BlockState state = level.getBlockState(center.offset(offset));
                var block = resolveBlock(entry.blockId());
                if (block == null || !state.is(block)) {
                    matches = false;
                    break;
                }
            }
            if (matches) return rotation;
        }
        return -1;
    }

    public boolean matches(ServerLevel level, BlockPos center, ItemStack stack) {
        var item = resolveItem(itemInputId);
        if (item == null || !stack.is(item)) return false;
        return matchingRotation(level, center) >= 0;
    }

    public boolean craft(ServerLevel level, BlockPos center, ServerPlayer player, ItemStack inputStack, int rotation) {
        return craft(level, center, player, inputStack, rotation, false);
    }

    public boolean craft(ServerLevel level, BlockPos center, ServerPlayer player, ItemStack inputStack, int rotation, boolean isDeployer) {
        if (rotation < 0 || rotation > 3) return false;
        var inputItem = resolveItem(itemInputId);
        if (inputItem == null || !inputStack.is(inputItem)) return false;

        // Destroy pattern blocks
        for (PatternEntry entry : pattern) {
            Vec3i offset = rotate(entry.offset(), rotation);
            BlockPos target = center.offset(offset);
            if (target.equals(center) && !consumeCenter) continue;
            if (!target.equals(center) && !consumePattern) continue;
            level.destroyBlock(target, false);
        }

        // Consume input item
        if (consumeInput && !player.getAbilities().instabuild) {
            inputStack.shrink(1);
        }

        // Drop results only for normal players
        // For Deployer, results are handled by BlockCraftingEvents via addItem
        if (!isDeployer) {
            for (String resultId : resultIds) {
                var item = resolveItem(resultId);
                if (item != null) {
                    Block.popResource(level, center, new ItemStack(item, 1));
                }
            }
        }

        // Execute custom actions; a failing action must never break crafting
        for (CraftingAction action : actions) {
            try {
                action.execute(level, center, player);
            } catch (Exception e) {
                CNMCore.LOGGER.error("[BlockCrafting] Action failed for recipe {} at {}:", id, center, e);
            }
        }

        return true;
    }

    private static Vec3i rotate(Vec3i offset, int rotation) {
        int x = offset.getX();
        int z = offset.getZ();
        return switch (rotation & 3) {
            case 0 -> new Vec3i(x, offset.getY(), z);
            case 1 -> new Vec3i(-z, offset.getY(), x);
            case 2 -> new Vec3i(-x, offset.getY(), -z);
            default -> new Vec3i(z, offset.getY(), -x);
        };
    }

    /** Resolves a block id defensively; invalid or missing ids yield {@code null}. */
    private static Block resolveBlock(String blockId) {
        if (blockId == null || blockId.isBlank()) return null;
        ResourceLocation id = ResourceLocation.tryParse(blockId);
        if (id == null) return null;
        var block = BuiltInRegistries.BLOCK.get(id);
        return block == Blocks.AIR ? null : block;
    }

    /** Resolves an item id defensively; invalid or missing ids yield {@code null}. */
    private static Item resolveItem(String itemId) {
        if (itemId == null || itemId.isBlank()) return null;
        ResourceLocation id = ResourceLocation.tryParse(itemId);
        if (id == null) return null;
        var item = BuiltInRegistries.ITEM.get(id);
        return item == Items.AIR ? null : item;
    }
}