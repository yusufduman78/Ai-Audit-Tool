package com.yusuf.audittool.prompt;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;

import com.yusuf.audittool.model.AgentContext;
import com.yusuf.audittool.model.AllowedValue;
import com.yusuf.audittool.model.ChecklistContext;
import com.yusuf.audittool.model.ChecklistItem;
import com.yusuf.audittool.model.EmptyField;
import com.yusuf.audittool.model.FieldMetadata;
import com.yusuf.audittool.model.NormalizedField;
import com.yusuf.audittool.model.SourceInfo;

class AgentContextRendererTest {

    private final AgentContextRenderer renderer = new AgentContextRenderer();

    @Test
    void rendersFieldsWithMatchedMetadataBesideValues() {
        AgentContext context = new AgentContext();
        context.setSourceInfo(sourceInfo());
        context.setActiveFields(List.of(activeField()));
        context.setEmptyFields(List.of(emptyField()));
        context.setChecklistContext(checklist());

        String rendered = renderer.render(context);

        assertTrue(rendered.contains("ENTITY"));
        assertTrue(rendered.contains("ID: REQ-101"));
        assertTrue(rendered.contains("- Acceptance Criteria"));
        assertTrue(rendered.contains("Value: User must be redirected after login."));
        assertTrue(rendered.contains("Metadata ID: customfield_13104"));
        assertTrue(rendered.contains("Description: Requirement kabul kriterlerini belirtir."));
        assertTrue(rendered.contains("Allowed Values:"));
        assertTrue(rendered.contains("- High"));
        assertTrue(rendered.contains("Description: Yüksek riskli değişiklik."));
        assertTrue(rendered.contains("- Test Evidence"));
        assertTrue(rendered.contains("Empty Type: array.empty"));
        assertTrue(rendered.contains("Done için test kanıtı bulunmalıdır."));
        assertFalse(rendered.contains("NULL FIELDS"));
        assertFalse(rendered.contains("STATISTICS"));
    }

    @Test
    void rendersMissingOptionalSectionsWithoutFailing() {
        AgentContext context = new AgentContext();
        context.setActiveFields(List.of());
        context.setEmptyFields(List.of());

        String rendered = renderer.render(context);

        assertTrue(rendered.contains("ID: unknown"));
        assertTrue(rendered.contains("ACTIVE FIELDS\n- None"));
        assertTrue(rendered.contains("EMPTY FIELDS\n- None"));
        assertTrue(rendered.contains("CHECKLIST\n- Not provided"));
    }

    private SourceInfo sourceInfo() {
        SourceInfo sourceInfo = new SourceInfo();
        sourceInfo.setEntityId("REQ-101");
        sourceInfo.setEntityLabel("Login requirement");
        return sourceInfo;
    }

    private NormalizedField activeField() {
        NormalizedField field = new NormalizedField();
        field.setPath("fields.customfield_13104");
        field.setKey("customfield_13104");
        field.setLabel("customfield_13104");
        field.setValue("User must be redirected after login.");
        field.setValueType("string");
        field.setMetadata(acceptanceCriteriaMetadata());
        return field;
    }

    private EmptyField emptyField() {
        EmptyField field = new EmptyField();
        field.setPath("fields.customfield_14500");
        field.setKey("customfield_14500");
        field.setLabel("customfield_14500");
        field.setEmptyType("array.empty");

        FieldMetadata metadata = new FieldMetadata();
        metadata.setProvided(true);
        metadata.setId("customfield_14500");
        metadata.setName("Test Evidence");
        metadata.setDescriptionTr("Test veya doğrulama kanıtlarını içerir.");
        field.setMetadata(metadata);
        return field;
    }

    private FieldMetadata acceptanceCriteriaMetadata() {
        FieldMetadata metadata = new FieldMetadata();
        metadata.setProvided(true);
        metadata.setId("customfield_13104");
        metadata.setName("Acceptance Criteria");
        metadata.setSchemaType("string");
        metadata.setRequired(false);
        metadata.setDescriptionTr("Requirement kabul kriterlerini belirtir.");

        AllowedValue allowedValue = new AllowedValue();
        allowedValue.setId("30003");
        allowedValue.setValue("High");
        allowedValue.setDescription("Yüksek riskli değişiklik.");
        metadata.setAllowedValues(List.of(allowedValue));
        return metadata;
    }

    private ChecklistContext checklist() {
        ChecklistItem item = new ChecklistItem();
        item.setText("Done için test kanıtı bulunmalıdır.");

        ChecklistContext checklist = new ChecklistContext();
        checklist.setProvided(true);
        checklist.setItems(List.of(item));
        return checklist;
    }

}
