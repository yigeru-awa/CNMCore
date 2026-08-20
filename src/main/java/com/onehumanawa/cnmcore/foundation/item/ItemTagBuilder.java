package com.onehumanawa.cnmcore.foundation.item;

import java.util.ArrayList;
import java.util.List;

import com.onehumanawa.cnmcore.CNMCore;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;

import org.jetbrains.annotations.Nullable;

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
    private final boolean valid;

    public ItemTagBuilder(String tagId) {
        ResourceLocation id = parseId(tagId);
        if (id == null) {
            CNMCore.LOGGER.error("[ItemTag] Invalid tag id: '{}' - expected \"modid:path\", modifications ignored", tagId);
            this.valid = false;
            this.tagKey = TagKey.create(Registries.ITEM, CNMCore.asResource("invalid_tag"));
            return;
        }
        this.valid = true;
        this.tagKey = TagKey.create(Registries.ITEM, id);
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
        if (valid) {
            TagModificationHandler.addAdditions(tagKey, parse(itemIds));
        }
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
        if (valid) {
            TagModificationHandler.addRemovals(tagKey, parse(itemIds));
        }
        return this;
    }

    private List<ResourceLocation> parse(String[] specs) {
        List<ResourceLocation> ids = new ArrayList<>(specs.length);
        for (String spec : specs) {
            ResourceLocation id = parseId(ItemSpec.decode(spec).id());
            if (id != null) {
                ids.add(id);
            } else {
                CNMCore.LOGGER.warn("[ItemTag] Invalid item id '{}' ignored for tag {}", spec, tagKey.location());
            }
        }
        return ids;
    }

    @Nullable
    private static ResourceLocation parseId(String s) {
        try {
            String[] parts = s.split(":", 2);
            return parts.length == 2
                    ? ResourceLocation.fromNamespaceAndPath(parts[0], parts[1])
                    : ResourceLocation.withDefaultNamespace(s);
        } catch (Exception e) {
            return null;
        }
    }
}
