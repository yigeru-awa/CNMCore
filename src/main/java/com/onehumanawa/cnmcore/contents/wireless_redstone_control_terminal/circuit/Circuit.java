package com.onehumanawa.cnmcore.contents.wireless_redstone_control_terminal.circuit;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.util.Mth;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * The programmed circuit of a wireless redstone control terminal.
 * Contains the node graph and the simulation routine.
 */
public class Circuit {
    public static final int MAX_NODES = 96;
    /** Editable canvas area size (pixels inside the GUI, scrollable). */
    public static final int AREA_WIDTH = 256;
    public static final int AREA_HEIGHT = 192;
    public static final int NODE_WIDTH = 36;
    public static final int NODE_MIN_HEIGHT = 16;
    /** Maximum custom waypoints per wire. */
    public static final int MAX_WAYPOINTS = 4;

    private final List<CircuitNode> nodes = new ArrayList<>();
    private final Map<Integer, CircuitNode> byId = new HashMap<>();
    private int nextId;

    public List<CircuitNode> getNodes() {
        return nodes;
    }

    @Nullable
    public CircuitNode nodeById(int id) {
        return byId.get(id);
    }

    @Nullable
    public CircuitNode addNode(NodeType type, int x, int y) {
        if (nodes.size() >= MAX_NODES || type == null) {
            return null;
        }
        CircuitNode node = new CircuitNode(nextId++, type, clampX(x), clampY(y));
        nodes.add(node);
        byId.put(node.id, node);
        return node;
    }

    public boolean removeNode(int id) {
        CircuitNode node = byId.remove(id);
        if (node == null) {
            return false;
        }
        nodes.remove(node);
        // Detach all wires pointing to the removed node
        for (CircuitNode other : nodes) {
            for (int i = 0; i < other.inputs.length; i++) {
                if (other.inputs[i] == id) {
                    other.inputs[i] = -1;
                    other.waypoints[i] = null;
                }
            }
        }
        return true;
    }

    public boolean moveNode(int id, int x, int y) {
        CircuitNode node = byId.get(id);
        if (node == null) {
            return false;
        }
        node.x = clampX(x);
        node.y = clampY(y);
        return true;
    }

    public boolean connect(int targetId, int port, int sourceId) {
        CircuitNode target = byId.get(targetId);
        CircuitNode source = byId.get(sourceId);
        if (target == null || source == null || target == source) {
            return false;
        }
        if (port < 0 || port >= target.inputs.length) {
            return false;
        }
        target.inputs[port] = sourceId;
        target.waypoints[port] = null;
        return true;
    }

    public boolean disconnect(int targetId, int port) {
        CircuitNode target = byId.get(targetId);
        if (target == null || port < 0 || port >= target.inputs.length) {
            return false;
        }
        target.inputs[port] = -1;
        target.waypoints[port] = null;
        return true;
    }

    /**
     * Appends a custom waypoint to an existing wire. The waypoint is packed as {@code (x << 8) | y}.
     */
    public boolean addWaypoint(int targetId, int port, int packedWaypoint) {
        CircuitNode target = byId.get(targetId);
        if (target == null || port < 0 || port >= target.inputs.length || target.inputs[port] < 0) {
            return false;
        }
        int[] existing = target.waypoints[port];
        int count = existing == null ? 0 : existing.length;
        if (count >= MAX_WAYPOINTS) {
            return false;
        }
        int[] updated = Arrays.copyOf(existing == null ? new int[0] : existing, count + 1);
        updated[count] = packedWaypoint;
        target.waypoints[port] = updated;
        return true;
    }

    public boolean setConfig(int id, int value) {
        CircuitNode node = byId.get(id);
        if (node == null || !node.type.isConfigurable()) {
            return false;
        }
        node.config = Mth.clamp(value, node.type.getMinConfig(), node.type.getMaxConfig());
        node.resizeInputs();
        return true;
    }

    public void clear() {
        nodes.clear();
        byId.clear();
    }

    public boolean isEmpty() {
        return nodes.isEmpty();
    }

    /**
     * Advances the whole circuit by one tick.
     * Sequential elements update their state first, then combinational logic is relaxed to a fixed point.
     */
    public void simulate() {
        for (CircuitNode node : nodes) {
            if (node.type.isSequential()) {
                node.tickSequential(this);
            }
        }
        boolean changed = true;
        int passes = 0;
        while (changed && passes < 32) {
            changed = false;
            passes++;
            for (CircuitNode node : nodes) {
                if (node.type.isCombinational()) {
                    int value = node.computeCombinational(this);
                    if (value != node.value) {
                        node.value = value;
                        changed = true;
                    }
                }
            }
        }
    }

    public CompoundTag save(HolderLookup.Provider registries) {
        CompoundTag tag = new CompoundTag();
        tag.putInt("nextId", nextId);
        ListTag list = new ListTag();
        for (CircuitNode node : nodes) {
            list.add(node.save(registries));
        }
        tag.put("nodes", list);
        return tag;
    }

    public void load(HolderLookup.Provider registries, CompoundTag tag) {
        nodes.clear();
        byId.clear();
        nextId = tag.getInt("nextId");
        ListTag list = tag.getList("nodes", Tag.TAG_COMPOUND);
        for (Tag element : list) {
            CompoundTag nodeTag = (CompoundTag) element;
            NodeType type;
            try {
                type = NodeType.valueOf(nodeTag.getString("type"));
            } catch (IllegalArgumentException e) {
                continue;
            }
            CircuitNode node = new CircuitNode(nodeTag.getInt("id"), type, 0, 0);
            node.load(registries, nodeTag);
            nodes.add(node);
            byId.put(node.id, node);
        }
    }

    /** Snapshot of all node output values, ordered like {@link #nodes}. */
    public byte[] snapshotValues() {
        byte[] values = new byte[nodes.size()];
        for (int i = 0; i < nodes.size(); i++) {
            values[i] = (byte) nodes.get(i).value;
        }
        return values;
    }

    public void applyValues(byte[] values) {
        for (int i = 0; i < nodes.size() && i < values.length; i++) {
            nodes.get(i).value = values[i] & 0xFF;
        }
    }

    public static int clampX(int x) {
        return Mth.clamp(x, 0, AREA_WIDTH - NODE_WIDTH);
    }

    public static int clampY(int y) {
        return Mth.clamp(y, 0, AREA_HEIGHT - NODE_MIN_HEIGHT);
    }
}
