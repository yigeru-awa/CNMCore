package com.onehumanawa.cnmcore.foundation.recipe;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

/**
 * An item id optionally carrying data components, encoded as
 * {@code "modid:item@{components json}"} ({@code @} is not a valid character
 * in resource locations, so plain ids can never be misread as encoded specs).
 * <p>
 * Specs are produced by {@link #of(String, String)}
 * ({@code KubeJavaRecipeModifier.itemOf}) and accepted anywhere a plain item
 * id is accepted: as replacement targets of
 * {@code replaceInput}/{@code replaceOutput}, and as ingredient/result
 * parameters of the {@code add*} recipe builders.
 * <p>
 * The {@code dataComponents} argument is the JSON text of the component map,
 * e.g. {@code "{\"minecraft:enchantments\":{\"levels\":{\"minecraft:sharpness\":5}}}"}.
 */
public final class ItemSpec {

    private static final char SEPARATOR = '@';

    /**
     * A decoded item spec.
     *
     * @param id         plain item id
     * @param components component map, or {@code null} if the spec carries none
     */
    public record Decoded(String id, JsonObject components) {
    }

    private ItemSpec() {
    }

    /**
     * Encodes an item with data components.
     *
     * @param id             item id, e.g. {@code "minecraft:diamond_sword"}
     * @param dataComponents JSON text of the component map, or {@code null}
     * @return encoded spec, or the plain id when no components are given
     */
    public static String of(String id, String dataComponents) {
        if (dataComponents == null || dataComponents.isBlank())
            return id;
        return id + SEPARATOR + dataComponents.trim();
    }

    static boolean isEncoded(String spec) {
        return spec.indexOf(SEPARATOR) > 0;
    }

    static Decoded decode(String spec) {
        int separator = spec.indexOf(SEPARATOR);
        if (separator < 0)
            return new Decoded(spec, null);
        String id = spec.substring(0, separator);
        JsonObject components = JsonParser.parseString(spec.substring(separator + 1)).getAsJsonObject();
        return new Decoded(id, components);
    }

    /**
     * Builds an ingredient JSON. Plain specs become {@code {"item": id}};
     * specs with components become a NeoForge component ingredient
     * {@code {"type": "neoforge:components", "items": [...], "components": {...}}}
     * (partial match, matching stacks may carry further components).
     */
    static JsonObject ingredientJson(String spec) {
        Decoded decoded = decode(spec);
        JsonObject json = new JsonObject();
        if (decoded.components() == null) {
            json.addProperty("item", decoded.id());
            return json;
        }
        json.addProperty("type", "neoforge:components");
        JsonArray items = new JsonArray();
        items.add(decoded.id());
        json.add("items", items);
        json.add("components", decoded.components());
        return json;
    }

    /**
     * Builds a result stack JSON: {@code {"id": ..., "count": n,
     * "components": {...}}}; the {@code count} field is omitted when 1 and
     * {@code components} when the spec carries none.
     */
    static JsonObject stackJson(String spec, int count) {
        Decoded decoded = decode(spec);
        JsonObject json = new JsonObject();
        json.addProperty("id", decoded.id());
        if (count != 1)
            json.addProperty("count", count);
        if (decoded.components() != null)
            json.add("components", decoded.components());
        return json;
    }
}
