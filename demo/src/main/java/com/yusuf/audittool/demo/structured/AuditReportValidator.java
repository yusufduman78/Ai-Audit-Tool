package com.yusuf.audittool.demo.structured;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.springframework.stereotype.Component;

import com.yusuf.audittool.demo.model.AuditFinding;
import com.yusuf.audittool.demo.model.AuditReport;

@Component
public class AuditReportValidator {

    private static final Set<String> SEVERITIES = Set.of("High", "Medium", "Low");

    public List<String> validate(AuditReport report) {
        List<String> errors = new ArrayList<>();
        if (report.getSummary() == null || report.getSummary().isBlank()) errors.add("Report summary is required.");
        if (report.getFindings() == null || report.getObservations() == null) {
            errors.add("Report findings and observations arrays are required.");
            return errors;
        }
        Set<String> keys = new HashSet<>();
        for (AuditFinding finding : report.getFindings()) {
            if (finding.getTitle() == null || finding.getTitle().isBlank() || finding.getCategory() == null || finding.getCategory().isBlank() || finding.getRationale() == null || finding.getRationale().isBlank() || finding.getRecommendedAction() == null || finding.getRecommendedAction().isBlank()) errors.add("Each finding must include its required text fields.");
            if (!SEVERITIES.contains(finding.getSeverity())) errors.add("Finding severity must be High, Medium, or Low.");
            if (finding.getEvidence() == null || finding.getEvidence().isEmpty()) errors.add("Each finding must include evidence.");
            if (!keys.add((finding.getTitle() + "|" + finding.getCategory()).toLowerCase())) errors.add("Duplicate findings are not allowed.");
        }
        return errors;
    }
}
