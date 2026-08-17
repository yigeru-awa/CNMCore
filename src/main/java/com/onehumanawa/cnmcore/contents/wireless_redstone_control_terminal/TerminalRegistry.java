package com.onehumanawa.cnmcore.contents.wireless_redstone_control_terminal;

import com.onehumanawa.cnmcore.CNMCore;
import com.onehumanawa.cnmcore.contents.wireless_redstone_control_terminal.network.TerminalEditPayload;
import com.onehumanawa.cnmcore.contents.wireless_redstone_control_terminal.network.TerminalSyncPayload;
import com.onehumanawa.cnmcore.foundation.data.CreateRegistrate;
import com.tterrag.registrate.util.entry.BlockEntry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntityType;
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

    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITY_TYPES =
            DeferredRegister.create(BuiltInRegistries.BLOCK_ENTITY_TYPE, CNMCore.ID);
    public static final Supplier<BlockEntityType<WirelessRedstoneControlTerminalBlockEntity>> TERMINAL_BLOCK_ENTITY =
            BLOCK_ENTITY_TYPES.register(NAME, () -> BlockEntityType.Builder
                    .of(WirelessRedstoneControlTerminalBlockEntity::new, WIRELESS_REDSTONE_CONTROL_TERMINAL.get())
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
        PayloadRegistrar registrar = event.registrar("1");
        registrar.playToServer(TerminalEditPayload.TYPE, TerminalEditPayload.CODEC, TerminalEditPayload::handle);
        registrar.playToClient(TerminalSyncPayload.TYPE, TerminalSyncPayload.CODEC, TerminalSyncPayload::handle);
    }
}
