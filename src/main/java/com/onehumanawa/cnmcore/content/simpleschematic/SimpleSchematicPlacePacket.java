package com.onehumanawa.cnmcore.content.simpleschematic;

import com.onehumanawa.cnmcore.foundation.net.CNMPackets;
import com.simibubi.create.AllSoundEvents;
import com.simibubi.create.foundation.utility.BlockHelper;
import com.simibubi.create.infrastructure.config.AllConfigs;

import net.createmod.catnip.codecs.stream.CatnipStreamCodecs;
import net.createmod.catnip.net.base.ServerboundPacketPayload;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;

/**
 * Ported from CreateSimpleSchematic by leaf, used with permission.
 * Instantly prints a deployed Simple Schematic on the server.
 */
public record SimpleSchematicPlacePacket(ItemStack stack, BlockPos anchor, Rotation rotation, Mirror mirror)
        implements ServerboundPacketPayload {

    public static final StreamCodec<RegistryFriendlyByteBuf, SimpleSchematicPlacePacket> STREAM_CODEC =
            StreamCodec.composite(
                    ItemStack.STREAM_CODEC, SimpleSchematicPlacePacket::stack,
                    BlockPos.STREAM_CODEC, SimpleSchematicPlacePacket::anchor,
                    CatnipStreamCodecs.ROTATION, SimpleSchematicPlacePacket::rotation,
                    CatnipStreamCodecs.MIRROR, SimpleSchematicPlacePacket::mirror,
                    SimpleSchematicPlacePacket::new
            );

    @Override
    public PacketTypeProvider getTypeProvider() {
        return CNMPackets.PLACE_SCHEMATIC;
    }

    @Override
    public void handle(ServerPlayer player) {
        if (player == null)
            return;

        // Defense against spoofed packets: the player must actually hold the matching item
        ItemStack heldItem = player.getMainHandItem();
        if (heldItem.isEmpty() || !ItemStack.matches(heldItem, stack))
            return;

        Level level = player.level();
        SimpleSchematicPrinter printer = new SimpleSchematicPrinter();
        printer.loadSimpleSchematic(stack, anchor, rotation, mirror, level, !player.canUseGameMasterBlocks());
        if (!printer.isLoaded() || printer.isErrored())
            return;

        boolean includeAir = AllConfigs.server().schematics.creativePrintIncludesAir.get();

        while (printer.advanceCurrentPos()) {
            if (!printer.shouldPlaceCurrent(level))
                continue;

            printer.handleCurrentTarget((pos, state, blockEntity) -> {
                boolean placingAir = state.isAir();
                if (placingAir && !includeAir)
                    return;

                CompoundTag data = BlockHelper.prepareBlockEntityData(level, state, blockEntity);
                BlockHelper.placeSchematicBlock(level, state, pos, null, data);
            }, (pos, entity) -> level.addFreshEntity(entity));
        }

        AllSoundEvents.SCHEMATICANNON_FINISH.playFrom(player);

        if (!player.isCreative())
            heldItem.shrink(1);
    }
}
