package com.yusuf.audittool.demo.model;

import java.util.List;

public class AuditReport {

    private String summary;
    private List<AuditFinding> findings;
    private List<AuditObservation> observations;
    private String recommendation;

    public String getSummary() { return summary; }
    public void setSummary(String summary) { this.summary = summary; }
    public List<AuditFinding> getFindings() { return findings; }
    public void setFindings(List<AuditFinding> findings) { this.findings = findings; }
    public List<AuditObservation> getObservations() { return observations; }
    public void setObservations(List<AuditObservation> observations) { this.observations = observations; }
    public String getRecommendation() { return recommendation; }
    public void setRecommendation(String recommendation) { this.recommendation = recommendation; }
}
