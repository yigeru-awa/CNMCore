package com.onehumanawa.cnmcore.content.simpleschematic;

import com.mojang.blaze3d.platform.Window;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.onehumanawa.cnmcore.mixin.ToolSelectionScreenAccessor;
import com.simibubi.create.AllKeys;
import com.simibubi.create.content.schematics.client.ToolSelectionScreen;
import com.simibubi.create.content.schematics.client.tools.ToolType;
import com.simibubi.create.foundation.gui.AllGuiTextures;
import com.simibubi.create.foundation.utility.CreateLang;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;

import java.util.List;
import java.util.function.Consumer;

/**
 * Green-tinted variant of Create's tool selection HUD for the Simple Schematic item.
 */
public class SimpleToolSelectionScreen extends ToolSelectionScreen {

    public SimpleToolSelectionScreen(List<ToolType> tools, Consumer<ToolType> callback) {
        super(tools, callback);
    }

    private void draw(GuiGraphics graphics) {
        PoseStack matrixStack = graphics.pose();
        // Defensive: minecraft/window may theoretically be unavailable while the HUD renders
        if (minecraft == null || minecraft.getWindow() == null)
            return;
        Window mainWindow = minecraft.getWindow();
        ToolSelectionScreenAccessor accessor = (ToolSelectionScreenAccessor) this;
        if (!accessor.hasInitialized())
            init(minecraft, mainWindow.getGuiScaledWidth(), mainWindow.getGuiScaledHeight());

        int x = (mainWindow.getGuiScaledWidth() - w) / 2 + 15;
        int y = mainWindow.getGuiScaledHeight() - h - 75;

        matrixStack.pushPose();
        matrixStack.translate(0, -accessor.getYOffset(), focused ? 100 : 0);

        AllGuiTextures gray = AllGuiTextures.HUD_BACKGROUND;
        RenderSystem.enableBlend();
        RenderSystem.setShaderColor(.8f, 1.1f, 0.5f, focused ? 7 / 8f : 1 / 2f);

        graphics.blit(gray.location, x - 15, y, gray.getStartX(), gray.getStartY(), w, h, gray.getWidth(), gray.getHeight());

        float toolTipAlpha = accessor.getYOffset() / 10;
        if (selection < 0 || selection >= tools.size())
            selection = 0;
        List<Component> toolTip = tools.get(selection).getDescription();
        int stringAlphaComponent = ((int) (toolTipAlpha * 0xFF)) << 24;

        if (toolTipAlpha > 0.25f) {
            RenderSystem.setShaderColor(.5f, .9f, .4f, toolTipAlpha);
            graphics.blit(gray.location, x - 15, y + 33, gray.getStartX(), gray.getStartY(), w, h + 22, gray.getWidth(), gray.getHeight());
            RenderSystem.setShaderColor(1, 1, 1, 1);

            if (!toolTip.isEmpty())
                graphics.drawString(font, toolTip.get(0), x - 10, y + 38, 0xEEEEEE + stringAlphaComponent, false);
            if (toolTip.size() > 1)
                graphics.drawString(font, toolTip.get(1), x - 10, y + 50, 0xCCDDFF + stringAlphaComponent, false);
            if (toolTip.size() > 2 && !tools.get(selection).equals(ToolType.PRINT))
                graphics.drawString(font, toolTip.get(2), x - 10, y + 60, 0xCCDDFF + stringAlphaComponent, false);
            if (toolTip.size() > 3)
                graphics.drawString(font, toolTip.get(3), x - 10, y + 72, 0xCCCCDD + stringAlphaComponent, false);
        }

        RenderSystem.setShaderColor(1, 1, 1, 1);
        if (tools.size() > 1) {
            String keyName = AllKeys.TOOL_MENU.getBoundKey();
            int width = mainWindow.getGuiScaledWidth();
            if (!focused)
                graphics.drawCenteredString(minecraft.font, CreateLang.translateDirect(holdToFocus, keyName), width / 2,
                        y - 10, 0xCCDDFF);
            else
                graphics.drawCenteredString(minecraft.font, scrollToCycle, width / 2, y - 10, 0xCCDDFF);
        } else {
            x += 65;
        }

        for (int i = 0; i < tools.size(); i++) {
            RenderSystem.enableBlend();
            matrixStack.pushPose();

            float alpha = focused ? 1 : .2f;
            if (i == selection) {
                matrixStack.translate(0, -10, 0);
                RenderSystem.setShaderColor(1, 1, 1, 1);
                graphics.drawCenteredString(minecraft.font, tools.get(i)
                        .getDisplayName()
                        .getString(), x + i * 50 + 24, y + 28, 0xCCDDFF);
                alpha = 1;
            }
            RenderSystem.setShaderColor(0, 0, 0, alpha);
            tools.get(i)
                    .getIcon()
                    .render(graphics, x + i * 50 + 16, y + 12);
            RenderSystem.setShaderColor(1, 1, 1, alpha);
            tools.get(i)
                    .getIcon()
                    .render(graphics, x + i * 50 + 16, y + 11);

            matrixStack.popPose();
        }

        RenderSystem.setShaderColor(1, 1, 1, 1);
        RenderSystem.disableBlend();
        matrixStack.popPose();
    }

    @Override
    public void renderPassive(GuiGraphics graphics, float partialTicks) {
        draw(graphics);
    }
}
