package com.onehumanawa.cnmcore.foundation.config;

import net.neoforged.fml.event.config.ModConfigEvent;
import net.neoforged.neoforge.common.ModConfigSpec;

/**
 * Ported from CreateSimpleSchematic by leaf, used with permission.
 * Common configuration for the Simple Schematic feature.
 */
public class SimpleSchematicConfig {
    private static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();

    public static final ModConfigSpec.BooleanValue ENABLE_SIMPLE_SCHEMATIC_FLIP_TOOL;
    public static final ModConfigSpec SPEC;

    static {
        BUILDER.comment("Simple Schematic").push("simple_schematic");
        ENABLE_SIMPLE_SCHEMATIC_FLIP_TOOL = BUILDER
                .comment("Enable the Simple Schematic's Flip Tool")
                .define("enable_simple_schematic_flip_tool", false);
        BUILDER.pop();

        SPEC = BUILDER.build();
    }

    // Cached config value, refreshed on config load/reload
    public static boolean enable_simple_schematic_flip_tool;

    public static void onLoad(final ModConfigEvent.Loading event) {
        if (event.getConfig().getSpec() != SPEC)
            return;
        enable_simple_schematic_flip_tool = ENABLE_SIMPLE_SCHEMATIC_FLIP_TOOL.get();
    }

    public static void onReload(final ModConfigEvent.Reloading event) {
        if (event.getConfig().getSpec() != SPEC)
            return;
        enable_simple_schematic_flip_tool = ENABLE_SIMPLE_SCHEMATIC_FLIP_TOOL.get();
    }

    /**
     * Safe accessor used by client tooling. Falls back to the live config value and
     * never throws even if config is not yet loaded.
     */
    public static boolean isFlipToolEnabled() {
        try {
            return ENABLE_SIMPLE_SCHEMATIC_FLIP_TOOL.get();
        } catch (Throwable t) {
            return enable_simple_schematic_flip_tool;
        }
    }
}
