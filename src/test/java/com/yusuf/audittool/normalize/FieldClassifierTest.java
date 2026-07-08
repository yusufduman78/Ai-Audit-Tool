package com.yusuf.audittool.normalize;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;

import org.junit.jupiter.api.Test;

import com.yusuf.audittool.model.EmptyField;
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
                    "self": "hidden",
                    "id": "10000",
                    "name": "Done"
                  },
                  "assignee": {
                    "displayName": "Ali Yilmaz"
                  }
                }
                """);

        FieldClassification classification = classifier.classify(walker.walk(payload));

        assertEquals(List.of("status", "assignee"), activePaths(classification));
        assertEquals("Done", activeByPath(classification, "status").getValue());
        assertEquals("Ali Yilmaz", activeByPath(classification, "assignee").getValue());
        assertEquals(0, classification.getStatistics().getSkippedNoiseFieldCount());
    }

    @Test
    void skipsNoiseFieldsWhenTheyAreNotPartOfCollapsedObject() throws Exception {
        JsonNode payload = jsonMapper.readTree("""
                {
                  "self": "hidden",
                  "fields": {
                    "summary": "Login requirement",
                    "avatarUrls": {
                      "small": "hidden"
                    }
                  }
                }
                """);

        FieldClassification classification = classifier.classify(walker.walk(payload));

        assertEquals(List.of("fields.summary"), activePaths(classification));
        assertEquals(2, classification.getStatistics().getSkippedNoiseFieldCount());
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
                .map(NormalizedField::getPath)
                .toList();
    }

    private List<String> emptyPaths(FieldClassification classification) {
        return classification.getEmptyFields().stream()
                .map(EmptyField::getPath)
                .toList();
    }

    private NormalizedField activeByPath(FieldClassification classification, String path) {
        return classification.getActiveFields().stream()
                .filter(field -> path.equals(field.getPath()))
                .findFirst()
                .orElseThrow();
    }
}
