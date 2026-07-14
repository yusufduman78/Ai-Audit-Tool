package com.yusuf.audittool.normalize;

import org.springframework.stereotype.Component;

import com.yusuf.audittool.model.SourceInfo;

import tools.jackson.databind.JsonNode;

@Component
public class SourceInfoExtractor {

    public SourceInfo extract(JsonNode payload) {
        SourceInfo sourceInfo = new SourceInfo();

        if (payload == null || payload.isNull()) {
            return sourceInfo;
        }

        sourceInfo.setEntityId(firstText(payload, "key", "id"));
        sourceInfo.setEntityLabel(firstText(payload, "fields.summary", "summary", "name", "title"));

        return sourceInfo;
    }

    private String firstText(JsonNode payload, String... paths) {
        for (String path : paths) {
            JsonNode value = valueAt(payload, path);
            if (value != null && value.isString() && !value.asString().isBlank()) {
                return value.asString();
            }
        }

        return null;
    }

    private JsonNode valueAt(JsonNode payload, String path) {
        JsonNode current = payload;

        for (String part : path.split("\\.")) {
            if (current == null || current.isNull()) {
                return null;
            }
            current = current.get(part);
        }

        return current;
    }
}

