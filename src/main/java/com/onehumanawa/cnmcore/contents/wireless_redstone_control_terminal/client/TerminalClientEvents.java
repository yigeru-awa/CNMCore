package com.onehumanawa.cnmcore.contents.wireless_redstone_control_terminal.client;

import com.onehumanawa.cnmcore.CNMCore;
import com.onehumanawa.cnmcore.contents.wireless_redstone_control_terminal.TerminalRegistry;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;

@EventBusSubscriber(modid = CNMCore.ID, value = Dist.CLIENT)
public class TerminalClientEvents {

    @SubscribeEvent
    public static void registerMenuScreens(RegisterMenuScreensEvent event) {
        event.register(TerminalRegistry.TERMINAL_MENU.get(), WirelessRedstoneControlTerminalScreen::new);
    }
}
