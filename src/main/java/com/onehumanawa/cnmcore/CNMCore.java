package com.onehumanawa.cnmcore;

import com.onehumanawa.cnmcore.foundation.config.SimpleSchematicConfig;
import com.onehumanawa.cnmcore.foundation.data.lang.ModLangProvider;
import com.onehumanawa.cnmcore.foundation.net.CNMPackets;
import com.onehumanawa.cnmcore.foundation.registry.KubeJavaRegistryHandler;
import com.onehumanawa.cnmcore.foundation.tooltip.KubeJavaTooltipModifier;
import com.simibubi.create.foundation.data.CreateRegistrate;
import net.minecraft.data.DataGenerator;
import net.minecraft.data.PackOutput;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.neoforge.data.event.GatherDataEvent;
import org.slf4j.Logger;
import com.mojang.logging.LogUtils;

@SuppressWarnings("unused")
@Mod(CNMCore.ID)
public class CNMCore {
    public static final String ID = "cnmcore";
    public static final String NAME = "CNM Core";
    public static final Logger LOGGER = LogUtils.getLogger();

    // Set default creative tab for ALL items registered through Registrate
    private static final CreateRegistrate REGISTRATE = CreateRegistrate.create(ID)
            .defaultCreativeTab(AllCreativeModeTabs.MAIN_TAB.getKey())
            // Create-style "Hold [Shift] for summary" tooltips, see KubeJavaTooltipModifier
            .setTooltipModifierFactory(KubeJavaTooltipModifier::modifierFor);

    public CNMCore(IEventBus modEventBus, ModContainer modContainer) {
        LOGGER.info("{} initializing!", NAME);

        // Register creative tabs
        AllCreativeModeTabs.register(modEventBus);

        // Register data components (schematic file reference)
        AllDataComponents.register(modEventBus);

        // Register network packets
        CNMPackets.register();

        // Register config + its load/reload listeners
        modContainer.registerConfig(ModConfig.Type.COMMON, SimpleSchematicConfig.SPEC);
        modEventBus.addListener(SimpleSchematicConfig::onLoad);
        modEventBus.addListener(SimpleSchematicConfig::onReload);

        // Load the modpack's tooltip configuration
        KubeJavaTooltipModifier.init();

        // Register simple items
        KubeJavaRegistryHandler.init();

        // Register items
        AllItems.register();

        // Register Registrate event listeners
        REGISTRATE.registerEventListeners(modEventBus);

        // Data generation
        modEventBus.addListener(this::gatherData);
    }

    private void gatherData(GatherDataEvent event) {
        DataGenerator generator = event.getGenerator();
        PackOutput output = generator.getPackOutput();

        if (event.includeClient()) {
            generator.addProvider(true, new ModLangProvider(output, "en_us"));
            generator.addProvider(true, new ModLangProvider(output, "zh_cn"));
        }
    }

    public static ResourceLocation asResource(String path) {
        return ResourceLocation.fromNamespaceAndPath(ID, path);
    }

    public static ResourceLocation asResource(String modId, String path) {
        return ResourceLocation.fromNamespaceAndPath(modId, path);
    }

    public static CreateRegistrate registrate() {
        return REGISTRATE;
    }
}