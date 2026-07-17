package com.yusuf.audittool.opencode;

import java.io.IOException;
import java.io.PrintStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import com.yusuf.audittool.api.AuditContextPreparer;
import com.yusuf.audittool.api.AuditInput;

import tools.jackson.core.JacksonException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

public final class NormalizeContextCommand {

    private static final String SCHEMA_VERSION = "1.0";
    private static final long MAX_JSON_BYTES = 20L * 1024L * 1024L;
    private static final int MAX_PATH_LENGTH = 1024;

    private static final String ISSUE_OPTION = "--issue";
    private static final String METADATA_OPTION = "--metadata";
    private static final String FIELD_DESCRIPTIONS_OPTION = "--field-descriptions";
    private static final String CHECKLIST_OPTION = "--checklist";
    private static final Set<String> SUPPORTED_OPTIONS = Set.of(
            ISSUE_OPTION,
            METADATA_OPTION,
            FIELD_DESCRIPTIONS_OPTION,
            CHECKLIST_OPTION
    );

    private NormalizeContextCommand() {
    }

    public static void main(String[] args) {
        int exitCode = run(args, Path.of(""), System.out, System.err);
        if (exitCode != 0) {
            System.exit(exitCode);
        }
    }

    static int run(String[] args, Path processDirectory, PrintStream output, PrintStream error) {
        JsonMapper jsonMapper = new JsonMapper();
        try {
            Path worktree = trustedWorktree(processDirectory);
            Map<String, String> options = parseOptions(args);

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
            if (context == null || context.isBlank()) {
                throw new CommandFailure("EMPTY_AUDIT_CONTEXT", "Normalizer produced an empty audit context.");
            }

            writeEnvelope(output, jsonMapper, new SuccessEnvelope(true, SCHEMA_VERSION, context));
            return 0;
        } catch (CommandFailure exception) {
            writeEnvelope(error, jsonMapper, new ErrorEnvelope(
                    false,
                    SCHEMA_VERSION,
                    exception.errorCode(),
                    exception.getMessage()
            ));
            return 2;
        } catch (JacksonException exception) {
            writeEnvelope(error, jsonMapper, new ErrorEnvelope(
                    false,
                    SCHEMA_VERSION,
                    "INVALID_JSON",
                    "One of the supplied files does not contain valid JSON."
            ));
            return 3;
        } catch (IOException exception) {
            writeEnvelope(error, jsonMapper, new ErrorEnvelope(
                    false,
                    SCHEMA_VERSION,
                    "INPUT_READ_FAILED",
                    "One of the supplied files could not be read."
            ));
            return 4;
        } catch (RuntimeException exception) {
            writeEnvelope(error, jsonMapper, new ErrorEnvelope(
                    false,
                    SCHEMA_VERSION,
                    "NORMALIZATION_FAILED",
                    "Normalization failed before an audit context was produced."
            ));
            return 5;
        }
    }

    private static Path trustedWorktree(Path processDirectory) throws IOException {
        Path worktree = processDirectory.toRealPath();
        if (!Files.isDirectory(worktree)) {
            throw new CommandFailure("INVALID_WORKTREE", "The adapter must run from a project directory.");
        }
        return worktree;
    }

    private static Map<String, String> parseOptions(String[] args) {
        Map<String, String> options = new HashMap<>();
        for (int index = 0; index < args.length; index += 2) {
            if (index + 1 >= args.length || !isOption(args[index])) {
                throw new CommandFailure(
                        "INVALID_ARGUMENTS",
                        "Arguments must be provided as supported --option value pairs."
                );
            }

            String option = args[index];
            String value = args[index + 1];
            if (!SUPPORTED_OPTIONS.contains(option)) {
                throw new CommandFailure("UNKNOWN_OPTION", "Unknown option: " + option);
            }
            if (value == null || value.isBlank() || isOption(value)) {
                throw new CommandFailure("MISSING_OPTION_VALUE", "Option requires a value: " + option);
            }
            if (options.put(option, value) != null) {
                throw new CommandFailure("DUPLICATE_OPTION", "Option was provided more than once: " + option);
            }
        }
        return options;
    }

    private static boolean isOption(String value) {
        return value != null && value.startsWith("--");
    }

    private static String required(Map<String, String> options, String option) {
        String value = options.get(option);
        if (value == null) {
            throw new CommandFailure("MISSING_REQUIRED_OPTION", "Required option is missing: " + option);
        }
        return value;
    }

    private static JsonNode readOptionalJson(
            JsonMapper jsonMapper,
            Path worktree,
            String fileName,
            String option
    ) throws IOException {
        return fileName == null ? null : readJson(jsonMapper, worktree, fileName, option, true);
    }

    private static JsonNode readJson(
            JsonMapper jsonMapper,
            Path worktree,
            String fileName,
            String option
    ) throws IOException {
        return readJson(jsonMapper, worktree, fileName, option, false);
    }

    private static JsonNode readJson(
            JsonMapper jsonMapper,
            Path worktree,
            String fileName,
            String option,
            boolean nullMeansNotProvided
    ) throws IOException {
        validatePortableRelativeJsonPath(fileName, option);

        Path candidate = worktree.resolve(fileName).normalize();
        if (!candidate.startsWith(worktree)) {
            throw invalidPath(option);
        }
        if (!Files.exists(candidate)) {
            throw new CommandFailure("INPUT_NOT_FOUND", option + " does not point to an existing file.");
        }

        Path resolved = candidate.toRealPath();
        if (!resolved.startsWith(worktree)) {
            throw invalidPath(option);
        }
        if (!Files.isRegularFile(resolved) || !Files.isReadable(resolved)) {
            throw new CommandFailure("INVALID_INPUT_FILE", option + " must point to a readable JSON file.");
        }
        if (Files.size(resolved) > MAX_JSON_BYTES) {
            throw new CommandFailure("INPUT_TOO_LARGE", option + " exceeds the 20 MiB input limit.");
        }

        JsonNode value = jsonMapper.readTree(Files.readString(resolved));
        if (value == null || value.isNull()) {
            if (nullMeansNotProvided) {
                return null;
            }
            throw new CommandFailure("EMPTY_JSON_VALUE", option + " must contain a non-null JSON value.");
        }
        return value;
    }

    private static void validatePortableRelativeJsonPath(String fileName, String option) {
        if (fileName.length() > MAX_PATH_LENGTH
                || containsControlCharacter(fileName)
                || containsShellMetacharacter(fileName)) {
            throw invalidPath(option);
        }
        if (fileName.indexOf('\\') >= 0 || fileName.matches("^[A-Za-z]:.*")) {
            throw new CommandFailure(
                    "INVALID_INPUT_PATH",
                    option + " must use a portable project-relative path with '/' separators."
            );
        }

        Path path;
        try {
            path = Path.of(fileName);
        } catch (RuntimeException exception) {
            throw invalidPath(option);
        }
        if (path.isAbsolute() || !fileName.toLowerCase().endsWith(".json")) {
            throw new CommandFailure(
                    "INVALID_INPUT_PATH",
                    option + " must be a project-relative path ending in .json."
            );
        }
    }

    private static boolean containsControlCharacter(String value) {
        return value.chars().anyMatch(character -> Character.isISOControl(character));
    }

    private static boolean containsShellMetacharacter(String value) {
        return value.chars().anyMatch(character -> ";&|$`<>".indexOf(character) >= 0);
    }

    private static CommandFailure invalidPath(String option) {
        return new CommandFailure(
                "INVALID_INPUT_PATH",
                option + " must resolve to a JSON file inside the project worktree."
        );
    }

    private static void writeEnvelope(PrintStream stream, JsonMapper jsonMapper, Object envelope) {
        try {
            stream.println(jsonMapper.writeValueAsString(envelope));
        } catch (JacksonException exception) {
            stream.println("{\"success\":false,\"schemaVersion\":\"1.0\","
                    + "\"errorCode\":\"ENVELOPE_WRITE_FAILED\","
                    + "\"message\":\"Adapter response could not be serialized.\"}");
        }
    }

    private record SuccessEnvelope(boolean success, String schemaVersion, String auditContext) {
    }

    private record ErrorEnvelope(boolean success, String schemaVersion, String errorCode, String message) {
    }

    private static final class CommandFailure extends IllegalArgumentException {

        private final String errorCode;

        private CommandFailure(String errorCode, String message) {
            super(message);
            this.errorCode = errorCode;
        }

        private String errorCode() {
            return errorCode;
        }
    }
}
