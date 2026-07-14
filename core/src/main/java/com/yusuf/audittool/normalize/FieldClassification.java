package com.yusuf.audittool.normalize;

import java.util.ArrayList;
import java.util.List;

import com.yusuf.audittool.model.ContextStatistics;
import com.yusuf.audittool.model.EmptyField;
import com.yusuf.audittool.model.NormalizedField;

public class FieldClassification {

    private List<NormalizedField> activeFields = new ArrayList<>();
    private List<EmptyField> emptyFields = new ArrayList<>();
    private ContextStatistics statistics = new ContextStatistics();

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

    public ContextStatistics getStatistics() {
        return statistics;
    }

    public void setStatistics(ContextStatistics statistics) {
        this.statistics = statistics;
    }
}

