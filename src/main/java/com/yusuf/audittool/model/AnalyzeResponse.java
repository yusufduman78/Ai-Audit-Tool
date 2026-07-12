package com.yusuf.audittool.model;

public class AnalyzeResponse {

    private String agentOutput;
    private AuditReport report;
    private boolean structuredOutput;

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

    public AuditReport getReport() { return report; }
    public void setReport(AuditReport report) { this.report = report; }
    public boolean isStructuredOutput() { return structuredOutput; }
    public void setStructuredOutput(boolean structuredOutput) { this.structuredOutput = structuredOutput; }
}
