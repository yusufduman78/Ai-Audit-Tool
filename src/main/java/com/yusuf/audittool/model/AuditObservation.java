package com.yusuf.audittool.model;

import java.util.List;

public class AuditObservation {

    private String type;
    private String description;
    private List<String> evidence;

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public List<String> getEvidence() { return evidence; }
    public void setEvidence(List<String> evidence) { this.evidence = evidence; }
}
