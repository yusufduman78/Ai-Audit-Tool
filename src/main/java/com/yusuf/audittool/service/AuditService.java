package com.yusuf.audittool.service;

import java.util.List;

import org.springframework.stereotype.Service;

import com.yusuf.audittool.agent.AgentClient;
import com.yusuf.audittool.model.AgentContext;
import com.yusuf.audittool.model.AnalyzeRequest;
import com.yusuf.audittool.model.AnalyzeResponse;
import com.yusuf.audittool.model.NormalizeResponse;
import com.yusuf.audittool.normalize.NormalizeService;
import com.yusuf.audittool.prompt.PromptBuilder;

@Service
public class AuditService {

    private final NormalizeService normalizeService;
    private final PromptBuilder promptBuilder;
    private final AgentClient agentClient;
    private final AuditReportParser reportParser;
    private final AuditReportValidator reportValidator;

    public AuditService(
            NormalizeService normalizeService,
            PromptBuilder promptBuilder,
            AgentClient agentClient,
            AuditReportParser reportParser,
            AuditReportValidator reportValidator
    ) {
        this.normalizeService = normalizeService;
        this.promptBuilder = promptBuilder;
        this.agentClient = agentClient;
        this.reportParser = reportParser;
        this.reportValidator = reportValidator;
    }

    public NormalizeResponse normalize(AnalyzeRequest request) {
        return new NormalizeResponse(normalizeService.normalize(request));
    }

    public AnalyzeResponse analyze(AnalyzeRequest request) {
        AgentContext context = normalizeService.normalize(request);
        String prompt = promptBuilder.build(context);
        String agentOutput = request.getAgentOptions() == null
                ? agentClient.analyze(prompt)
                : agentClient.analyze(prompt, request.getAgentOptions());

        AnalyzeResponse response = new AnalyzeResponse(agentOutput);
        reportParser.parse(agentOutput).ifPresentOrElse(report -> {
            var errors = reportValidator.validate(report);
            response.setReportValidationErrors(errors);
            if (errors.isEmpty()) {
                response.setReport(report);
                response.setStructuredOutput(true);
            }
        }, () -> response.setReportValidationErrors(List.of("Agent output is not valid JSON.")));
        return response;
    }
}
