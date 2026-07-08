package com.yusuf.audittool.normalize;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.springframework.stereotype.Component;

import com.yusuf.audittool.model.ContextStatistics;
import com.yusuf.audittool.model.EmptyField;
import com.yusuf.audittool.model.FieldMetadata;
import com.yusuf.audittool.model.NormalizedField;

import tools.jackson.databind.JsonNode;

@Component
public class FieldClassifier {

    private static final Set<String> NOISE_KEYS = Set.of(
            "self",
            "avatarUrls",
            "iconUrl",
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

    public FieldClassification classify(List<RawField> rawFields) {
        FieldClassification classification = new FieldClassification();
        ContextStatistics statistics = classification.getStatistics();
        Set<String> collapsedObjectPaths = new HashSet<>();
        Set<String> skippedNoisePaths = new HashSet<>();

        for (RawField rawField : rawFields) {
            if (isDescendantOf(rawField, skippedNoisePaths)) {
                continue;
            }

            if (isDescendantOfCollapsedObject(rawField, collapsedObjectPaths)) {
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

                JsonNode simpleValue = extractSimpleObjectValue(value);
                if (simpleValue != null && !isEmptyString(simpleValue)) {
                    addActiveField(classification, rawField, simpleValue.asString(), "object.simple");
                    collapsedObjectPaths.add(rawField.getPath());
                }
                continue;
            }

            addActiveField(classification, rawField, value.asString(), rawField.getDetectedType());
        }

        statistics.setActiveFieldCount(classification.getActiveFields().size());
        statistics.setEmptyFieldCount(classification.getEmptyFields().size());
        return classification;
    }

    private boolean isNoise(RawField rawField) {
        return rawField.getKey() != null && NOISE_KEYS.contains(rawField.getKey());
    }

    private boolean isDescendantOfCollapsedObject(RawField rawField, Set<String> collapsedObjectPaths) {
        return isDescendantOf(rawField, collapsedObjectPaths);
    }

    private boolean isDescendantOf(RawField rawField, Set<String> parentPaths) {
        return parentPaths.stream()
                .anyMatch(path -> rawField.getPath().startsWith(path + ".")
                        || rawField.getPath().startsWith(path + "["));
    }

    private JsonNode extractSimpleObjectValue(JsonNode value) {
        if (!value.isObject()) {
            return null;
        }

        for (String key : SIMPLE_VALUE_KEYS) {
            if (value.hasNonNull(key)) {
                return value.get(key);
            }
        }

        return null;
    }

    private boolean isEmptyString(JsonNode value) {
        return value != null && value.isString() && value.asString().trim().isEmpty();
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
}
