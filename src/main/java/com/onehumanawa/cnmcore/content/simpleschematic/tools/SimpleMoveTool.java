package com.onehumanawa.cnmcore.content.simpleschematic.tools;

import com.mojang.blaze3d.vertex.PoseStack;
import com.onehumanawa.cnmcore.content.simpleschematic.SimpleSchematicHandler;
import com.simibubi.create.content.schematics.client.tools.MoveTool;

import net.createmod.catnip.render.SuperRenderTypeBuffer;

public class SimpleMoveTool extends MoveTool {
    @Override
    public void init() {
        super.init();
        schematicHandler = SimpleSchematicHandler.SIMPLE_SCHEMATIC_HANDLER;
    }

    @Override
    public void renderOnSchematic(PoseStack ms, SuperRenderTypeBuffer buffer) {
        ISimpleSchematicTool.renderOnSchematic(ms, buffer, schematicHandler, renderSelectedFace, selectedFace);
    }
}
