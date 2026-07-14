package com.yusuf.audittool.demo.model;

import java.util.List;

public record AvailableModel(
        String name,
        long sizeBytes,
        String parameterSize,
        String quantizationLevel,
        List<String> capabilities,
        boolean supportsThinking,
        boolean defaultModel
) {
}
