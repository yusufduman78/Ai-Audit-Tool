package com.yusuf.audittool.opencode;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

class NormalizeContextCommandTest {

    private final JsonMapper jsonMapper = new JsonMapper();

    @TempDir
    Path worktree;

    @Test
    void rendersFilesInsideWorktreeInVersionedEnvelope() throws Exception {
        write("issue.json", """
                {
                  "key": "REQ-601",
                  "fields": { "summary": "Flight display requirement" }
                }
                """);
        write("checklist.json", """
                ["Completed requirements must include verification evidence."]
                """);

        CommandResult result = run(
                "--issue", "issue.json",
                "--checklist", "checklist.json"
        );

        assertEquals(0, result.exitCode());
        JsonNode envelope = jsonMapper.readTree(result.output());
        assertTrue(envelope.get("success").asBoolean());
        assertEquals("1.0", envelope.get("schemaVersion").stringValue());
        assertTrue(envelope.get("auditContext").stringValue().contains("ID: REQ-601"));
        assertTrue(envelope.get("auditContext").stringValue().contains("Flight display requirement"));
        assertTrue(envelope.get("auditContext").stringValue().contains(
                "Completed requirements must include verification evidence."
        ));
        assertEquals("", result.error());
    }

    @Test
    void rejectsUnknownAndFormerWorktreeOptions() throws Exception {
        write("issue.json", "{\"key\":\"REQ-602\"}");

        assertErrorCode(run("--unknown", "value", "--issue", "issue.json"), "UNKNOWN_OPTION");
        assertErrorCode(run("--worktree", ".", "--issue", "issue.json"), "UNKNOWN_OPTION");
    }

    @Test
    void rejectsDuplicateOptions() throws Exception {
        write("issue.json", "{\"key\":\"REQ-603\"}");

        assertErrorCode(
                run("--issue", "issue.json", "--issue", "issue.json"),
                "DUPLICATE_OPTION"
        );
    }

    @Test
    void rejectsMissingOptionValuesAndMissingIssue() throws Exception {
        write("metadata.json", "{}");

        assertErrorCode(run("--issue"), "INVALID_ARGUMENTS");
        assertErrorCode(run("--issue", "--metadata", "metadata.json", "unused.json"), "MISSING_OPTION_VALUE");
        assertErrorCode(run("--metadata", "metadata.json"), "MISSING_REQUIRED_OPTION");
    }

    @Test
    void rejectsTraversalAndAbsolutePaths() throws Exception {
        Path outsideFile = Files.createTempFile("audittool-outside-", ".json");
        Files.writeString(outsideFile, "{\"key\":\"REQ-604\"}");

        assertErrorCode(run("--issue", "../" + outsideFile.getFileName()), "INVALID_INPUT_PATH");
        assertErrorCode(run("--issue", outsideFile.toString()), "INVALID_INPUT_PATH");
    }

    @Test
    void rejectsSymlinksThatEscapeWorktree() throws Exception {
        Path outsideFile = Files.createTempFile("audittool-symlink-target-", ".json");
        Files.writeString(outsideFile, "{\"key\":\"REQ-605\"}");
        Files.createSymbolicLink(worktree.resolve("linked.json"), outsideFile);

        assertErrorCode(run("--issue", "linked.json"), "INVALID_INPUT_PATH");
    }

    @Test
    void rejectsNonPortableAndNonJsonPaths() throws Exception {
        write("issue.txt", "{\"key\":\"REQ-606\"}");

        assertErrorCode(run("--issue", "issue.txt"), "INVALID_INPUT_PATH");
        assertErrorCode(run("--issue", "folder\\issue.json"), "INVALID_INPUT_PATH");
        assertErrorCode(run("--issue", "C:/outside.json"), "INVALID_INPUT_PATH");
        assertErrorCode(run("--issue", "issue.json\nsecond.json"), "INVALID_INPUT_PATH");
    }

    @Test
    void rejectsShellShapedPathValuesBeforeFileAccess() throws Exception {
        assertErrorCode(run("--issue", "data/issue.json;echo-test.json"), "INVALID_INPUT_PATH");
        assertErrorCode(run("--issue", "data/issue.json&&test.json"), "INVALID_INPUT_PATH");
        assertErrorCode(run("--issue", "data/issue$(test).json"), "INVALID_INPUT_PATH");
        assertErrorCode(run("--issue", "data/issue`test`.json"), "INVALID_INPUT_PATH");
    }

    @Test
    void reportsInvalidJsonWithoutPrintingItsContent() throws Exception {
        write("issue.json", "{SUPER_SECRET_INVALID_JSON}");

        CommandResult result = run("--issue", "issue.json");

        assertEquals(3, result.exitCode());
        JsonNode envelope = jsonMapper.readTree(result.error());
        assertEquals("INVALID_JSON", envelope.get("errorCode").stringValue());
        assertFalse(result.error().contains("SUPER_SECRET_INVALID_JSON"));
        assertEquals("", result.output());
    }

    @Test
    void rejectsNullJsonDocument() throws Exception {
        write("issue.json", "null");

        assertErrorCode(run("--issue", "issue.json"), "EMPTY_JSON_VALUE");
    }

    @Test
    void treatsNullOptionalDocumentsAsNotProvided() throws Exception {
        write("issue.json", "{\"key\":\"REQ-607\"}");
        write("metadata.json", "null");
        write("field-descriptions.json", "null");
        write("checklist.json", "null");

        CommandResult result = run(
                "--issue", "issue.json",
                "--metadata", "metadata.json",
                "--field-descriptions", "field-descriptions.json",
                "--checklist", "checklist.json"
        );

        assertEquals(0, result.exitCode());
        JsonNode envelope = jsonMapper.readTree(result.output());
        assertTrue(envelope.get("success").asBoolean());
        assertTrue(envelope.get("auditContext").stringValue().contains("ID: REQ-607"));
        assertTrue(envelope.get("auditContext").stringValue().contains("CHECKLIST\n- Not provided"));
        assertEquals("", result.error());
    }

    @Test
    void rejectsFilesLargerThanConfiguredLimit() throws Exception {
        Files.write(worktree.resolve("large.json"), new byte[(20 * 1024 * 1024) + 1]);

        assertErrorCode(run("--issue", "large.json"), "INPUT_TOO_LARGE");
    }

    private void write(String name, String content) throws Exception {
        Files.writeString(worktree.resolve(name), content);
    }

    private void assertErrorCode(CommandResult result, String expectedCode) throws Exception {
        assertEquals(2, result.exitCode());
        JsonNode envelope = jsonMapper.readTree(result.error());
        assertFalse(envelope.get("success").asBoolean());
        assertEquals("1.0", envelope.get("schemaVersion").stringValue());
        assertEquals(expectedCode, envelope.get("errorCode").stringValue());
        assertEquals("", result.output());
    }

    private CommandResult run(String... args) {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        ByteArrayOutputStream error = new ByteArrayOutputStream();
        int exitCode = NormalizeContextCommand.run(
                args,
                worktree,
                new PrintStream(output, true, StandardCharsets.UTF_8),
                new PrintStream(error, true, StandardCharsets.UTF_8)
        );
        return new CommandResult(
                exitCode,
                output.toString(StandardCharsets.UTF_8).strip(),
                error.toString(StandardCharsets.UTF_8).strip()
        );
    }

    private record CommandResult(int exitCode, String output, String error) {
    }
}
