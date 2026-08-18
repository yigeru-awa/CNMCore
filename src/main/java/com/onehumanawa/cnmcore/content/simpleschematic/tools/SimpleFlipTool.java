package com.onehumanawa.cnmcore.content.simpleschematic.tools;

import com.mojang.blaze3d.vertex.PoseStack;
import com.onehumanawa.cnmcore.content.simpleschematic.SimpleSchematicHandler;
import com.simibubi.create.AllSpecialTextures;
import com.simibubi.create.content.schematics.client.tools.FlipTool;

import net.createmod.catnip.animation.AnimationTickHolder;
import net.createmod.catnip.outliner.AABBOutline;
import net.createmod.catnip.render.SuperRenderTypeBuffer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

public class SimpleFlipTool extends FlipTool {

    private final AABBOutline outline = new AABBOutline(new AABB(BlockPos.ZERO));

    @Override
    public void init() {
        super.init();
        schematicHandler = SimpleSchematicHandler.SIMPLE_SCHEMATIC_HANDLER;
    }

    @Override
    public void renderOnSchematic(PoseStack ms, SuperRenderTypeBuffer buffer) {
        AABB bounds = schematicHandler == null ? null : schematicHandler.getBounds();
        if (!schematicSelected || selectedFace == null || bounds == null
                || !selectedFace.getAxis().isHorizontal()) {
            super.renderOnSchematic(ms, buffer);
            return;
        }

        Direction facing = selectedFace.getClockWise();

        Vec3 directionVec = Vec3.atLowerCornerOf(Direction.get(Direction.AxisDirection.POSITIVE, facing.getAxis())
                .getNormal());
        Vec3 boundsSize = new Vec3(bounds.getXsize(), bounds.getYsize(), bounds.getZsize());
        Vec3 vec = boundsSize.multiply(directionVec);
        bounds = bounds.contract(vec.x, vec.y, vec.z)
                .inflate(1 - directionVec.x, 1 - directionVec.y, 1 - directionVec.z);
        bounds = bounds.move(directionVec.scale(.5f)
                .multiply(boundsSize));

        outline.setBounds(bounds);
        AllSpecialTextures tex = AllSpecialTextures.CHECKERED;
        outline.getParams()
                .lineWidth(1 / 16f)
                .disableLineNormals()
                .colored(0xdddddd)
                .withFaceTextures(tex, tex);
        outline.render(ms, buffer, Vec3.ZERO, AnimationTickHolder.getPartialTicks());

        ISimpleSchematicTool.renderOnSchematic(ms, buffer, schematicHandler, renderSelectedFace, selectedFace);
    }
}
