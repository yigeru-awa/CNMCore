package com.onehumanawa.cnmcore.foundation.item;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;

/**
 * Chained builder modifying the membership of one item tag at runtime,
 * created through {@code KubeJavaTooltipModifier.itemTagOf(tagId)}.
 * <p>
 * Additions and removals are collected here and applied by
 * {@link TagModificationHandler} whenever tags are (re)bound - on server
 * start and after {@code /reload}. The modified tags are synced to clients
 * automatically. Tag members are plain item ids; encoded specs from
 * {@code itemOf} are accepted too, their data components are ignored since
 * tags cannot carry them.
 */
@SuppressWarnings("unused")
public final class ItemTagBuilder {

    private final TagKey<Item> tagKey;

    public ItemTagBuilder(String tagId) {
        this.tagKey = TagKey.create(Registries.ITEM, parseId(tagId));
    }

    /** The tag this builder mutates. */
    public TagKey<Item> tagKey() {
        return tagKey;
    }

    /**
     * Adds items to this tag. May be called repeatedly; duplicates and ids
     * already present in the tag are ignored.
     *
     * @param itemIds item ids (or specs from {@code itemOf}, components
     *                discarded) to add, e.g. {@code "create:brass_ingot"}
     * @return this builder
     */
    public ItemTagBuilder add(String... itemIds) {
        TagModificationHandler.addAdditions(tagKey, parse(itemIds));
        return this;
    }

    /**
     * Removes items from this tag. May be called repeatedly; ids not present
     * in the tag are ignored.
     *
     * @param itemIds item ids (or specs from {@code itemOf}, components
     *                discarded) to remove
     * @return this builder
     */
    public ItemTagBuilder remove(String... itemIds) {
        TagModificationHandler.addRemovals(tagKey, parse(itemIds));
        return this;
    }

    private static List<ResourceLocation> parse(String[] specs) {
        List<ResourceLocation> ids = new ArrayList<>(specs.length);
        for (String spec : specs) {
            ids.add(parseId(ItemSpec.decode(spec).id()));
        }
        return ids;
    }

    private static ResourceLocation parseId(String s) {
        String[] parts = s.split(":", 2);
        return parts.length == 2
                ? ResourceLocation.fromNamespaceAndPath(parts[0], parts[1])
                : ResourceLocation.withDefaultNamespace(s);
    }
}
