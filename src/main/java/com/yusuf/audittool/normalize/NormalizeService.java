package com.yusuf.audittool.normalize;

import org.springframework.stereotype.Service;

import com.yusuf.audittool.model.AgentContext;
import com.yusuf.audittool.model.AnalyzeRequest;
import com.yusuf.audittool.model.ChecklistContext;
import com.yusuf.audittool.model.SourceInfo;

@Service
public class NormalizeService {

    private final GenericJsonWalker jsonWalker;
    private final FieldClassifier fieldClassifier;
    private final SourceInfoExtractor sourceInfoExtractor;

    public NormalizeService(
            GenericJsonWalker jsonWalker,
            FieldClassifier fieldClassifier,
            SourceInfoExtractor sourceInfoExtractor
    ) {
        this.jsonWalker = jsonWalker;
        this.fieldClassifier = fieldClassifier;
        this.sourceInfoExtractor = sourceInfoExtractor;
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
        context.setChecklistContext(emptyChecklistContext());
        context.setStatistics(classification.getStatistics());

        return context;
    }

    private ChecklistContext emptyChecklistContext() {
        ChecklistContext checklistContext = new ChecklistContext();
        checklistContext.setProvided(false);
        return checklistContext;
    }
}
