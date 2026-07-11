package com.yusuf.audittool.agent;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import com.yusuf.audittool.config.OllamaProperties;

class OllamaAgentClientTest {

    @Test
    void sendsPromptToOllamaAndReturnsItsResponse() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        OllamaAgentClient client = new OllamaAgentClient(
                builder.baseUrl("http://localhost:11434").build(), properties());

        server.expect(requestTo("http://localhost:11434/api/generate"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(content().json("""
                        {
                          "model": "qwen3:4b",
                          "prompt": "Review this issue",
                          "stream": false,
                          "think": false,
                          "options": {
                            "num_ctx": 8192,
                            "num_predict": 1200
                          }
                        }
                        """))
                .andRespond(withSuccess("""
                        { "response": "Review completed." }
                        """, MediaType.APPLICATION_JSON));

        String result = client.analyze("Review this issue");

        assertEquals("Review completed.", result);
        server.verify();
    }

    @Test
    void rejectsBlankPromptsBeforeCallingOllama() {
        OllamaAgentClient client = new OllamaAgentClient(properties());

        assertThrows(IllegalArgumentException.class, () -> client.analyze("  "));
    }

    @Test
    void rejectsAnEmptyAgentResponse() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        OllamaAgentClient client = new OllamaAgentClient(
                builder.baseUrl("http://localhost:11434").build(), properties());

        server.expect(requestTo("http://localhost:11434/api/generate"))
                .andRespond(withSuccess("{ \"response\": \"\" }", MediaType.APPLICATION_JSON));

        assertThrows(AgentRuntimeException.class, () -> client.analyze("Review this issue"));
        server.verify();
    }

    private OllamaProperties properties() {
        OllamaProperties properties = new OllamaProperties();
        properties.setUrl("http://localhost:11434");
        properties.setModel("qwen3:4b");
        properties.setContextWindow(8192);
        properties.setMaxOutputTokens(1200);
        return properties;
    }
}
