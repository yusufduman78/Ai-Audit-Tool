package com.yusuf.audittool.api;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.net.URI;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.Test;

import com.yusuf.audittool.agent.AgentRuntimeException;
import com.yusuf.audittool.agent.AgentTransport;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

class AuditEngineTest {

    private final JsonMapper jsonMapper = new JsonMapper();

    @Test
    void sendsBuiltPromptToProvidedEndpointAndReturnsTextResponse() throws Exception {
        AtomicReference<String> sentPrompt = new AtomicReference<>();
        AtomicReference<AgentEndpoint> sentEndpoint = new AtomicReference<>();
        AgentTransport transport = (prompt, endpoint) -> {
            sentPrompt.set(prompt);
            sentEndpoint.set(endpoint);
            return "  ## Ozet\nKayit incelendi.  ";
        };
        AuditEngine engine = new AuditEngine(transport);
        AgentEndpoint endpoint = new AgentEndpoint(
                URI.create("https://ai.example.test/message"),
                Map.of("Authorization", "Bearer test-token")
        );

        String result = engine.analyze(input(), endpoint);

        assertEquals("## Ozet\nKayit incelendi.", result);
        assertEquals(endpoint, sentEndpoint.get());
        assertEquals("Bearer test-token", sentEndpoint.get().getHeaders().get("Authorization"));
        assertTrue(sentPrompt.get().contains("REQ-401"));
        assertTrue(sentPrompt.get().contains("Navigation requirement"));
        assertTrue(sentPrompt.get().contains("Verification Evidence"));
        assertTrue(sentPrompt.get().contains("Evidence must be provided for completed records."));
        assertTrue(sentPrompt.get().contains("## Özet"));
        assertTrue(sentPrompt.get().contains("## Bulgular"));
        assertTrue(!sentPrompt.get().contains("valid JSON object"));
    }

    @Test
    void rejectsEmptyTransportResponse() throws Exception {
        AuditEngine engine = new AuditEngine((prompt, endpoint) -> "  ");

        AgentRuntimeException exception = assertThrows(
                AgentRuntimeException.class,
                () -> engine.analyze(input(), AgentEndpoint.of("https://ai.example.test/message"))
        );

        assertEquals("Agent returned an empty response.", exception.getMessage());
    }

    @Test
    void endpointRequiresAbsoluteHttpUrl() {
        assertThrows(IllegalArgumentException.class, () -> AgentEndpoint.of("/message"));
        assertThrows(IllegalArgumentException.class, () -> AgentEndpoint.of("ftp://ai.example.test/message"));
    }

    private AuditInput input() throws Exception {
        JsonNode payload = jsonMapper.readTree("""
                {
                  "key": "REQ-401",
                  "fields": {
                    "summary": "Navigation requirement",
                    "customfield_21001": "Test report TR-401"
                  }
                }
                """);
        JsonNode metadata = jsonMapper.readTree("""
                {
                  "values": [
                    {
                      "fieldId": "customfield_21001",
                      "name": "Verification Evidence",
                      "schema": { "type": "string" }
                    }
                  ]
                }
                """);
        JsonNode checklist = jsonMapper.readTree("""
                ["Evidence must be provided for completed records."]
                """);

        return new AuditInput(payload, metadata, null, checklist);
    }
}
