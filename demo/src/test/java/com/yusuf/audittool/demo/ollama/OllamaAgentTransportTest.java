package com.yusuf.audittool.demo.ollama;

import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import java.net.URI;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import com.yusuf.audittool.agent.AgentRuntimeException;
import com.yusuf.audittool.api.AgentEndpoint;
import com.yusuf.audittool.api.AuditInput;

import tools.jackson.databind.json.JsonMapper;

class OllamaAgentTransportTest {

    @Test
    void sendsPromptToTheProvidedEndpointAndReturnsOllamaResponse() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        OllamaAgentTransport transport = new OllamaAgentTransport(
                builder.build(), properties());
        AgentEndpoint endpoint = new AgentEndpoint(
                URI.create("http://localhost:11434/api/generate"),
                Map.of("Authorization", "Bearer test-token"));

        server.expect(requestTo("http://localhost:11434/api/generate"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header("Authorization", "Bearer test-token"))
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(content().json("""
                        {
                          "model": "qwen3:4b-instruct",
                          "prompt": "Review this record",
                          "stream": false,
                          "think": false,
                          "options": {
                            "num_ctx": 8192,
                            "num_predict": 1200,
                            "temperature": 0.2,
                            "seed": 42,
                            "top_p": 0.8,
                            "top_k": 20
                          }
                        }
                        """))
                .andRespond(withSuccess("""
                        { "response": "  ## Özet\\nKayıt incelendi.  " }
                        """, MediaType.APPLICATION_JSON));

        String result = transport.send("Review this record", endpoint);

        assertEquals("## Özet\nKayıt incelendi.", result);
        server.verify();
    }

    @Test
    void rejectsBlankPromptsBeforeCallingOllama() {
        OllamaAgentTransport transport = new OllamaAgentTransport(properties());

        assertThrows(
                IllegalArgumentException.class,
                () -> transport.send("  ", AgentEndpoint.of("http://localhost:11434/api/generate"))
        );
    }

    @Test
    void rejectsEmptyOllamaResponses() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        OllamaAgentTransport transport = new OllamaAgentTransport(
                builder.build(), properties());
        AgentEndpoint endpoint = AgentEndpoint.of("http://localhost:11434/api/generate");

        server.expect(requestTo("http://localhost:11434/api/generate"))
                .andRespond(withSuccess("{ \"response\": \"\" }", MediaType.APPLICATION_JSON));

        assertThrows(AgentRuntimeException.class, () -> transport.send("Review this record", endpoint));
        server.verify();
    }

    @Test
    void sendsThePromptBuiltByCoreToOllama() throws Exception {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        OllamaAgentTransport transport = new OllamaAgentTransport(
                builder.build(), properties());
        AgentEndpoint endpoint = AgentEndpoint.of("http://localhost:11434/api/generate");
        var engine = new com.yusuf.audittool.api.AuditEngine(transport);

        server.expect(requestTo("http://localhost:11434/api/generate"))
                .andExpect(content().string(containsString("REQ-701")))
                .andExpect(content().string(containsString("Verification Evidence")))
                .andExpect(content().string(containsString("## Özet")))
                .andRespond(withSuccess("""
                        { "response": "## Özet\\nCore transport testi tamamlandı." }
                        """, MediaType.APPLICATION_JSON));

        String result = engine.analyze(input(), endpoint);

        assertEquals("## Özet\nCore transport testi tamamlandı.", result);
        server.verify();
    }

    private OllamaProperties properties() {
        OllamaProperties properties = new OllamaProperties();
        properties.setModel("qwen3:4b-instruct");
        properties.setContextWindow(8192);
        properties.setMaxOutputTokens(1200);
        properties.setTemperature(0.2);
        properties.setSeed(42);
        properties.setThinkingEnabled(false);
        properties.setTopP(0.8);
        properties.setTopK(20);
        return properties;
    }

    private AuditInput input() throws Exception {
        JsonMapper mapper = new JsonMapper();
        return new AuditInput(
                mapper.readTree("""
                        {
                          "key": "REQ-701",
                          "fields": {
                            "summary": "Guidance software verification requirement",
                            "status": { "name": "Done" },
                            "customfield_21001": "Verification record TR-701 is attached."
                          }
                        }
                        """),
                mapper.readTree("""
                        {
                          "values": [
                            {
                              "fieldId": "customfield_21001",
                              "name": "Verification Evidence",
                              "description": "Evidence that the requirement was verified.",
                              "schema": { "type": "string" }
                            }
                          ]
                        }
                        """),
                null,
                mapper.readTree("""
                        ["Completed records must contain verification evidence."]
                        """)
        );
    }
}
