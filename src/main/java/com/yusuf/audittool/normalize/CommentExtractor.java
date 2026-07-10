package com.yusuf.audittool.normalize;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.springframework.stereotype.Component;

import com.yusuf.audittool.model.AuditComment;
import com.yusuf.audittool.model.CommentContext;
import com.yusuf.audittool.model.CommentCoverage;

import tools.jackson.databind.JsonNode;

@Component
public class CommentExtractor {

    private final CommentTextExtractor textExtractor = new CommentTextExtractor();

    public CommentExtraction extract(JsonNode payload) {
        CommentSource source = findSource(payload);
        if (source == null) {
            return new CommentExtraction(emptyContext(), Set.of());
        }

        List<AuditComment> comments = readComments(source.comments(), source.path());
        CommentContext context = new CommentContext();
        context.setProvided(true);
        context.setSourcePath(source.path());
        context.setTotalCount(integerValue(source.pagination(), "total"));
        context.setIncludedCount(comments.size());
        context.setCoverage(resolveCoverage(source.pagination(), source.comments().size()));
        context.setComments(comments);

        return new CommentExtraction(context, Set.of(source.excludedPathPrefix()));
    }

    private CommentSource findSource(JsonNode payload) {
        if (payload == null || payload.isNull()) {
            return null;
        }

        JsonNode jiraComment = valueAt(payload, "fields.comment");
        if (jiraComment != null && jiraComment.isObject() && isCommentArray(jiraComment.get("comments"))) {
            return new CommentSource(
                    jiraComment.get("comments"),
                    jiraComment,
                    "fields.comment.comments",
                    "fields.comment"
            );
        }

        for (String path : List.of("comment.comments", "fields.comments", "comments")) {
            JsonNode comments = valueAt(payload, path);
            if (isCommentArray(comments)) {
                return new CommentSource(comments, null, path, path);
            }
        }

        return null;
    }

    private boolean isCommentArray(JsonNode node) {
        if (node == null || !node.isArray()) {
            return false;
        }
        if (node.isEmpty()) {
            return true;
        }

        for (int index = 0; index < node.size(); index++) {
            if (textExtractor.hasBody(node.get(index))) {
                return true;
            }
        }
        return false;
    }

    private List<AuditComment> readComments(JsonNode comments, String sourcePath) {
        List<AuditComment> result = new ArrayList<>();

        for (int index = 0; index < comments.size(); index++) {
            JsonNode node = comments.get(index);
            String body = textExtractor.extract(node);
            if (body == null) {
                continue;
            }

            AuditComment comment = new AuditComment();
            comment.setId(text(node, "id"));
            comment.setBody(body);
            comment.setAuthorName(authorName(node));
            comment.setCreatedAt(text(node, "created"));
            comment.setUpdatedAt(text(node, "updated"));
            comment.setVisibilityRestricted(hasRestrictedVisibility(node));
            comment.setSourcePath(sourcePath + "[" + index + "]");
            result.add(comment);
        }

        return result;
    }

    private String authorName(JsonNode comment) {
        JsonNode author = comment == null ? null : comment.get("author");
        if (author != null && author.isString() && !author.asString().isBlank()) {
            return author.asString().trim();
        }
        return text(author, "displayName", "name");
    }

    private Boolean hasRestrictedVisibility(JsonNode comment) {
        if (comment == null || !comment.isObject()) {
            return null;
        }
        JsonNode visibility = comment.get("visibility");
        return visibility == null || visibility.isNull() ? null : true;
    }

    private CommentCoverage resolveCoverage(JsonNode pagination, int returnedCommentCount) {
        Integer total = integerValue(pagination, "total");
        Integer startAt = integerValue(pagination, "startAt");
        if (total == null || startAt == null || total < startAt + returnedCommentCount) {
            return CommentCoverage.UNKNOWN;
        }
        if (startAt > 0 || total > returnedCommentCount) {
            return CommentCoverage.PARTIAL;
        }
        return CommentCoverage.FULL;
    }

    private Integer integerValue(JsonNode node, String fieldName) {
        JsonNode value = node == null || !node.isObject() ? null : node.get(fieldName);
        if (value == null || !value.isNumber()) {
            return null;
        }

        int number = value.asInt();
        return number < 0 ? null : number;
    }

    private CommentContext emptyContext() {
        CommentContext context = new CommentContext();
        context.setCoverage(CommentCoverage.UNKNOWN);
        return context;
    }

    private JsonNode valueAt(JsonNode payload, String path) {
        JsonNode current = payload;
        for (String part : path.split("\\.")) {
            if (current == null || current.isNull()) {
                return null;
            }
            current = current.get(part);
        }
        return current;
    }

    private String text(JsonNode node, String... fieldNames) {
        if (node == null || !node.isObject()) {
            return null;
        }
        for (String fieldName : fieldNames) {
            JsonNode value = node.get(fieldName);
            if (value != null && !value.isNull() && !value.isObject() && !value.isArray()) {
                String text = value.asString().trim();
                if (!text.isBlank()) {
                    return text;
                }
            }
        }
        return null;
    }

    private record CommentSource(
            JsonNode comments,
            JsonNode pagination,
            String path,
            String excludedPathPrefix
    ) {
    }
}
