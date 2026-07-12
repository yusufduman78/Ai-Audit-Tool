package com.yusuf.audittool.agent;

import java.util.Comparator;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import com.yusuf.audittool.config.OllamaProperties;
import com.yusuf.audittool.model.AvailableModel;
import com.yusuf.audittool.model.ModelCatalogResponse;

import com.fasterxml.jackson.annotation.JsonProperty;

@Component
public class OllamaModelCatalog {

    private final RestClient restClient;
    private final OllamaProperties properties;

    @Autowired
    public OllamaModelCatalog(OllamaProperties properties) {
        this(RestClient.builder().baseUrl(properties.getUrl()).build(), properties);
    }

    OllamaModelCatalog(RestClient restClient, OllamaProperties properties) {
        this.restClient = restClient;
        this.properties = properties;
    }

    public ModelCatalogResponse getModels() {
        try {
            OllamaTagsResponse response = restClient.get()
                    .uri("/api/tags")
                    .retrieve()
                    .body(OllamaTagsResponse.class);

            List<AvailableModel> models = response == null || response.models() == null
                    ? List.of()
                    : response.models().stream()
                            .map(this::toAvailableModel)
                            .sorted(Comparator
                                    .comparing(AvailableModel::defaultModel).reversed()
                                    .thenComparing(AvailableModel::name))
                            .toList();

            return new ModelCatalogResponse(
                    properties.getModel(),
                    properties.isThinkingEnabled(),
                    models
            );
        } catch (RestClientException exception) {
            throw new AgentRuntimeException("Agent model catalog is not available.", exception);
        }
    }

    private AvailableModel toAvailableModel(OllamaModel model) {
        OllamaModelDetails details = model.details();
        return new AvailableModel(
                model.name(),
                model.size(),
                details == null ? null : details.parameterSize(),
                details == null ? null : details.quantizationLevel(),
                model.capabilities() == null ? List.of() : List.copyOf(model.capabilities()),
                model.capabilities() != null && model.capabilities().contains("thinking"),
                properties.getModel().equals(model.name())
        );
    }

    private record OllamaTagsResponse(List<OllamaModel> models) {
    }

    private record OllamaModel(
            String name,
            long size,
            OllamaModelDetails details,
            List<String> capabilities
    ) {
    }

    private record OllamaModelDetails(
            @JsonProperty("parameter_size") String parameterSize,
            @JsonProperty("quantization_level") String quantizationLevel
    ) {
    }
}
