package com.yusuf.audittool.api;

import java.util.Objects;

import com.yusuf.audittool.agent.AgentRuntimeException;
import com.yusuf.audittool.agent.AgentTransport;
import com.yusuf.audittool.model.AgentContext;
import com.yusuf.audittool.normalize.NormalizeService;
import com.yusuf.audittool.prompt.AgentContextRenderer;
import com.yusuf.audittool.prompt.PromptBuilder;
import com.yusuf.audittool.prompt.PromptTemplateLoader;

public final class AuditEngine {

    private static final String DEFAULT_TEMPLATE_PATH = "prompts/core_auditor.md";
    private static final String MARKDOWN_OUTPUT_PATH = "prompts/output_markdown.md";

    private final NormalizeService normalizeService;
    private final PromptBuilder promptBuilder;
    private final AgentTransport agentTransport;

    public AuditEngine(AgentTransport agentTransport) {
        this(defaultNormalizeService(), defaultPromptBuilder(), agentTransport);
    }

    public AuditEngine(
            NormalizeService normalizeService,
            PromptBuilder promptBuilder,
            AgentTransport agentTransport
    ) {
        this.normalizeService = Objects.requireNonNull(normalizeService, "Normalize service must not be null.");
        this.promptBuilder = Objects.requireNonNull(promptBuilder, "Prompt builder must not be null.");
        this.agentTransport = Objects.requireNonNull(agentTransport, "Agent transport must not be null.");
    }

    public String analyze(AuditInput input, AgentEndpoint endpoint) {
        Objects.requireNonNull(input, "Audit input must not be null.");
        Objects.requireNonNull(endpoint, "Agent endpoint must not be null.");

        AgentContext context = normalizeService.normalize(input);
        String prompt = promptBuilder.build(context);
        String response = agentTransport.send(prompt, endpoint);

        if (response == null || response.isBlank()) {
            throw new AgentRuntimeException("Agent returned an empty response.");
        }
        return response.strip();
    }

    private static NormalizeService defaultNormalizeService() {
        return AuditContextPreparer.defaultNormalizeService();
    }

    private static PromptBuilder defaultPromptBuilder() {
        return new PromptBuilder(
                new PromptTemplateLoader(DEFAULT_TEMPLATE_PATH),
                new AgentContextRenderer(),
                new PromptTemplateLoader(MARKDOWN_OUTPUT_PATH)
        );
    }
}
