package com.yusuf.audittool.model;

import java.util.ArrayList;
import java.util.List;

public class ChecklistContext {

    private boolean provided;
    private String rawText;
    private List<ChecklistItem> items = new ArrayList<>();

    public boolean isProvided() {
        return provided;
    }

    public void setProvided(boolean provided) {
        this.provided = provided;
    }

    public String getRawText() {
        return rawText;
    }

    public void setRawText(String rawText) {
        this.rawText = rawText;
    }

    public List<ChecklistItem> getItems() {
        return items;
    }

    public void setItems(List<ChecklistItem> items) {
        this.items = items;
    }
}

