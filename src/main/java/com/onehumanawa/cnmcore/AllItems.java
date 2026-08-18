package com.onehumanawa.cnmcore;

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

    public static void register() {
    }
}