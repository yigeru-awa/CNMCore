package com.onehumanawa.cnmcore.contents.wireless_redstone_control_terminal;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Containers;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.Nullable;

/**
 * Wireless Redstone Control Terminal: hosts a programmable redstone logic circuit
 * connected to Create's wireless Redstone Link network. Model and textures are provided externally.
 */
public class WirelessRedstoneControlTerminalBlock extends Block implements EntityBlock {

    public WirelessRedstoneControlTerminalBlock(Properties properties) {
        super(properties);
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new WirelessRedstoneControlTerminalBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return level.isClientSide ? null
                : createTickerHelper(type, TerminalRegistry.TERMINAL_BLOCK_ENTITY.get(),
                        WirelessRedstoneControlTerminalBlockEntity::serverTick);
    }

    @Nullable
    @SuppressWarnings("unchecked")
    private static <T extends BlockEntity> BlockEntityTicker<T> createTickerHelper(BlockEntityType<T> blockType,
            BlockEntityType<? extends WirelessRedstoneControlTerminalBlockEntity> terminalType,
            BlockEntityTicker<? super WirelessRedstoneControlTerminalBlockEntity> ticker) {
        return terminalType == blockType ? (BlockEntityTicker<T>) (BlockEntityTicker<?>) ticker : null;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hitResult) {
        if (!level.isClientSide && player instanceof ServerPlayer serverPlayer
                && level.getBlockEntity(pos) instanceof WirelessRedstoneControlTerminalBlockEntity terminal) {
            serverPlayer.openMenu(terminal, buf -> buf.writeBlockPos(pos));
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    // Redstone output: strongest active OUTPUT node weakly powers every face
    @Override
    protected int getSignal(BlockState state, BlockGetter level, BlockPos pos, Direction direction) {
        return level.getBlockEntity(pos) instanceof WirelessRedstoneControlTerminalBlockEntity terminal
                ? terminal.getOutputStrength() : 0;
    }

    @Override
    protected int getDirectSignal(BlockState state, BlockGetter level, BlockPos pos, Direction direction) {
        // Weak-only output. Strong power would make adjacent conductor blocks carry our signal
        // back into getBestNeighborSignal, so the terminal would read its own output as input.
        return 0;
    }

    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean isMoving) {
        if (!state.is(newState.getBlock())) {
            if (level.getBlockEntity(pos) instanceof WirelessRedstoneControlTerminalBlockEntity terminal) {
                for (int i = 0; i < terminal.frequencySlots.getSlots(); i++) {
                    Containers.dropItemStack(level, pos.getX(), pos.getY(), pos.getZ(),
                            terminal.frequencySlots.getStackInSlot(i));
                }
            }
            super.onRemove(state, level, pos, newState, isMoving);
        }
    }
}
