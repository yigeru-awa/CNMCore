package com.onehumanawa.cnmcore.contents.wireless_redstone_control_terminal.program;

import com.onehumanawa.cnmcore.contents.wireless_redstone_control_terminal.circuit.Circuit;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;

/**
 * One tab of the terminal. A terminal can host many programs at once and runs all of them
 * in parallel; the GUI shows them as switchable tabs.
 */
public class TerminalProgram {
    public final int id;
    public String name;
    public final ProgramType type;
    /** Redstone graph, only used when {@link #type} is {@link ProgramType#REDSTONE}. */
    public final Circuit circuit = new Circuit();

    public TerminalProgram(int id, String name, ProgramType type) {
        this.id = id;
        this.name = name;
        this.type = type;
    }

    public CompoundTag save(HolderLookup.Provider registries) {
        CompoundTag tag = new CompoundTag();
        tag.putInt("id", id);
        tag.putString("name", name);
        tag.putString("type", type.name());
        if (type == ProgramType.REDSTONE) {
            tag.put("circuit", circuit.save(registries));
        }
        return tag;
    }

    /** Serializes only the tab metadata (id, name, type) for lightweight GUI sync. */
    public CompoundTag saveMeta() {
        CompoundTag tag = new CompoundTag();
        tag.putInt("id", id);
        tag.putString("name", name);
        tag.putString("type", type.name());
        return tag;
    }

    public static TerminalProgram load(HolderLookup.Provider registries, CompoundTag tag) {
        TerminalProgram program = new TerminalProgram(tag.getInt("id"), tag.getString("name"),
                ProgramType.byName(tag.getString("type")));
        if (program.type == ProgramType.REDSTONE && tag.contains("circuit")) {
            program.circuit.load(registries, tag.getCompound("circuit"));
        }
        return program;
    }
}
