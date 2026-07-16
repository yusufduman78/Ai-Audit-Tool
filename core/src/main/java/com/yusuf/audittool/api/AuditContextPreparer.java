package com.yusuf.audittool.api;

import java.util.Objects;

import com.yusuf.audittool.checklist.ChecklistMapper;
import com.yusuf.audittool.metadata.MetadataMapper;
import com.yusuf.audittool.model.AgentContext;
import com.yusuf.audittool.normalize.CommentExtractor;
import com.yusuf.audittool.normalize.FieldClassifier;
import com.yusuf.audittool.normalize.GenericJsonWalker;
import com.yusuf.audittool.normalize.NormalizeService;
import com.yusuf.audittool.normalize.SourceInfoExtractor;
import com.yusuf.audittool.prompt.AgentContextRenderer;

public final class AuditContextPreparer {

    private final NormalizeService normalizeService;
    private final AgentContextRenderer contextRenderer;

    public AuditContextPreparer() {
        this(defaultNormalizeService(), new AgentContextRenderer());
    }

    AuditContextPreparer(
            NormalizeService normalizeService,
            AgentContextRenderer contextRenderer
    ) {
        this.normalizeService = Objects.requireNonNull(normalizeService, "Normalize service must not be null.");
        this.contextRenderer = Objects.requireNonNull(contextRenderer, "Context renderer must not be null.");
    }

    public AgentContext normalize(AuditInput input) {
        return normalizeService.normalize(input);
    }

    public String prepare(AuditInput input) {
        return contextRenderer.render(normalize(input));
    }

    static NormalizeService defaultNormalizeService() {
        return new NormalizeService(
                new GenericJsonWalker(),
                new FieldClassifier(),
                new CommentExtractor(),
                new SourceInfoExtractor(),
                new ChecklistMapper(),
                new MetadataMapper()
        );
    }
}
