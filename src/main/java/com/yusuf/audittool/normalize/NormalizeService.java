package com.yusuf.audittool.normalize;

import org.springframework.stereotype.Service;

import com.yusuf.audittool.model.AgentContext;
import com.yusuf.audittool.model.AnalyzeRequest;
import com.yusuf.audittool.checklist.ChecklistMapper;
import com.yusuf.audittool.model.SourceInfo;

@Service
public class NormalizeService {

    private final GenericJsonWalker jsonWalker;
    private final FieldClassifier fieldClassifier;
    private final SourceInfoExtractor sourceInfoExtractor;
    private final ChecklistMapper checklistMapper;

    public NormalizeService(
            GenericJsonWalker jsonWalker,
            FieldClassifier fieldClassifier,
            SourceInfoExtractor sourceInfoExtractor,
            ChecklistMapper checklistMapper
    ) {
        this.jsonWalker = jsonWalker;
        this.fieldClassifier = fieldClassifier;
        this.sourceInfoExtractor = sourceInfoExtractor;
        this.checklistMapper = checklistMapper;
    }

    public AgentContext normalize(AnalyzeRequest request) {
        if (request == null || request.getPayload() == null || request.getPayload().isNull()) {
            throw new IllegalArgumentException("Payload is required.");
        }

        FieldClassification classification = fieldClassifier.classify(jsonWalker.walk(request.getPayload()));

        AgentContext context = new AgentContext();
        context.setSourceInfo(sourceInfoExtractor.extract(request.getPayload()));
        context.setActiveFields(classification.getActiveFields());
        context.setEmptyFields(classification.getEmptyFields());
        context.setChecklistContext(checklistMapper.map(request.getChecklist()));
        context.setStatistics(classification.getStatistics());

        return context;
    }
}
