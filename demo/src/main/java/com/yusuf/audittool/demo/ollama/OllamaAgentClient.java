package com.yusuf.audittool.demo.ollama;

import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import com.yusuf.audittool.agent.AgentRuntimeException;
import com.yusuf.audittool.demo.model.AgentOptions;

@Component
public class OllamaAgentClient implements AgentClient {

    private static final Map<String, Object> AUDIT_REPORT_SCHEMA = objectSchema(
            Map.of(
                    "summary", stringSchema(),
                    "findings", arraySchema(findingSchema()),
                    "observations", arraySchema(observationSchema()),
                    "recommendation", stringSchema()),
            List.of("summary", "findings", "observations", "recommendation"));

    private final RestClient restClient;
    private final OllamaProperties properties;

    @Autowired
    public OllamaAgentClient(OllamaProperties properties) {
        this(RestClient.builder()
                .baseUrl(properties.getUrl())
                .build(), properties);
    }

    OllamaAgentClient(RestClient restClient, OllamaProperties properties) {
        this.restClient = restClient;
        this.properties = properties;
    }

    @Override
    public String analyze(String prompt) {
        return analyze(prompt, null);
    }

    @Override
    public String analyze(String prompt, AgentOptions options) {
        if (prompt == null || prompt.isBlank()) {
            throw new IllegalArgumentException("Prompt must not be blank.");
        }

        String model = selectedModel(options);
        boolean thinkingEnabled = selectedThinking(options);

        try {
            OllamaGenerateResponse response = restClient.post()
                    .uri("/api/generate")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(new OllamaGenerateRequest(
                            model,
                            prompt,
                            false,
                            thinkingEnabled,
                            thinkingEnabled ? null : AUDIT_REPORT_SCHEMA,
                            Map.<String, Number>of(
                                    "num_ctx", properties.getContextWindow(),
                                    "num_predict", properties.getMaxOutputTokens(),
                                    "temperature", properties.getTemperature(),
                                    "seed", properties.getSeed(),
                                    "top_p", properties.getTopP(),
                                    "top_k", properties.getTopK()
                            )
                    ))
                    .retrieve()
                    .body(OllamaGenerateResponse.class);

            if (response == null || response.response() == null || response.response().isBlank()) {
                throw new AgentRuntimeException("Agent returned an empty response.");
            }

            return response.response().strip();
        } catch (RestClientException exception) {
            throw new AgentRuntimeException("Agent runtime is not available.", exception);
        }
    }

    private String selectedModel(AgentOptions options) {
        if (options == null || options.getModel() == null || options.getModel().isBlank()) {
            return properties.getModel();
        }
        return options.getModel().strip();
    }

    private boolean selectedThinking(AgentOptions options) {
        if (options == null || options.getThinkingEnabled() == null) {
            return properties.isThinkingEnabled();
        }
        return options.getThinkingEnabled();
    }

    private record OllamaGenerateRequest(
            String model,
            String prompt,
            boolean stream,
            boolean think,
            Object format,
            Map<String, Number> options
    ) {
    }

    private static Map<String, Object> findingSchema() {
        return objectSchema(
                Map.of(
                        "title", stringSchema(),
                        "category", stringSchema(),
                        "severity", Map.of("type", "string", "enum", List.of("High", "Medium", "Low")),
                        "evidence", arraySchema(stringSchema()),
                        "rationale", stringSchema(),
                        "recommendedAction", stringSchema()),
                List.of("title", "category", "severity", "evidence", "rationale", "recommendedAction"));
    }

    private static Map<String, Object> observationSchema() {
        return objectSchema(
                Map.of(
                        "type", Map.of("type", "string", "enum", List.of("Observation", "Insufficient Context")),
                        "description", stringSchema(),
                        "evidence", arraySchema(stringSchema())),
                List.of("type", "description", "evidence"));
    }

    private static Map<String, Object> stringSchema() {
        return Map.of("type", "string");
    }

    private static Map<String, Object> arraySchema(Map<String, Object> itemSchema) {
        return Map.of("type", "array", "items", itemSchema);
    }

    private static Map<String, Object> objectSchema(Map<String, Object> properties, List<String> requiredFields) {
        return Map.of(
                "type", "object",
                "properties", properties,
                "required", requiredFields,
                "additionalProperties", false);
    }

    private record OllamaGenerateResponse(String response) {
    }
}
