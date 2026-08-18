package com.onehumanawa.cnmcore.content.redprint;

import com.onehumanawa.cnmcore.CNMCore;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import java.util.ArrayList;
import java.util.List;

/**
 * Server-bound packet to execute the redprint removal.
 * Removes all non-air blocks in the specified area and drops their loot.
 */
public record RedprintPacket(BlockPos from, BlockPos to, ItemStack tool) implements CustomPacketPayload {

    public static final Type<RedprintPacket> TYPE = new Type<>(CNMCore.asResource("redprint"));
    public static final StreamCodec<RegistryFriendlyByteBuf, RedprintPacket> CODEC = StreamCodec.composite(
            BlockPos.STREAM_CODEC, RedprintPacket::from,
            BlockPos.STREAM_CODEC, RedprintPacket::to,
            ItemStack.STREAM_CODEC, RedprintPacket::tool,
            RedprintPacket::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(RedprintPacket packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            ServerPlayer player = (ServerPlayer) context.player();
            Level level = player.serverLevel();

            // Validate: ensure the area is not too large
            long totalBlocks = (long) (packet.to().getX() - packet.from().getX() + 1)
                    * (packet.to().getY() - packet.from().getY() + 1)
                    * (packet.to().getZ() - packet.from().getZ() + 1);

            if (totalBlocks > 100000) {
                player.displayClientMessage(
                        net.minecraft.network.chat.Component.translatable("cnmcore.redprint.too_large"),
                        true
                );
                return;
            }

            List<ItemStack> allDrops = new ArrayList<>();

            // Iterate through the area and collect drops
            for (int x = packet.from().getX(); x <= packet.to().getX(); x++) {
                for (int y = packet.from().getY(); y <= packet.to().getY(); y++) {
                    for (int z = packet.from().getZ(); z <= packet.to().getZ(); z++) {
                        BlockPos pos = new BlockPos(x, y, z);
                        BlockState state = level.getBlockState(pos);

                        if (state.isAir()) continue;

                        BlockEntity be = level.getBlockEntity(pos);

                        // Get drops with silk touch behavior (via the tool)
                        List<ItemStack> drops = Block.getDrops(
                                state,
                                (ServerLevel) level,
                                pos,
                                be,
                                player,
                                packet.tool()
                        );

                        allDrops.addAll(drops);

                        // Replace with air
                        level.setBlock(pos, Blocks.AIR.defaultBlockState(), 51);
                    }
                }
            }

            // Give drops to player or drop at their feet
            for (ItemStack stack : allDrops) {
                if (!player.addItem(stack)) {
                    player.drop(stack, false);
                }
            }

            player.displayClientMessage(
                    net.minecraft.network.chat.Component.translatable("cnmcore.redprint.success", allDrops.size()),
                    true
            );

        });
    }
}