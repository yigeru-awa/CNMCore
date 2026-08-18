package com.onehumanawa.cnmcore;

import com.onehumanawa.cnmcore.content.redprint.RedprintPacket;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

@SuppressWarnings("removal")
@EventBusSubscriber(modid = CNMCore.ID, bus = EventBusSubscriber.Bus.MOD)
public class AllPackets {

    @SubscribeEvent
    public static void registerPayloads(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar("1");
        registrar.playToServer(RedprintPacket.TYPE, RedprintPacket.CODEC, RedprintPacket::handle);
    }

    public static void sendToServer(CustomPacketPayload packet) {
        net.neoforged.neoforge.network.PacketDistributor.sendToServer(packet);
    }
}