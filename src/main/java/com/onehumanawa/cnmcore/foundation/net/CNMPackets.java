package com.onehumanawa.cnmcore.foundation.net;

import java.util.Locale;

import com.onehumanawa.cnmcore.CNMCore;
import com.onehumanawa.cnmcore.content.simpleschematic.SimpleSchematicPlacePacket;

import net.createmod.catnip.net.base.BasePacketPayload;
import net.createmod.catnip.net.base.CatnipPacketRegistry;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

import net.neoforged.fml.ModList;

public enum CNMPackets implements BasePacketPayload.PacketTypeProvider {
    // Client to Server
    PLACE_SCHEMATIC(SimpleSchematicPlacePacket.class, SimpleSchematicPlacePacket.STREAM_CODEC);

    private final CatnipPacketRegistry.PacketType<?> type;

    <T extends BasePacketPayload> CNMPackets(Class<T> clazz, StreamCodec<? super RegistryFriendlyByteBuf, T> codec) {
        String name = this.name().toLowerCase(Locale.ROOT);
        this.type = new CatnipPacketRegistry.PacketType<>(
                new CustomPacketPayload.Type<>(CNMCore.asResource(name)),
                clazz, codec
        );
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T extends CustomPacketPayload> CustomPacketPayload.Type<T> getType() {
        return (CustomPacketPayload.Type<T>) this.type.type();
    }

    public static void register() {
        CatnipPacketRegistry packetRegistry = new CatnipPacketRegistry(CNMCore.ID, getModVersion());
        for (CNMPackets packet : CNMPackets.values()) {
            packetRegistry.registerPacket(packet.type);
        }
        packetRegistry.registerAllPackets();
    }

    private static String getModVersion() {
        try {
            ModList modList = ModList.get();
            if (modList != null) {
                var modContainer = modList.getModContainerById(CNMCore.ID);
                if (modContainer.isPresent())
                    return modContainer.get().getModInfo().getVersion().toString();
            }
        } catch (Exception e) {
            CNMCore.LOGGER.warn("Failed to resolve mod version for packet registration", e);
        }
        return "unknown";
    }
}
