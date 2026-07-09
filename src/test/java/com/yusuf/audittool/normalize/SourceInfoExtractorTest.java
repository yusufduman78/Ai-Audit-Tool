package com.yusuf.audittool.normalize;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.Test;

import com.yusuf.audittool.model.SourceInfo;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

class SourceInfoExtractorTest {

    private final SourceInfoExtractor extractor = new SourceInfoExtractor();
    private final JsonMapper jsonMapper = new JsonMapper();

    @Test
    void extractsJiraIssueKeyAndSummary() throws Exception {
        JsonNode payload = jsonMapper.readTree("""
                {
                  "key": "REQ-101",
                  "fields": {
                    "summary": "Login requirement"
                  }
                }
                """);

        SourceInfo sourceInfo = extractor.extract(payload);

        assertEquals("REQ-101", sourceInfo.getEntityId());
        assertEquals("Login requirement", sourceInfo.getEntityLabel());
    }

    @Test
    void fallsBackToGenericFields() throws Exception {
        JsonNode payload = jsonMapper.readTree("""
                {
                  "id": "42",
                  "title": "Generic review item"
                }
                """);

        SourceInfo sourceInfo = extractor.extract(payload);

        assertEquals("42", sourceInfo.getEntityId());
        assertEquals("Generic review item", sourceInfo.getEntityLabel());
    }

    @Test
    void leavesValuesEmptyWhenNoSourceInfoExists() throws Exception {
        JsonNode payload = jsonMapper.readTree("""
                {
                  "fields": {
                    "description": "No obvious identifier"
                  }
                }
                """);

        SourceInfo sourceInfo = extractor.extract(payload);

        assertNull(sourceInfo.getEntityId());
        assertNull(sourceInfo.getEntityLabel());
    }
}

