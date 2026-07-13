package com.yusuf.audittool.service;

import java.util.Optional;

import org.springframework.stereotype.Component;

import com.yusuf.audittool.model.AuditReport;

import tools.jackson.core.json.JsonReadFeature;
import tools.jackson.databind.json.JsonMapper;

@Component
public class AuditReportParser {

    private final JsonMapper strictMapper = new JsonMapper();
    private final JsonMapper trailingCommaMapper = JsonMapper.builder()
            .enable(JsonReadFeature.ALLOW_TRAILING_COMMA)
            .build();

    public Optional<AuditReport> parse(String agentOutput) {
        if (agentOutput == null || agentOutput.isBlank()) {
            return Optional.empty();
        }

        Optional<AuditReport> strictReport = read(strictMapper, agentOutput);
        if (strictReport.isPresent()) {
            return strictReport;
        }

        String jsonObject = extractJsonObject(agentOutput);
        if (!jsonObject.equals(agentOutput)) {
            strictReport = read(strictMapper, jsonObject);
            if (strictReport.isPresent()) {
                return strictReport;
            }
        }

        // Accept only a narrow, mechanical JSON extension. The validator still
        // rejects missing report fields, empty evidence, and duplicate findings.
        return read(trailingCommaMapper, jsonObject);
    }

    private Optional<AuditReport> read(JsonMapper mapper, String value) {
        try {
            return Optional.of(mapper.readValue(value, AuditReport.class));
        } catch (Exception exception) {
            return Optional.empty();
        }
    }

    private String extractJsonObject(String value) {
        String text = value.strip();
        int start = text.indexOf('{');
        if (start < 0) {
            return text;
        }

        int depth = 0;
        boolean insideString = false;
        boolean escaped = false;
        for (int index = start; index < text.length(); index++) {
            char character = text.charAt(index);
            if (insideString) {
                if (escaped) {
                    escaped = false;
                } else if (character == '\\') {
                    escaped = true;
                } else if (character == '"') {
                    insideString = false;
                }
                continue;
            }

            if (character == '"') {
                insideString = true;
            } else if (character == '{') {
                depth++;
            } else if (character == '}') {
                depth--;
                if (depth == 0) {
                    return text.substring(start, index + 1);
                }
            }
        }

        return text;
    }
}
