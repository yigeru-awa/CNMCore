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
import org.jetbrains.annotations.Nullable;

/**
 * Container for the wireless redstone control terminal.
 * It holds no terminal slots of its own: wireless frequencies are ghost references set from
 * the GUI (JEI drag or click with a held item) and can never be consumed or extracted.
 */
public class WirelessRedstoneControlTerminalMenu extends AbstractContainerMenu {
    @Nullable
    public final WirelessRedstoneControlTerminalBlockEntity blockEntity;

    public WirelessRedstoneControlTerminalMenu(int containerId, Inventory playerInventory,
            @Nullable WirelessRedstoneControlTerminalBlockEntity blockEntity) {
        super(TerminalRegistry.TERMINAL_MENU.get(), containerId);
        this.blockEntity = blockEntity;

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
        // Only player inventory slots exist; nothing to move into the terminal
        return ItemStack.EMPTY;
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
