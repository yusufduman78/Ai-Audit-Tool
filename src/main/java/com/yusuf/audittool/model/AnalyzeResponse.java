package com.yusuf.audittool.model;

public class AnalyzeResponse {

    private String agentOutput;

    public AnalyzeResponse() {
    }

    public AnalyzeResponse(String agentOutput) {
        this.agentOutput = agentOutput;
    }

    public String getAgentOutput() {
        return agentOutput;
    }

    public void setAgentOutput(String agentOutput) {
        this.agentOutput = agentOutput;
    }
}

