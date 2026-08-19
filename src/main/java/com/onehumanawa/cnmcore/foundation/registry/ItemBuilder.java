package com.onehumanawa.cnmcore.foundation.registry;

import com.onehumanawa.cnmcore.CNMCore;
import com.simibubi.create.foundation.data.CreateRegistrate;
import com.tterrag.registrate.util.entry.ItemEntry;
import net.minecraft.world.item.Item;

/**
 * Builder for registering items via KubeJavaRegistryHandler.
 * <p>
 * Supported properties:
 * <ul>
 *   <li>{@link #stacksTo(int)} - max stack size (default 64)</li>
 *   <li>{@link #maxDurability(int)} - max durability (0 = no durability)</li>
 *   <li>{@link #fireResistant()} - makes item fire resistant</li>
 *   <li>{@link #lang(String, String)} - english and chinese translation</li>
 * </ul>
 */
public final class ItemBuilder {

    private final String id;
    private final LangCollector langCollector;
    private int maxStackSize = 64;
    private int maxDurability = 0;
    private boolean fireResistant = false;
    private String langEn = null;
    private String langZh = null;

    ItemBuilder(String id, LangCollector langCollector) {
        this.id = id;
        this.langCollector = langCollector;
    }

    /**
     * Sets the maximum stack size for this item.
     *
     * @param size max stack size (1-64)
     * @return this builder
     */
    public ItemBuilder stacksTo(int size) {
        this.maxStackSize = Math.max(1, Math.min(64, size));
        return this;
    }

    /**
     * Sets the maximum durability for this item.
     * A value of 0 means no durability (regular item).
     *
     * @param durability max durability
     * @return this builder
     */
    public ItemBuilder maxDurability(int durability) {
        this.maxDurability = Math.max(0, durability);
        return this;
    }

    /**
     * Makes this item fire resistant (does not burn in lava/fire).
     *
     * @return this builder
     */
    public ItemBuilder fireResistant() {
        this.fireResistant = true;
        return this;
    }

    /**
     * Sets the english and chinese translation for this item.
     *
     * @param en english translation (e.g. "My Item")
     * @param zh chinese translation (e.g. "我的物品")
     * @return this builder
     */
    public ItemBuilder lang(String en, String zh) {
        this.langEn = en;
        this.langZh = zh;
        return this;
    }

    /**
     * Registers the item with the configured properties.
     *
     * @return the registered ItemEntry
     */
    public ItemEntry<Item> register() {
        CreateRegistrate registrate = KubeJavaRegistryHandler.registrate();

        ItemEntry<Item> entry = registrate.item(id, properties -> {
            Item.Properties props = new Item.Properties();
            props.stacksTo(maxStackSize);
            if (maxDurability > 0) {
                props.durability(maxDurability);
            }
            if (fireResistant) {
                props.fireResistant();
            }
            return new Item(props);
        }).register();

        // Collect lang entries
        if (langEn != null && langZh != null) {
            String key = "item." + CNMCore.ID + "." + id;
            langCollector.add(key, langEn, langZh);
        }

        return entry;
    }
}