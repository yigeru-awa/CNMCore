package com.onehumanawa.cnmcore;

import com.onehumanawa.cnmcore.content.simpleschematic.SimpleSchematicHandler;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;
import net.neoforged.neoforge.client.gui.VanillaGuiLayers;

/**
 * Client-only entry point for CNMCore, mirroring Create's Create/CreateClient split.
 * Keeps all client-exclusive wiring (GUI layers, client events) off the common mod class.
 */
@Mod(value = CNMCore.ID, dist = Dist.CLIENT)
public class CNMCoreClient {

    public CNMCoreClient(IEventBus modEventBus) {
        modEventBus.addListener(CNMCoreClient::registerGuiLayers);
    }

    private static void registerGuiLayers(RegisterGuiLayersEvent event) {
        event.registerAbove(VanillaGuiLayers.HOTBAR, CNMCore.asResource("simple_schematic"),
                SimpleSchematicHandler.SIMPLE_SCHEMATIC_HANDLER);
    }
}
