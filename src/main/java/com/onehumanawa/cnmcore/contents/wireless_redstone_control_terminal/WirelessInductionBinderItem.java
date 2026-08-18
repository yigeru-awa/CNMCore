package com.onehumanawa.cnmcore.contents.wireless_redstone_control_terminal;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * Wireless Induction Binder. Two binding flows:
 * OUT: shift-right-click a redstone component to store it, then open the terminal and press
 * "Bind" on the selected OUT node; the terminal places its invisible endpoint next to it.
 * IN: shift-right-click the terminal to cycle through its INPUT nodes, then right-click a
 * redstone component to bind the selected input to it directly.
 * Sneak-right-clicking the air clears any stored state. The binder itself is never consumed.
 */
public class WirelessInductionBinderItem extends Item {
    public static final String TAG_BOUND_POS = "BoundPos";
    public static final String TAG_BOUND_DIM = "BoundDim";
    public static final String TAG_BOUND_FACE = "BoundFace";
    public static final String TAG_IN_TERMINAL = "InTerminal";
    public static final String TAG_IN_DIM = "InDim";
    public static final String TAG_IN_PROGRAM = "InProgram";
    public static final String TAG_IN_NODE = "InNode";

    public WirelessInductionBinderItem(Properties properties) {
        super(properties.stacksTo(1));
    }

    @Nullable
    public static BlockPos getBoundPos(ItemStack stack) {
        CustomData data = stack.get(DataComponents.CUSTOM_DATA);
        if (data == null) {
            return null;
        }
        CompoundTag tag = data.copyTag();
        return tag.contains(TAG_BOUND_POS) ? NbtUtils.readBlockPos(tag, TAG_BOUND_POS).orElse(null) : null;
    }

    @Nullable
    public static ResourceLocation getBoundDim(ItemStack stack) {
        CustomData data = stack.get(DataComponents.CUSTOM_DATA);
        if (data == null) {
            return null;
        }
        CompoundTag tag = data.copyTag();
        String dim = tag.getString(TAG_BOUND_DIM);
        return dim.isEmpty() ? null : ResourceLocation.tryParse(dim);
    }

    @Nullable
    public static Direction getBoundFace(ItemStack stack) {
        CustomData data = stack.get(DataComponents.CUSTOM_DATA);
        if (data == null) {
            return null;
        }
        CompoundTag tag = data.copyTag();
        return tag.contains(TAG_BOUND_FACE) ? Direction.from3DDataValue(tag.getByte(TAG_BOUND_FACE)) : null;
    }

    public static void setBound(ItemStack stack, BlockPos pos, Direction face, ResourceLocation dim) {
        stack.update(DataComponents.CUSTOM_DATA, CustomData.EMPTY, data -> {
            CompoundTag tag = data.copyTag();
            tag.put(TAG_BOUND_POS, NbtUtils.writeBlockPos(pos));
            tag.putString(TAG_BOUND_DIM, dim.toString());
            tag.putByte(TAG_BOUND_FACE, (byte) face.get3DDataValue());
            return CustomData.of(tag);
        });
    }

    public static void clearBound(ItemStack stack) {
        stack.remove(DataComponents.CUSTOM_DATA);
    }

    // Pending INPUT selection (shift-right-clicked terminal, waiting for a component click)

    @Nullable
    public static BlockPos getPendingTerminal(ItemStack stack) {
        CustomData data = stack.get(DataComponents.CUSTOM_DATA);
        if (data == null) {
            return null;
        }
        CompoundTag tag = data.copyTag();
        return tag.contains(TAG_IN_TERMINAL) ? BlockPos.of(tag.getLong(TAG_IN_TERMINAL)) : null;
    }

    @Nullable
    public static ResourceLocation getPendingDim(ItemStack stack) {
        CustomData data = stack.get(DataComponents.CUSTOM_DATA);
        if (data == null) {
            return null;
        }
        CompoundTag tag = data.copyTag();
        String dim = tag.getString(TAG_IN_DIM);
        return dim.isEmpty() ? null : ResourceLocation.tryParse(dim);
    }

    public static int getPendingProgram(ItemStack stack) {
        CustomData data = stack.get(DataComponents.CUSTOM_DATA);
        CompoundTag tag = data == null ? new CompoundTag() : data.copyTag();
        return tag.getInt(TAG_IN_PROGRAM);
    }

    public static int getPendingNode(ItemStack stack) {
        CustomData data = stack.get(DataComponents.CUSTOM_DATA);
        CompoundTag tag = data == null ? new CompoundTag() : data.copyTag();
        return tag.getInt(TAG_IN_NODE);
    }

    public static boolean hasPendingInput(ItemStack stack) {
        return getPendingTerminal(stack) != null;
    }

    public static void setPendingInput(ItemStack stack, BlockPos terminalPos, ResourceLocation dim, int programId, int nodeId) {
        stack.update(DataComponents.CUSTOM_DATA, CustomData.EMPTY, data -> {
            CompoundTag tag = data.copyTag();
            tag.putLong(TAG_IN_TERMINAL, terminalPos.asLong());
            tag.putString(TAG_IN_DIM, dim.toString());
            tag.putInt(TAG_IN_PROGRAM, programId);
            tag.putInt(TAG_IN_NODE, nodeId);
            return CustomData.of(tag);
        });
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Player player = context.getPlayer();
        if (player == null) {
            return InteractionResult.PASS;
        }
        Level level = context.getLevel();
        ItemStack stack = context.getItemInHand();
        BlockPos clicked = context.getClickedPos();
        BlockState state = level.getBlockState(clicked);
        // Shift on the terminal: cycle the INPUT node selection for the direct binding flow
        if (player.isShiftKeyDown() && state.hasBlockEntity()
                && level.getBlockEntity(clicked) instanceof WirelessRedstoneControlTerminalBlockEntity terminal) {
            if (!level.isClientSide) {
                terminal.selectNextInput(player, stack, level.dimension().location());
            }
            return InteractionResult.sidedSuccess(level.isClientSide);
        }
        // Pending INPUT selection: bind it to the clicked component directly
        if (hasPendingInput(stack)) {
            if (!level.isClientSide) {
                applyInputBinding(level, player, stack, clicked, context.getClickedFace());
            }
            return InteractionResult.sidedSuccess(level.isClientSide);
        }
        if (!player.isShiftKeyDown()) {
            return InteractionResult.PASS;
        }
        // Bind the clicked component itself; the terminal later places its invisible endpoint
        // in the empty space on the clicked face of that component
        if (level.isClientSide) {
            return InteractionResult.SUCCESS;
        }
        setBound(stack, clicked, context.getClickedFace(), level.dimension().location());
        level.playSound(null, clicked, SoundEvents.ITEM_FRAME_ADD_ITEM, SoundSource.BLOCKS, 0.25F, 0.1F);
        player.displayClientMessage(Component.translatable("cnmcore.wrt.binder.set",
                clicked.getX() + ", " + clicked.getY() + ", " + clicked.getZ()), true);
        return InteractionResult.CONSUME;
    }

    /** Writes the pending INPUT selection into the owning terminal and clears the pending state. */
    private static void applyInputBinding(Level level, Player player, ItemStack stack, BlockPos target, Direction face) {
        BlockPos terminalPos = getPendingTerminal(stack);
        ResourceLocation dim = getPendingDim(stack);
        if (terminalPos == null || dim == null || !dim.equals(level.dimension().location())) {
            return;
        }
        if (!level.isLoaded(terminalPos)
                || !(level.getBlockEntity(terminalPos) instanceof WirelessRedstoneControlTerminalBlockEntity terminal)) {
            player.displayClientMessage(Component.translatable("cnmcore.wrt.binder.offline"), true);
            return;
        }
        if (terminal.bindInput(getPendingProgram(stack), getPendingNode(stack), target, face)) {
            stack.update(DataComponents.CUSTOM_DATA, CustomData.EMPTY, data -> {
                CompoundTag tag = data.copyTag();
                tag.remove(TAG_IN_TERMINAL);
                tag.remove(TAG_IN_DIM);
                tag.remove(TAG_IN_PROGRAM);
                tag.remove(TAG_IN_NODE);
                return tag.isEmpty() ? CustomData.EMPTY : CustomData.of(tag);
            });
            level.playSound(null, target, SoundEvents.ITEM_FRAME_ADD_ITEM, SoundSource.BLOCKS, 0.25F, 0.3F);
            player.displayClientMessage(Component.translatable("cnmcore.wrt.binder.in.set",
                    target.getX() + ", " + target.getY() + ", " + target.getZ()), true);
        }
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (player.isShiftKeyDown() && (getBoundPos(stack) != null || hasPendingInput(stack))) {
            if (!level.isClientSide) {
                clearBound(stack);
                level.playSound(null, player.blockPosition(), SoundEvents.ITEM_FRAME_REMOVE_ITEM,
                        SoundSource.PLAYERS, 0.25F, 1.0F);
                player.displayClientMessage(Component.translatable("cnmcore.wrt.binder.cleared"), true);
            }
            return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
        }
        return InteractionResultHolder.pass(stack);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltipComponents, TooltipFlag tooltipFlag) {
        BlockPos pos = getBoundPos(stack);
        if (pos != null) {
            tooltipComponents.add(Component.translatable("cnmcore.wrt.binder.tooltip.bound",
                    pos.getX() + ", " + pos.getY() + ", " + pos.getZ()));
        }
        if (hasPendingInput(stack)) {
            tooltipComponents.add(Component.translatable("cnmcore.wrt.binder.tooltip.pending",
                    getPendingProgram(stack) + 1, getPendingNode(stack)));
        }
        if (pos == null && !hasPendingInput(stack)) {
            tooltipComponents.add(Component.translatable("cnmcore.wrt.binder.tooltip.hint"));
            tooltipComponents.add(Component.translatable("cnmcore.wrt.binder.tooltip.hint2"));
        }
    }
}
