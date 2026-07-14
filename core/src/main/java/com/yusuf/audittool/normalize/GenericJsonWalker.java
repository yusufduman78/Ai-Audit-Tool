package com.yusuf.audittool.normalize;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Component;

import tools.jackson.databind.JsonNode;

@Component
public class GenericJsonWalker {

    public List<RawField> walk(JsonNode payload) {
        List<RawField> fields = new ArrayList<>();

        if (payload == null || payload.isNull()) {
            return fields;
        }

        if (payload.isObject()) {
            walkObject(payload, "", -1, fields);
        } else if (payload.isArray()) {
            walkArray(payload, "", -1, fields);
        }

        return fields;
    }

    private void walkObject(JsonNode node, String parentPath, int parentDepth, List<RawField> fields) {
        for (Map.Entry<String, JsonNode> property : node.properties()) {
            String key = property.getKey();
            JsonNode value = property.getValue();
            String path = appendProperty(parentPath, key);
            int depth = parentDepth + 1;

            fields.add(rawField(path, parentPath, key, value, depth));

            if (value.isObject() && !value.isEmpty()) {
                walkObject(value, path, depth, fields);
            } else if (value.isArray() && !value.isEmpty()) {
                walkArray(value, path, depth, fields);
            }
        }
    }

    private void walkArray(JsonNode node, String parentPath, int parentDepth, List<RawField> fields) {
        for (int index = 0; index < node.size(); index++) {
            JsonNode value = node.get(index);
            String key = "[" + index + "]";
            String path = appendIndex(parentPath, index);
            int depth = parentDepth + 1;

            fields.add(rawField(path, parentPath, key, value, depth));

            if (value.isObject() && !value.isEmpty()) {
                walkObject(value, path, depth, fields);
            } else if (value.isArray() && !value.isEmpty()) {
                walkArray(value, path, depth, fields);
            }
        }
    }

    private RawField rawField(String path, String parentPath, String key, JsonNode value, int depth) {
        RawField field = new RawField();
        field.setPath(path);
        field.setParentPath(parentPath);
        field.setKey(key);
        field.setValue(value);
        field.setDetectedType(detectType(value));
        field.setDepth(depth);
        return field;
    }

    private String appendProperty(String parentPath, String key) {
        if (parentPath == null || parentPath.isBlank()) {
            return key;
        }
        return parentPath + "." + key;
    }

    private String appendIndex(String parentPath, int index) {
        if (parentPath == null || parentPath.isBlank()) {
            return "[" + index + "]";
        }
        return parentPath + "[" + index + "]";
    }

    private String detectType(JsonNode value) {
        if (value == null || value.isNull()) {
            return "null";
        }
        if (value.isString()) {
            return "string";
        }
        if (value.isNumber()) {
            return "number";
        }
        if (value.isBoolean()) {
            return "boolean";
        }
        if (value.isArray()) {
            return value.isEmpty() ? "array.empty" : "array";
        }
        if (value.isObject()) {
            return detectObjectType(value);
        }
        return "unknown";
    }

    private String detectObjectType(JsonNode value) {
        if (value.isEmpty()) {
            return "object.empty";
        }

        boolean hasNestedContainer = value.valueStream()
                .anyMatch(child -> child.isObject() || child.isArray());

        return hasNestedContainer ? "object" : "object.simple";
    }
}
