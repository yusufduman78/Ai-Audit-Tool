package com.yusuf.audittool.controller;

import java.util.Map;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.yusuf.audittool.model.AnalyzeRequest;
import com.yusuf.audittool.model.NormalizeResponse;
import com.yusuf.audittool.service.AuditService;

@RestController
@RequestMapping("/api")
public class AnalyzeController {

    private final AuditService auditService;

    public AnalyzeController(AuditService auditService) {
        this.auditService = auditService;
    }

    @GetMapping("/health")
    public Map<String, String> health() {
        return Map.of("status", "OK");
    }

    @PostMapping("/normalize")
    public NormalizeResponse normalize(@RequestBody AnalyzeRequest request) {
        return auditService.normalize(request);
    }
}
