package com.yusuf.audittool.metadata;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;

import com.yusuf.audittool.model.AllowedValue;
import com.yusuf.audittool.model.ContextStatistics;
import com.yusuf.audittool.model.EmptyField;
import com.yusuf.audittool.model.NormalizedField;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

class MetadataMapperTest {

    private final MetadataMapper mapper = new MetadataMapper();
    private final JsonMapper jsonMapper = new JsonMapper();

    @Test
    void enrichesFieldsByKeyAndMergesDescription() throws Exception {
        NormalizedField summary = activeField("fields.summary", "summary", "summary");
        EmptyField acceptanceCriteria = emptyField("fields.customfield_13104", "customfield_13104", "customfield_13104");
        ContextStatistics statistics = new ContextStatistics();

        JsonNode metadata = jsonMapper.readTree("""
                {
                  "summary": {
                    "name": "Summary",
                    "schemaType": "string",
                    "required": true
                  },
                  "customfield_13104": {
                    "name": "Acceptance Criteria",
                    "schemaType": "string"
                  }
                }
                """);
        JsonNode descriptions = jsonMapper.readTree("""
                {
                  "customfield_13104": "Requirement kabul kriterlerini belirtir."
                }
                """);

        mapper.enrich(List.of(summary), List.of(acceptanceCriteria), metadata, descriptions, statistics);

        assertTrue(summary.getMetadata().isProvided());
        assertEquals("Summary", summary.getMetadata().getName());
        assertEquals("string", summary.getMetadata().getSchemaType());
        assertEquals(true, summary.getMetadata().getRequired());

        assertTrue(acceptanceCriteria.getMetadata().isProvided());
        assertEquals("Acceptance Criteria", acceptanceCriteria.getMetadata().getName());
        assertEquals("Requirement kabul kriterlerini belirtir.", acceptanceCriteria.getMetadata().getDescriptionTr());
        assertEquals(2, statistics.getMetadataMatchedCount());
        assertEquals(0, statistics.getMetadataMissingCount());
    }

    @Test
    void leavesMetadataMissingWhenNoMatchExists() {
        NormalizedField field = activeField("fields.unknown", "unknown", "unknown");
        ContextStatistics statistics = new ContextStatistics();

        mapper.enrich(List.of(field), List.of(), null, null, statistics);

        assertFalse(field.getMetadata().isProvided());
        assertEquals(0, statistics.getMetadataMatchedCount());
        assertEquals(1, statistics.getMetadataMissingCount());
    }

    @Test
    void matchesByMetadataNameFallback() throws Exception {
        NormalizedField field = activeField("fields.customfield_20000", "customfield_20000", "Risk Level");
        ContextStatistics statistics = new ContextStatistics();

        JsonNode metadata = jsonMapper.readTree("""
                {
                  "risk": {
                    "name": "Risk Level",
                    "schemaType": "option"
                  }
                }
                """);

        mapper.enrich(List.of(field), List.of(), metadata, null, statistics);

        assertTrue(field.getMetadata().isProvided());
        assertEquals("Risk Level", field.getMetadata().getName());
        assertEquals("option", field.getMetadata().getSchemaType());
    }

    @Test
    void readsJiraPagedFieldMetadataValues() throws Exception {
        NormalizedField field = activeField("fields.customfield_13104", "customfield_13104", "customfield_13104");
        ContextStatistics statistics = new ContextStatistics();

        JsonNode metadata = jsonMapper.readTree("""
                {
                  "maxResults": 50,
                  "startAt": 0,
                  "total": 34,
                  "isLast": true,
                  "values": [
                    {
                      "id": "customfield_13104",
                      "key": "customfield_13104",
                      "name": "Acceptance Criteria",
                      "schema": {
                        "type": "string",
                        "custom": "com.atlassian.jira.plugin.system.customfieldtypes:textarea"
                      },
                      "clauseNames": [
                        "Acceptance Criteria",
                        "cf[13104]"
                      ]
                    }
                  ]
                }
                """);

        mapper.enrich(List.of(field), List.of(), metadata, null, statistics);

        assertTrue(field.getMetadata().isProvided());
        assertEquals("customfield_13104", field.getMetadata().getId());
        assertEquals("Acceptance Criteria", field.getMetadata().getName());
        assertEquals("string", field.getMetadata().getSchemaType());
        assertEquals("com.atlassian.jira.plugin.system.customfieldtypes:textarea", field.getMetadata().getCustomType());
        assertEquals(1, statistics.getMetadataMatchedCount());
        assertEquals(0, statistics.getMetadataMissingCount());
    }

    @Test
    void readsJiraFieldIdAndAllowedValueNames() throws Exception {
        NormalizedField field = activeField("fields.issuetype.name", "name", "issuetype");
        ContextStatistics statistics = new ContextStatistics();

        JsonNode metadata = jsonMapper.readTree("""
                {
                  "required": true,
                  "schema": {
                    "type": "issuetype",
                    "system": "issuetype"
                  },
                  "name": "Issue Type",
                  "fieldId": "issuetype",
                  "hasDefaultValue": false,
                  "operations": [],
                  "allowedValues": [
                    {
                      "self": "x",
                      "id": "10903",
                      "description": "",
                      "iconUrl": "x",
                      "name": "SPCR",
                      "subtask": false,
                      "avatarId": 10313
                    }
                  ]
                }
                """);

        mapper.enrich(List.of(field), List.of(), metadata, null, statistics);

        assertTrue(field.getMetadata().isProvided());
        assertEquals("issuetype", field.getMetadata().getId());
        assertEquals("Issue Type", field.getMetadata().getName());
        assertEquals("issuetype", field.getMetadata().getSchemaType());
        assertEquals("issuetype", field.getMetadata().getSchemaSystem());
        assertEquals(true, field.getMetadata().getRequired());
        assertEquals(false, field.getMetadata().getHasDefaultValue());

        AllowedValue allowedValue = field.getMetadata().getAllowedValues().get(0);
        assertEquals("10903", allowedValue.getId());
        assertEquals("SPCR", allowedValue.getValue());
        assertEquals("", allowedValue.getDescription());
    }

    @Test
    void preservesUsefulOptionDescriptionsWithoutUiLinks() throws Exception {
        NormalizedField field = activeField("fields.customfield_20000.value", "value", "Risk Level");
        ContextStatistics statistics = new ContextStatistics();

        JsonNode metadata = jsonMapper.readTree("""
                {
                  "fieldId": "customfield_20000",
                  "name": "Risk Level",
                  "schema": {
                    "type": "option",
                    "custom": "com.atlassian.jira.plugin.system.customfieldtypes:select",
                    "customId": 20000
                  },
                  "allowedValues": [
                    {
                      "self": "https://jira.example.com/rest/api/2/customFieldOption/30003",
                      "value": "High",
                      "id": "30003",
                      "description": "Yüksek riskli ve detaylı inceleme gerektiren değişiklik."
                    }
                  ]
                }
                """);

        mapper.enrich(List.of(field), List.of(), metadata, null, statistics);

        assertTrue(field.getMetadata().isProvided());
        assertEquals("customfield_20000", field.getMetadata().getId());
        assertEquals("option", field.getMetadata().getSchemaType());
        assertEquals("com.atlassian.jira.plugin.system.customfieldtypes:select", field.getMetadata().getCustomType());
        assertEquals("20000", field.getMetadata().getCustomId());

        AllowedValue allowedValue = field.getMetadata().getAllowedValues().get(0);
        assertEquals("30003", allowedValue.getId());
        assertEquals("High", allowedValue.getValue());
        assertEquals("Yüksek riskli ve detaylı inceleme gerektiren değişiklik.", allowedValue.getDescription());
    }

    @Test
    void readsFieldsFromJiraCreateMetaResponse() throws Exception {
        NormalizedField field = activeField("fields.customfield_20001", "customfield_20001", "customfield_20001");
        ContextStatistics statistics = new ContextStatistics();

        JsonNode metadata = jsonMapper.readTree("""
                {
                  "projects": [
                    {
                      "id": "10000",
                      "key": "AUD",
                      "name": "Audit Project",
                      "issuetypes": [
                        {
                          "id": "10903",
                          "name": "SPCR",
                          "fields": {
                            "customfield_20001": {
                              "required": false,
                              "schema": {
                                "type": "array",
                                "items": "option",
                                "custom": "com.atlassian.jira.plugin.system.customfieldtypes:multiselect",
                                "customId": 20001
                              },
                              "name": "Affected Modules",
                              "fieldId": "customfield_20001",
                              "hasDefaultValue": false,
                              "operations": [
                                "add",
                                "set",
                                "remove"
                              ],
                              "allowedValues": [
                                {
                                  "self": "https://jira.example.com/rest/api/2/customFieldOption/30100",
                                  "value": "Authentication",
                                  "id": "30100"
                                }
                              ]
                            }
                          }
                        }
                      ]
                    }
                  ]
                }
                """);

        mapper.enrich(List.of(field), List.of(), metadata, null, statistics);

        assertTrue(field.getMetadata().isProvided());
        assertEquals("Affected Modules", field.getMetadata().getName());
        assertEquals("array", field.getMetadata().getSchemaType());
        assertEquals("option", field.getMetadata().getSchemaItems());
        assertEquals("20001", field.getMetadata().getCustomId());
        assertEquals(false, field.getMetadata().getHasDefaultValue());
        assertEquals("Authentication", field.getMetadata().getAllowedValues().get(0).getValue());
    }

    @Test
    void matchesArrayItemsWithTheirOwningFieldMetadata() throws Exception {
        NormalizedField field = activeField("record.affectedModules[0]", "[0]", "record.affectedModules[0]");
        ContextStatistics statistics = new ContextStatistics();

        JsonNode metadata = jsonMapper.readTree("""
                {
                  "values": [
                    {
                      "fieldId": "affectedModules",
                      "name": "Affected Modules",
                      "schema": {
                        "type": "array",
                        "items": "option"
                      }
                    }
                  ]
                }
                """);

        mapper.enrich(List.of(field), List.of(), metadata, null, statistics);

        assertTrue(field.getMetadata().isProvided());
        assertEquals("affectedModules", field.getMetadata().getId());
        assertEquals("Affected Modules", field.getMetadata().getName());
        assertEquals("array", field.getMetadata().getSchemaType());
        assertEquals("option", field.getMetadata().getSchemaItems());
        assertEquals(1, statistics.getMetadataMatchedCount());
        assertEquals(0, statistics.getMetadataMissingCount());
    }

    private NormalizedField activeField(String path, String key, String label) {
        NormalizedField field = new NormalizedField();
        field.setPath(path);
        field.setKey(key);
        field.setLabel(label);
        return field;
    }

    private EmptyField emptyField(String path, String key, String label) {
        EmptyField field = new EmptyField();
        field.setPath(path);
        field.setKey(key);
        field.setLabel(label);
        return field;
    }
}
