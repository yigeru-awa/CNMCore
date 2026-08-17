package com.onehumanawa.cnmcore.contents.wireless_redstone_control_terminal.client;

import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.registration.IGuiHandlerRegistration;
import net.minecraft.resources.ResourceLocation;

import com.onehumanawa.cnmcore.CNMCore;

/**
 * JEI integration: allows dragging an item straight from the JEI ingredient list
 * onto the selected wireless node's frequency slot (ghost only, nothing is consumed).
 */
@JeiPlugin
public class TerminalJeiPlugin implements IModPlugin {

    @Override
    public ResourceLocation getPluginUid() {
        return CNMCore.asResource("terminal");
    }

    @Override
    public void registerGuiHandlers(IGuiHandlerRegistration registration) {
        registration.addGhostIngredientHandler(WirelessRedstoneControlTerminalScreen.class,
                new TerminalGhostIngredientHandler());
    }
}
