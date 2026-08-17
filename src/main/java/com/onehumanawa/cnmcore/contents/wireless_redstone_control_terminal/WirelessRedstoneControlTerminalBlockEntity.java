package com.onehumanawa.cnmcore.contents.wireless_redstone_control_terminal;

import com.onehumanawa.cnmcore.contents.wireless_redstone_control_terminal.circuit.Circuit;
import com.onehumanawa.cnmcore.contents.wireless_redstone_control_terminal.circuit.CircuitNode;
import com.onehumanawa.cnmcore.contents.wireless_redstone_control_terminal.circuit.NodeType;
import com.onehumanawa.cnmcore.contents.wireless_redstone_control_terminal.network.TerminalSyncPayload;
import com.simibubi.create.Create;
import com.simibubi.create.content.redstone.link.IRedstoneLinkable;
import com.simibubi.create.content.redstone.link.RedstoneLinkNetworkHandler;
import com.simibubi.create.content.redstone.link.RedstoneLinkNetworkHandler.Frequency;
import net.createmod.catnip.data.Couple;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.items.ItemStackHandler;
import net.neoforged.neoforge.network.PacketDistributor;
import org.jetbrains.annotations.Nullable;

import java.util.HashSet;
import java.util.Set;

/**
 * Holds the programmed circuit and joins Create's Redstone Link network with two frequency bands:
 * one receiving band (read by W_IN nodes) and one transmitting band (written by W_OUT nodes).
 * Like vanilla Redstone Links, each band is identified by a pair of arbitrary items.
 */
public class WirelessRedstoneControlTerminalBlockEntity extends BlockEntity implements MenuProvider {
    public static final int SLOT_RX_FIRST = 0;
    public static final int SLOT_RX_SECOND = 1;
    public static final int SLOT_TX_FIRST = 2;
    public static final int SLOT_TX_SECOND = 3;
    private static final int SYNC_INTERVAL = 4;

    private final Circuit circuit = new Circuit();
    private final Set<ServerPlayer> openPlayers = new HashSet<>();
    private final TerminalLink rxLink = new TerminalLink(true);
    private final TerminalLink txLink = new TerminalLink(false);

    private int receivedStrength;
    private int transmitStrength;
    private int outputStrength;
    private boolean linksRegistered;
    private boolean chunkUnloaded;

    public final ItemStackHandler frequencySlots = new ItemStackHandler(4) {
        @Override
        protected void onContentsChanged(int slot) {
            onFrequencyChanged();
        }
    };

    public WirelessRedstoneControlTerminalBlockEntity(BlockPos pos, BlockState state) {
        super(TerminalRegistry.TERMINAL_BLOCK_ENTITY.get(), pos, state);
    }

    public Circuit getCircuit() {
        return circuit;
    }

    public int getOutputStrength() {
        return outputStrength;
    }

    public int getReceivedStrength() {
        return receivedStrength;
    }

    @Override
    public Component getDisplayName() {
        return getBlockState().getBlock().getName();
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
        if (player instanceof ServerPlayer serverPlayer) {
            playerOpened(serverPlayer);
        }
        return new WirelessRedstoneControlTerminalMenu(containerId, playerInventory, this);
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, WirelessRedstoneControlTerminalBlockEntity terminal) {
        terminal.tickServer();
    }

    private void tickServer() {
        Level level = getLevel();
        if (level == null) {
            return;
        }
        registerLinks();

        // 1. Refresh external sources (redstone inputs / wireless receiver band)
        for (CircuitNode node : circuit.getNodes()) {
            if (node.type == NodeType.INPUT) {
                node.value = readRedstoneInput(node.config);
            } else if (node.type == NodeType.W_IN) {
                node.value = receivedStrength;
            }
        }

        // 2. Simulate the circuit
        circuit.simulate();

        // 3. Apply outputs
        int output = 0;
        int transmit = 0;
        for (CircuitNode node : circuit.getNodes()) {
            if (node.type == NodeType.OUTPUT) {
                output = Math.max(output, node.value);
            } else if (node.type == NodeType.W_OUT) {
                transmit = Math.max(transmit, node.value);
            }
        }
        if (output != outputStrength) {
            outputStrength = output;
            level.updateNeighborsAt(worldPosition, getBlockState().getBlock());
        }
        if (transmit != transmitStrength) {
            transmitStrength = transmit;
            if (linksRegistered) {
                Create.REDSTONE_LINK_NETWORK_HANDLER.updateNetworkOf(level, txLink);
            }
        }

        // 4. Push state to open screens
        if (!openPlayers.isEmpty() && level.getGameTime() % SYNC_INTERVAL == 0) {
            syncToOpenPlayers();
        }
    }

    /** Reads the redstone input for an INPUT node. Config 0 = any side, 1-6 = specific Direction. */
    private int readRedstoneInput(int side) {
        Level level = getLevel();
        if (side <= 0) {
            return level.getBestNeighborSignal(worldPosition);
        }
        Direction direction = Direction.from3DDataValue(side - 1);
        return level.getSignal(worldPosition.relative(direction), direction);
    }

    // Redstone Link network membership

    private void registerLinks() {
        Level level = getLevel();
        if (level == null || level.isClientSide || linksRegistered) {
            return;
        }
        rxLink.refreshKey();
        txLink.refreshKey();
        RedstoneLinkNetworkHandler handler = Create.REDSTONE_LINK_NETWORK_HANDLER;
        handler.addToNetwork(level, rxLink);
        handler.addToNetwork(level, txLink);
        linksRegistered = true;
    }

    private void unregisterLinks() {
        Level level = getLevel();
        if (level == null || !linksRegistered) {
            return;
        }
        RedstoneLinkNetworkHandler handler = Create.REDSTONE_LINK_NETWORK_HANDLER;
        handler.removeFromNetwork(level, rxLink);
        handler.removeFromNetwork(level, txLink);
        linksRegistered = false;
    }

    private void onFrequencyChanged() {
        setChanged();
        Level level = getLevel();
        if (level == null || level.isClientSide || !linksRegistered) {
            return;
        }
        // Re-join the network under the new frequency keys
        RedstoneLinkNetworkHandler handler = Create.REDSTONE_LINK_NETWORK_HANDLER;
        handler.removeFromNetwork(level, rxLink);
        handler.removeFromNetwork(level, txLink);
        rxLink.refreshKey();
        txLink.refreshKey();
        handler.addToNetwork(level, rxLink);
        handler.addToNetwork(level, txLink);
    }

    @Override
    public void setRemoved() {
        super.setRemoved();
        unregisterLinks();
    }

    @Override
    public void onChunkUnloaded() {
        super.onChunkUnloaded();
        chunkUnloaded = true;
        unregisterLinks();
    }

    /** One endpoint of the terminal on Create's Redstone Link network. */
    private final class TerminalLink implements IRedstoneLinkable {
        private final boolean listening;
        private Couple<Frequency> networkKey = Couple.create(Frequency.EMPTY, Frequency.EMPTY);

        TerminalLink(boolean listening) {
            this.listening = listening;
        }

        void refreshKey() {
            int firstSlot = listening ? SLOT_RX_FIRST : SLOT_TX_FIRST;
            int secondSlot = listening ? SLOT_RX_SECOND : SLOT_TX_SECOND;
            networkKey = Couple.create(
                    Frequency.of(frequencySlots.getStackInSlot(firstSlot)),
                    Frequency.of(frequencySlots.getStackInSlot(secondSlot)));
        }

        @Override
        public int getTransmittedStrength() {
            return listening ? 0 : transmitStrength;
        }

        @Override
        public void setReceivedStrength(int power) {
            if (listening) {
                receivedStrength = power;
            }
        }

        @Override
        public boolean isListening() {
            return listening;
        }

        @Override
        public boolean isAlive() {
            Level linkLevel = getLevel();
            return !chunkUnloaded && !isRemoved() && linkLevel != null && linkLevel.isLoaded(worldPosition)
                    && linkLevel.getBlockEntity(worldPosition) == WirelessRedstoneControlTerminalBlockEntity.this;
        }

        @Override
        public Couple<Frequency> getNetworkKey() {
            return networkKey;
        }

        @Override
        public BlockPos getLocation() {
            return worldPosition;
        }
    }

    // Circuit editing (0=add, 1=move, 2=remove, 3=connect, 4=disconnect, 5=setConfig, 6=clear, 7=addWaypoint)

    public void handleEdit(int action, int a, int b, int c) {
        boolean changed = switch (action) {
            case 0 -> circuit.addNode(NodeType.byId(a), b, c) != null;
            case 1 -> circuit.moveNode(a, b, c);
            case 2 -> circuit.removeNode(a);
            case 3 -> circuit.connect(a, b, c);
            case 4 -> circuit.disconnect(a, b);
            case 5 -> circuit.setConfig(a, b);
            case 6 -> {
                circuit.clear();
                yield true;
            }
            case 7 -> circuit.addWaypoint(a, b, c);
            default -> false;
        };
        if (changed) {
            setChanged();
            syncToOpenPlayers();
        }
    }

    public void playerOpened(ServerPlayer player) {
        openPlayers.add(player);
        syncToOpenPlayers();
    }

    public void playerClosed(ServerPlayer player) {
        openPlayers.remove(player);
    }

    public void syncToOpenPlayers() {
        Level level = getLevel();
        if (level == null || level.isClientSide || openPlayers.isEmpty()) {
            return;
        }
        CustomPacketPayload payload = new TerminalSyncPayload(worldPosition, circuit.save(), circuit.snapshotValues());
        for (ServerPlayer player : openPlayers) {
            PacketDistributor.sendToPlayer(player, payload);
        }
    }

    /** Applies a server snapshot on the client side. */
    public void applySync(CompoundTag circuitTag, byte[] values) {
        circuit.load(circuitTag);
        circuit.applyValues(values);
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.put("circuit", circuit.save());
        tag.put("freq", frequencySlots.serializeNBT(registries));
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        circuit.load(tag.getCompound("circuit"));
        frequencySlots.deserializeNBT(registries, tag.getCompound("freq"));
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        return new CompoundTag();
    }
}
