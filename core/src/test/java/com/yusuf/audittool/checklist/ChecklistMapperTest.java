package com.yusuf.audittool.checklist;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import com.yusuf.audittool.model.ChecklistContext;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

class ChecklistMapperTest {

    private final ChecklistMapper mapper = new ChecklistMapper();
    private final JsonMapper jsonMapper = new JsonMapper();

    @Test
    void mapsStringArrayChecklist() throws Exception {
        JsonNode checklist = jsonMapper.readTree("""
                [
                  "Requirement açık olmalıdır.",
                  "Done için test kanıtı bulunmalıdır."
                ]
                """);

        ChecklistContext context = mapper.map(checklist);

        assertTrue(context.isProvided());
        assertEquals("Requirement açık olmalıdır.\nDone için test kanıtı bulunmalıdır.", context.getRawText());
        assertEquals(2, context.getItems().size());
        assertEquals("1", context.getItems().get(0).getId());
        assertEquals("Requirement açık olmalıdır.", context.getItems().get(0).getText());
    }

    @Test
    void mapsObjectArrayChecklist() throws Exception {
        JsonNode checklist = jsonMapper.readTree("""
                [
                  {
                    "id": "AC-1",
                    "text": "Acceptance criteria boş olmamalıdır."
                  },
                  {
                    "id": "QA-1",
                    "description": "Test kanıtı bulunmalıdır."
                  }
                ]
                """);

        ChecklistContext context = mapper.map(checklist);

        assertTrue(context.isProvided());
        assertEquals(2, context.getItems().size());
        assertEquals("AC-1", context.getItems().get(0).getId());
        assertEquals("Acceptance criteria boş olmamalıdır.", context.getItems().get(0).getText());
        assertEquals("QA-1", context.getItems().get(1).getId());
        assertEquals("Test kanıtı bulunmalıdır.", context.getItems().get(1).getText());
    }

    @Test
    void mapsMultiLineTextChecklist() throws Exception {
        JsonNode checklist = jsonMapper.readTree("""
                "- Requirement açık olmalıdır.\\n2. Test kanıtı bulunmalıdır."
                """);

        ChecklistContext context = mapper.map(checklist);

        assertTrue(context.isProvided());
        assertEquals(2, context.getItems().size());
        assertEquals("Requirement açık olmalıdır.", context.getItems().get(0).getText());
        assertEquals("Test kanıtı bulunmalıdır.", context.getItems().get(1).getText());
    }

    @Test
    void returnsEmptyContextWhenChecklistIsMissing() {
        ChecklistContext context = mapper.map(null);

        assertFalse(context.isProvided());
        assertEquals(0, context.getItems().size());
    }
}

