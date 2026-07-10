package com.yusuf.audittool.normalize;

import org.springframework.stereotype.Service;

import com.yusuf.audittool.model.AgentContext;
import com.yusuf.audittool.model.AnalyzeRequest;
import com.yusuf.audittool.checklist.ChecklistMapper;
import com.yusuf.audittool.metadata.MetadataMapper;

@Service
public class NormalizeService {

    private final GenericJsonWalker jsonWalker;
    private final FieldClassifier fieldClassifier;
    private final CommentExtractor commentExtractor;
    private final SourceInfoExtractor sourceInfoExtractor;
    private final ChecklistMapper checklistMapper;
    private final MetadataMapper metadataMapper;

    public NormalizeService(
            GenericJsonWalker jsonWalker,
            FieldClassifier fieldClassifier,
            CommentExtractor commentExtractor,
            SourceInfoExtractor sourceInfoExtractor,
            ChecklistMapper checklistMapper,
            MetadataMapper metadataMapper
    ) {
        this.jsonWalker = jsonWalker;
        this.fieldClassifier = fieldClassifier;
        this.commentExtractor = commentExtractor;
        this.sourceInfoExtractor = sourceInfoExtractor;
        this.checklistMapper = checklistMapper;
        this.metadataMapper = metadataMapper;
    }

    public AgentContext normalize(AnalyzeRequest request) {
        if (request == null || request.getPayload() == null || request.getPayload().isNull()) {
            throw new IllegalArgumentException("Payload is required.");
        }

        CommentExtraction commentExtraction = commentExtractor.extract(request.getPayload());
        FieldClassification classification = fieldClassifier.classify(
                jsonWalker.walk(request.getPayload()),
                commentExtraction.excludedPathPrefixes()
        );
        metadataMapper.enrich(
                classification.getActiveFields(),
                classification.getEmptyFields(),
                request.getMetadata(),
                request.getFieldDescriptions(),
                classification.getStatistics()
        );

        AgentContext context = new AgentContext();
        context.setSourceInfo(sourceInfoExtractor.extract(request.getPayload()));
        context.setActiveFields(classification.getActiveFields());
        context.setEmptyFields(classification.getEmptyFields());
        context.setCommentContext(commentExtraction.commentContext());
        context.setChecklistContext(checklistMapper.map(request.getChecklist()));
        context.setStatistics(classification.getStatistics());

        return context;
    }
}
