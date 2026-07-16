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
        assertTrue(template.contains("{{OUTPUT_REQUIREMENTS}}"));
        assertTrue(template.contains("untrusted data"));
        assertTrue(template.contains("DO-178C / ED-12C"));
        assertTrue(template.contains("not certification evidence"));
        assertTrue(template.contains("the supplied record may be complete and internally consistent"));
        assertTrue(template.contains("zero findings and zero observations is a valid result"));
        assertTrue(template.contains("Prefer a supported clean result"));
        assertTrue(template.contains("EMPTY_ARRAY"));
        assertTrue(template.contains("comment coverage"));
        assertTrue(template.contains("Evaluate tension between a comment and a field in this order"));
        assertTrue(template.contains("A populated field satisfies"));
        assertTrue(template.contains("Insufficient Context"));
        assertTrue(template.contains("apply this sequence"));
        assertTrue(template.contains("report the timing tension as an `Observation`"));
        assertTrue(template.contains("criterion satisfied; no finding for this criterion"));
        assertTrue(template.contains("produce a finding-free report without adding a compensating observation"));
        assertTrue(template.contains("already evaluated"));
        assertTrue(template.contains("Source: payload"));
        assertTrue(template.contains("Do not introduce a new artifact type"));
        assertTrue(template.contains("status value"));
        assertTrue(template.contains("Observations express uncertainty or risk"));
        assertFalse(template.contains("no-finding sentence"));
        assertFalse(template.contains("Gozlemler ve Yetersiz Baglam"));
        assertTrue(template.contains("BEGIN_AUDIT_CONTEXT"));
        assertTrue(template.contains("END_AUDIT_CONTEXT"));
    }

    @Test
    void loadsJsonOutputRequirementsFromClasspath() {
        String requirements = new PromptTemplateLoader("prompts/output_json.md").load();

        assertTrue(requirements.contains("valid JSON object"));
        assertTrue(requirements.contains("`findings` and `observations` are present as arrays"));
        assertTrue(requirements.contains("Write `summary`"));
        assertTrue(requirements.contains("Final Self-Check"));
        assertTrue(requirements.contains("without a severity field"));
        assertTrue(requirements.contains("self-corrections"));
    }

    @Test
    void buildsMarkdownReportPromptForLibraryUse() {
        PromptBuilder builder = new PromptBuilder(
                new PromptTemplateLoader("prompts/core_auditor.md"),
                new AgentContextRenderer(),
                new PromptTemplateLoader("prompts/output_markdown.md")
        );

        String prompt = builder.build(context());

        assertTrue(prompt.contains("## Özet"));
        assertTrue(prompt.contains("## Bulgular"));
        assertTrue(prompt.contains("## Gözlemler ve Yetersiz Bağlam"));
        assertTrue(prompt.contains("## Önerilen Aksiyonlar"));
        assertTrue(prompt.contains("Do not add an observation merely to avoid an empty section"));
        assertFalse(prompt.contains("valid JSON object"));
        assertFalse(prompt.contains("{{OUTPUT_REQUIREMENTS}}"));
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
        assertFalse(prompt.contains("{{OUTPUT_REQUIREMENTS}}"));
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
