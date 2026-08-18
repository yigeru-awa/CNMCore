package com.onehumanawa.cnmcore.foundation;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.onehumanawa.cnmcore.content.simpleschematic.SimpleSchematicHandler;

import net.createmod.catnip.levelWrappers.WrappedClientLevel;
import net.createmod.catnip.render.DefaultSuperRenderTypeBuffer;
import net.createmod.catnip.render.SuperRenderTypeBuffer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.InputEvent;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import net.neoforged.neoforge.event.level.LevelEvent;

/**
 * Ported from CreateSimpleSchematic by leaf, used with permission.
 * Client game-bus event wiring for the {@link SimpleSchematicHandler}.
 */
@EventBusSubscriber(value = Dist.CLIENT)
public class ClientEvents {

    @SubscribeEvent
    public static void onTick(ClientTickEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null)
            return;

        SimpleSchematicHandler.SIMPLE_SCHEMATIC_HANDLER.tick();
    }

    @SubscribeEvent
    public static void onKeyInput(InputEvent.Key event) {
        if (Minecraft.getInstance().screen != null)
            return;

        SimpleSchematicHandler.SIMPLE_SCHEMATIC_HANDLER.onKeyInput(event.getKey(), event.getAction() != 0);
    }

    @SubscribeEvent
    public static void onMouseScrolled(InputEvent.MouseScrollingEvent event) {
        if (Minecraft.getInstance().screen != null)
            return;

        double delta = event.getScrollDeltaY();
        if (SimpleSchematicHandler.SIMPLE_SCHEMATIC_HANDLER.mouseScrolled(delta))
            event.setCanceled(true);
    }

    @SubscribeEvent
    public static void onMouseInput(InputEvent.MouseButton.Pre event) {
        if (Minecraft.getInstance().screen != null)
            return;

        if (SimpleSchematicHandler.SIMPLE_SCHEMATIC_HANDLER.onMouseInput(event.getButton(), event.getAction() != 0))
            event.setCanceled(true);
    }

    @SubscribeEvent
    public static void onLoadWorld(LevelEvent.Load event) {
        LevelAccessor world = event.getLevel();
        if (world.isClientSide() && world instanceof ClientLevel && !(world instanceof WrappedClientLevel)) {
            SimpleSchematicHandler.SIMPLE_SCHEMATIC_HANDLER.updateRenderers();
        }
    }

    @SubscribeEvent
    public static void onUnloadWorld(LevelEvent.Unload event) {
        if (!event.getLevel().isClientSide())
            return;
        SimpleSchematicHandler.SIMPLE_SCHEMATIC_HANDLER.updateRenderers();
    }

    @SubscribeEvent
    public static void onRenderWorld(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_PARTICLES)
            return;

        PoseStack ms = event.getPoseStack();
        SuperRenderTypeBuffer buffer = DefaultSuperRenderTypeBuffer.getInstance();
        Vec3 camera = Minecraft.getInstance().gameRenderer.getMainCamera().getPosition();

        ms.pushPose();

        SimpleSchematicHandler.SIMPLE_SCHEMATIC_HANDLER.render(ms, buffer, camera);

        buffer.draw();
        RenderSystem.enableCull();
        ms.popPose();
    }
}
