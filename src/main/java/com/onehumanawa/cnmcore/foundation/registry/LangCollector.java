package com.onehumanawa.cnmcore.foundation.registry;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Collects language entries for items, blocks and KubeJava declarations
 * (item/block translations, block crafting feedback, ...). Consumed by
 * ModLangProvider during data generation.
 */
public final class LangCollector {

    private final Map<String, LangEntry> entries = new HashMap<>();

    /** Registers (or overwrites) the bilingual translation of one key. */
    public void add(String key, String en, String zh) {
        entries.put(key, new LangEntry(en, zh));
    }

    public Map<String, LangEntry> getEntries() {
        return entries;
    }

    public record LangEntry(String en, String zh) {}

    /**
     * Generates all language entries for ModLangProvider.
     * Returns a map of locale -> (key -> value).
     */
    public Map<String, Map<String, String>> generateAll() {
        Map<String, Map<String, String>> result = new HashMap<>();
        Map<String, String> enMap = new HashMap<>();
        Map<String, String> zhMap = new HashMap<>();

        for (Map.Entry<String, LangEntry> entry : entries.entrySet()) {
            enMap.put(entry.getKey(), entry.getValue().en());
            zhMap.put(entry.getKey(), entry.getValue().zh());
        }

        result.put("en_us", enMap);
        result.put("zh_cn", zhMap);
        return result;
    }
}