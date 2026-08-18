package com.onehumanawa.cnmcore.contents.wireless_redstone_control_terminal;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

/**
 * Invisible delivery endpoint placed by the wireless induction binder flow. A bound OUT node of
 * the terminal pushes its signal strength into this block, which weakly powers its neighbours
 * exactly like a regular redstone power source. The terminal owns and removes it.
 */
public class TerminalEndpointBlock extends Block implements EntityBlock {

    public TerminalEndpointBlock(Properties properties) {
        super(properties);
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new TerminalEndpointBlockEntity(pos, state);
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        // Fully invisible: no model rendering at all, so the transparent texture can never show up black
        return RenderShape.INVISIBLE;
    }

    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
        // Replaced or destroyed by the world (e.g. a block placed into it): tell the owning terminal
        // to drop the binding, before the block entity gets removed by super
        if (!newState.is(state.getBlock()) && !level.isClientSide
                && level.getBlockEntity(pos) instanceof TerminalEndpointBlockEntity endpoint
                && endpoint.getOwner() != null && level.isLoaded(endpoint.getOwner())
                && level.getBlockEntity(endpoint.getOwner()) instanceof WirelessRedstoneControlTerminalBlockEntity terminal) {
            terminal.onEndpointRemoved(pos);
        }
        super.onRemove(state, level, pos, newState, movedByPiston);
    }

    @Override
    protected boolean isSignalSource(BlockState state) {
        return true;
    }

    @Override
    protected int getSignal(BlockState state, BlockGetter level, BlockPos pos, Direction direction) {
        return level.getBlockEntity(pos) instanceof TerminalEndpointBlockEntity endpoint
                ? endpoint.getStrength() : 0;
    }

    @Override
    protected int getDirectSignal(BlockState state, BlockGetter level, BlockPos pos, Direction direction) {
        // Weak-only, same reasoning as the terminal block itself
        return 0;
    }
}
