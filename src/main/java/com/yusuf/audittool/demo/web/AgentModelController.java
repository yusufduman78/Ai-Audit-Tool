package com.yusuf.audittool.demo.web;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.yusuf.audittool.demo.model.ModelCatalogResponse;
import com.yusuf.audittool.demo.ollama.OllamaModelCatalog;

@RestController
@RequestMapping("/demo/api/models")
public class AgentModelController {

    private final OllamaModelCatalog modelCatalog;

    public AgentModelController(OllamaModelCatalog modelCatalog) {
        this.modelCatalog = modelCatalog;
    }

    @GetMapping
    public ModelCatalogResponse getModels() {
        return modelCatalog.getModels();
    }
}
