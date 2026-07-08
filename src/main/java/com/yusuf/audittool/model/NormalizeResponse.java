package com.yusuf.audittool.model;

public class NormalizeResponse {

    private AgentContext agentContext;

    public NormalizeResponse() {
    }

    public NormalizeResponse(AgentContext agentContext) {
        this.agentContext = agentContext;
    }

    public AgentContext getAgentContext() {
        return agentContext;
    }

    public void setAgentContext(AgentContext agentContext) {
        this.agentContext = agentContext;
    }
}

