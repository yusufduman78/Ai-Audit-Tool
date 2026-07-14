package com.yusuf.audittool.normalize;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;

import com.yusuf.audittool.api.AuditInput;
import com.yusuf.audittool.checklist.ChecklistMapper;
import com.yusuf.audittool.metadata.MetadataMapper;
import com.yusuf.audittool.model.AgentContext;
import com.yusuf.audittool.model.EmptyField;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

class NormalizeServiceTest {

    private final JsonMapper jsonMapper = new JsonMapper();
    private final NormalizeService normalizeService = new NormalizeService(
            new GenericJsonWalker(),
            new FieldClassifier(),
            new CommentExtractor(),
            new SourceInfoExtractor(),
            new ChecklistMapper(),
            new MetadataMapper()
    );

    @Test
    void normalizesPayloadIntoAgentContext() throws Exception {
        AuditInput request = request("""
                {
                  "key": "REQ-101",
                  "fields": {
                    "summary": "Login requirement",
                    "status": {
                      "self": "https://jira.example/status/10000",
                      "name": "Done"
                    },
                    "customfield_13104": "",
                    "customfield_14500": [],
                    "customfield_15000": null
                  }
                }
                """);

        AgentContext context = normalizeService.normalize(request);

        assertEquals(List.of("key", "fields.summary", "fields.status"), activePaths(context));
        assertEquals("REQ-101", context.getSourceInfo().getEntityId());
        assertEquals("Login requirement", context.getSourceInfo().getEntityLabel());
        assertEquals(List.of("fields.customfield_13104", "fields.customfield_14500"), emptyPaths(context));
        assertEquals(3, context.getStatistics().getActiveFieldCount());
        assertEquals(2, context.getStatistics().getEmptyFieldCount());
        assertEquals(1, context.getStatistics().getNullFieldCount());
        assertEquals(1, context.getStatistics().getSkippedNoiseFieldCount());
        assertEquals(0, context.getStatistics().getMetadataMatchedCount());
        assertEquals(5, context.getStatistics().getMetadataMissingCount());
        assertFalse(context.getCommentContext().isProvided());
        assertFalse(context.getChecklistContext().isProvided());
    }

    @Test
    void enrichesContextWithMetadataAndDescriptions() throws Exception {
        JsonNode payload = payload("""
                {
                  "key": "REQ-101",
                  "fields": {
                    "summary": "Login requirement",
                    "customfield_13104": ""
                  }
                }
                """);
        JsonNode metadata = jsonMapper.readTree("""
                {
                  "customfield_13104": {
                    "name": "Acceptance Criteria",
                    "schemaType": "string"
                  }
                }
                """);
        JsonNode descriptions = jsonMapper.readTree("""
                {
                  "customfield_13104": "Requirement kabul kriterlerini belirtir."
                }
                """);
        AuditInput request = new AuditInput(payload, metadata, descriptions, null);

        AgentContext context = normalizeService.normalize(request);

        EmptyField emptyField = context.getEmptyFields().get(0);
        assertEquals("Acceptance Criteria", emptyField.getMetadata().getName());
        assertEquals("Requirement kabul kriterlerini belirtir.", emptyField.getMetadata().getDescriptionTr());
        assertEquals(1, context.getStatistics().getMetadataMatchedCount());
    }

    @Test
    void includesChecklistContextWhenProvided() throws Exception {
        JsonNode payload = payload("""
                {
                  "key": "REQ-101",
                  "fields": {
                    "summary": "Login requirement"
                  }
                }
                """);
        JsonNode checklist = jsonMapper.readTree("""
                [
                  "Requirement açık olmalıdır.",
                  "Done için test kanıtı bulunmalıdır."
                ]
                """);
        AuditInput request = new AuditInput(payload, null, null, checklist);

        AgentContext context = normalizeService.normalize(request);

        assertEquals(true, context.getChecklistContext().isProvided());
        assertEquals(2, context.getChecklistContext().getItems().size());
        assertEquals("Requirement açık olmalıdır.", context.getChecklistContext().getItems().get(0).getText());
    }

    @Test
    void extractsCommentsWithoutAddingTheirChildrenAsGenericFields() throws Exception {
        AuditInput request = request("""
                {
                  "key": "REQ-101",
                  "fields": {
                    "summary": "Login requirement",
                    "comment": {
                      "startAt": 0,
                      "total": 1,
                      "comments": [
                        {
                          "body": "Test execution is pending approval.",
                          "author": { "displayName": "Reviewer A" },
                          "created": "2026-07-10T10:30:00.000+0000"
                        }
                      ]
                    }
                  }
                }
                """);

        AgentContext context = normalizeService.normalize(request);

        assertEquals(List.of("key", "fields.summary"), activePaths(context));
        assertTrue(context.getCommentContext().isProvided());
        assertEquals("Test execution is pending approval.", context.getCommentContext().getComments().getFirst().getBody());
        assertEquals("Reviewer A", context.getCommentContext().getComments().getFirst().getAuthorName());
        assertEquals(2, context.getStatistics().getActiveFieldCount());
    }

    @Test
    void rejectsMissingPayload() {
        AuditInput request = new AuditInput(null);

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> normalizeService.normalize(request)
        );

        assertEquals("Payload is required.", exception.getMessage());
    }

    private AuditInput request(String json) throws Exception {
        return new AuditInput(payload(json));
    }

    private JsonNode payload(String json) throws Exception {
        return jsonMapper.readTree(json);
    }

    private List<String> activePaths(AgentContext context) {
        return context.getActiveFields().stream()
                .map(field -> field.getPath())
                .toList();
    }

    private List<String> emptyPaths(AgentContext context) {
        return context.getEmptyFields().stream()
                .map(field -> field.getPath())
                .toList();
    }
}
