package com.onehumanawa.cnmcore.contents.wireless_redstone_control_terminal;

import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemStackHandler;
import net.neoforged.neoforge.items.SlotItemHandler;
import org.jetbrains.annotations.Nullable;

/**
 * Container for the wireless redstone control terminal.
 * Slots 0-3 hold the two Redstone Link frequency pairs (receiving first/second, transmitting first/second).
 */
public class WirelessRedstoneControlTerminalMenu extends AbstractContainerMenu {
    public static final int FREQUENCY_SLOT_COUNT = 4;
    @Nullable
    public final WirelessRedstoneControlTerminalBlockEntity blockEntity;
    private final ItemStackHandler dummyHandler = new ItemStackHandler(FREQUENCY_SLOT_COUNT);

    public WirelessRedstoneControlTerminalMenu(int containerId, Inventory playerInventory,
            @Nullable WirelessRedstoneControlTerminalBlockEntity blockEntity) {
        super(TerminalRegistry.TERMINAL_MENU.get(), containerId);
        this.blockEntity = blockEntity;

        IItemHandler handler = blockEntity != null ? blockEntity.frequencySlots : dummyHandler;
        // Receiving band
        addSlot(new SlotItemHandler(handler, WirelessRedstoneControlTerminalBlockEntity.SLOT_RX_FIRST, 8, 8));
        addSlot(new SlotItemHandler(handler, WirelessRedstoneControlTerminalBlockEntity.SLOT_RX_SECOND, 26, 8));
        // Transmitting band
        addSlot(new SlotItemHandler(handler, WirelessRedstoneControlTerminalBlockEntity.SLOT_TX_FIRST, 64, 8));
        addSlot(new SlotItemHandler(handler, WirelessRedstoneControlTerminalBlockEntity.SLOT_TX_SECOND, 82, 8));

        int inventoryX = 62;
        int inventoryY = 158;
        for (int row = 0; row < 3; row++) {
            for (int column = 0; column < 9; column++) {
                addSlot(new Slot(playerInventory, column + row * 9 + 9, inventoryX + column * 18, inventoryY + row * 18));
            }
        }
        for (int column = 0; column < 9; column++) {
            addSlot(new Slot(playerInventory, column, inventoryX + column * 18, inventoryY + 58));
        }
    }

    public static WirelessRedstoneControlTerminalMenu fromNetwork(int containerId, Inventory playerInventory, FriendlyByteBuf buffer) {
        BlockPos pos = buffer.readBlockPos();
        BlockEntity blockEntity = playerInventory.player.level().getBlockEntity(pos);
        return new WirelessRedstoneControlTerminalMenu(containerId, playerInventory,
                blockEntity instanceof WirelessRedstoneControlTerminalBlockEntity terminal ? terminal : null);
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        ItemStack result = ItemStack.EMPTY;
        Slot slot = this.slots.get(index);
        if (slot != null && slot.hasItem()) {
            ItemStack stack = slot.getItem();
            result = stack.copy();
            if (index < FREQUENCY_SLOT_COUNT) {
                if (!this.moveItemStackTo(stack, FREQUENCY_SLOT_COUNT, this.slots.size(), true)) {
                    return ItemStack.EMPTY;
                }
            } else {
                // Try to place into a free frequency slot, one item at a time
                ItemStack single = stack.copyWithCount(1);
                if (this.moveItemStackTo(single, 0, FREQUENCY_SLOT_COUNT, false)) {
                    stack.shrink(1);
                    slot.setChanged();
                    if (stack.isEmpty()) {
                        return result;
                    }
                }
                if (!this.moveItemStackTo(stack, FREQUENCY_SLOT_COUNT, this.slots.size(), true)) {
                    return ItemStack.EMPTY;
                }
            }
            if (stack.isEmpty()) {
                slot.set(ItemStack.EMPTY);
            } else {
                slot.setChanged();
            }
        }
        return result;
    }

    @Override
    public boolean stillValid(Player player) {
        return blockEntity != null && blockEntity.getLevel() == player.level()
                && player.distanceToSqr(Vec3.atCenterOf(blockEntity.getBlockPos())) <= 64.0D;
    }

    @Override
    public void removed(Player player) {
        super.removed(player);
        if (blockEntity != null && player instanceof net.minecraft.server.level.ServerPlayer serverPlayer) {
            blockEntity.playerClosed(serverPlayer);
        }
    }
}
