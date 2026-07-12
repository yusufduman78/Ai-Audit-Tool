package com.yusuf.audittool.service;

import java.util.Optional;

import org.springframework.stereotype.Component;

import com.yusuf.audittool.model.AuditReport;

import tools.jackson.databind.json.JsonMapper;

@Component
public class AuditReportParser {

    private final JsonMapper jsonMapper = new JsonMapper();

    public Optional<AuditReport> parse(String agentOutput) {
        try {
            return Optional.of(jsonMapper.readValue(agentOutput, AuditReport.class));
        } catch (Exception exception) {
            return Optional.empty();
        }
    }
}
