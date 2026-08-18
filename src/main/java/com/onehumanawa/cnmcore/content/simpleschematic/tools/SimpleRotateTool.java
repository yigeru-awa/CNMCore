package com.onehumanawa.cnmcore.content.simpleschematic.tools;

import com.mojang.blaze3d.vertex.PoseStack;
import com.onehumanawa.cnmcore.content.simpleschematic.SimpleSchematicHandler;
import com.simibubi.create.content.schematics.client.tools.RotateTool;

import net.createmod.catnip.animation.AnimationTickHolder;
import net.createmod.catnip.outliner.LineOutline;
import net.createmod.catnip.render.SuperRenderTypeBuffer;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

public class SimpleRotateTool extends RotateTool {

    private final LineOutline line = new LineOutline();

    @Override
    public void init() {
        super.init();
        schematicHandler = SimpleSchematicHandler.SIMPLE_SCHEMATIC_HANDLER;
    }

    @Override
    public void renderOnSchematic(PoseStack ms, SuperRenderTypeBuffer buffer) {
        AABB bounds = schematicHandler.getBounds();
        if (bounds == null || schematicHandler.getTransformation() == null) {
            ISimpleSchematicTool.renderOnSchematic(ms, buffer, schematicHandler, renderSelectedFace, selectedFace);
            return;
        }

        double height = bounds.getYsize() + Math.max(20, bounds.getYsize());
        Vec3 center = bounds.getCenter()
                .add(schematicHandler.getTransformation()
                        .getRotationOffset(false));
        Vec3 start = center.subtract(0, height / 2, 0);
        Vec3 end = center.add(0, height / 2, 0);

        line.getParams()
                .disableCull()
                .disableLineNormals()
                .colored(0xdddddd)
                .lineWidth(1 / 16f);
        line.set(start, end)
                .render(ms, buffer, Vec3.ZERO, AnimationTickHolder.getPartialTicks());

        ISimpleSchematicTool.renderOnSchematic(ms, buffer, schematicHandler, renderSelectedFace, selectedFace);
    }
}
