package com.yusuf.audittool.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.yusuf.audittool.agent.OllamaModelCatalog;
import com.yusuf.audittool.model.ModelCatalogResponse;

@RestController
@RequestMapping("/api/models")
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
