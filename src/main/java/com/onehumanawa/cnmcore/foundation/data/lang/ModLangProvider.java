package com.onehumanawa.cnmcore.foundation.data.lang;

import com.onehumanawa.cnmcore.CNMCore;
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
    }

    private void generateEnUs() {
        generateLang("item.cnmcore.logistic_mechanism", "Logistic Mechanism");
        generateLang("item.cnmcore.fluid_mechanism", "Fluid Mechanism");

        generateLang("itemGroup.cnmcore.main", "CNM Core");
    }

    private void generateZhCn() {
        generateLang("item.cnmcore.logistic_mechanism", "物流构件");
        generateLang("item.cnmcore.fluid_mechanism", "流体构件");

        generateLang("itemGroup.cnmcore.main", "联结机构 | 核心");
    }

    private void generateLang(String key, String value) {
        add(key, value);
    }
}