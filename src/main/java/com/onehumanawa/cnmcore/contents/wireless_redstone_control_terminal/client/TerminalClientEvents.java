package com.onehumanawa.cnmcore.contents.wireless_redstone_control_terminal.client;

import com.onehumanawa.cnmcore.CNMCore;
import com.onehumanawa.cnmcore.contents.wireless_redstone_control_terminal.TerminalRegistry;
import com.onehumanawa.cnmcore.contents.wireless_redstone_control_terminal.WirelessInductionBinderItem;
import net.createmod.catnip.outliner.Outliner;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;

/**
 * Client-side terminal events: menu screen registration and the binder outline renderer.
 */
@EventBusSubscriber(modid = CNMCore.ID, value = Dist.CLIENT)
public class TerminalClientEvents {

    @SubscribeEvent
    public static void registerMenuScreens(RegisterMenuScreensEvent event) {
        event.register(TerminalRegistry.TERMINAL_MENU.get(), WirelessRedstoneControlTerminalScreen::new);
    }

    /** Renders the Create-style outline around the position stored in a held wireless induction binder. */
    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        Player player = mc.player;
        if (player == null || mc.level == null) {
            return;
        }
        for (InteractionHand hand : InteractionHand.values()) {
            ItemStack stack = player.getItemInHand(hand);
            if (!(stack.getItem() instanceof WirelessInductionBinderItem)) {
                continue;
            }
            BlockPos pos = WirelessInductionBinderItem.getBoundPos(stack);
            if (pos == null) {
                continue;
            }
            ResourceLocation dim = WirelessInductionBinderItem.getBoundDim(stack);
            if (dim != null && !dim.equals(mc.level.dimension().location())) {
                continue;
            }
            Outliner.getInstance().showAABB("cnmcore_binder", new AABB(pos))
                    .colored(0x59C959)
                    .lineWidth(1 / 16f);
            return;
        }
    }
}
