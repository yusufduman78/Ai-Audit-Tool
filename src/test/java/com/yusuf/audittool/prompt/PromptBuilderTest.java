package com.yusuf.audittool.prompt;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;

import com.yusuf.audittool.model.AgentContext;
import com.yusuf.audittool.model.NormalizedField;

class PromptBuilderTest {

    @Test
    void replacesContextPlaceholderWithRenderedContext() {
        PromptTemplateLoader loader = new PromptTemplateLoader("unused") {
            @Override
            public String load() {
                return "Audit this:\n{{CONTEXT}}";
            }
        };
        PromptBuilder builder = new PromptBuilder(loader, new AgentContextRenderer());

        String prompt = builder.build(context());

        assertTrue(prompt.startsWith("Audit this:"));
        assertTrue(prompt.contains("ACTIVE FIELDS"));
        assertTrue(prompt.contains("Summary"));
        assertFalse(prompt.contains("{{CONTEXT}}"));
        assertFalse(prompt.contains("NULL FIELDS"));
    }

    @Test
    void loadsDefaultPromptTemplateFromClasspath() {
        PromptTemplateLoader loader = new PromptTemplateLoader("prompts/core_auditor.md");

        String template = loader.load();

        assertTrue(template.contains("{{CONTEXT}}"));
        assertTrue(template.contains("untrusted data"));
        assertTrue(template.contains("EMPTY_ARRAY"));
        assertTrue(template.contains("comment coverage"));
        assertTrue(template.contains("Insufficient Context"));
        assertTrue(template.contains("apply this sequence"));
        assertTrue(template.contains("already evaluated"));
        assertTrue(template.contains("self-corrections"));
        assertTrue(template.contains("professional Turkish"));
        assertTrue(template.contains("BEGIN_AUDIT_CONTEXT"));
        assertTrue(template.contains("END_AUDIT_CONTEXT"));
    }

    @Test
    void placesRenderedContextInsideUntrustedDataBoundaries() {
        PromptTemplateLoader loader = new PromptTemplateLoader("prompts/core_auditor.md");
        PromptBuilder builder = new PromptBuilder(loader, new AgentContextRenderer());

        String prompt = builder.build(context());
        int contextStart = prompt.lastIndexOf("\nBEGIN_AUDIT_CONTEXT\n");
        int fieldValue = prompt.indexOf("Login requirement");
        int contextEnd = prompt.lastIndexOf("\nEND_AUDIT_CONTEXT");

        assertTrue(contextStart < fieldValue);
        assertTrue(fieldValue < contextEnd);
        assertFalse(prompt.contains("{{CONTEXT}}"));
        assertFalse(prompt.contains("Metadata: not provided"));
        assertFalse(prompt.contains("STATISTICS"));
        assertTrue(prompt.contains("COMMENTS"));
    }

    private AgentContext context() {
        NormalizedField field = new NormalizedField();
        field.setPath("fields.summary");
        field.setKey("summary");
        field.setLabel("Summary");
        field.setValue("Login requirement");
        field.setValueType("string");

        AgentContext context = new AgentContext();
        context.setActiveFields(List.of(field));
        context.setEmptyFields(List.of());
        return context;
    }
}
