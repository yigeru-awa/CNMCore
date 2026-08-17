package com.onehumanawa.cnmcore.contents.wireless_redstone_control_terminal.circuit;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.IntArrayTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;

import java.util.Arrays;

/**
 * A single element placed on the circuit canvas.
 * Holds its wiring (input port -> source node id), its configuration and its simulation state.
 */
public class CircuitNode {
    public final int id;
    public NodeType type;
    public int x;
    public int y;
    public int config;
    /** Source node id for each input port, {@code -1} means unconnected. */
    public int[] inputs = new int[0];
    /** Custom wire waypoints per input port, each packed as {@code (x << 8) | y}, null when the wire is straight. */
    public int[][] waypoints = new int[0][];
    /** Current output signal strength (0-15). */
    public int value;

    // Runtime state for sequential elements (not persisted)
    private int timer;
    private boolean prevInput;
    private boolean latchOn;
    private int counter;
    private int[] delayBuffer = new int[0];
    private int delayHead;

    public CircuitNode(int id, NodeType type, int x, int y) {
        this.id = id;
        this.type = type;
        this.x = x;
        this.y = y;
        this.config = type.getDefaultConfig();
        resizeInputs();
    }

    public void resizeInputs() {
        int count = type.inputCount(config);
        if (count == inputs.length) {
            return;
        }
        int[] old = inputs;
        inputs = new int[count];
        Arrays.fill(inputs, -1);
        System.arraycopy(old, 0, inputs, 0, Math.min(old.length, count));
        int[][] oldWaypoints = waypoints;
        waypoints = new int[count][];
        System.arraycopy(oldWaypoints, 0, waypoints, 0, Math.min(oldWaypoints.length, count));
    }

    public int inputValue(Circuit circuit, int port) {
        if (port < 0 || port >= inputs.length) {
            return 0;
        }
        int sourceId = inputs[port];
        if (sourceId < 0) {
            return 0;
        }
        CircuitNode source = circuit.nodeById(sourceId);
        return source == null ? 0 : source.value;
    }

    private boolean inputOn(Circuit circuit, int port) {
        return inputValue(circuit, port) > 0;
    }

    private int countOn(Circuit circuit) {
        int count = 0;
        for (int i = 0; i < inputs.length; i++) {
            if (inputOn(circuit, i)) {
                count++;
            }
        }
        return count;
    }

    private boolean allOn(Circuit circuit) {
        for (int i = 0; i < inputs.length; i++) {
            if (!inputOn(circuit, i)) {
                return false;
            }
        }
        return inputs.length > 0;
    }

    private boolean anyOn(Circuit circuit) {
        return countOn(circuit) > 0;
    }

    /** Advances the internal state of a sequential element by one tick. */
    public void tickSequential(Circuit circuit) {
        switch (type) {
            case PULSE -> {
                boolean in = inputOn(circuit, 0);
                if (in && !prevInput) {
                    timer = Math.max(1, config);
                }
                prevInput = in;
                value = timer > 0 ? 15 : 0;
                if (timer > 0) {
                    timer--;
                }
            }
            case LATCH -> {
                // Reset input has priority over the set input
                if (inputOn(circuit, 1)) {
                    latchOn = false;
                } else if (inputOn(circuit, 0)) {
                    latchOn = true;
                }
                value = latchOn ? 15 : 0;
            }
            case CLOCK -> {
                timer++;
                if (timer >= config) {
                    timer = 0;
                }
                value = timer * 2 < config ? 15 : 0;
            }
            case DELAY -> {
                int length = Math.max(1, config);
                if (delayBuffer.length != length) {
                    delayBuffer = new int[length];
                    delayHead = 0;
                }
                value = delayBuffer[delayHead];
                delayBuffer[delayHead] = inputValue(circuit, 0);
                delayHead = (delayHead + 1) % length;
            }
            case COUNTER -> {
                boolean in = inputOn(circuit, 0);
                if (in && !prevInput) {
                    counter++;
                }
                prevInput = in;
                // Second input (when connected) acts as a level-triggered reset
                if (inputs.length > 1 && inputs[1] >= 0 && inputOn(circuit, 1)) {
                    counter = 0;
                }
                if (counter > config) {
                    counter = config;
                }
                value = counter >= config ? 15 : 0;
            }
            default -> {
            }
        }
    }

    /** Computes the output of a combinational element from its current inputs. */
    public int computeCombinational(Circuit circuit) {
        return switch (type) {
            case AND -> allOn(circuit) ? 15 : 0;
            case OR -> anyOn(circuit) ? 15 : 0;
            case NOT -> inputOn(circuit, 0) ? 0 : 15;
            case XOR -> (countOn(circuit) & 1) == 1 ? 15 : 0;
            case NAND -> allOn(circuit) ? 0 : 15;
            case NOR -> anyOn(circuit) ? 0 : 15;
            case XNOR -> (countOn(circuit) & 1) == 1 ? 0 : 15;
            case COMPARE -> {
                int a = inputValue(circuit, 0);
                int b = inputValue(circuit, 1);
                boolean result = switch (config) {
                    case 0 -> a > b;
                    case 1 -> a < b;
                    default -> a == b;
                };
                yield result ? 15 : 0;
            }
            case CONST -> Math.max(0, Math.min(15, config));
            case OUTPUT, W_OUT -> inputValue(circuit, 0);
            default -> 0;
        };
    }

    public CompoundTag save() {
        CompoundTag tag = new CompoundTag();
        tag.putInt("id", id);
        tag.putString("type", type.name());
        tag.putInt("x", x);
        tag.putInt("y", y);
        tag.putInt("cfg", config);
        tag.putIntArray("in", inputs);
        boolean hasWaypoints = false;
        ListTag waypointList = new ListTag();
        for (int i = 0; i < inputs.length; i++) {
            int[] portWaypoints = waypoints[i];
            waypointList.add(new IntArrayTag(portWaypoints == null ? new int[0] : portWaypoints));
            hasWaypoints |= portWaypoints != null && portWaypoints.length > 0;
        }
        if (hasWaypoints) {
            tag.put("wp", waypointList);
        }
        return tag;
    }

    public void load(CompoundTag tag) {
        this.x = tag.getInt("x");
        this.y = tag.getInt("y");
        this.config = tag.getInt("cfg");
        resizeInputs();
        int[] saved = tag.getIntArray("in");
        for (int i = 0; i < saved.length && i < inputs.length; i++) {
            inputs[i] = saved[i];
        }
        ListTag waypointList = tag.getList("wp", Tag.TAG_INT_ARRAY);
        for (int i = 0; i < waypointList.size() && i < waypoints.length; i++) {
            int[] portWaypoints = waypointList.getIntArray(i);
            waypoints[i] = portWaypoints.length == 0 ? null : portWaypoints;
        }
    }
}
