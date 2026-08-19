package com.onehumanawa.cnmcore.foundation.registry;

import com.onehumanawa.cnmcore.CNMCore;
import com.simibubi.create.foundation.data.CreateRegistrate;
import com.tterrag.registrate.util.entry.BlockEntry;
import com.tterrag.registrate.util.entry.ItemEntry;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;

/**
 * KubeJava-style registry handler for items and blocks.
 * <p>
 * Usage:
 * <pre>{@code
 * KubeJavaRegistryHandler.init();
 *
 * // Inside init():
 * item("my_item").maxStackSize(16).lang("My Item", "我的物品").register();
 * block("my_block").lang("My Block", "我的方块").register();
 * }</pre>
 */
public final class KubeJavaRegistryHandler {

    private static final CreateRegistrate REGISTRATE = CNMCore.registrate();
    private static final LangCollector LANG_COLLECTOR = new LangCollector();

    private KubeJavaRegistryHandler() {}

    /**
     * Entry point for all registry declarations.
     * Called during CNMCore construction.
     */
    public static void init() {
        item("logistic_mechanism").lang("Logistic Mechanism", "物流构件").register();
        item("fluid_mechanism").lang("Fluid Mechanism", "流体构件").register();
    }

    /**
     * Starts an item builder for the given id.
     *
     * @param id item id (without namespace, defaults to cnmcore)
     * @return a new ItemBuilder
     */
    public static ItemBuilder item(String id) {
        return new ItemBuilder(id, LANG_COLLECTOR);
    }

    /**
     * Starts a block builder for the given id.
     *
     * @param id block id (without namespace, defaults to cnmcore)
     * @return a new BlockBuilder
     */
    public static BlockBuilder block(String id) {
        return new BlockBuilder(id, LANG_COLLECTOR);
    }

    static CreateRegistrate registrate() {
        return REGISTRATE;
    }

    static LangCollector langCollector() {
        return LANG_COLLECTOR;
    }

    /**
     * Internal: collects language entries for all registered items/blocks.
     * Used by ModLangProvider to generate language files.
     */
    public static LangCollector getLangCollector() {
        return LANG_COLLECTOR;
    }
}