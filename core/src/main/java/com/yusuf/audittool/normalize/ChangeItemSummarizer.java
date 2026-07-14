package com.yusuf.audittool.normalize;

import java.util.List;
import java.util.Locale;
import java.util.Set;

import tools.jackson.databind.JsonNode;

final class ChangeItemSummarizer {

    private static final List<String> FIELD_KEYS = List.of("field", "fieldname", "path", "property");
    private static final List<String> FROM_KEYS = List.of("fromstring", "oldvalue", "previousvalue", "from");
    private static final List<String> TO_KEYS = List.of("tostring", "newvalue", "currentvalue", "to");
    private static final Set<String> CHANGE_KEYS = Set.of(
            "field", "fieldname", "path", "property",
            "from", "fromstring", "oldvalue", "previousvalue",
            "to", "tostring", "newvalue", "currentvalue"
    );

    Summary summarize(JsonNode value) {
        if (!isChangeItem(value)) {
            return null;
        }

        ChangeValue field = firstUsableValue(value, FIELD_KEYS);
        ChangeValue from = firstUsableValue(value, FROM_KEYS);
        ChangeValue to = firstUsableValue(value, TO_KEYS);
        List<String> consumedKeys = value.propertyNames().stream()
                .filter(key -> CHANGE_KEYS.contains(key.toLowerCase(Locale.ROOT)))
                .toList();

        return new Summary(
                String.join(" | ", field.render(), from.render(), to.render()),
                consumedKeys
        );
    }

    private boolean isChangeItem(JsonNode value) {
        return value != null
                && value.isObject()
                && hasUsableValue(value, FIELD_KEYS)
                && hasUsableValue(value, FROM_KEYS)
                && hasUsableValue(value, TO_KEYS);
    }

    private boolean hasUsableValue(JsonNode value, List<String> candidates) {
        return firstUsableValue(value, candidates) != null;
    }

    private ChangeValue firstUsableValue(JsonNode value, List<String> candidates) {
        for (String candidate : candidates) {
            for (String actualKey : value.propertyNames()) {
                if (candidate.equals(actualKey.toLowerCase(Locale.ROOT)) && isUsableScalar(value.get(actualKey))) {
                    return new ChangeValue(actualKey, value.get(actualKey).asString());
                }
            }
        }
        return null;
    }

    private boolean isUsableScalar(JsonNode value) {
        if (value == null || value.isNull() || value.isObject() || value.isArray()) {
            return false;
        }
        if (!value.isString()) {
            return true;
        }

        String text = value.asString().trim();
        String normalized = text.toLowerCase(Locale.ROOT);
        return !text.isEmpty()
                && !normalized.startsWith("http://")
                && !normalized.startsWith("https://")
                && !normalized.startsWith("www.");
    }

    record Summary(String value, List<String> consumedKeys) {
    }

    private record ChangeValue(String key, String value) {

        private String render() {
            return key + "=" + value;
        }
    }
}
