package com.yusuf.audittool.opencode;

import java.io.IOException;
import java.io.PrintStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

import com.yusuf.audittool.api.AuditContextPreparer;
import com.yusuf.audittool.api.AuditInput;

import tools.jackson.core.JacksonException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

public final class NormalizeContextCommand {

    private static final String WORKTREE_OPTION = "--worktree";
    private static final String ISSUE_OPTION = "--issue";
    private static final String METADATA_OPTION = "--metadata";
    private static final String FIELD_DESCRIPTIONS_OPTION = "--field-descriptions";
    private static final String CHECKLIST_OPTION = "--checklist";

    private NormalizeContextCommand() {
    }

    public static void main(String[] args) {
        int exitCode = run(args, System.out, System.err);
        if (exitCode != 0) {
            System.exit(exitCode);
        }
    }

    static int run(String[] args, PrintStream output, PrintStream error) {
        try {
            Map<String, String> options = parseOptions(args);
            Path worktree = requiredDirectory(options, WORKTREE_OPTION);
            JsonMapper jsonMapper = new JsonMapper();

            JsonNode issue = readJson(jsonMapper, worktree, required(options, ISSUE_OPTION), ISSUE_OPTION);
            JsonNode metadata = readOptionalJson(jsonMapper, worktree, options.get(METADATA_OPTION), METADATA_OPTION);
            JsonNode descriptions = readOptionalJson(
                    jsonMapper,
                    worktree,
                    options.get(FIELD_DESCRIPTIONS_OPTION),
                    FIELD_DESCRIPTIONS_OPTION
            );
            JsonNode checklist = readOptionalJson(jsonMapper, worktree, options.get(CHECKLIST_OPTION), CHECKLIST_OPTION);

            String context = new AuditContextPreparer().prepare(
                    new AuditInput(issue, metadata, descriptions, checklist)
            );
            output.println(context);
            return 0;
        } catch (IllegalArgumentException exception) {
            error.println(exception.getMessage());
            return 2;
        } catch (JacksonException exception) {
            error.println("JSON input is not valid: " + exception.getOriginalMessage());
            return 3;
        } catch (IOException exception) {
            error.println("JSON input could not be read: " + exception.getMessage());
            return 4;
        }
    }

    private static Map<String, String> parseOptions(String[] args) {
        Map<String, String> options = new HashMap<>();
        for (int index = 0; index < args.length; index += 2) {
            if (index + 1 >= args.length || !args[index].startsWith("--")) {
                throw new IllegalArgumentException("Arguments must be provided as --option value pairs.");
            }
            if (!isSupportedOption(args[index])) {
                throw new IllegalArgumentException("Unknown option: " + args[index]);
            }
            if (options.put(args[index], args[index + 1]) != null) {
                throw new IllegalArgumentException("Option was provided more than once: " + args[index]);
            }
        }
        return options;
    }

    private static boolean isSupportedOption(String option) {
        return WORKTREE_OPTION.equals(option)
                || ISSUE_OPTION.equals(option)
                || METADATA_OPTION.equals(option)
                || FIELD_DESCRIPTIONS_OPTION.equals(option)
                || CHECKLIST_OPTION.equals(option);
    }

    private static Path requiredDirectory(Map<String, String> options, String option) throws IOException {
        Path directory = Path.of(required(options, option)).toRealPath();
        if (!Files.isDirectory(directory)) {
            throw new IllegalArgumentException(option + " must point to a directory.");
        }
        return directory;
    }

    private static String required(Map<String, String> options, String option) {
        String value = options.get(option);
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Required option is missing: " + option);
        }
        return value;
    }

    private static JsonNode readOptionalJson(
            JsonMapper jsonMapper,
            Path worktree,
            String fileName,
            String option
    ) throws IOException {
        if (fileName == null || fileName.isBlank()) {
            return null;
        }
        return readJson(jsonMapper, worktree, fileName, option);
    }

    private static JsonNode readJson(
            JsonMapper jsonMapper,
            Path worktree,
            String fileName,
            String option
    ) throws IOException {
        Path file = Path.of(fileName);
        Path resolved = file.isAbsolute() ? file.toRealPath() : worktree.resolve(file).toRealPath();

        if (!resolved.startsWith(worktree)) {
            throw new IllegalArgumentException(option + " must point to a file inside the project worktree.");
        }
        if (!Files.isRegularFile(resolved)) {
            throw new IllegalArgumentException(option + " must point to a JSON file.");
        }

        JsonNode value = jsonMapper.readTree(Files.readString(resolved));
        if (value == null || value.isNull()) {
            throw new IllegalArgumentException(option + " must contain a JSON value.");
        }
        return value;
    }
}
