package com.onehumanawa.cnmcore.foundation.data.lang;

import com.onehumanawa.cnmcore.CNMCore;
import com.onehumanawa.cnmcore.foundation.tooltip.KubeJavaTooltipModifier;
import net.minecraft.data.PackOutput;
import net.neoforged.neoforge.common.data.LanguageProvider;

public class ModLangProvider extends LanguageProvider {
    private final String locale;

    public ModLangProvider(PackOutput output, String locale) {
        super(output, CNMCore.ID, locale);
        this.locale = locale;
    }

    @Override
    protected void addTranslations() {
        if (locale.equals("en_us")) {
            generateEnUs();
        } else if (locale.equals("zh_cn")) {
            generateZhCn();
        }

        // Create-style "Hold [Shift] for summary" tooltip keys
        for (KubeJavaTooltipModifier.Entry entry : KubeJavaTooltipModifier.entries()) {
            entry.translations(locale).forEach(this::add);
        }
    }

    private void generateEnUs() {
        generateLang("item.cnmcore.logistic_mechanism", "Logistic Mechanism");
        generateLang("item.cnmcore.fluid_mechanism", "Fluid Mechanism");
        generateLang("item.cnmcore.simple_schematic", "Engineering Set");
        generateLang("item.cnmcore.simple_schematic.dash", " - ");
        generateLang("item.cnmcore.redprint", "Redprint");
        generateLang("cnmcore.redprint.first_pos", "First Corner: %s, %s, %s");
        generateLang("cnmcore.redprint.second_pos", "Second Corner: %s, %s, %s");
        generateLang("cnmcore.redprint.no_target", "No target block selected");
        generateLang("cnmcore.redprint.executing", "Executing removal...");
        generateLang("cnmcore.redprint.success", "Removed %s blocks");
        generateLang("cnmcore.redprint.cancelled", "Selection cancelled");
        generateLang("cnmcore.redprint.too_large", "Area too large (max 100000 blocks)");
        generateLang("cnmcore.redprint.range", "Range: %s");
        generateLang("cnmcore.blockcrafting.success", "Block crafted successfully");
        generateLang("cnmcore.recipe.block_crafting", "Block Crafting");
        generateLang("cnmcore.recipe.block_crafting.input", "Input");
        generateLang("cnmcore.recipe.block_crafting.pattern", "Pattern");
        generateLang("cnmcore.recipe.block_crafting.output", "Output");
        generateLang("cnmcore.recipe.block_crafting.not_consumed", "Not Consumed");
        generateLang("cnmcore.recipe.block_crafting.consumed", "Consumed");
        generateLang("cnmcore.recipe.block_crafting.consumed_status", "Status: %s");
        generateLang("cnmcore.recipe.block_crafting.center_marker", "Center Block");
        generateLang("cnmcore.recipe.block_crafting.not_consumed_short", "Kept");

        generateLang("itemGroup.cnmcore.main", "CNM Core");
    }

    private void generateZhCn() {
        generateLang("item.cnmcore.logistic_mechanism", "物流构件");
        generateLang("item.cnmcore.fluid_mechanism", "流体构件");
        generateLang("item.cnmcore.simple_schematic", "工程集合");
        generateLang("item.cnmcore.simple_schematic.dash", " - ");
        generateLang("item.cnmcore.redprint", "红图");
        generateLang("cnmcore.redprint.first_pos", "第一角: %s, %s, %s");
        generateLang("cnmcore.redprint.second_pos", "第二角: %s, %s, %s");
        generateLang("cnmcore.redprint.no_target", "没有选中目标方块");
        generateLang("cnmcore.redprint.executing", "正在执行移除...");
        generateLang("cnmcore.redprint.success", "已移除 %s 个方块");
        generateLang("cnmcore.redprint.cancelled", "已取消选择");
        generateLang("cnmcore.redprint.too_large", "区域过大 (最大 100000 方块)");
        generateLang("cnmcore.redprint.range", "范围: %s");
        generateLang("cnmcore.blockcrafting.success", "成功进行方块合成");
        generateLang("cnmcore.recipe.block_crafting", "方块合成");
        generateLang("cnmcore.recipe.block_crafting.input", "输入");
        generateLang("cnmcore.recipe.block_crafting.pattern", "结构");
        generateLang("cnmcore.recipe.block_crafting.output", "产出");
        generateLang("cnmcore.recipe.block_crafting.not_consumed", "不会被消耗");
        generateLang("cnmcore.recipe.block_crafting.consumed", "被消耗");
        generateLang("cnmcore.recipe.block_crafting.consumed_status", "状态: %s");
        generateLang("cnmcore.recipe.block_crafting.center_marker", "中心方块");
        generateLang("cnmcore.recipe.block_crafting.not_consumed_short", "保留");

        generateLang("itemGroup.cnmcore.main", "联结机构 | 核心");
    }

    private void generateLang(String key, String value) {
        add(key, value);
    }
}