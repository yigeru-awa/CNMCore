package com.onehumanawa.cnmcore;

import com.onehumanawa.cnmcore.content.simpleschematic.SimpleSchematicItem;
import com.simibubi.create.foundation.data.AssetLookup;
import com.simibubi.create.foundation.data.CreateRegistrate;
import com.tterrag.registrate.util.entry.ItemEntry;
import net.minecraft.world.item.Item;

public class AllItems {
    private static final CreateRegistrate REGISTRATE = CNMCore.registrate();

    public static final ItemEntry<Item> LOGISTIC_MECHANISM = REGISTRATE
            .item("logistic_mechanism", Item::new)
            .register();

    public static final ItemEntry<Item> FLUID_MECHANISM = REGISTRATE
            .item("fluid_mechanism", Item::new)
            .register();

    // Hand-written item model lives in resources, skip datagen model generation
    public static final ItemEntry<SimpleSchematicItem> SIMPLE_SCHEMATIC = REGISTRATE
            .item("simple_schematic", SimpleSchematicItem::new)
            .properties(p -> p.stacksTo(16))
            .model(AssetLookup.existingItemModel())
            .register();

    public static void register() {
    }
}