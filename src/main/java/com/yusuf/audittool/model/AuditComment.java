package com.yusuf.audittool.model;

public class AuditComment {

    private String id;
    private String body;
    private String authorName;
    private String createdAt;
    private String updatedAt;
    private Boolean visibilityRestricted;
    private String sourcePath;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getBody() {
        return body;
    }

    public void setBody(String body) {
        this.body = body;
    }

    public String getAuthorName() {
        return authorName;
    }

    public void setAuthorName(String authorName) {
        this.authorName = authorName;
    }

    public String getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(String createdAt) {
        this.createdAt = createdAt;
    }

    public String getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(String updatedAt) {
        this.updatedAt = updatedAt;
    }

    public Boolean getVisibilityRestricted() {
        return visibilityRestricted;
    }

    public void setVisibilityRestricted(Boolean visibilityRestricted) {
        this.visibilityRestricted = visibilityRestricted;
    }

    public String getSourcePath() {
        return sourcePath;
    }

    public void setSourcePath(String sourcePath) {
        this.sourcePath = sourcePath;
    }
}
