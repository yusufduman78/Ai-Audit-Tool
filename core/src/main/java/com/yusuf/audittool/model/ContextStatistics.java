package com.yusuf.audittool.model;

public class ContextStatistics {

    private int activeFieldCount;
    private int emptyFieldCount;
    private int nullFieldCount;
    private int skippedNoiseFieldCount;
    private int metadataMatchedCount;
    private int metadataMissingCount;

    public int getActiveFieldCount() {
        return activeFieldCount;
    }

    public void setActiveFieldCount(int activeFieldCount) {
        this.activeFieldCount = activeFieldCount;
    }

    public int getEmptyFieldCount() {
        return emptyFieldCount;
    }

    public void setEmptyFieldCount(int emptyFieldCount) {
        this.emptyFieldCount = emptyFieldCount;
    }

    public int getNullFieldCount() {
        return nullFieldCount;
    }

    public void setNullFieldCount(int nullFieldCount) {
        this.nullFieldCount = nullFieldCount;
    }

    public int getSkippedNoiseFieldCount() {
        return skippedNoiseFieldCount;
    }

    public void setSkippedNoiseFieldCount(int skippedNoiseFieldCount) {
        this.skippedNoiseFieldCount = skippedNoiseFieldCount;
    }

    public int getMetadataMatchedCount() {
        return metadataMatchedCount;
    }

    public void setMetadataMatchedCount(int metadataMatchedCount) {
        this.metadataMatchedCount = metadataMatchedCount;
    }

    public int getMetadataMissingCount() {
        return metadataMissingCount;
    }

    public void setMetadataMissingCount(int metadataMissingCount) {
        this.metadataMissingCount = metadataMissingCount;
    }
}

