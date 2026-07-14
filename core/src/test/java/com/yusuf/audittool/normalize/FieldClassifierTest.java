package com.yusuf.audittool.normalize;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;

import org.junit.jupiter.api.Test;

import com.yusuf.audittool.model.NormalizedField;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

class FieldClassifierTest {

    private final GenericJsonWalker walker = new GenericJsonWalker();
    private final FieldClassifier classifier = new FieldClassifier();
    private final JsonMapper jsonMapper = new JsonMapper();

    @Test
    void separatesActiveEmptyAndNullFields() throws Exception {
        JsonNode payload = jsonMapper.readTree("""
                {
                  "summary": "Login requirement",
                  "acceptanceCriteria": "",
                  "testEvidence": [],
                  "details": {},
                  "optionalDate": null
                }
                """);

        FieldClassification classification = classifier.classify(walker.walk(payload));

        assertEquals(List.of("summary"), activePaths(classification));
        assertEquals(List.of("acceptanceCriteria", "testEvidence", "details"), emptyPaths(classification));
        assertEquals(1, classification.getStatistics().getActiveFieldCount());
        assertEquals(3, classification.getStatistics().getEmptyFieldCount());
        assertEquals(1, classification.getStatistics().getNullFieldCount());
    }

    @Test
    void collapsesSimpleObjectsUsingReadableValue() throws Exception {
        JsonNode payload = jsonMapper.readTree("""
                {
                  "status": {
                    "self": "https://jira.example/status/10000",
                    "id": "10000",
                    "name": "Done"
                  },
                  "resolution": {
                    "description": "Issue has been completed",
                    "name": "Resolved"
                  },
                  "assignee": {
                    "displayName": "Ali Yilmaz"
                  }
                }
                """);

        FieldClassification classification = classifier.classify(walker.walk(payload));

        assertEquals(List.of("status", "resolution", "resolution.description", "assignee"),
                activePaths(classification));
        assertEquals("Done", activeByPath(classification, "status").getValue());
        assertEquals("Resolved", activeByPath(classification, "resolution").getValue());
        assertEquals("Issue has been completed", activeByPath(classification, "resolution.description").getValue());
        assertEquals("Ali Yilmaz", activeByPath(classification, "assignee").getValue());
        assertEquals(2, classification.getStatistics().getSkippedNoiseFieldCount());
    }

    @Test
    void keepsIdentifiersWhenObjectHasNoReadableSummaryValue() throws Exception {
        JsonNode payload = jsonMapper.readTree("""
                {
                  "reference": {
                    "id": "DOC-42",
                    "revision": "C"
                  }
                }
                """);

        FieldClassification classification = classifier.classify(walker.walk(payload));

        assertEquals(List.of("reference.id", "reference.revision"), activePaths(classification));
        assertEquals("DOC-42", activeByPath(classification, "reference.id").getValue());
    }

    @Test
    void compactsChangeDataWithoutIncludingLinksOrNestedTechnicalValues() throws Exception {
        JsonNode payload = jsonMapper.readTree("""
                {
                  "change": {
                    "field": "status",
                    "fromString": "Open",
                    "to": ["10001"],
                    "toString": "Done",
                    "self": "https://example.test/change/42"
                  }
                }
                """);

        FieldClassification classification = classifier.classify(walker.walk(payload));

        assertEquals(List.of("change"), activePaths(classification));
        assertEquals("field=status | fromString=Open | toString=Done",
                activeByPath(classification, "change").getValue());
        assertEquals("change.compact", activeByPath(classification, "change").getValueType());
        assertEquals(2, classification.getStatistics().getSkippedNoiseFieldCount());
    }

    @Test
    void skipsStructuralNoiseAndUrlValues() throws Exception {
        JsonNode payload = jsonMapper.readTree("""
                {
                  "self": "https://jira.example/issue/REQ-101",
                  "fields": {
                    "summary": "Login requirement",
                    "avatarUrls": {
                      "small": "https://jira.example/avatar-small.png"
                    },
                    "iconUrl": "https://jira.example/icon.png",
                    "schema": {
                      "type": "string"
                    }
                  }
                }
                """);

        FieldClassification classification = classifier.classify(walker.walk(payload));

        assertEquals(List.of("fields.summary"), activePaths(classification));
        assertEquals(4, classification.getStatistics().getSkippedNoiseFieldCount());
    }

    @Test
    void keepsPrimitiveArrayItemsAsActiveFields() throws Exception {
        JsonNode payload = jsonMapper.readTree("""
                {
                  "labels": [
                    "Requirement",
                    "Audit"
                  ]
                }
                """);

        FieldClassification classification = classifier.classify(walker.walk(payload));

        assertEquals(List.of("labels[0]", "labels[1]"), activePaths(classification));
        assertEquals("Requirement", activeByPath(classification, "labels[0]").getValue());
        assertEquals("Audit", activeByPath(classification, "labels[1]").getValue());
    }

    private List<String> activePaths(FieldClassification classification) {
        return classification.getActiveFields().stream()
                .map(field -> field.getPath())
                .toList();
    }

    private List<String> emptyPaths(FieldClassification classification) {
        return classification.getEmptyFields().stream()
                .map(field -> field.getPath())
                .toList();
    }

    private NormalizedField activeByPath(FieldClassification classification, String path) {
        return classification.getActiveFields().stream()
                .filter(field -> path.equals(field.getPath()))
                .findFirst()
                .orElseThrow();
    }
}
