package com.onehumanawa.cnmcore.foundation.util.outliner;

import com.mojang.blaze3d.vertex.PoseStack;
import com.onehumanawa.cnmcore.CNMCore;
import net.createmod.catnip.animation.AnimationTickHolder;
import net.createmod.catnip.render.DefaultSuperRenderTypeBuffer;
import net.createmod.catnip.render.SuperRenderTypeBuffer;
import net.minecraft.client.Minecraft;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;

/**
 * Hooks the Outliner render and tick loops into client events.
 */
@EventBusSubscriber(modid = CNMCore.ID, value = Dist.CLIENT)
public final class OutlineRenderHandler {

    private OutlineRenderHandler() {}

    @SubscribeEvent
    public static void onRenderLevel(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_TRANSLUCENT_BLOCKS) return;

        PoseStack poseStack = event.getPoseStack();
        SuperRenderTypeBuffer buffer = DefaultSuperRenderTypeBuffer.getInstance();
        Vec3 camera = Minecraft.getInstance().gameRenderer.getMainCamera().getPosition();
        float partialTick = AnimationTickHolder.getPartialTicks();

        net.createmod.catnip.outliner.Outliner.getInstance()
                .renderOutlines(poseStack, buffer, camera, partialTick);
    }

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Pre event) {
        net.createmod.catnip.outliner.Outliner.getInstance().tickOutlines();
    }
}