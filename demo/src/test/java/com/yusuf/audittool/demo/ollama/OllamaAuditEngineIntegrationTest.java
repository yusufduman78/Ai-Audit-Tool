package com.yusuf.audittool.demo.ollama;

import static org.junit.jupiter.api.Assertions.assertFalse;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;

import com.yusuf.audittool.api.AgentEndpoint;
import com.yusuf.audittool.api.AuditEngine;
import com.yusuf.audittool.api.AuditInput;

import tools.jackson.databind.json.JsonMapper;

/**
 * Manual smoke test. It contacts a locally running Ollama server only when explicitly enabled.
 */
@EnabledIfEnvironmentVariable(named = "RUN_OLLAMA_INTEGRATION", matches = "true")
class OllamaAuditEngineIntegrationTest {

    @Test
    void sendsACompleteCoreAuditPromptToLocalOllama() throws Exception {
        OllamaProperties properties = new OllamaProperties();
        properties.setModel("qwen3:4b-instruct");
        properties.setContextWindow(8192);
        properties.setMaxOutputTokens(700);
        properties.setTemperature(0.2);
        properties.setSeed(42);
        properties.setThinkingEnabled(false);
        properties.setTopP(0.8);
        properties.setTopK(20);

        AuditEngine engine = new AuditEngine(new OllamaAgentTransport(properties));
        JsonMapper mapper = new JsonMapper();
        AuditInput input = new AuditInput(
                mapper.readTree("""
                        {
                          "key": "REQ-OLLAMA-001",
                          "fields": {
                            "summary": "Guidance software startup verification requirement",
                            "status": { "name": "Done" },
                            "customfield_21001": ""
                          }
                        }
                        """),
                mapper.readTree("""
                        {
                          "values": [
                            {
                              "fieldId": "customfield_21001",
                              "name": "Verification Evidence",
                              "required": true,
                              "description": "Evidence proving that the completed requirement was verified.",
                              "schema": { "type": "string" }
                            }
                          ]
                        }
                        """),
                null,
                mapper.readTree("""
                        ["Completed records must include verification evidence."]
                        """)
        );
        String report = engine.analyze(
                input,
                AgentEndpoint.of("http://localhost:11434/api/generate")
        );

        assertFalse(report.isBlank());
        System.out.println("\n--- Core / Ollama integration report ---\n" + report);
    }
}
