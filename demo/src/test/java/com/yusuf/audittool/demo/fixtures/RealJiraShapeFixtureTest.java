package com.yusuf.audittool.demo.fixtures;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;

import com.yusuf.audittool.api.AgentEndpoint;
import com.yusuf.audittool.api.AuditEngine;
import com.yusuf.audittool.api.AuditInput;
import com.yusuf.audittool.checklist.ChecklistMapper;
import com.yusuf.audittool.demo.ollama.OllamaAgentTransport;
import com.yusuf.audittool.demo.ollama.OllamaProperties;
import com.yusuf.audittool.metadata.MetadataMapper;
import com.yusuf.audittool.model.AgentContext;
import com.yusuf.audittool.model.CommentCoverage;
import com.yusuf.audittool.model.EmptyField;
import com.yusuf.audittool.model.NormalizedField;
import com.yusuf.audittool.normalize.CommentExtractor;
import com.yusuf.audittool.normalize.FieldClassifier;
import com.yusuf.audittool.normalize.GenericJsonWalker;
import com.yusuf.audittool.normalize.NormalizeService;
import com.yusuf.audittool.normalize.SourceInfoExtractor;
import com.yusuf.audittool.prompt.AgentContextRenderer;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

class RealJiraShapeFixtureTest {

    private static final String FIXTURE_DIRECTORY =
            "evaluation/demo-inputs/aud-016-real-jira-shape-synthetic";

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
    void normalizesRealisticJiraShapeWithoutLosingAuditContext() throws Exception {
        AgentContext context = normalizeService.normalize(readInput());

        assertEquals("SYSREQ-842", context.getSourceInfo().getEntityId());
        assertEquals(
                "Guidance computer startup self-test requirement",
                context.getSourceInfo().getEntityLabel()
        );
        assertEquals(61, context.getStatistics().getNullFieldCount());
        assertTrue(context.getStatistics().getSkippedNoiseFieldCount() > 0);
        assertTrue(context.getStatistics().getMetadataMatchedCount() >= 13);

        EmptyField evidence = emptyField(context, "fields.customfield_13313");
        assertEquals("EMPTY_STRING", evidence.getEmptyType());
        assertEquals("Verification Evidence", evidence.getMetadata().getName());
        assertEquals(Boolean.TRUE, evidence.getMetadata().getRequired());
        assertTrue(evidence.getMetadata().getDescriptionTr().contains("nesnel dogrulama kanitini"));

        NormalizedField verificationMethod = activeField(context, "fields.customfield_11023");
        assertEquals("Test", verificationMethod.getValue());
        assertEquals("Verification Method", verificationMethod.getMetadata().getName());

        NormalizedField verificationImpact = activeField(context, "fields.customfield_13355");
        assertEquals("Verification Impact", verificationImpact.getMetadata().getName());
        assertTrue(verificationImpact.getValue().contains("Regression verification"));
        assertTrue(verificationImpact.getMetadata().getDescriptionTr().contains("tekrar test ihtiyaci"));

        String renderedContext = new AgentContextRenderer().render(context);
        assertTrue(renderedContext.contains("Verification Impact"));
        assertTrue(renderedContext.contains(verificationImpact.getValue()));

        NormalizedField statusChange = activeField(context, "changelog.histories[0].items[0]");
        assertEquals("change.compact", statusChange.getValueType());
        assertEquals(
                "field=status | fromString=In Review | toString=Done",
                statusChange.getValue()
        );

        assertTrue(context.getCommentContext().isProvided());
        assertEquals(CommentCoverage.FULL, context.getCommentContext().getCoverage());
        assertEquals(2, context.getCommentContext().getIncludedCount());
        assertEquals(
                "Verification Engineer",
                context.getCommentContext().getComments().get(0).getAuthorName()
        );
        assertTrue(context.getCommentContext().getComments().get(1).getBody()
                .contains("evidence registration remains an open action"));

        assertEquals(4, context.getChecklistContext().getItems().size());
        assertTrue(context.getChecklistContext().getItems().get(3).getText()
                .contains("verification impact"));

        assertFalse(context.getActiveFields().stream()
                .anyMatch(field -> field.getPath().startsWith("fields.comment")));
        assertFalse(context.getEmptyFields().stream()
                .anyMatch(field -> field.getPath().startsWith("fields.comment")));
        assertFalse(context.getActiveFields().stream()
                .map(NormalizedField::getValue)
                .anyMatch(this::isStandaloneLink));
    }

    @Test
    void buildsPromptWithPopulatedVerificationImpact() throws Exception {
        AtomicReference<String> capturedPrompt = new AtomicReference<>();
        AuditEngine engine = new AuditEngine((prompt, endpoint) -> {
            capturedPrompt.set(prompt);
            return "test response";
        });

        engine.analyze(readInput(), AgentEndpoint.of("http://localhost:9999/message"));

        String prompt = capturedPrompt.get();
        assertTrue(prompt.contains("""
                - Verification Impact
                  Path: fields.customfield_13355
                  Value: Regression verification shall cover startup self-test timing and navigation enablement interfaces in the integration environment.
                """.strip()));
        assertFalse(prompt.contains("""
                - Verification Impact
                  Path: fields.customfield_13355
                  Empty Type:
                """.strip()));
    }

    @Test
    @EnabledIfEnvironmentVariable(named = "RUN_OLLAMA_INTEGRATION", matches = "true")
    void analyzesRealisticJiraShapeThroughLocalOllama() throws Exception {
        OllamaProperties properties = new OllamaProperties();
        properties.setModel(environmentValue("OLLAMA_TEST_MODEL", "qwen3.5:4b"));
        properties.setContextWindow(Integer.parseInt(environmentValue("OLLAMA_TEST_CONTEXT", "32768")));
        properties.setMaxOutputTokens(1600);
        properties.setTemperature(0.2);
        properties.setSeed(42);
        properties.setThinkingEnabled(false);
        properties.setTopP(0.8);
        properties.setTopK(20);

        AuditEngine engine = new AuditEngine(new OllamaAgentTransport(properties));
        String report = engine.analyze(
                readInput(),
                AgentEndpoint.of("http://localhost:11434/api/generate")
        );

        assertFalse(report.isBlank());
        System.out.println("\n--- AUD-016 report / " + properties.getModel() + " ---\n" + report);
    }

    private AuditInput readInput() throws IOException {
        Path directory = findRepositoryRoot().resolve(FIXTURE_DIRECTORY);
        return new AuditInput(
                readJson(directory.resolve("issue.json")),
                readJson(directory.resolve("metadata.json")),
                readJson(directory.resolve("field-descriptions.json")),
                readJson(directory.resolve("checklist.json"))
        );
    }

    private JsonNode readJson(Path path) throws IOException {
        return jsonMapper.readTree(Files.readString(path));
    }

    private Path findRepositoryRoot() {
        Path current = Path.of(System.getProperty("user.dir")).toAbsolutePath();
        while (current != null) {
            if (Files.isDirectory(current.resolve("evaluation/demo-inputs"))) {
                return current;
            }
            current = current.getParent();
        }
        throw new IllegalStateException("Repository root could not be located.");
    }

    private NormalizedField activeField(AgentContext context, String path) {
        return context.getActiveFields().stream()
                .filter(field -> path.equals(field.getPath()))
                .findFirst()
                .orElseThrow(() -> new AssertionError("Active field not found: " + path));
    }

    private EmptyField emptyField(AgentContext context, String path) {
        return context.getEmptyFields().stream()
                .filter(field -> path.equals(field.getPath()))
                .findFirst()
                .orElseThrow(() -> new AssertionError("Empty field not found: " + path));
    }

    private boolean isStandaloneLink(String value) {
        if (value == null) {
            return false;
        }
        String normalized = value.strip().toLowerCase();
        return normalized.startsWith("http://")
                || normalized.startsWith("https://")
                || normalized.startsWith("www.");
    }

    private String environmentValue(String name, String fallback) {
        String value = System.getenv(name);
        return value == null || value.isBlank() ? fallback : value;
    }
}
