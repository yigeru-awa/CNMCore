package com.onehumanawa.cnmcore.content.simpleschematic.tools;

import com.onehumanawa.cnmcore.CNMCore;
import com.onehumanawa.cnmcore.foundation.config.SimpleSchematicConfig;
import com.simibubi.create.content.schematics.client.tools.ISchematicTool;
import com.simibubi.create.content.schematics.client.tools.ToolType;

import java.util.ArrayList;
import java.util.List;

public enum SimpleToolType {

    DEPLOY(new SimpleDeployTool()),
    MOVE(new SimpleMoveTool()),
    MOVE_Y(new SimpleMoveVerticalTool()),
    ROTATE(new SimpleRotateTool()),
    FLIP(new SimpleFlipTool()),
    PRINT(new SimplePlaceTool());

    private final ISchematicTool tool;

    SimpleToolType(ISchematicTool tool) {
        this.tool = tool;
    }

    public ISchematicTool getTool() {
        return tool;
    }

    /**
     * Tools offered by the selection screen. The flip tool is opt-in via config,
     * and the print tool is only offered in creative mode (Create 6 semantics).
     */
    @SuppressWarnings("unused")
    public static List<ToolType> getTools(boolean creative) {
        List<ToolType> tools = new ArrayList<>(List.of(
                ToolType.MOVE, ToolType.MOVE_Y, ToolType.DEPLOY, ToolType.ROTATE));
        if (SimpleSchematicConfig.isFlipToolEnabled())
            tools.add(ToolType.FLIP);
        tools.add(ToolType.PRINT);
        return List.copyOf(tools);
    }

    public static SimpleToolType of(ToolType toolType) {
        if (toolType != null) {
            for (SimpleToolType simple : values()) {
                if (simple.name().equals(toolType.name()))
                    return simple;
            }
        }
        CNMCore.LOGGER.warn("Unknown ToolType '{}' requested, falling back to DEPLOY", toolType);
        return DEPLOY;
    }

    public ToolType getToolType() {
        try {
            return ToolType.valueOf(name());
        } catch (IllegalArgumentException e) {
            CNMCore.LOGGER.warn("No ToolType matches '{}', falling back to DEPLOY", name());
            return ToolType.DEPLOY;
        }
    }
}
