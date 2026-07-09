package com.yusuf.audittool.service;

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

    public AuditService(
            NormalizeService normalizeService,
            PromptBuilder promptBuilder,
            AgentClient agentClient
    ) {
        this.normalizeService = normalizeService;
        this.promptBuilder = promptBuilder;
        this.agentClient = agentClient;
    }

    public NormalizeResponse normalize(AnalyzeRequest request) {
        return new NormalizeResponse(normalizeService.normalize(request));
    }

    public AnalyzeResponse analyze(AnalyzeRequest request) {
        AgentContext context = normalizeService.normalize(request);
        String prompt = promptBuilder.build(context);
        String agentOutput = agentClient.analyze(prompt);

        return new AnalyzeResponse(agentOutput);
    }
}
