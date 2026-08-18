package com.onehumanawa.cnmcore.contents.wireless_redstone_control_terminal;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

/**
 * Stores the signal strength delivered by a bound OUT node. Persists so the endpoint keeps
 * powering its neighbours across chunk reloads even while the terminal is offline.
 * Remembers the owning terminal so its binding can be dropped when this block is destroyed.
 */
public class TerminalEndpointBlockEntity extends BlockEntity {
    private int strength;
    @Nullable
    private BlockPos owner;

    public TerminalEndpointBlockEntity(BlockPos pos, BlockState state) {
        super(TerminalRegistry.ENDPOINT_BLOCK_ENTITY.get(), pos, state);
    }

    public int getStrength() {
        return strength;
    }

    public void setStrength(int strength) {
        if (this.strength != strength) {
            this.strength = strength;
            setChanged();
        }
    }

    @Nullable
    public BlockPos getOwner() {
        return owner;
    }

    public void setOwner(BlockPos owner) {
        this.owner = owner;
        setChanged();
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putInt("strength", strength);
        if (owner != null) {
            tag.putLong("owner", owner.asLong());
        }
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        strength = tag.getInt("strength");
        owner = tag.contains("owner") ? BlockPos.of(tag.getLong("owner")) : null;
    }
}
