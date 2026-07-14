package com.yusuf.audittool.demo.model;

import com.yusuf.audittool.model.AgentContext;

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
