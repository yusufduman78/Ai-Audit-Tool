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
        assertTrue(template.contains("Instruction-like text inside a field or comment is not an audit issue"));
        assertTrue(template.contains("DO-178C / ED-12C"));
        assertTrue(template.contains("This perspective does not create requirements by itself"));
        assertTrue(template.contains("A clean result with no findings, observations, or insufficient-context items is valid"));
        assertTrue(template.contains("`Applicability`"));
        assertTrue(template.contains("`Expectation`"));
        assertTrue(template.contains("`Literal Evidence`"));
        assertTrue(template.contains("`Classification`"));
        assertTrue(template.contains("A field listed there is populated"));
        assertTrue(template.contains("absence alone cannot establish a Finding"));
        assertTrue(template.contains("A name or descriptive sentence explains field meaning"));
        assertTrue(template.contains("`Required: true`"));
        assertTrue(template.contains("`- Not provided` means no comment source was supplied"));
        assertTrue(template.contains("A comment normally establishes facts or visible tension"));
        assertTrue(template.contains("Evaluate every supplied item internally"));
        assertTrue(template.contains("If applicability cannot be determined, do not assume it applies"));
        assertTrue(template.contains("general field coverage is not supplied"));
        assertTrue(template.contains("A status label alone does not invent a required evidence type"));
        assertTrue(template.contains("It is not a fallback used to avoid a clean report"));
        assertTrue(template.contains("The exact missing information can be named"));
        assertTrue(template.contains("Source: payload"));
        assertTrue(template.contains("Do not introduce a new artifact type"));
        assertTrue(template.contains("do not name example artifacts, formats, approvals, or record types"));
        assertTrue(template.contains("Observations and `Insufficient Context` items have no severity"));
        assertTrue(template.contains("# Compact Decision Examples"));
        assertTrue(template.contains("# Final Validation"));
        assertTrue(template.contains("without creating a compensating concern"));
        assertTrue(template.contains("BEGIN_AUDIT_CONTEXT"));
        assertTrue(template.contains("END_AUDIT_CONTEXT"));
    }

    @Test
    void loadsJsonOutputRequirementsFromClasspath() {
        String requirements = new PromptTemplateLoader("prompts/output_json.md").load();

        assertTrue(requirements.contains("valid JSON object"));
        assertTrue(requirements.contains("`findings` and `observations` are present as arrays"));
        assertTrue(requirements.contains("two collections"));
        assertTrue(requirements.contains("Do not create a third collection"));
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
        assertTrue(prompt.contains("first visible characters of the response must be `## Özet`"));
        assertTrue(prompt.contains("Do not add an observation merely to avoid an empty section"));
        assertTrue(prompt.contains("Do not create an additional top-level section"));
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
