package com.yusuf.audittool.demo.ollama;

import java.util.Map;
import java.util.Objects;

import org.springframework.http.MediaType;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import com.yusuf.audittool.agent.AgentRuntimeException;
import com.yusuf.audittool.agent.AgentTransport;
import com.yusuf.audittool.api.AgentEndpoint;

/**
 * Demo-only adapter that sends the core library prompt to an Ollama generate endpoint.
 */
public class OllamaAgentTransport implements AgentTransport {

    private final RestClient restClient;
    private final OllamaProperties properties;

    public OllamaAgentTransport(OllamaProperties properties) {
        this(RestClient.create(), properties);
    }

    OllamaAgentTransport(RestClient restClient, OllamaProperties properties) {
        this.restClient = Objects.requireNonNull(restClient, "Rest client must not be null.");
        this.properties = Objects.requireNonNull(properties, "Ollama properties must not be null.");
    }

    @Override
    public String send(String prompt, AgentEndpoint endpoint) {
        if (prompt == null || prompt.isBlank()) {
            throw new IllegalArgumentException("Prompt must not be blank.");
        }
        Objects.requireNonNull(endpoint, "Agent endpoint must not be null.");

        try {
            OllamaGenerateResponse response = restClient.post()
                    .uri(endpoint.getUri())
                    .headers(headers -> endpoint.getHeaders().forEach(headers::set))
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(new OllamaGenerateRequest(
                            requiredModel(),
                            prompt,
                            false,
                            properties.isThinkingEnabled(),
                            generationOptions()
                    ))
                    .retrieve()
                    .body(OllamaGenerateResponse.class);

            if (response == null || response.response() == null || response.response().isBlank()) {
                throw new AgentRuntimeException("Agent returned an empty response.");
            }
            return response.response().strip();
        } catch (RestClientException exception) {
            throw new AgentRuntimeException("Ollama transport request failed.", exception);
        }
    }

    private String requiredModel() {
        String model = properties.getModel();
        if (model == null || model.isBlank()) {
            throw new IllegalStateException("Ollama model must be configured for the demo transport.");
        }
        return model.strip();
    }

    private Map<String, Number> generationOptions() {
        return Map.of(
                "num_ctx", properties.getContextWindow(),
                "num_predict", properties.getMaxOutputTokens(),
                "temperature", properties.getTemperature(),
                "seed", properties.getSeed(),
                "top_p", properties.getTopP(),
                "top_k", properties.getTopK()
        );
    }

    private record OllamaGenerateRequest(
            String model,
            String prompt,
            boolean stream,
            boolean think,
            Map<String, Number> options
    ) {
    }

    private record OllamaGenerateResponse(String response) {
    }
}
