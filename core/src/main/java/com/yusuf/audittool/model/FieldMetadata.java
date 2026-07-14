package com.yusuf.audittool.model;

import java.util.ArrayList;
import java.util.List;

public class FieldMetadata {

    private boolean provided;
    private String id;
    private String name;
    private String schemaType;
    private String schemaSystem;
    private String schemaItems;
    private String customType;
    private String customId;
    private String descriptionTr;
    private Boolean required;
    private Boolean hasDefaultValue;
    private List<AllowedValue> allowedValues = new ArrayList<>();

    public boolean isProvided() {
        return provided;
    }

    public void setProvided(boolean provided) {
        this.provided = provided;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getSchemaType() {
        return schemaType;
    }

    public void setSchemaType(String schemaType) {
        this.schemaType = schemaType;
    }

    public String getSchemaSystem() {
        return schemaSystem;
    }

    public void setSchemaSystem(String schemaSystem) {
        this.schemaSystem = schemaSystem;
    }

    public String getSchemaItems() {
        return schemaItems;
    }

    public void setSchemaItems(String schemaItems) {
        this.schemaItems = schemaItems;
    }

    public String getCustomType() {
        return customType;
    }

    public void setCustomType(String customType) {
        this.customType = customType;
    }

    public String getCustomId() {
        return customId;
    }

    public void setCustomId(String customId) {
        this.customId = customId;
    }

    public String getDescriptionTr() {
        return descriptionTr;
    }

    public void setDescriptionTr(String descriptionTr) {
        this.descriptionTr = descriptionTr;
    }

    public Boolean getRequired() {
        return required;
    }

    public void setRequired(Boolean required) {
        this.required = required;
    }

    public Boolean getHasDefaultValue() {
        return hasDefaultValue;
    }

    public void setHasDefaultValue(Boolean hasDefaultValue) {
        this.hasDefaultValue = hasDefaultValue;
    }

    public List<AllowedValue> getAllowedValues() {
        return allowedValues;
    }

    public void setAllowedValues(List<AllowedValue> allowedValues) {
        this.allowedValues = allowedValues;
    }
}
