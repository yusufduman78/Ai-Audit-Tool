package com.yusuf.audittool.normalize;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;

import org.junit.jupiter.api.Test;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

class GenericJsonWalkerTest {

    private final GenericJsonWalker walker = new GenericJsonWalker();
    private final JsonMapper jsonMapper = new JsonMapper();

    @Test
    void walksObjectFieldsWithPathsAndDepth() throws Exception {
        JsonNode payload = jsonMapper.readTree("""
                {
                  "key": "REQ-101",
                  "fields": {
                    "summary": "Login requirement",
                    "status": {
                      "name": "Done"
                    }
                  }
                }
                """);

        List<RawField> fields = walker.walk(payload);

        assertEquals(List.of("key", "fields", "fields.summary", "fields.status", "fields.status.name"),
                fields.stream().map(RawField::getPath).toList());

        RawField status = fieldByPath(fields, "fields.status");
        assertEquals("fields", status.getParentPath());
        assertEquals("status", status.getKey());
        assertEquals(1, status.getDepth());
        assertEquals("object.simple", status.getDetectedType());
    }

    @Test
    void walksArrayItemsWithReadableIndexes() throws Exception {
        JsonNode payload = jsonMapper.readTree("""
                {
                  "items": [
                    {
                      "name": "first"
                    },
                    {
                      "name": "second"
                    }
                  ]
                }
                """);

        List<RawField> fields = walker.walk(payload);

        assertEquals(List.of("items", "items[0]", "items[0].name", "items[1]", "items[1].name"),
                fields.stream().map(RawField::getPath).toList());

        RawField secondItemName = fieldByPath(fields, "items[1].name");
        assertEquals("items[1]", secondItemName.getParentPath());
        assertEquals("name", secondItemName.getKey());
        assertEquals(2, secondItemName.getDepth());
        assertEquals("string", secondItemName.getDetectedType());
    }

    @Test
    void detectsEmptyAndNullFields() throws Exception {
        JsonNode payload = jsonMapper.readTree("""
                {
                  "emptyText": "",
                  "emptyArray": [],
                  "emptyObject": {},
                  "missingValue": null
                }
                """);

        List<RawField> fields = walker.walk(payload);

        assertEquals("string", fieldByPath(fields, "emptyText").getDetectedType());
        assertEquals("array.empty", fieldByPath(fields, "emptyArray").getDetectedType());
        assertEquals("object.empty", fieldByPath(fields, "emptyObject").getDetectedType());
        assertEquals("null", fieldByPath(fields, "missingValue").getDetectedType());
    }

    private RawField fieldByPath(List<RawField> fields, String path) {
        return fields.stream()
                .filter(field -> path.equals(field.getPath()))
                .findFirst()
                .orElseThrow();
    }
}
