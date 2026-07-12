package com.yusuf.audittool.agent;

import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import com.yusuf.audittool.config.OllamaProperties;

@Component
public class OllamaAgentClient implements AgentClient {

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
        if (prompt == null || prompt.isBlank()) {
            throw new IllegalArgumentException("Prompt must not be blank.");
        }

        try {
            OllamaGenerateResponse response = restClient.post()
                    .uri("/api/generate")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(new OllamaGenerateRequest(
                            properties.getModel(),
                            prompt,
                            false,
                            false,
                            "json",
                            Map.of(
                                    "num_ctx", properties.getContextWindow(),
                                    "num_predict", properties.getMaxOutputTokens()
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

    private record OllamaGenerateRequest(
            String model,
            String prompt,
            boolean stream,
            boolean think,
            String format,
            Map<String, Integer> options
    ) {
    }

    private record OllamaGenerateResponse(String response) {
    }
}
