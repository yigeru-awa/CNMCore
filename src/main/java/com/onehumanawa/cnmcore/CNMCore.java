package com.onehumanawa.cnmcore;

import com.onehumanawa.cnmcore.foundation.data.lang.ModLangProvider;
import com.simibubi.create.foundation.data.CreateRegistrate;
import net.minecraft.data.DataGenerator;
import net.minecraft.data.PackOutput;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.data.event.GatherDataEvent;
import org.slf4j.Logger;
import com.mojang.logging.LogUtils;

@Mod(CNMCore.ID)
public class CNMCore {
    public static final String ID = "cnmcore";
    public static final String NAME = "CNM Core";
    public static final Logger LOGGER = LogUtils.getLogger();

    // Set default creative tab for ALL items registered through Registrate
    private static final CreateRegistrate REGISTRATE = CreateRegistrate.create(ID)
            .defaultCreativeTab(AllCreativeModeTabs.MAIN_TAB.getKey());

    public CNMCore(IEventBus modEventBus) {
        LOGGER.info("{} initializing!", NAME);

        // Register creative tabs
        AllCreativeModeTabs.register(modEventBus);

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