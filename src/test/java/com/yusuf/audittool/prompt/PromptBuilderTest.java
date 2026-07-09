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
        assertTrue(template.contains("audit"));
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
