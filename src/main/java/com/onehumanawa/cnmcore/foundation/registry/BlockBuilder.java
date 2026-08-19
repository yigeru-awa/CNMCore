package com.onehumanawa.cnmcore.foundation.registry;

import com.onehumanawa.cnmcore.CNMCore;
import com.simibubi.create.foundation.data.CreateRegistrate;
import com.tterrag.registrate.util.entry.BlockEntry;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;

import java.util.function.Consumer;

/**
 * Builder for registering blocks via KubeJavaRegistryHandler.
 * <p>
 * Supported properties:
 * <ul>
 *   <li>{@link #soundType(SoundType)} - block sound type</li>
 *   <li>{@link #strength(float)} - hardness and resistance</li>
 *   <li>{@link #strength(float, float)} - custom hardness and resistance</li>
 *   <li>{@link #mapColor(MapColor)} - block map color</li>
 *   <li>{@link #noBlockItem()} - do not register a BlockItem</li>
 *   <li>{@link #blockItem(Consumer)} - configure the BlockItem properties</li>
 *   <li>{@link #lang(String, String)} - english and chinese translation</li>
 * </ul>
 */
public final class BlockBuilder {

    private final String id;
    private final LangCollector langCollector;
    private SoundType soundType = SoundType.STONE;
    private float hardness = 1.5F;
    private float resistance = 6.0F;
    private MapColor mapColor = MapColor.STONE;
    private boolean hasBlockItem = true;
    private Consumer<Item.Properties> blockItemProps = props -> {};
    private String langEn = null;
    private String langZh = null;

    BlockBuilder(String id, LangCollector langCollector) {
        this.id = id;
        this.langCollector = langCollector;
    }

    /**
     * Sets the sound type for this block.
     *
     * @param sound the sound type
     * @return this builder
     */
    public BlockBuilder soundType(SoundType sound) {
        this.soundType = sound;
        return this;
    }

    /**
     * Sets both hardness and resistance to the same value.
     *
     * @param strength hardness and resistance
     * @return this builder
     */
    public BlockBuilder strength(float strength) {
        this.hardness = strength;
        this.resistance = strength;
        return this;
    }

    /**
     * Sets custom hardness and resistance.
     *
     * @param hardness   block hardness
     * @param resistance block resistance
     * @return this builder
     */
    public BlockBuilder strength(float hardness, float resistance) {
        this.hardness = hardness;
        this.resistance = resistance;
        return this;
    }

    /**
     * Sets the map color for this block.
     *
     * @param color the map color
     * @return this builder
     */
    public BlockBuilder mapColor(MapColor color) {
        this.mapColor = color;
        return this;
    }

    /**
     * Prevents a BlockItem from being registered for this block.
     *
     * @return this builder
     */
    public BlockBuilder noBlockItem() {
        this.hasBlockItem = false;
        return this;
    }

    /**
     * Configures the BlockItem properties for this block.
     *
     * @param configurator a consumer that modifies Item.Properties
     * @return this builder
     */
    public BlockBuilder blockItem(Consumer<Item.Properties> configurator) {
        this.blockItemProps = configurator;
        return this;
    }

    /**
     * Sets the english and chinese translation for this block.
     *
     * @param en english translation (e.g. "My Block")
     * @param zh chinese translation (e.g. "我的方块")
     * @return this builder
     */
    public BlockBuilder lang(String en, String zh) {
        this.langEn = en;
        this.langZh = zh;
        return this;
    }

    /**
     * Registers the block with the configured properties.
     *
     * @return the registered BlockEntry
     */
    public BlockEntry<Block> register() {
        CreateRegistrate registrate = KubeJavaRegistryHandler.registrate();

        var builder = registrate.block(id, props -> new Block(props))
                .properties(props -> props
                        .strength(hardness, resistance)
                        .sound(soundType)
                        .mapColor(mapColor)
                );

        if (hasBlockItem) {
            builder = builder.item((block, props) -> {
                Item.Properties itemProps = new Item.Properties();
                blockItemProps.accept(itemProps);
                return new BlockItem(block.defaultBlockState().getBlock(), itemProps);
            }).build();
        }

        BlockEntry<Block> entry = builder.register();

        // Collect lang entries
        if (langEn != null && langZh != null) {
            String key = "block." + CNMCore.ID + "." + id;
            langCollector.add(key, langEn, langZh);
        }

        return entry;
    }
}