package com.yusuf.audittool.api;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import com.yusuf.audittool.model.AgentContext;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

class AuditContextPreparerTest {

    private final JsonMapper jsonMapper = new JsonMapper();
    private final AuditContextPreparer contextPreparer = new AuditContextPreparer();

    @Test
    void preparesNormalizedTextWithoutCallingAnAgent() throws Exception {
        JsonNode issue = jsonMapper.readTree("""
                {
                  "key": "REQ-501",
                  "fields": {
                    "summary": "Navigation integrity requirement",
                    "customfield_22001": "",
                    "comment": {
                      "total": 1,
                      "comments": [
                        {
                          "author": { "displayName": "Reviewer A" },
                          "body": "Impact analysis is being reviewed."
                        }
                      ]
                    }
                  }
                }
                """);
        JsonNode metadata = jsonMapper.readTree("""
                {
                  "values": [
                    {
                      "fieldId": "customfield_22001",
                      "name": "Impact Analysis",
                      "description": "Change impact assessment"
                    }
                  ]
                }
                """);
        JsonNode checklist = jsonMapper.readTree("""
                ["Approved changes must include an impact analysis."]
                """);

        String preparedContext = contextPreparer.prepare(
                new AuditInput(issue, metadata, null, checklist)
        );

        assertTrue(preparedContext.contains("ID: REQ-501"));
        assertTrue(preparedContext.contains("Impact Analysis"));
        assertTrue(preparedContext.contains("Empty Type: EMPTY_STRING"));
        assertTrue(preparedContext.contains("Author: Reviewer A"));
        assertTrue(preparedContext.contains("Approved changes must include an impact analysis."));
    }

    @Test
    void exposesStructuredContextForLibraryConsumers() throws Exception {
        JsonNode issue = jsonMapper.readTree("""
                {
                  "key": "REQ-502",
                  "fields": { "summary": "Display requirement" }
                }
                """);

        AgentContext context = contextPreparer.normalize(new AuditInput(issue));

        assertEquals("REQ-502", context.getSourceInfo().getEntityId());
        assertEquals(2, context.getActiveFields().size());
    }

    @Test
    void rejectsMissingIssuePayload() {
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> contextPreparer.prepare(new AuditInput(null))
        );

        assertEquals("Payload is required.", exception.getMessage());
    }
}
