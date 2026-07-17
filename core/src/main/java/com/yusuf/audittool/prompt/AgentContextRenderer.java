package com.yusuf.audittool.prompt;

import java.util.List;

import org.springframework.stereotype.Component;

import com.yusuf.audittool.model.AllowedValue;
import com.yusuf.audittool.model.AgentContext;
import com.yusuf.audittool.model.AuditComment;
import com.yusuf.audittool.model.ChecklistContext;
import com.yusuf.audittool.model.ChecklistItem;
import com.yusuf.audittool.model.CommentContext;
import com.yusuf.audittool.model.EmptyField;
import com.yusuf.audittool.model.FieldMetadata;
import com.yusuf.audittool.model.NormalizedField;
import com.yusuf.audittool.model.SourceInfo;

@Component
public class AgentContextRenderer {

    public String render(AgentContext context) {
        StringBuilder output = new StringBuilder();

        renderSourceInfo(context.getSourceInfo(), output);
        renderActiveFields(context.getActiveFields(), output);
        renderEmptyFields(context.getEmptyFields(), output);
        renderComments(context.getCommentContext(), output);
        renderChecklist(context.getChecklistContext(), output);

        return output.toString().trim();
    }

    private void renderSourceInfo(SourceInfo sourceInfo, StringBuilder output) {
        output.append("ENTITY\n");
        appendLine(output, "ID", sourceInfo == null ? null : sourceInfo.getEntityId());
        appendLine(output, "Label", sourceInfo == null ? null : sourceInfo.getEntityLabel());
        output.append('\n');
    }

    private void renderActiveFields(List<NormalizedField> fields, StringBuilder output) {
        output.append("ACTIVE FIELDS\n");
        if (fields == null || fields.isEmpty()) {
            output.append("- None\n\n");
            return;
        }

        for (NormalizedField field : fields) {
            FieldMetadata metadata = field.getMetadata();
            output.append("- ").append(escaped(displayName(field.getLabel(), metadata))).append('\n');
            appendIndented(output, "Path", field.getPath());
            appendIndented(output, "Value", field.getValue());
            appendIndented(output, "Detected Type", field.getValueType());
            renderMetadata(metadata, output);
        }
        output.append('\n');
    }

    private void renderEmptyFields(List<EmptyField> fields, StringBuilder output) {
        output.append("EMPTY FIELDS\n");
        if (fields == null || fields.isEmpty()) {
            output.append("- None\n\n");
            return;
        }

        for (EmptyField field : fields) {
            FieldMetadata metadata = field.getMetadata();
            output.append("- ").append(escaped(displayName(field.getLabel(), metadata))).append('\n');
            appendIndented(output, "Path", field.getPath());
            appendIndented(output, "Empty Type", field.getEmptyType());
            renderMetadata(metadata, output);
        }
        output.append('\n');
    }

    private void renderMetadata(FieldMetadata metadata, StringBuilder output) {
        if (metadata == null || !metadata.isProvided()) {
            return;
        }

        appendIndented(output, "Metadata ID", metadata.getId());
        appendIndented(output, "Schema Type", metadata.getSchemaType());
        appendIndented(output, "Schema System", metadata.getSchemaSystem());
        appendIndented(output, "Schema Items", metadata.getSchemaItems());
        appendIndented(output, "Custom Type", metadata.getCustomType());
        appendIndented(output, "Custom ID", metadata.getCustomId());
        appendIndented(output, "Required", text(metadata.getRequired()));
        appendIndented(output, "Has Default", text(metadata.getHasDefaultValue()));
        appendIndented(output, "Description", metadata.getDescriptionTr());
        renderAllowedValues(metadata.getAllowedValues(), output);
    }

    private void renderAllowedValues(List<AllowedValue> allowedValues, StringBuilder output) {
        if (allowedValues == null || allowedValues.isEmpty()) {
            return;
        }

        output.append("  Allowed Values:\n");
        for (AllowedValue allowedValue : allowedValues) {
            output.append("    - ")
                    .append(escaped(nonBlank(allowedValue.getValue(), allowedValue.getId())))
                    .append('\n');
            appendNested(output, "ID", allowedValue.getId());
            appendNested(output, "Description", allowedValue.getDescription());
        }
    }

    private void renderChecklist(ChecklistContext checklistContext, StringBuilder output) {
        output.append("CHECKLIST\n");
        if (checklistContext == null || !checklistContext.isProvided()) {
            output.append("- Not provided\n\n");
            return;
        }

        List<ChecklistItem> items = checklistContext.getItems();
        if (items != null && !items.isEmpty()) {
            for (ChecklistItem item : items) {
                output.append("- ").append(escaped(item.getText())).append('\n');
            }
        } else if (hasText(checklistContext.getRawText())) {
            output.append(escaped(checklistContext.getRawText())).append('\n');
        } else {
            output.append("- Provided but empty\n");
        }
        output.append('\n');
    }

    private void renderComments(CommentContext commentContext, StringBuilder output) {
        output.append("COMMENTS\n");
        if (commentContext == null || !commentContext.isProvided()) {
            output.append("- Not provided\n\n");
            return;
        }

        appendLine(output, "Coverage", text(commentContext.getCoverage()));
        appendLine(output, "Included", String.valueOf(commentContext.getIncludedCount()));
        if (commentContext.getTotalCount() != null) {
            appendLine(output, "Reported Total", String.valueOf(commentContext.getTotalCount()));
        }

        List<AuditComment> comments = commentContext.getComments();
        if (comments == null || comments.isEmpty()) {
            output.append("- Provided but empty\n\n");
            return;
        }

        for (AuditComment comment : comments) {
            output.append("- Comment\n");
            appendIndented(output, "Author", comment.getAuthorName());
            appendIndented(output, "Created", comment.getCreatedAt());
            appendIndented(output, "Updated", comment.getUpdatedAt());
            if (Boolean.TRUE.equals(comment.getVisibilityRestricted())) {
                appendIndented(output, "Visibility", "restricted");
            }
            appendIndented(output, "Body", comment.getBody());
            appendIndented(output, "Path", comment.getSourcePath());
        }
        output.append('\n');
    }

    private String displayName(String fallback, FieldMetadata metadata) {
        if (metadata != null && hasText(metadata.getName())) {
            return metadata.getName();
        }
        return nonBlank(fallback, "Unnamed Field");
    }

    private void appendLine(StringBuilder output, String label, String value) {
        output.append(label).append(": ").append(escaped(nonBlank(value, "unknown"))).append('\n');
    }

    private void appendIndented(StringBuilder output, String label, String value) {
        if (hasText(value)) {
            output.append("  ").append(label).append(": ").append(escaped(value)).append('\n');
        }
    }

    private void appendNested(StringBuilder output, String label, String value) {
        if (hasText(value)) {
            output.append("      ").append(label).append(": ").append(escaped(value)).append('\n');
        }
    }

    private String text(Boolean value) {
        return value == null ? null : String.valueOf(value);
    }

    private String text(Object value) {
        return value == null ? null : String.valueOf(value);
    }

    private String nonBlank(String value, String fallback) {
        return hasText(value) ? value : fallback;
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    private String escaped(String value) {
        if (value == null) {
            return "";
        }
        StringBuilder escaped = new StringBuilder(value.length());
        for (int index = 0; index < value.length(); index++) {
            char character = value.charAt(index);
            switch (character) {
                case '\\' -> escaped.append("\\\\");
                case '\n' -> escaped.append("\\n");
                case '\r' -> escaped.append("\\r");
                case '\t' -> escaped.append("\\t");
                default -> {
                    if (Character.isISOControl(character)) {
                        escaped.append(String.format("\\u%04x", (int) character));
                    } else {
                        escaped.append(character);
                    }
                }
            }
        }
        return escaped.toString();
    }
}
