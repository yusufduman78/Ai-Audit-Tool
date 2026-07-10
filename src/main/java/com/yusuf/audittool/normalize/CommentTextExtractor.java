package com.yusuf.audittool.normalize;

import java.util.ArrayList;
import java.util.List;

import tools.jackson.databind.JsonNode;

final class CommentTextExtractor {

    boolean hasBody(JsonNode comment) {
        return extract(comment) != null;
    }

    String extract(JsonNode comment) {
        if (comment == null || !comment.isObject()) {
            return null;
        }

        JsonNode body = comment.get("body");
        if (hasText(body)) {
            return usableText(body.asString());
        }

        String richText = richText(body);
        if (richText != null) {
            return usableText(richText);
        }

        JsonNode renderedBody = comment.get("renderedBody");
        return hasText(renderedBody) ? usableText(renderedBody.asString()) : null;
    }

    private String richText(JsonNode body) {
        if (body == null || !body.isObject()) {
            return null;
        }

        List<String> fragments = new ArrayList<>();
        collectText(body, fragments);
        String text = String.join(" ", fragments).trim();
        return text.isBlank() ? null : text;
    }

    private void collectText(JsonNode node, List<String> fragments) {
        if (node == null || node.isNull()) {
            return;
        }
        if (node.isArray()) {
            for (int index = 0; index < node.size(); index++) {
                collectText(node.get(index), fragments);
            }
            return;
        }
        if (!node.isObject()) {
            return;
        }

        JsonNode text = node.get("text");
        if (hasText(text)) {
            fragments.add(text.asString().trim());
        }
        collectText(node.get("content"), fragments);
    }

    private String usableText(String text) {
        String value = text.trim();
        String normalized = value.toLowerCase();
        if (normalized.matches("https?://\\S+") || normalized.matches("www\\.\\S+")) {
            return null;
        }
        return value;
    }

    private boolean hasText(JsonNode node) {
        return node != null && node.isString() && !node.asString().isBlank();
    }
}
