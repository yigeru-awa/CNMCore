package com.onehumanawa.cnmcore.contents.wireless_redstone_control_terminal.network;

import com.onehumanawa.cnmcore.CNMCore;
import com.onehumanawa.cnmcore.contents.wireless_redstone_control_terminal.WirelessRedstoneControlTerminalBlockEntity;
import io.netty.buffer.ByteBuf;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/**
 * Client -> server: set one slot of a wireless node's ghost frequency pair from the GUI
 * (JEI ghost drag or click with a held item). An empty stack clears the slot.
 */
public record TerminalFrequencyPayload(BlockPos pos, int program, int nodeId, int slot, ItemStack stack) implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<TerminalFrequencyPayload> TYPE =
            new CustomPacketPayload.Type<>(CNMCore.asResource("wrt_freq"));
    public static final StreamCodec<RegistryFriendlyByteBuf, TerminalFrequencyPayload> CODEC = StreamCodec.composite(
            BlockPos.STREAM_CODEC, TerminalFrequencyPayload::pos,
            ByteBufCodecs.VAR_INT, TerminalFrequencyPayload::program,
            ByteBufCodecs.VAR_INT, TerminalFrequencyPayload::nodeId,
            ByteBufCodecs.VAR_INT, TerminalFrequencyPayload::slot,
            ItemStack.OPTIONAL_STREAM_CODEC, TerminalFrequencyPayload::stack,
            TerminalFrequencyPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(TerminalFrequencyPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            Player player = context.player();
            Level level = player.level();
            if (!(level.getBlockEntity(payload.pos()) instanceof WirelessRedstoneControlTerminalBlockEntity terminal)) {
                return;
            }
            // Basic reach validation
            if (player.distanceToSqr(Vec3.atCenterOf(payload.pos())) > 64.0D) {
                return;
            }
            terminal.setNodeFrequency(payload.program(), payload.nodeId(), payload.slot(), payload.stack());
        });
    }
}
