package com.onehumanawa.cnmcore.contents.wireless_redstone_control_terminal;

import com.onehumanawa.cnmcore.contents.wireless_redstone_control_terminal.circuit.Circuit;
import com.onehumanawa.cnmcore.contents.wireless_redstone_control_terminal.circuit.CircuitNode;
import com.onehumanawa.cnmcore.contents.wireless_redstone_control_terminal.circuit.NodeType;
import com.onehumanawa.cnmcore.contents.wireless_redstone_control_terminal.network.TerminalSyncPayload;
import com.onehumanawa.cnmcore.contents.wireless_redstone_control_terminal.program.ProgramType;
import com.onehumanawa.cnmcore.contents.wireless_redstone_control_terminal.program.TerminalProgram;
import com.simibubi.create.Create;
import com.simibubi.create.content.redstone.link.IRedstoneLinkable;
import com.simibubi.create.content.redstone.link.RedstoneLinkNetworkHandler;
import com.simibubi.create.content.redstone.link.RedstoneLinkNetworkHandler.Frequency;
import net.createmod.catnip.data.Couple;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
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
import net.neoforged.neoforge.network.PacketDistributor;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * The "smart brain" block entity: hosts many programs (GUI tabs) that all run in parallel.
 * Redstone programs simulate a logic circuit; every W_IN / W_OUT node joins Create's
 * Redstone Link network on its own frequency, identified by a ghost item reference
 * (never a physical item, so nothing is consumed or extractable).
 */
public class WirelessRedstoneControlTerminalBlockEntity extends BlockEntity implements MenuProvider {
    public static final int MAX_PROGRAMS = 16;
    private static final int SYNC_INTERVAL = 4;

    private final List<TerminalProgram> programs = new ArrayList<>();
    private int activeProgramId;
    private int nextProgramId;
    private final Set<ServerPlayer> openPlayers = new HashSet<>();
    /** One Redstone Link endpoint per wireless node, keyed by (programId, nodeId). */
    private final Map<Long, TerminalLink> links = new HashMap<>();

    private int outputStrength;
    private boolean chunkUnloaded;

    public WirelessRedstoneControlTerminalBlockEntity(BlockPos pos, BlockState state) {
        super(TerminalRegistry.TERMINAL_BLOCK_ENTITY.get(), pos, state);
        addProgram();
    }

    public List<TerminalProgram> getPrograms() {
        return programs;
    }

    public int getActiveProgramId() {
        return activeProgramId;
    }

    /** Client-side optimistic tab switch; the server confirms with the next sync. */
    public void setActiveProgramIdClient(int id) {
        activeProgramId = id;
    }

    @Nullable
    public TerminalProgram programById(int id) {
        for (TerminalProgram program : programs) {
            if (program.id == id) {
                return program;
            }
        }
        return null;
    }

    @Nullable
    public TerminalProgram activeProgram() {
        return programById(activeProgramId);
    }

    public int getOutputStrength() {
        return outputStrength;
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
        refreshLinks();

        int output = 0;
        for (TerminalProgram program : programs) {
            if (program.type != ProgramType.REDSTONE) {
                continue;
            }
            Circuit circuit = program.circuit;

            // 1. Refresh external sources (redstone inputs / per-node wireless receivers)
            for (CircuitNode node : circuit.getNodes()) {
                if (node.type == NodeType.INPUT) {
                    node.value = readRedstoneInput(node.config);
                } else if (node.type == NodeType.W_IN) {
                    TerminalLink link = links.get(linkKey(program.id, node.id));
                    if (link != null) {
                        node.value = link.receivedStrength;
                    }
                }
            }

            // 2. Simulate
            circuit.simulate();

            // 3. Apply outputs
            for (CircuitNode node : circuit.getNodes()) {
                if (node.type == NodeType.OUTPUT) {
                    output = Math.max(output, node.value);
                } else if (node.type == NodeType.W_OUT) {
                    TerminalLink link = links.get(linkKey(program.id, node.id));
                    if (link != null && link.lastTransmitted != node.value) {
                        link.lastTransmitted = node.value;
                        Create.REDSTONE_LINK_NETWORK_HANDLER.updateNetworkOf(level, link);
                    }
                }
            }
        }
        if (output != outputStrength) {
            outputStrength = output;
            level.updateNeighborsAt(worldPosition, getBlockState().getBlock());
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

    // Redstone Link network membership (one link per wireless node)

    private static long linkKey(int programId, int nodeId) {
        return ((long) programId << 32) | (nodeId & 0xFFFFFFFFL);
    }

    /** Brings the link map in line with the wireless nodes of all programs. Server side only. */
    private void refreshLinks() {
        Level level = getLevel();
        if (level == null || level.isClientSide) {
            return;
        }
        RedstoneLinkNetworkHandler handler = Create.REDSTONE_LINK_NETWORK_HANDLER;
        Set<Long> wanted = new HashSet<>();
        for (TerminalProgram program : programs) {
            if (program.type != ProgramType.REDSTONE) {
                continue;
            }
            for (CircuitNode node : program.circuit.getNodes()) {
                if (!node.type.isWireless()) {
                    continue;
                }
                long key = linkKey(program.id, node.id);
                wanted.add(key);
                if (!links.containsKey(key)) {
                    TerminalLink link = new TerminalLink(program.id, node.id, node.type == NodeType.W_IN);
                    links.put(key, link);
                    handler.addToNetwork(level, link);
                }
            }
        }
        Iterator<Map.Entry<Long, TerminalLink>> iterator = links.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<Long, TerminalLink> entry = iterator.next();
            if (!wanted.contains(entry.getKey())) {
                handler.removeFromNetwork(level, entry.getValue());
                iterator.remove();
            }
        }
    }

    /** Re-joins one node's link under its (changed) frequency. */
    private void reregisterLink(int programId, int nodeId) {
        Level level = getLevel();
        TerminalLink link = links.get(linkKey(programId, nodeId));
        if (level == null || level.isClientSide || link == null) {
            return;
        }
        RedstoneLinkNetworkHandler handler = Create.REDSTONE_LINK_NETWORK_HANDLER;
        handler.removeFromNetwork(level, link);
        link.refreshKey();
        handler.addToNetwork(level, link);
    }

    private void unregisterLinks() {
        Level level = getLevel();
        if (level == null || level.isClientSide) {
            return;
        }
        RedstoneLinkNetworkHandler handler = Create.REDSTONE_LINK_NETWORK_HANDLER;
        for (TerminalLink link : links.values()) {
            handler.removeFromNetwork(level, link);
        }
        links.clear();
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

    /** One wireless node's endpoint on Create's Redstone Link network. */
    private final class TerminalLink implements IRedstoneLinkable {
        final int programId;
        final int nodeId;
        final boolean listening;
        int receivedStrength;
        int lastTransmitted;
        private Couple<Frequency> networkKey = Couple.create(Frequency.EMPTY, Frequency.EMPTY);

        TerminalLink(int programId, int nodeId, boolean listening) {
            this.programId = programId;
            this.nodeId = nodeId;
            this.listening = listening;
            refreshKey();
        }

        @Nullable
        private CircuitNode node() {
            TerminalProgram program = programById(programId);
            return program == null ? null : program.circuit.nodeById(nodeId);
        }

        void refreshKey() {
            CircuitNode node = node();
            ItemStack frequency = node == null ? ItemStack.EMPTY : node.frequency;
            // One frequency item per node; the second slot stays empty, matching vanilla links
            // whose first frequency slot carries the same item.
            networkKey = Couple.create(Frequency.of(frequency), Frequency.EMPTY);
        }

        @Override
        public int getTransmittedStrength() {
            CircuitNode node = node();
            return listening || node == null ? 0 : node.value;
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
                    && linkLevel.getBlockEntity(worldPosition) == WirelessRedstoneControlTerminalBlockEntity.this
                    && node() != null;
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

    // Program management

    private boolean addProgram() {
        if (programs.size() >= MAX_PROGRAMS) {
            return false;
        }
        int id = nextProgramId++;
        programs.add(new TerminalProgram(id, String.valueOf(id + 1), ProgramType.REDSTONE));
        activeProgramId = id;
        return true;
    }

    public boolean createProgram() {
        Level level = getLevel();
        if (level == null || level.isClientSide || !addProgram()) {
            return false;
        }
        setChanged();
        syncToOpenPlayers();
        return true;
    }

    public boolean deleteProgram(int id) {
        Level level = getLevel();
        if (level == null || level.isClientSide || programs.size() <= 1) {
            return false;
        }
        TerminalProgram program = programById(id);
        if (program == null) {
            return false;
        }
        programs.remove(program);
        // Drop this program's links; refreshLinks will prune them from the map
        refreshLinks();
        if (activeProgramId == id) {
            activeProgramId = programs.get(0).id;
        }
        setChanged();
        syncToOpenPlayers();
        return true;
    }

    public boolean switchProgram(int id) {
        if (programById(id) == null || activeProgramId == id) {
            return false;
        }
        activeProgramId = id;
        setChanged();
        syncToOpenPlayers();
        return true;
    }

    // Circuit editing. Actions: 0=add(type,x,y), 1=move(id,x,y), 2=remove(id), 3=connect(target,port,source),
    // 4=disconnect(target,port), 5=setConfig(id,value), 6=clear, 7=addWaypoint(target,port,packed),
    // 8=createProgram, 9=deleteProgram(id), 10=switchProgram(id)

    public void handleEdit(int programId, int action, int a, int b, int c) {
        if (action == 8) {
            createProgram();
            return;
        }
        if (action == 9) {
            deleteProgram(a);
            return;
        }
        if (action == 10) {
            switchProgram(a);
            return;
        }
        TerminalProgram program = programById(programId);
        if (program == null || program.type != ProgramType.REDSTONE) {
            return;
        }
        Circuit circuit = program.circuit;
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
            if (action == 0 || action == 2) {
                refreshLinks();
            }
            setChanged();
            syncToOpenPlayers();
        }
    }

    /** Sets a wireless node's ghost frequency. Empty stack clears it. */
    public void setNodeFrequency(int programId, int nodeId, ItemStack stack) {
        Level level = getLevel();
        if (level == null || level.isClientSide) {
            return;
        }
        TerminalProgram program = programById(programId);
        if (program == null) {
            return;
        }
        CircuitNode node = program.circuit.nodeById(nodeId);
        if (node == null || !node.type.isWireless()) {
            return;
        }
        node.frequency = stack.isEmpty() ? ItemStack.EMPTY : stack.copyWithCount(1);
        reregisterLink(programId, nodeId);
        setChanged();
        syncToOpenPlayers();
    }

    public void playerOpened(ServerPlayer player) {
        openPlayers.add(player);
        syncToOpenPlayers();
    }

    public void playerClosed(ServerPlayer player) {
        openPlayers.remove(player);
    }

    // Sync

    /** Tab metadata of all programs, for the client tab bar. */
    public CompoundTag saveMeta() {
        CompoundTag tag = new CompoundTag();
        tag.putInt("active", activeProgramId);
        ListTag list = new ListTag();
        for (TerminalProgram program : programs) {
            list.add(program.saveMeta());
        }
        tag.put("programs", list);
        return tag;
    }

    public void syncToOpenPlayers() {
        Level level = getLevel();
        if (level == null || level.isClientSide || openPlayers.isEmpty()) {
            return;
        }
        TerminalProgram active = activeProgram();
        CompoundTag circuitTag = active != null && active.type == ProgramType.REDSTONE
                ? active.circuit.save(level.registryAccess()) : new CompoundTag();
        byte[] values = active != null && active.type == ProgramType.REDSTONE
                ? active.circuit.snapshotValues() : new byte[0];
        CustomPacketPayload payload = new TerminalSyncPayload(worldPosition, saveMeta(), circuitTag, values);
        for (ServerPlayer player : openPlayers) {
            PacketDistributor.sendToPlayer(player, payload);
        }
    }

    /** Applies a server snapshot on the client side. */
    public void applySync(CompoundTag meta, CompoundTag circuitTag, byte[] values) {
        Level level = getLevel();
        HolderLookup.Provider registries = level == null ? null : level.registryAccess();
        programs.clear();
        activeProgramId = meta.getInt("active");
        ListTag list = meta.getList("programs", Tag.TAG_COMPOUND);
        for (Tag element : list) {
            CompoundTag programTag = (CompoundTag) element;
            programs.add(new TerminalProgram(programTag.getInt("id"), programTag.getString("name"),
                    ProgramType.byName(programTag.getString("type"))));
        }
        if (programs.isEmpty()) {
            addProgram();
        } else if (programById(activeProgramId) == null) {
            activeProgramId = programs.get(0).id;
        }
        TerminalProgram active = activeProgram();
        if (registries != null && active != null && active.type == ProgramType.REDSTONE) {
            active.circuit.load(registries, circuitTag);
            active.circuit.applyValues(values);
        }
    }

    // Persistence

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putInt("nextProgramId", nextProgramId);
        tag.putInt("active", activeProgramId);
        ListTag list = new ListTag();
        for (TerminalProgram program : programs) {
            list.add(program.save(registries));
        }
        tag.put("programs", list);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        programs.clear();
        links.clear();
        if (tag.contains("programs")) {
            nextProgramId = tag.getInt("nextProgramId");
            activeProgramId = tag.getInt("active");
            ListTag list = tag.getList("programs", Tag.TAG_COMPOUND);
            for (Tag element : list) {
                programs.add(TerminalProgram.load(registries, (CompoundTag) element));
            }
        } else if (tag.contains("circuit")) {
            // Migration from the old single-circuit format
            nextProgramId = 1;
            activeProgramId = 0;
            TerminalProgram program = new TerminalProgram(0, "1", ProgramType.REDSTONE);
            program.circuit.load(registries, tag.getCompound("circuit"));
            programs.add(program);
        }
        if (programs.isEmpty()) {
            addProgram();
        } else if (programById(activeProgramId) == null) {
            activeProgramId = programs.get(0).id;
        }
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        return new CompoundTag();
    }
}
