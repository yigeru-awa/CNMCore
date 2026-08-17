package com.onehumanawa.cnmcore.contents.wireless_redstone_control_terminal.circuit;

import net.minecraft.util.Mth;

/**
 * All logic elements available in the wireless redstone control terminal circuit editor.
 */
public enum NodeType {
    // Logic gate module
    AND(Category.GATE, "AND", 2, 2, 2, 8),
    OR(Category.GATE, "OR", 2, 2, 2, 8),
    NOT(Category.GATE, "NOT", 1, 1, 1, 1),
    XOR(Category.GATE, "XOR", 2, 2, 2, 8),
    NAND(Category.GATE, "NAND", 2, 2, 2, 8),
    NOR(Category.GATE, "NOR", 2, 2, 2, 8),
    XNOR(Category.GATE, "XNOR", 2, 2, 2, 8),
    // Sequential logic module
    PULSE(Category.SEQUENTIAL, "PLS", 1, 2, 1, 20),
    LATCH(Category.SEQUENTIAL, "LAT", 2, 0, 0, 0),
    CLOCK(Category.SEQUENTIAL, "CLK", 0, 20, 1, 100),
    DELAY(Category.SEQUENTIAL, "DLY", 1, 5, 1, 200),
    COUNTER(Category.SEQUENTIAL, "CNT", 2, 5, 1, 64),
    COMPARE(Category.SEQUENTIAL, "CMP", 2, 0, 0, 2),
    // I/O module
    INPUT(Category.IO, "IN", 0, 0, 0, 6),
    W_IN(Category.IO, "W.I", 0, 0, 0, 0),
    OUTPUT(Category.IO, "OUT", 1, 0, 0, 0),
    W_OUT(Category.IO, "W.O", 1, 0, 0, 0),
    CONST(Category.IO, "CST", 0, 1, 0, 15);

    private final Category category;
    private final String symbol;
    private final int fixedInputCount;
    private final int defaultConfig;
    private final int minConfig;
    private final int maxConfig;

    NodeType(Category category, String symbol, int fixedInputCount, int defaultConfig, int minConfig, int maxConfig) {
        this.category = category;
        this.symbol = symbol;
        this.fixedInputCount = fixedInputCount;
        this.defaultConfig = defaultConfig;
        this.minConfig = minConfig;
        this.maxConfig = maxConfig;
    }

    public Category getCategory() {
        return category;
    }

    public String getSymbol() {
        return symbol;
    }

    public int getDefaultConfig() {
        return defaultConfig;
    }

    public int getMinConfig() {
        return minConfig;
    }

    public int getMaxConfig() {
        return maxConfig;
    }

    public boolean isConfigurable() {
        return minConfig < maxConfig;
    }

    /**
     * Number of input ports for the given config value.
     * Multi-input gates derive their port count from the config, others use a fixed count.
     */
    public int inputCount(int config) {
        return switch (this) {
            case AND, OR, XOR, NAND, NOR, XNOR -> Mth.clamp(config, 2, 8);
            default -> fixedInputCount;
        };
    }

    /** Whether the node keeps internal state updated once per tick. */
    public boolean isSequential() {
        return switch (this) {
            case PULSE, LATCH, CLOCK, DELAY, COUNTER -> true;
            default -> false;
        };
    }

    /** Whether the node's value is driven by the block entity (redstone / wireless read) instead of the simulator. */
    public boolean isExternalSource() {
        return this == INPUT || this == W_IN;
    }

    /** Whether the node's value is recomputed by combinational relaxation. */
    public boolean isCombinational() {
        return !isSequential() && !isExternalSource();
    }

    public static NodeType byId(int id) {
        NodeType[] values = values();
        return id >= 0 && id < values.length ? values[id] : AND;
    }

    public enum Category {
        GATE,
        SEQUENTIAL,
        IO
    }
}
