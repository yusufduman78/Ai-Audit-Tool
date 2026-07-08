package com.yusuf.audittool.model;

import java.util.ArrayList;
import java.util.List;

public class AgentContext {

    private SourceInfo sourceInfo;
    private List<NormalizedField> activeFields = new ArrayList<>();
    private List<EmptyField> emptyFields = new ArrayList<>();
    private ChecklistContext checklistContext;
    private ContextStatistics statistics;

    public SourceInfo getSourceInfo() {
        return sourceInfo;
    }

    public void setSourceInfo(SourceInfo sourceInfo) {
        this.sourceInfo = sourceInfo;
    }

    public List<NormalizedField> getActiveFields() {
        return activeFields;
    }

    public void setActiveFields(List<NormalizedField> activeFields) {
        this.activeFields = activeFields;
    }

    public List<EmptyField> getEmptyFields() {
        return emptyFields;
    }

    public void setEmptyFields(List<EmptyField> emptyFields) {
        this.emptyFields = emptyFields;
    }

    public ChecklistContext getChecklistContext() {
        return checklistContext;
    }

    public void setChecklistContext(ChecklistContext checklistContext) {
        this.checklistContext = checklistContext;
    }

    public ContextStatistics getStatistics() {
        return statistics;
    }

    public void setStatistics(ContextStatistics statistics) {
        this.statistics = statistics;
    }
}

