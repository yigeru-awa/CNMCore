package com.onehumanawa.cnmcore.foundation.tooltip;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Pattern;

import com.onehumanawa.cnmcore.AllItems;
import org.jetbrains.annotations.Nullable;

import com.simibubi.create.foundation.item.ItemDescription;
import com.simibubi.create.foundation.item.TooltipModifier;

import net.createmod.catnip.lang.FontHelper.Palette;
import net.minecraft.client.Minecraft;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.ItemTooltipEvent;
import net.neoforged.neoforge.registries.DeferredHolder;

import com.onehumanawa.cnmcore.CNMCore;
import com.onehumanawa.cnmcore.foundation.item.ItemSpec;
import com.onehumanawa.cnmcore.foundation.item.ItemTagBuilder;

/**
 * ====================================================================
 * KubeJava Tooltip Modifier - modpack item tooltip entry point
 * ====================================================================
 *
 * <h2>Overview</h2>
 * Every Create-style "Hold [Shift] for summary" tooltip of the modpack is
 * declared here, inside {@link #init()}. The declaration style follows
 * three steps:
 *
 * <ol>
 *   <li><b>Select</b> an item with {@link #tooltip(String)} (or the
 *       {@link DeferredHolder}/{@link ItemTagBuilder} overloads)</li>
 *   <li><b>Declare</b> lines on the returned {@link TooltipBuilder}
 *       ({@code summary}, {@code behaviour}, {@code control}, {@code palette})</li>
 *   <li>Finalize with {@code .register()} - the tooltip is then applied and
 *       translated automatically, without any further code</li>
 * </ol>
 *
 * Rendering is delegated to Create's {@link ItemDescription}. Items
 * registered through {@code CNMCore.registrate()} receive their lines via
 * the {@link TooltipModifier} factory ({@link #modifierFor}); any other
 * item - foreign ids and tag-scoped entries - is covered by the global
 * {@link ItemTooltipEvent} listener ({@link GlobalTooltips}). The matching
 * language keys are generated automatically by {@code ModLangProvider}; the
 * "Hold [Shift] ..." prompt itself reuses Create's built-in translations.
 *
 * <h2>Line types</h2>
 * <table border="1">
 *   <caption>available lines</caption>
 *   <tr><th>method</th><th>shown when</th><th>content</th></tr>
 *   <tr><td>{@code summary(en, zh)}</td><td>holding [Shift]</td>
 *       <td>one short description line</td></tr>
 *   <tr><td>{@code behaviour(condEn, textEn, condZh, textZh)}</td><td>holding [Shift]</td>
 *       <td>condition label + indented behaviour text, repeatable</td></tr>
 *   <tr><td>{@code control(ctrlEn, textEn, ctrlZh, textZh)}</td><td>holding [Ctrl]</td>
 *       <td>key label + indented action text, repeatable</td></tr>
 *   <tr><td>{@code palette(palette)}</td><td>always</td>
 *       <td>custom color palette, defaults to Create's standard palette</td></tr>
 * </table>
 *
 * Text wrapped in _underscores_ is rendered with the palette's highlight
 * color (Create's {@code FontHelper} convention).
 *
 * <h2>Generated language keys</h2>
 * For an item with description id {@code item.cnmcore.foo}, the builder
 * emits the keys Create's {@link ItemDescription} expects:
 * {@code item.cnmcore.foo.tooltip.summary},
 * {@code .condition1}/{@code .behaviour1}, ... and
 * {@code .control1}/{@code .action1}, ...
 * Tag-scoped entries use the tag id instead:
 * {@code tag.minecraft.logs.tooltip.summary}, ...
 *
 * <h2>Examples</h2>
 * <pre>{@code
 * // 1. Minimal: summary only
 * tooltip("cnmcore:logistic_mechanism")
 *         .summary("A core component for _logistic_ contraptions", "用于_物流_装置的核心构件")
 *         .register();
 *
 * // 2. Full: summary + behaviours + controls
 * tooltip("cnmcore:controller")
 *         .summary("The _brain_ of every contraption", "所有装置的_大脑_")
 *         .behaviour("When powered", "It broadcasts its signal",
 *                 "通入动力时", "广播自身信号")
 *         .control("Hold [Ctrl]", "Shows binding options",
 *                 "按住 [Ctrl]", "显示绑定选项")
 *         .register();
 *
 * // 3. From a Registrate entry, with a custom palette
 * tooltip(AllItems.FLUID_MECHANISM)
 *         .summary("A core component for _fluid_ contraptions", "用于_流体_装置的核心构件")
 *         .palette(Palette.GRAY_AND_BLUE)
 *         .register();
 *
 * // 4. Tagged items: one tooltip for every member of a tag
 * tooltip(itemTagOf("minecraft:logs"))
 *         .summary("Any kind of _log_", "任意种类的原木")
 *         .register();
 * }</pre>
 *
 * <h2>Item specs</h2>
 * {@link #itemOf(String, String)} encodes an item carrying data components
 * ({@code "modid:item@{components json}"}), the same spec format accepted by
 * the recipe modifier; see {@link ItemSpec}.
 *
 * <h2>Item tags</h2>
 * {@link #itemTagOf(String)} returns an {@link ItemTagBuilder} to mutate an
 * item tag at runtime: {@code .add(itemIds...)} appends members,
 * {@code .remove(itemIds...)} strips members, freely chainable. Changes
 * apply on server start and after {@code /reload}, and sync to clients
 * automatically. Plain ids and specs from {@link #itemOf(String, String)}
 * are both accepted (components are discarded, tags cannot carry them).
 * Passing the builder to {@link #tooltip(ItemTagBuilder)} instead attaches
 * one tooltip to every member of the tag.
 * <pre>{@code
 * itemTagOf("c:ingots/copper")
 *         .add("create:brass_ingot")
 *         .remove("minecraft:copper_ingot");
 * }</pre>
 */
@SuppressWarnings({"unused"})
public final class KubeJavaTooltipModifier {

    /** Pattern every item id must match, mirroring {@link ResourceLocation}. */
    private static final Pattern ID_PATTERN = Pattern.compile("[a-z0-9_.-]+:[a-z0-9/._-]+");

    /** All registered tooltip entries; consumed by language datagen. */
    private static final List<Entry> ENTRIES = new ArrayList<>();

    /** Palette overrides, keyed by "modid:path" item id. */
    private static final Map<String, Palette> PALETTES = new HashMap<>();

    private KubeJavaTooltipModifier() {
    }

    /**
     * Configuration entry point, invoked once during {@link CNMCore}
     * construction. Declare all item tooltips here.
     */
    public static void init() {
        tooltip(AllItems.REDPRINT)
                .summary(
                        "A _batch removal_ tool for clearing large areas",
                        "用于_批量移除_大范围方块的工具"
                )
                .behaviour(
                        "Right-click block face",
                        "Select first corner (adjacent block)",
                        "右键点击方块面",
                        "选择第一角（点击面相邻的方块）"
                )
                .behaviour(
                        "Right-click again",
                        "Select second corner, expands selection",
                        "再次右键点击",
                        "选择第二角，扩展选区"
                )
                .behaviour(
                        "Right-click when both corners set",
                        "Remove all blocks in the selected area",
                        "两个角都选好后右键",
                        "移除选中区域内所有方块"
                )
                .behaviour(
                        "Shift + Right-click",
                        "Cancel current selection",
                        "Shift + 右键",
                        "取消当前选择"
                )
                .register();
    }

    // ------------------------------------------------------------------
    // Item specs with data components
    // ------------------------------------------------------------------

    /**
     * Creates an item spec carrying data components, the shared
     * {@link ItemSpec} encoding usable across all KubeJava entry points.
     * For tags ({@link #itemTagOf}) the components are discarded.
     *
     * @param id             item id, e.g. {@code "minecraft:diamond_sword"}
     * @param dataComponent  JSON text of the component map, e.g.
     *                       {@code "{\"minecraft:enchantments\":{\"levels\":{\"minecraft:sharpness\":5}}}"}
     * @return encoded item spec, or the plain id when {@code dataComponent}
     *         is {@code null} or blank
     */
    public static String itemOf(String id, String dataComponent) {
        return ItemSpec.of(id, dataComponent);
    }

    // ------------------------------------------------------------------
    // Item tags (backed by TagModificationHandler)
    // ------------------------------------------------------------------

    /**
     * Starts modifying the membership of an item tag at runtime. Applies on
     * server start and after {@code /reload}, synced to clients.
     *
     * @param tagId item tag id in "modid:path" form, e.g. {@code "c:ingots/copper"}
     * @return a builder to {@code add}/{@code remove} members on
     */
    public static ItemTagBuilder itemTagOf(String tagId) {
        return new ItemTagBuilder(tagId);
    }

    // ------------------------------------------------------------------
    // Entry factories
    // ------------------------------------------------------------------

    /**
     * Starts a tooltip declaration for the item with the given id.
     *
     * @param itemId item id in "modid:path" form, e.g. {@code "cnmcore:logistic_mechanism"}
     * @return a builder to declare the tooltip lines on
     */
    public static TooltipBuilder tooltip(String itemId) {
        return new TooltipBuilder(itemId, false);
    }

    /**
     * Starts a tooltip declaration for a Registrate/DeferredRegister entry.
     *
     * @param item the registered item entry, e.g. {@code AllItems.LOGISTIC_MECHANISM}
     * @return a builder to declare the tooltip lines on
     */
    public static TooltipBuilder tooltip(DeferredHolder<Item, ?> item) {
        return tooltip(item.getId().toString());
    }

    /**
     * Starts a tooltip declaration covering every member of the tag carried
     * by the given {@link ItemTagBuilder} (from {@link #itemTagOf(String)}).
     * The same builder can both mutate tag membership and declare the
     * tooltip; do NOT stringify it - pass it in directly.
     *
     * @param tag the item tag builder, e.g. {@code itemTagOf("minecraft:logs")}
     * @return a builder to declare the tooltip lines on
     */
    public static TooltipBuilder tooltip(ItemTagBuilder tag) {
        return new TooltipBuilder(tag.tagKey().location().toString(), true);
    }

    /**
     * Starts a tooltip declaration covering every member of the given tag.
     *
     * @param tag the item tag, e.g. {@code ItemTags.LOGS}
     * @return a builder to declare the tooltip lines on
     */
    public static TooltipBuilder tooltip(TagKey<Item> tag) {
        return new TooltipBuilder(tag.location().toString(), true);
    }

    /**
     * Shortcut for a summary-only tooltip.
     *
     * @param itemId item id in "modid:path" form
     * @param en     english summary line
     * @param zh     chinese summary line
     */
    public static void summary(String itemId, String en, String zh) {
        tooltip(itemId).summary(en, zh).register();
    }

    // ------------------------------------------------------------------
    // Integration points (registrate factory + language datagen)
    // ------------------------------------------------------------------

    /**
     * All registered entries, consumed by {@code ModLangProvider} to
     * generate the tooltip language keys.
     */
    public static List<Entry> entries() {
        return Collections.unmodifiableList(ENTRIES);
    }

    /**
     * Builds the {@link TooltipModifier} attached to items of
     * {@code CNMCore.registrate()} through {@code setTooltipModifierFactory}.
     * Mirrors Create's {@link ItemDescription.Modifier}, but resolves the
     * palette lazily so entries declared with string ids work regardless of
     * registration order.
     */
    public static TooltipModifier modifierFor(Item item) {
        return new TooltipModifier() {
            private String cachedLanguage;
            private ItemDescription description;

            @Override
            public void modify(ItemTooltipEvent context) {
                String currentLanguage = Minecraft.getInstance().getLanguageManager().getSelected();
                if (!currentLanguage.equals(cachedLanguage)) {
                    cachedLanguage = currentLanguage;
                    description = ItemDescription.create(item, paletteOf(item));
                }
                if (description != null) {
                    context.getToolTip().addAll(1, description.getCurrentLines());
                }
            }
        };
    }

    /**
     * Resolves the palette for an item: an override declared via
     * {@link TooltipBuilder#palette(Palette)}, or Create's standard palette.
     */
    public static Palette paletteOf(Item item) {
        ResourceLocation id = BuiltInRegistries.ITEM.getKey(item);
        Palette palette = PALETTES.get(id.toString());
        return palette != null ? palette : Palette.STANDARD_CREATE;
    }

    /**
     * Resolves the palette declared for an entry (item id or tag id),
     * falling back to Create's standard palette.
     */
    public static Palette paletteFor(Entry entry) {
        Palette palette = PALETTES.get(entry.paletteKey());
        return palette != null ? palette : Palette.STANDARD_CREATE;
    }

    static void register(Entry entry, @Nullable Palette palette) {
        ENTRIES.add(entry);
        if (palette != null) {
            PALETTES.put(entry.paletteKey(), palette);
        }
    }

    /**
     * Client-side fallback applying entries to items not covered by the
     * Registrate tooltip modifier: foreign item ids and tag-scoped entries.
     * Entries targeting our own items are skipped, those run through
     * {@link #modifierFor(Item)}.
     */
    @EventBusSubscriber(value = Dist.CLIENT, modid = CNMCore.ID)
    public static final class GlobalTooltips {

        private static final Map<Entry, Optional<ItemDescription>> CACHE = new HashMap<>();
        private static String cachedLanguage;

        @SubscribeEvent
        public static void onItemTooltip(ItemTooltipEvent event) {
            ItemStack stack = event.getItemStack();
            String language = Minecraft.getInstance().getLanguageManager().getSelected();
            if (!language.equals(cachedLanguage)) {
                cachedLanguage = language;
                CACHE.clear();
            }
            for (Entry entry : ENTRIES) {
                // Items of our own registrate are handled by modifierFor
                if (!entry.tag() && entry.targetId().startsWith(CNMCore.ID + ":")) {
                    continue;
                }
                if (!entry.matches(stack)) {
                    continue;
                }
                CACHE
                        .computeIfAbsent(entry, e -> Optional.ofNullable(
                                ItemDescription.create(e.baseKey(), paletteFor(e)))).ifPresent(description -> event.getToolTip().addAll(1, description.getCurrentLines()));
            }
        }
    }

    // ------------------------------------------------------------------
    // Entry model
    // ------------------------------------------------------------------

    /**
     * A collected tooltip definition for one item, or for all members of
     * one item tag. Behaviour/control entries are stored as {@code [en, zh]}
     * condition and text pairs.
     */
    public record Entry(String targetId, boolean tag, String summaryEn, String summaryZh,
                        List<String[]> behaviours, List<String[]> controls) {

        /** Key into the palette override map. */
        public String paletteKey() {
            return tag ? "tag:" + targetId : targetId;
        }

        /**
         * Base translation key, e.g. {@code item.cnmcore.logistic_mechanism.tooltip}
         * or {@code tag.minecraft.logs.tooltip}. Item entries derive from the
         * item id alone, matching {@code Item.getDescriptionId()} for
         * vanilla-style items.
         */
        public String baseKey() {
            ResourceLocation id = ResourceLocation.parse(targetId);
            String prefix = tag ? "tag" : "item";
            return prefix + "." + id.getNamespace() + "." + id.getPath() + ".tooltip";
        }

        /** Whether the hovered stack is covered by this entry. */
        public boolean matches(ItemStack stack) {
            if (tag) {
                return stack.is(TagKey.create(Registries.ITEM, ResourceLocation.parse(targetId)));
            }
            return BuiltInRegistries.ITEM.getKey(stack.getItem()).toString().equals(targetId);
        }

        /**
         * All translation entries for the given locale, keyed as Create's
         * {@link ItemDescription} expects them.
         */
        public Map<String, String> translations(String locale) {
            String base = baseKey();
            boolean zh = locale.equals("zh_cn");
            Map<String, String> out = new LinkedHashMap<>();
            String summary = zh ? summaryZh : summaryEn;
            if (summary != null) {
                out.put(base + ".summary", summary);
            }
            int i = 1;
            for (String[] behaviour : behaviours) {
                out.put(base + ".condition" + i, zh ? behaviour[2] : behaviour[0]);
                out.put(base + ".behaviour" + i, zh ? behaviour[3] : behaviour[1]);
                i++;
            }
            i = 1;
            for (String[] control : controls) {
                out.put(base + ".control" + i, zh ? control[2] : control[0]);
                out.put(base + ".action" + i, zh ? control[3] : control[1]);
                i++;
            }
            return out;
        }
    }

    /**
     * Chained builder collecting bilingual tooltip lines for one item.
     */
    public static final class TooltipBuilder {
        private final String targetId;
        private final boolean tag;
        private String summaryEn;
        private String summaryZh;
        private Palette palette;
        private final List<String[]> behaviours = new ArrayList<>();
        private final List<String[]> controls = new ArrayList<>();

        private TooltipBuilder(String targetId, boolean tag) {
            if (!ID_PATTERN.matcher(targetId).matches()) {
                throw new IllegalArgumentException(
                        "[Tooltip] Invalid " + (tag ? "tag" : "item") + " id: " + targetId
                                + " - expected \"modid:path\", e.g. \"cnmcore:logistic_mechanism\""
                                + " (pass itemTagOf(...) to tooltip() directly, without toString())");
            }
            this.targetId = targetId;
            this.tag = tag;
        }

        /**
         * Single summary line shown while holding [Shift].
         *
         * @param en english line, {@code _underscore_} segments are highlighted
         * @param zh chinese line, same highlighting rules
         */
        public TooltipBuilder summary(String en, String zh) {
            this.summaryEn = en;
            this.summaryZh = zh;
            return this;
        }

        /**
         * Adds a condition/behaviour pair shown under the summary while
         * holding [Shift]. May be called multiple times.
         *
         * @param conditionEn condition label, e.g. {@code "When powered"}
         * @param behaviourEn behaviour text, indented below the condition
         * @param conditionZh chinese condition label
         * @param behaviourZh chinese behaviour text
         */
        public TooltipBuilder behaviour(String conditionEn, String behaviourEn,
                                        String conditionZh, String behaviourZh) {
            behaviours.add(new String[]{conditionEn, behaviourEn, conditionZh, behaviourZh});
            return this;
        }

        /**
         * Adds a control/action pair shown while holding [Ctrl]. May be
         * called multiple times.
         *
         * @param controlEn key label, e.g. {@code "Hold [Ctrl]"}
         * @param actionEn  action text, indented below the label
         * @param controlZh chinese key label
         * @param actionZh  chinese action text
         */
        public TooltipBuilder control(String controlEn, String actionEn,
                                      String controlZh, String actionZh) {
            controls.add(new String[]{controlEn, actionEn, controlZh, actionZh});
            return this;
        }

        /**
         * Overrides the color palette for this item; defaults to
         * {@link Palette#STANDARD_CREATE}.
         */
        public TooltipBuilder palette(Palette palette) {
            this.palette = palette;
            return this;
        }

        /**
         * Finalizes the tooltip; language keys are generated by
         * {@code ModLangProvider}.
         */
        public void register() {
            KubeJavaTooltipModifier.register(
                    new Entry(targetId, tag, summaryEn, summaryZh,
                            List.copyOf(behaviours), List.copyOf(controls)),
                    palette);
        }
    }
}
