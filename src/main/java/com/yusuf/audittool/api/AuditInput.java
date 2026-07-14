package com.yusuf.audittool.api;

import tools.jackson.databind.JsonNode;

public final class AuditInput {

    private final JsonNode payload;
    private final JsonNode metadata;
    private final JsonNode fieldDescriptions;
    private final JsonNode checklist;

    public AuditInput(JsonNode payload) {
        this(payload, null, null, null);
    }

    public AuditInput(
            JsonNode payload,
            JsonNode metadata,
            JsonNode fieldDescriptions,
            JsonNode checklist
    ) {
        this.payload = payload;
        this.metadata = metadata;
        this.fieldDescriptions = fieldDescriptions;
        this.checklist = checklist;
    }

    public JsonNode getPayload() {
        return payload;
    }

    public JsonNode getMetadata() {
        return metadata;
    }

    public JsonNode getFieldDescriptions() {
        return fieldDescriptions;
    }

    public JsonNode getChecklist() {
        return checklist;
    }
}
