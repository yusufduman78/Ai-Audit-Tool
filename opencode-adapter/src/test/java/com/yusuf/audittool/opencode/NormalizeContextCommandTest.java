package com.yusuf.audittool.opencode;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class NormalizeContextCommandTest {

    @TempDir
    Path worktree;

    @Test
    void rendersFilesInsideWorktree() throws Exception {
        Files.writeString(worktree.resolve("issue.json"), """
                {
                  "key": "REQ-601",
                  "fields": { "summary": "Flight display requirement" }
                }
                """);
        Files.writeString(worktree.resolve("checklist.json"), """
                ["Completed requirements must include verification evidence."]
                """);
        CommandResult result = run(
                "--worktree", worktree.toString(),
                "--issue", "issue.json",
                "--checklist", "checklist.json"
        );

        assertEquals(0, result.exitCode());
        assertTrue(result.output().contains("ID: REQ-601"));
        assertTrue(result.output().contains("Flight display requirement"));
        assertTrue(result.output().contains("Completed requirements must include verification evidence."));
        assertEquals("", result.error());
    }

    @Test
    void rejectsFilesOutsideWorktree() throws Exception {
        Path outsideFile = Files.createTempFile("audittool-outside-", ".json");
        Files.writeString(outsideFile, "{\"key\":\"REQ-602\"}");

        CommandResult result = run(
                "--worktree", worktree.toString(),
                "--issue", outsideFile.toString()
        );

        assertEquals(2, result.exitCode());
        assertTrue(result.error().contains("inside the project worktree"));
    }

    @Test
    void reportsInvalidJsonWithoutPrintingItsContent() throws Exception {
        Files.writeString(worktree.resolve("issue.json"), "{invalid-json}");

        CommandResult result = run(
                "--worktree", worktree.toString(),
                "--issue", "issue.json"
        );

        assertEquals(3, result.exitCode());
        assertTrue(result.error().startsWith("JSON input is not valid:"));
        assertEquals("", result.output());
    }

    private CommandResult run(String... args) {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        ByteArrayOutputStream error = new ByteArrayOutputStream();
        int exitCode = NormalizeContextCommand.run(
                args,
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
