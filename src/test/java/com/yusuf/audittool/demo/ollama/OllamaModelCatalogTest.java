package com.yusuf.audittool.demo.ollama;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

class OllamaModelCatalogTest {

    @Test
    void mapsInstalledModelsAndThinkingCapabilities() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        OllamaProperties properties = properties();
        OllamaModelCatalog catalog = new OllamaModelCatalog(
                builder.baseUrl(properties.getUrl()).build(), properties);

        server.expect(requestTo("http://localhost:11434/api/tags"))
                .andRespond(withSuccess("""
                        {
                          "models": [
                            {
                              "name": "qwen3.5:4b",
                              "size": 3389983735,
                              "details": {
                                "parameter_size": "4.7B",
                                "quantization_level": "Q4_K_M"
                              },
                              "capabilities": ["completion", "thinking"]
                            },
                            {
                              "name": "qwen3:4b-instruct",
                              "size": 2497293803,
                              "details": {
                                "parameter_size": "4.0B",
                                "quantization_level": "Q4_K_M"
                              },
                              "capabilities": ["completion"]
                            }
                          ]
                        }
                        """, MediaType.APPLICATION_JSON));

        var response = catalog.getModels();

        assertEquals("qwen3:4b-instruct", response.defaultModel());
        assertEquals(2, response.models().size());
        assertTrue(response.models().get(0).defaultModel());
        assertFalse(response.models().get(0).supportsThinking());
        assertEquals("4.7B", response.models().get(1).parameterSize());
        assertTrue(response.models().get(1).supportsThinking());
        server.verify();
    }

    private OllamaProperties properties() {
        OllamaProperties properties = new OllamaProperties();
        properties.setUrl("http://localhost:11434");
        properties.setModel("qwen3:4b-instruct");
        return properties;
    }
}
