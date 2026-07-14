package com.yusuf.audittool.metadata;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Component;

import com.yusuf.audittool.model.AllowedValue;
import com.yusuf.audittool.model.ContextStatistics;
import com.yusuf.audittool.model.EmptyField;
import com.yusuf.audittool.model.FieldMetadata;
import com.yusuf.audittool.model.NormalizedField;

import tools.jackson.databind.JsonNode;

@Component
public class MetadataMapper {

    public void enrich(
            List<NormalizedField> activeFields,
            List<EmptyField> emptyFields,
            JsonNode metadata,
            JsonNode fieldDescriptions,
            ContextStatistics statistics
    ) {
        Map<String, FieldMetadata> registry = buildRegistry(metadata, fieldDescriptions);

        for (NormalizedField field : activeFields) {
            FieldMetadata fieldMetadata = findMetadata(field.getPath(), field.getKey(), field.getLabel(), registry);
            field.setMetadata(fieldMetadata);
            updateStatistics(fieldMetadata, statistics);
        }

        for (EmptyField field : emptyFields) {
            FieldMetadata fieldMetadata = findMetadata(field.getPath(), field.getKey(), field.getLabel(), registry);
            field.setMetadata(fieldMetadata);
            updateStatistics(fieldMetadata, statistics);
        }
    }

    private Map<String, FieldMetadata> buildRegistry(JsonNode metadata, JsonNode fieldDescriptions) {
        Map<String, FieldMetadata> registry = new HashMap<>();

        collectMetadata(metadata, registry);

        if (fieldDescriptions != null && fieldDescriptions.isObject()) {
            for (Map.Entry<String, JsonNode> entry : fieldDescriptions.properties()) {
                String id = entry.getKey();
                FieldMetadata fieldMetadata = registry.getOrDefault(normalize(id), new FieldMetadata());
                fieldMetadata.setProvided(true);
                if (fieldMetadata.getId() == null) {
                    fieldMetadata.setId(id);
                }
                if (entry.getValue() != null && entry.getValue().isString()) {
                    fieldMetadata.setDescriptionTr(entry.getValue().asString());
                }
                putAliases(registry, fieldMetadata);
            }
        }

        return registry;
    }

    private void collectMetadata(JsonNode metadata, Map<String, FieldMetadata> registry) {
        if (metadata == null || metadata.isNull()) {
            return;
        }

        if (metadata.isArray()) {
            for (int index = 0; index < metadata.size(); index++) {
                collectMetadata(metadata.get(index), registry);
            }
            return;
        }

        if (!metadata.isObject()) {
            return;
        }

        JsonNode values = metadata.get("values");
        if (values != null && values.isArray()) {
            collectMetadata(values, registry);
            return;
        }

        boolean collectedNestedMetadata = false;

        JsonNode fields = metadata.get("fields");
        if (fields != null && fields.isObject()) {
            collectFieldMap(fields, registry);
            collectedNestedMetadata = true;
        }

        JsonNode projects = metadata.get("projects");
        if (projects != null) {
            collectMetadata(projects, registry);
            collectedNestedMetadata = true;
        }

        JsonNode issueTypes = metadata.get("issuetypes");
        if (issueTypes != null) {
            collectMetadata(issueTypes, registry);
            collectedNestedMetadata = true;
        }

        if (collectedNestedMetadata) {
            return;
        }

        if (looksLikeSingleFieldMetadata(metadata)) {
            FieldMetadata fieldMetadata = readMetadata(null, metadata);
            putAliases(registry, fieldMetadata, metadata);
            return;
        }

        if (looksLikeFieldMap(metadata)) {
            collectFieldMap(metadata, registry);
        }
    }

    private void collectFieldMap(JsonNode fields, Map<String, FieldMetadata> registry) {
        for (Map.Entry<String, JsonNode> entry : fields.properties()) {
            String id = entry.getKey();
            FieldMetadata fieldMetadata = readMetadata(id, entry.getValue());
            putAliases(registry, fieldMetadata, entry.getValue());
        }
    }

    private FieldMetadata readMetadata(String fallbackId, JsonNode value) {
        JsonNode schema = value == null || !value.isObject() ? null : value.get("schema");
        FieldMetadata fieldMetadata = new FieldMetadata();
        fieldMetadata.setProvided(true);
        fieldMetadata.setId(text(value, "id", text(value, "key", text(value, "fieldId", fallbackId))));
        fieldMetadata.setName(text(value, "name", null));
        fieldMetadata.setSchemaType(text(value, "schemaType", text(value, "type", text(schema, "type", null))));
        fieldMetadata.setSchemaSystem(text(schema, "system", null));
        fieldMetadata.setSchemaItems(text(schema, "items", null));
        fieldMetadata.setCustomType(text(value, "customType", text(schema, "custom", null)));
        fieldMetadata.setCustomId(text(schema, "customId", null));
        fieldMetadata.setDescriptionTr(text(value, "descriptionTr", text(value, "description", null)));
        fieldMetadata.setRequired(booleanValue(value, "required"));
        fieldMetadata.setHasDefaultValue(booleanValue(value, "hasDefaultValue"));
        fieldMetadata.setAllowedValues(allowedValues(value));
        return fieldMetadata;
    }

    private void putAliases(Map<String, FieldMetadata> registry, FieldMetadata fieldMetadata) {
        putAlias(registry, fieldMetadata.getId(), fieldMetadata);
        putAlias(registry, fieldMetadata.getName(), fieldMetadata);
    }

    private void putAliases(Map<String, FieldMetadata> registry, FieldMetadata fieldMetadata, JsonNode source) {
        putAliases(registry, fieldMetadata);
        putAlias(registry, text(source, "key", null), fieldMetadata);
        putAlias(registry, text(source, "fieldId", null), fieldMetadata);

        JsonNode clauseNames = source == null || !source.isObject() ? null : source.get("clauseNames");
        if (clauseNames != null && clauseNames.isArray()) {
            for (int index = 0; index < clauseNames.size(); index++) {
                JsonNode clauseName = clauseNames.get(index);
                if (clauseName != null && !clauseName.isNull()) {
                    putAlias(registry, clauseName.asString(), fieldMetadata);
                }
            }
        }
    }

    private void putAlias(Map<String, FieldMetadata> registry, String key, FieldMetadata fieldMetadata) {
        if (key != null && !key.isBlank()) {
            registry.put(normalize(key), fieldMetadata);
        }
    }

    private FieldMetadata findMetadata(
            String path,
            String key,
            String label,
            Map<String, FieldMetadata> registry
    ) {
        for (String candidate : candidates(
                key,
                withoutArrayIndexes(parentPathPart(path)),
                withoutArrayIndexes(lastPathPart(path)),
                parentPathPart(path),
                lastPathPart(path),
                label
        )) {
            FieldMetadata fieldMetadata = registry.get(normalize(candidate));
            if (fieldMetadata != null) {
                return fieldMetadata;
            }
        }

        FieldMetadata fallback = new FieldMetadata();
        fallback.setProvided(false);
        return fallback;
    }

    private List<String> candidates(String... values) {
        List<String> result = new ArrayList<>();
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                result.add(value);
            }
        }
        return result;
    }

    private String lastPathPart(String path) {
        if (path == null || path.isBlank()) {
            return path;
        }

        int dotIndex = path.lastIndexOf('.');
        if (dotIndex == -1) {
            return path;
        }
        return path.substring(dotIndex + 1);
    }

    private String parentPathPart(String path) {
        if (path == null || path.isBlank()) {
            return path;
        }

        int dotIndex = path.lastIndexOf('.');
        if (dotIndex == -1) {
            return path;
        }

        String parentPath = path.substring(0, dotIndex);
        return lastPathPart(parentPath);
    }

    private String withoutArrayIndexes(String pathPart) {
        if (pathPart == null || pathPart.isBlank()) {
            return pathPart;
        }

        return pathPart.replaceAll("\\[\\d+\\]", "");
    }

    private void updateStatistics(FieldMetadata fieldMetadata, ContextStatistics statistics) {
        if (fieldMetadata.isProvided()) {
            statistics.setMetadataMatchedCount(statistics.getMetadataMatchedCount() + 1);
        } else {
            statistics.setMetadataMissingCount(statistics.getMetadataMissingCount() + 1);
        }
    }

    private String text(JsonNode node, String fieldName, String fallback) {
        if (node == null || !node.isObject()) {
            return fallback;
        }

        JsonNode value = node.get(fieldName);
        if (value == null || value.isNull()) {
            return fallback;
        }

        if (value.isObject() || value.isArray()) {
            return fallback;
        }

        return value.asString();
    }

    private Boolean booleanValue(JsonNode node, String fieldName) {
        if (node == null || !node.isObject()) {
            return null;
        }

        JsonNode value = node.get(fieldName);
        if (value == null || value.isNull()) {
            return null;
        }

        return value.asBoolean();
    }

    private List<AllowedValue> allowedValues(JsonNode node) {
        if (node == null || !node.isObject()) {
            return List.of();
        }

        JsonNode value = node.get("allowedValues");
        if (value == null || !value.isArray()) {
            return List.of();
        }

        List<AllowedValue> result = new ArrayList<>();
        for (int index = 0; index < value.size(); index++) {
            JsonNode item = value.get(index);
            if (item != null && !item.isNull()) {
                if (item.isObject()) {
                    result.add(allowedValue(item));
                } else {
                    AllowedValue allowedValue = new AllowedValue();
                    allowedValue.setValue(item.asString());
                    result.add(allowedValue);
                }
            }
        }
        return result;
    }

    private AllowedValue allowedValue(JsonNode item) {
        AllowedValue allowedValue = new AllowedValue();
        allowedValue.setId(text(item, "id", null));
        allowedValue.setValue(optionLabel(item));
        allowedValue.setDescription(text(item, "description", null));
        return allowedValue;
    }

    private String optionLabel(JsonNode item) {
        String value = text(item, "value", null);
        if (value != null) {
            return value;
        }

        String name = text(item, "name", null);
        if (name != null) {
            return name;
        }

        String id = text(item, "id", null);
        return id == null ? "" : id;
    }

    private boolean looksLikeFieldMap(JsonNode metadata) {
        for (Map.Entry<String, JsonNode> entry : metadata.properties()) {
            JsonNode value = entry.getValue();
            if (value != null && value.isObject() && looksLikeSingleFieldMetadata(value)) {
                return true;
            }
        }
        return false;
    }

    private boolean looksLikeSingleFieldMetadata(JsonNode metadata) {
        return metadata.has("id")
                || metadata.has("key")
                || metadata.has("fieldId")
                || metadata.has("schema")
                || metadata.has("name");
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim().toLowerCase();
    }
}
