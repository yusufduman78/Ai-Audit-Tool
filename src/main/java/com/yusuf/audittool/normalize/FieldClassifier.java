package com.yusuf.audittool.normalize;

import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import org.springframework.stereotype.Component;

import com.yusuf.audittool.model.ContextStatistics;
import com.yusuf.audittool.model.EmptyField;
import com.yusuf.audittool.model.FieldMetadata;
import com.yusuf.audittool.model.NormalizedField;

import tools.jackson.databind.JsonNode;

@Component
public class FieldClassifier {

    private final ChangeItemSummarizer changeItemSummarizer = new ChangeItemSummarizer();

    private static final Set<String> NOISE_KEYS = Set.of(
            "expand",
            "operations",
            "schema"
    );

    private static final List<String> SIMPLE_VALUE_KEYS = List.of(
            "displayName",
            "name",
            "value",
            "key"
    );

    private static final Set<String> COLLAPSED_OBJECT_DETAIL_KEYS = Set.of(
            "id",
            "self",
            "iconurl",
            "avatarid",
            "avatarurls"
    );

    public FieldClassification classify(List<RawField> rawFields) {
        return classify(rawFields, Set.of());
    }

    public FieldClassification classify(List<RawField> rawFields, Set<String> excludedPathPrefixes) {
        FieldClassification classification = new FieldClassification();
        ContextStatistics statistics = classification.getStatistics();
        Set<String> consumedSimpleValuePaths = new HashSet<>();
        Set<String> collapsedObjectDetailPaths = new HashSet<>();
        Set<String> skippedNoisePaths = new HashSet<>();

        for (RawField rawField : rawFields) {
            if (isExcluded(rawField, excludedPathPrefixes)) {
                continue;
            }

            if (consumedSimpleValuePaths.contains(rawField.getPath())) {
                continue;
            }

            if (isAtOrDescendantOf(rawField, collapsedObjectDetailPaths)) {
                statistics.setSkippedNoiseFieldCount(statistics.getSkippedNoiseFieldCount() + 1);
                continue;
            }

            if (isDescendantOf(rawField, skippedNoisePaths)) {
                continue;
            }

            if (isNoise(rawField)) {
                statistics.setSkippedNoiseFieldCount(statistics.getSkippedNoiseFieldCount() + 1);
                skippedNoisePaths.add(rawField.getPath());
                continue;
            }

            JsonNode value = rawField.getValue();

            if (value == null || value.isNull()) {
                statistics.setNullFieldCount(statistics.getNullFieldCount() + 1);
                continue;
            }

            if (isLinkValue(value)) {
                statistics.setSkippedNoiseFieldCount(statistics.getSkippedNoiseFieldCount() + 1);
                continue;
            }

            if (isEmptyString(value)) {
                addEmptyField(classification, rawField, "EMPTY_STRING");
                continue;
            }

            if (value.isArray()) {
                if (value.isEmpty()) {
                    addEmptyField(classification, rawField, "EMPTY_ARRAY");
                }
                continue;
            }

            if (value.isObject()) {
                if (value.isEmpty()) {
                    addEmptyField(classification, rawField, "EMPTY_OBJECT");
                    continue;
                }

                SimpleValue simpleValue = extractSimpleObjectValue(value);
                if (simpleValue != null) {
                    addActiveField(classification, rawField, simpleValue.value().asString(), "object.simple");
                    consumedSimpleValuePaths.add(rawField.getPath() + "." + simpleValue.key());
                    collectCollapsedObjectDetailPaths(rawField, value, collapsedObjectDetailPaths);
                } else {
                    ChangeItemSummarizer.Summary summary = changeItemSummarizer.summarize(value);
                    if (summary != null) {
                        addActiveField(classification, rawField, summary.value(), "change.compact");
                        for (String consumedKey : summary.consumedKeys()) {
                            String consumedPath = rawField.getPath() + "." + consumedKey;
                            consumedSimpleValuePaths.add(consumedPath);
                            collapsedObjectDetailPaths.add(consumedPath);
                        }
                    }
                }
                continue;
            }

            addActiveField(classification, rawField, value.asString(), rawField.getDetectedType());
        }

        statistics.setActiveFieldCount(classification.getActiveFields().size());
        statistics.setEmptyFieldCount(classification.getEmptyFields().size());
        return classification;
    }

    private boolean isExcluded(RawField rawField, Set<String> excludedPathPrefixes) {
        if (rawField == null || rawField.getPath() == null || excludedPathPrefixes == null) {
            return false;
        }

        return excludedPathPrefixes.stream()
                .filter(prefix -> prefix != null && !prefix.isBlank())
                .anyMatch(prefix -> rawField.getPath().equals(prefix)
                        || rawField.getPath().startsWith(prefix + ".")
                        || rawField.getPath().startsWith(prefix + "["));
    }

    private boolean isNoise(RawField rawField) {
        return rawField.getKey() != null && NOISE_KEYS.contains(rawField.getKey());
    }

    private boolean isDescendantOf(RawField rawField, Set<String> parentPaths) {
        return parentPaths.stream()
                .anyMatch(path -> rawField.getPath().startsWith(path + ".")
                        || rawField.getPath().startsWith(path + "["));
    }

    private boolean isAtOrDescendantOf(RawField rawField, Set<String> paths) {
        return paths.stream()
                .anyMatch(path -> rawField.getPath().equals(path)
                        || rawField.getPath().startsWith(path + ".")
                        || rawField.getPath().startsWith(path + "["));
    }

    private void collectCollapsedObjectDetailPaths(
            RawField rawField,
            JsonNode value,
            Set<String> detailPaths
    ) {
        for (String key : value.propertyNames()) {
            if (COLLAPSED_OBJECT_DETAIL_KEYS.contains(key.toLowerCase(Locale.ROOT))) {
                detailPaths.add(rawField.getPath() + "." + key);
            }
        }
    }

    private SimpleValue extractSimpleObjectValue(JsonNode value) {
        if (!value.isObject()) {
            return null;
        }

        for (String key : SIMPLE_VALUE_KEYS) {
            JsonNode candidate = value.get(key);
            if (candidate != null && candidate.isString() && !isEmptyString(candidate) && !isLinkValue(candidate)) {
                return new SimpleValue(key, candidate);
            }
        }

        return null;
    }

    private boolean isEmptyString(JsonNode value) {
        return value != null && value.isString() && value.asString().trim().isEmpty();
    }

    private boolean isLinkValue(JsonNode value) {
        if (value == null || !value.isString()) {
            return false;
        }

        String text = value.asString().trim().toLowerCase();
        return text.startsWith("http://")
                || text.startsWith("https://")
                || text.startsWith("www.");
    }

    private void addActiveField(FieldClassification classification, RawField rawField, String value, String valueType) {
        NormalizedField field = new NormalizedField();
        field.setPath(rawField.getPath());
        field.setParentPath(rawField.getParentPath());
        field.setKey(rawField.getKey());
        field.setLabel(defaultLabel(rawField));
        field.setValue(value);
        field.setValueType(valueType);
        field.setDepth(rawField.getDepth());
        field.setMetadata(defaultMetadata());
        classification.getActiveFields().add(field);
    }

    private void addEmptyField(FieldClassification classification, RawField rawField, String emptyType) {
        EmptyField field = new EmptyField();
        field.setPath(rawField.getPath());
        field.setParentPath(rawField.getParentPath());
        field.setKey(rawField.getKey());
        field.setLabel(defaultLabel(rawField));
        field.setEmptyType(emptyType);
        field.setDepth(rawField.getDepth());
        field.setMetadata(defaultMetadata());
        classification.getEmptyFields().add(field);
    }

    private String defaultLabel(RawField rawField) {
        if (rawField.getKey() == null || rawField.getKey().startsWith("[")) {
            return rawField.getPath();
        }
        return rawField.getKey();
    }

    private FieldMetadata defaultMetadata() {
        FieldMetadata metadata = new FieldMetadata();
        metadata.setProvided(false);
        return metadata;
    }

    private record SimpleValue(String key, JsonNode value) {
    }
}
