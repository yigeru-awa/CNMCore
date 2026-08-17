package com.onehumanawa.cnmcore.contents.wireless_redstone_control_terminal.network;

import com.onehumanawa.cnmcore.CNMCore;
import com.onehumanawa.cnmcore.contents.wireless_redstone_control_terminal.WirelessRedstoneControlTerminalBlockEntity;
import io.netty.buffer.ByteBuf;
import net.minecraft.core.BlockPos;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/**
 * Client -> server terminal edit request, targeting one program (tab).
 * Actions: 0=add(type,x,y), 1=move(id,x,y), 2=remove(id), 3=connect(target,port,source),
 * 4=disconnect(target,port), 5=setConfig(id,value), 6=clear, 7=addWaypoint(target,port,packed),
 * 8=createProgram, 9=deleteProgram(id), 10=switchProgram(id).
 */
public record TerminalEditPayload(BlockPos pos, int program, int action, int a, int b, int c) implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<TerminalEditPayload> TYPE =
            new CustomPacketPayload.Type<>(CNMCore.asResource("wrt_edit"));
    public static final StreamCodec<ByteBuf, TerminalEditPayload> CODEC = StreamCodec.composite(
            BlockPos.STREAM_CODEC, TerminalEditPayload::pos,
            ByteBufCodecs.VAR_INT, TerminalEditPayload::program,
            ByteBufCodecs.VAR_INT, TerminalEditPayload::action,
            ByteBufCodecs.VAR_INT, TerminalEditPayload::a,
            ByteBufCodecs.VAR_INT, TerminalEditPayload::b,
            ByteBufCodecs.VAR_INT, TerminalEditPayload::c,
            TerminalEditPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(TerminalEditPayload payload, IPayloadContext context) {
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
            terminal.handleEdit(payload.program(), payload.action(), payload.a(), payload.b(), payload.c());
        });
    }
}
