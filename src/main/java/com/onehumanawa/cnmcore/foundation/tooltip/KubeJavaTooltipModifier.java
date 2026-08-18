package com.onehumanawa.cnmcore.foundation.tooltip;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.jetbrains.annotations.Nullable;

import com.simibubi.create.foundation.item.ItemDescription;
import com.simibubi.create.foundation.item.TooltipModifier;

import net.createmod.catnip.lang.FontHelper.Palette;
import net.minecraft.client.Minecraft;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.neoforged.neoforge.event.entity.player.ItemTooltipEvent;
import net.neoforged.neoforge.registries.DeferredHolder;

import com.onehumanawa.cnmcore.CNMCore;

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
 *       {@link DeferredHolder} overload)</li>
 *   <li><b>Declare</b> lines on the returned {@link TooltipBuilder}
 *       ({@code summary}, {@code behaviour}, {@code control}, {@code palette})</li>
 *   <li>Finalize with {@code .register()} - the tooltip is then applied and
 *       translated automatically, without any further code</li>
 * </ol>
 *
 * Rendering is delegated to Create's {@link ItemDescription} through the
 * {@link TooltipModifier#REGISTRY}: {@code CNMCore.registrate()} attaches an
 * {@link ItemDescription.Modifier}-equivalent to every item it registers, so
 * only items declared through that registrate are covered. The matching
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
 * }</pre>
 */
@SuppressWarnings({"unused"})
public final class KubeJavaTooltipModifier {

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
        return new TooltipBuilder(itemId);
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
        Palette palette = id == null ? null : PALETTES.get(id.toString());
        return palette != null ? palette : Palette.STANDARD_CREATE;
    }

    static void register(Entry entry, @Nullable Palette palette) {
        ENTRIES.add(entry);
        if (palette != null) {
            PALETTES.put(entry.itemId(), palette);
        }
    }

    // ------------------------------------------------------------------
    // Entry model
    // ------------------------------------------------------------------

    /**
     * A collected tooltip definition for one item. Behaviour/control entries
     * are stored as {@code [en, zh]} condition and text pairs.
     */
    public record Entry(String itemId, String summaryEn, String summaryZh,
                        List<String[]> behaviours, List<String[]> controls) {

        /**
         * Base translation key, e.g. {@code item.cnmcore.logistic_mechanism.tooltip}.
         * Requires the item to be registered; only call during datagen.
         */
        @Nullable
        public String baseKey() {
            Item item = BuiltInRegistries.ITEM.get(ResourceLocation.parse(itemId));
            if (item == null) {
                CNMCore.LOGGER.warn("[Tooltip] No item found for id {}, skipping its tooltip keys", itemId);
                return null;
            }
            return item.getDescriptionId() + ".tooltip";
        }

        /**
         * All translation entries for the given locale, keyed as Create's
         * {@link ItemDescription} expects them.
         */
        public Map<String, String> translations(String locale) {
            String base = baseKey();
            if (base == null) {
                return Map.of();
            }
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
        private final String itemId;
        private String summaryEn;
        private String summaryZh;
        private Palette palette;
        private final List<String[]> behaviours = new ArrayList<>();
        private final List<String[]> controls = new ArrayList<>();

        private TooltipBuilder(String itemId) {
            this.itemId = itemId;
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
                    new Entry(itemId, summaryEn, summaryZh,
                            List.copyOf(behaviours), List.copyOf(controls)),
                    palette);
        }
    }
}
