package com.yusuf.audittool.model;

import java.util.List;

public record ModelCatalogResponse(
        String defaultModel,
        boolean defaultThinkingEnabled,
        List<AvailableModel> models
) {
}
