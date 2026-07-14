package com.yusuf.audittool.model;

public class EmptyField {

    private String path;
    private String parentPath;
    private String key;
    private String label;
    private String emptyType;
    private int depth;
    private FieldMetadata metadata;

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public String getParentPath() {
        return parentPath;
    }

    public void setParentPath(String parentPath) {
        this.parentPath = parentPath;
    }

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    public String getEmptyType() {
        return emptyType;
    }

    public void setEmptyType(String emptyType) {
        this.emptyType = emptyType;
    }

    public int getDepth() {
        return depth;
    }

    public void setDepth(int depth) {
        this.depth = depth;
    }

    public FieldMetadata getMetadata() {
        return metadata;
    }

    public void setMetadata(FieldMetadata metadata) {
        this.metadata = metadata;
    }
}

