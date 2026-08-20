package com.onehumanawa.cnmcore.foundation.recipe.blockcrafting;

import com.onehumanawa.cnmcore.CNMCore;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

import java.util.*;

public class BlockCraftingRecipe {

    private final ResourceLocation id;
    private final List<PatternEntry> pattern;
    private final String centerId;
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
            String centerId,
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
        this.centerId = centerId;
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
    public String centerId() { return centerId; }
    public String itemInputId() { return itemInputId; }
    public List<String> resultIds() { return resultIds; }
    public List<CraftingAction> actions() { return actions; }
    public String feedback() { return feedback; }
    public boolean consumePattern() { return consumePattern; }
    public boolean consumeCenter() { return consumeCenter; }
    public boolean consumeInput() { return consumeInput; }

    public record PatternEntry(Vec3i offset, String blockId, char symbol) {}

    public int matchingRotation(ServerLevel level, BlockPos center) {
        for (int rotation = 0; rotation < 4; rotation++) {
            boolean matches = true;
            for (PatternEntry entry : pattern) {
                Vec3i offset = rotate(entry.offset(), rotation);
                BlockPos target = center.offset(offset);
                BlockState state = level.getBlockState(target);
                var block = BuiltInRegistries.BLOCK.get(ResourceLocation.parse(entry.blockId()));
                CNMCore.LOGGER.info("[BlockCrafting] rotation={}, target={}, expected={}, actual={}",
                        rotation, target, entry.blockId(), state.getBlock().getDescriptionId());
                if (block == null || block == Blocks.AIR || !state.is(block)) {
                    matches = false;
                    break;
                }
            }
            if (matches) return rotation;
        }
        return -1;
    }

    public boolean matches(ServerLevel level, BlockPos center, ItemStack stack) {
        var item = BuiltInRegistries.ITEM.get(ResourceLocation.parse(itemInputId));
        CNMCore.LOGGER.info("[BlockCrafting] matches: itemInputId={}, resolvedItem={}, stack={}",
                itemInputId, item, stack.getHoverName().getString());
        if (item == null || !stack.is(item)) {
            CNMCore.LOGGER.info("[BlockCrafting]   item mismatch");
            return false;
        }
        int rotation = matchingRotation(level, center);
        CNMCore.LOGGER.info("[BlockCrafting]   rotation={}", rotation);
        return rotation >= 0;
    }

    public boolean craft(ServerLevel level, BlockPos center, ServerPlayer player, ItemStack inputStack, int rotation) {
        if (rotation < 0 || rotation > 3) return false;
        var inputItem = BuiltInRegistries.ITEM.get(ResourceLocation.parse(itemInputId));
        if (inputItem == null || !inputStack.is(inputItem)) return false;

        for (PatternEntry entry : pattern) {
            Vec3i offset = rotate(entry.offset(), rotation);
            BlockPos target = center.offset(offset);
            if (target.equals(center) && !consumeCenter) continue;
            if (!target.equals(center) && !consumePattern) continue;
            level.destroyBlock(target, false);
        }

        if (consumeInput && !player.getAbilities().instabuild) {
            inputStack.shrink(1);
        }

        for (String resultId : resultIds) {
            var item = BuiltInRegistries.ITEM.get(ResourceLocation.parse(resultId));
            if (item != null && item != net.minecraft.world.item.Items.AIR) {
                net.minecraft.world.level.block.Block.popResource(level, center, new ItemStack(item, 1));
            }
        }

        for (CraftingAction action : actions) {
            action.execute(level, center, player);
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
}