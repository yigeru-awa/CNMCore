package com.onehumanawa.cnmcore.contents.wireless_redstone_control_terminal.network;

import com.onehumanawa.cnmcore.CNMCore;
import com.onehumanawa.cnmcore.contents.wireless_redstone_control_terminal.WirelessRedstoneControlTerminalBlockEntity;
import io.netty.buffer.ByteBuf;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/**
 * Server -> client snapshot sent while the terminal screen is open:
 * tab metadata of all programs plus the full graph and signal values of the active program.
 */
public record TerminalSyncPayload(BlockPos pos, CompoundTag meta, CompoundTag circuit, byte[] values) implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<TerminalSyncPayload> TYPE =
            new CustomPacketPayload.Type<>(CNMCore.asResource("wrt_sync"));
    public static final StreamCodec<ByteBuf, TerminalSyncPayload> CODEC = StreamCodec.composite(
            BlockPos.STREAM_CODEC, TerminalSyncPayload::pos,
            ByteBufCodecs.TRUSTED_COMPOUND_TAG, TerminalSyncPayload::meta,
            ByteBufCodecs.TRUSTED_COMPOUND_TAG, TerminalSyncPayload::circuit,
            ByteBufCodecs.BYTE_ARRAY, TerminalSyncPayload::values,
            TerminalSyncPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(TerminalSyncPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            Level level = Minecraft.getInstance().level;
            if (level != null
                    && level.getBlockEntity(payload.pos()) instanceof WirelessRedstoneControlTerminalBlockEntity terminal) {
                terminal.applySync(payload.meta(), payload.circuit(), payload.values());
            }
        });
    }
}
