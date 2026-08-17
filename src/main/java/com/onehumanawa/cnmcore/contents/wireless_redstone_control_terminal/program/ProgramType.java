package com.onehumanawa.cnmcore.contents.wireless_redstone_control_terminal.program;

/**
 * Kind of program a terminal tab runs. The terminal is a general-purpose "smart brain":
 * only redstone management is implemented for now, logistics (Create Stock Ticker networks)
 * and stress management will follow.
 */
public enum ProgramType {
    REDSTONE;

    public static ProgramType byName(String name) {
        try {
            return valueOf(name);
        } catch (IllegalArgumentException e) {
            return REDSTONE;
        }
    }
}
