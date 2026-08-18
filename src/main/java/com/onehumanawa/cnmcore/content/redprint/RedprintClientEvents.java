package com.onehumanawa.cnmcore.content.redprint;

import com.onehumanawa.cnmcore.CNMCore;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.InputEvent;

@EventBusSubscriber(modid = CNMCore.ID, value = Dist.CLIENT)
public final class RedprintClientEvents {

    private RedprintClientEvents() {}

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Pre event) {
        RedprintHandler.getInstance().tick();
    }

    @SubscribeEvent
    public static void onMouseScroll(InputEvent.MouseScrollingEvent event) {
        if (RedprintHandler.getInstance().mouseScrolled(event.getScrollDeltaX())) {
            event.setCanceled(true);
        }
    }
}