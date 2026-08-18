package com.onehumanawa.cnmcore.content.simpleschematic.tools;

import com.mojang.blaze3d.vertex.PoseStack;
import com.simibubi.create.AllKeys;
import com.simibubi.create.AllSpecialTextures;
import com.simibubi.create.content.schematics.client.SchematicHandler;

import net.createmod.catnip.animation.AnimationTickHolder;
import net.createmod.catnip.outliner.AABBOutline;
import net.createmod.catnip.render.SuperRenderTypeBuffer;
import net.minecraft.core.Direction;
import net.minecraft.world.phys.Vec3;

/**
 * Ported from CreateSimpleSchematic by leaf, used with permission.
 * Shared green-tinted outline rendering for all Simple Schematic tools.
 */
public class ISimpleSchematicTool {

    public static void renderOnSchematic(
            PoseStack ms, SuperRenderTypeBuffer buffer,
            SchematicHandler handler, Boolean renderSelected, Direction selected
    ) {
        if (handler == null || !handler.isDeployed())
            return;

        AABBOutline outline = handler.getOutline();
        if (outline == null)
            return;

        ms.pushPose();
        if (Boolean.TRUE.equals(renderSelected)) {
            outline.getParams()
                    .highlightFace(selected)
                    .withFaceTextures(AllSpecialTextures.CHECKERED,
                            AllKeys.ctrlDown() ? AllSpecialTextures.HIGHLIGHT_CHECKERED : AllSpecialTextures.CHECKERED);
        }
        outline.getParams()
                .colored(0x32CD32)
                .withFaceTexture(AllSpecialTextures.CHECKERED)
                .lineWidth(1 / 16f);
        outline.render(ms, buffer, Vec3.ZERO, AnimationTickHolder.getPartialTicks());
        outline.getParams()
                .clearTextures();
        ms.popPose();
    }
}
