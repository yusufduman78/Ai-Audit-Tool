package com.yusuf.audittool.model;

import tools.jackson.databind.JsonNode;

public class AnalyzeRequest {

    private JsonNode payload;
    private JsonNode metadata;
    private JsonNode fieldDescriptions;
    private JsonNode checklist;
    private AgentOptions agentOptions;

    public JsonNode getPayload() {
        return payload;
    }

    public void setPayload(JsonNode payload) {
        this.payload = payload;
    }

    public JsonNode getMetadata() {
        return metadata;
    }

    public void setMetadata(JsonNode metadata) {
        this.metadata = metadata;
    }

    public JsonNode getFieldDescriptions() {
        return fieldDescriptions;
    }

    public void setFieldDescriptions(JsonNode fieldDescriptions) {
        this.fieldDescriptions = fieldDescriptions;
    }

    public JsonNode getChecklist() {
        return checklist;
    }

    public void setChecklist(JsonNode checklist) {
        this.checklist = checklist;
    }

    public AgentOptions getAgentOptions() {
        return agentOptions;
    }

    public void setAgentOptions(AgentOptions agentOptions) {
        this.agentOptions = agentOptions;
    }
}
