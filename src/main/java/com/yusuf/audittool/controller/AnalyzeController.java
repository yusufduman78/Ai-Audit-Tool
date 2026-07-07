package com.yusuf.audittool.controller;

import java.util.Map;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class AnalyzeController {

    @GetMapping("/health")
    public Map<String, String> health() {
        return Map.of("status", "OK");
    }
}

