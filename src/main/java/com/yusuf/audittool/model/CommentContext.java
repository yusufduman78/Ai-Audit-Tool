package com.yusuf.audittool.model;

import java.util.ArrayList;
import java.util.List;

public class CommentContext {

    private boolean provided;
    private String sourcePath;
    private Integer totalCount;
    private int includedCount;
    private CommentCoverage coverage = CommentCoverage.UNKNOWN;
    private List<AuditComment> comments = new ArrayList<>();

    public boolean isProvided() {
        return provided;
    }

    public void setProvided(boolean provided) {
        this.provided = provided;
    }

    public String getSourcePath() {
        return sourcePath;
    }

    public void setSourcePath(String sourcePath) {
        this.sourcePath = sourcePath;
    }

    public Integer getTotalCount() {
        return totalCount;
    }

    public void setTotalCount(Integer totalCount) {
        this.totalCount = totalCount;
    }

    public int getIncludedCount() {
        return includedCount;
    }

    public void setIncludedCount(int includedCount) {
        this.includedCount = includedCount;
    }

    public CommentCoverage getCoverage() {
        return coverage;
    }

    public void setCoverage(CommentCoverage coverage) {
        this.coverage = coverage;
    }

    public List<AuditComment> getComments() {
        return comments;
    }

    public void setComments(List<AuditComment> comments) {
        this.comments = comments;
    }
}
