package com.yusuf.audittool.service;

import org.springframework.stereotype.Service;

import com.yusuf.audittool.model.AnalyzeRequest;
import com.yusuf.audittool.model.NormalizeResponse;
import com.yusuf.audittool.normalize.NormalizeService;

@Service
public class AuditService {

    private final NormalizeService normalizeService;

    public AuditService(NormalizeService normalizeService) {
        this.normalizeService = normalizeService;
    }

    public NormalizeResponse normalize(AnalyzeRequest request) {
        return new NormalizeResponse(normalizeService.normalize(request));
    }
}

