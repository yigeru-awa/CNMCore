package com.onehumanawa.cnmcore.contents.wireless_redstone_control_terminal;

import com.onehumanawa.cnmcore.CNMCore;
import com.onehumanawa.cnmcore.contents.wireless_redstone_control_terminal.network.TerminalEditPayload;
import com.onehumanawa.cnmcore.contents.wireless_redstone_control_terminal.network.TerminalFrequencyPayload;
import com.onehumanawa.cnmcore.contents.wireless_redstone_control_terminal.network.TerminalSyncPayload;
import com.onehumanawa.cnmcore.foundation.data.CreateRegistrate;
import com.tterrag.registrate.util.entry.BlockEntry;
import com.tterrag.registrate.util.entry.ItemEntry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.material.MapColor;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.extensions.IMenuTypeExtension;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.function.Supplier;

/**
 * Registration entry point for the wireless redstone control terminal content.
 */
public class TerminalRegistry {
    public static final String NAME = "wireless_redstone_control_terminal";
    public static final String ENDPOINT_NAME = "wrt_endpoint";
    public static final String BINDER_NAME = "wireless_induction_binder";
    private static final CreateRegistrate REGISTRATE = CNMCore.registrate();

    public static final BlockEntry<WirelessRedstoneControlTerminalBlock> WIRELESS_REDSTONE_CONTROL_TERMINAL =
            REGISTRATE.block(NAME, WirelessRedstoneControlTerminalBlock::new)
                    .initialProperties(() -> Blocks.IRON_BLOCK)
                    .properties(properties -> properties.strength(3.5F).sound(SoundType.METAL))
                    .blockstate((ctx, prov) -> prov.simpleBlock(ctx.getEntry(),
                            prov.models().cubeAll(ctx.getName(), prov.modLoc("block/" + NAME))))
                    .item()
                    .model((ctx, prov) -> prov.withExistingParent(ctx.getName(), prov.modLoc("block/" + NAME)))
                    .build()
                    .register();

    /** Invisible redstone delivery endpoint owned by bound OUT nodes. Not obtainable as an item.
     *  Barrier-like properties: solid and unbreakable, so it can never be accidentally replaced
     *  (its invisible, collision-less AIR form let placements pass straight through it). */
    public static final BlockEntry<TerminalEndpointBlock> ENDPOINT =
            REGISTRATE.block(ENDPOINT_NAME, TerminalEndpointBlock::new)
                    .initialProperties(() -> Blocks.BARRIER)
                    // The map color callback copied from BARRIER queries the WATERLOGGED property this block
                    // never defines, and it already runs during BlockState construction - swap in a constant
                    .properties(properties -> properties.mapColor(MapColor.NONE))
                    .blockstate((ctx, prov) -> prov.simpleBlock(ctx.getEntry(),
                            prov.models().cubeAll(ctx.getName(), prov.modLoc("block/" + ENDPOINT_NAME))))
                    .register();

    public static final ItemEntry<WirelessInductionBinderItem> WIRELESS_INDUCTION_BINDER =
            REGISTRATE.item(BINDER_NAME, WirelessInductionBinderItem::new)
                    .register();

    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITY_TYPES =
            DeferredRegister.create(BuiltInRegistries.BLOCK_ENTITY_TYPE, CNMCore.ID);
    public static final Supplier<BlockEntityType<WirelessRedstoneControlTerminalBlockEntity>> TERMINAL_BLOCK_ENTITY =
            BLOCK_ENTITY_TYPES.register(NAME, () -> BlockEntityType.Builder
                    .of(WirelessRedstoneControlTerminalBlockEntity::new, WIRELESS_REDSTONE_CONTROL_TERMINAL.get())
                    .build(null));
    public static final Supplier<BlockEntityType<TerminalEndpointBlockEntity>> ENDPOINT_BLOCK_ENTITY =
            BLOCK_ENTITY_TYPES.register(ENDPOINT_NAME, () -> BlockEntityType.Builder
                    .of(TerminalEndpointBlockEntity::new, ENDPOINT.get())
                    .build(null));

    public static final DeferredRegister<MenuType<?>> MENU_TYPES =
            DeferredRegister.create(BuiltInRegistries.MENU, CNMCore.ID);
    public static final Supplier<MenuType<WirelessRedstoneControlTerminalMenu>> TERMINAL_MENU =
            MENU_TYPES.register(NAME, () -> IMenuTypeExtension.create(WirelessRedstoneControlTerminalMenu::fromNetwork));

    public static void register(IEventBus modBus) {
        BLOCK_ENTITY_TYPES.register(modBus);
        MENU_TYPES.register(modBus);
        modBus.addListener(TerminalRegistry::registerPayloads);
    }

    private static void registerPayloads(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar("3");
        registrar.playToServer(TerminalEditPayload.TYPE, TerminalEditPayload.CODEC, TerminalEditPayload::handle);
        registrar.playToServer(TerminalFrequencyPayload.TYPE, TerminalFrequencyPayload.CODEC, TerminalFrequencyPayload::handle);
        registrar.playToClient(TerminalSyncPayload.TYPE, TerminalSyncPayload.CODEC, TerminalSyncPayload::handle);
    }
}