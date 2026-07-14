package com.yusuf.audittool.checklist;

import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Component;

import com.yusuf.audittool.model.ChecklistContext;
import com.yusuf.audittool.model.ChecklistItem;

import tools.jackson.databind.JsonNode;

@Component
public class ChecklistMapper {

    public ChecklistContext map(JsonNode checklist) {
        ChecklistContext context = new ChecklistContext();

        if (checklist == null || checklist.isNull() || isEmptyValue(checklist)) {
            context.setProvided(false);
            return context;
        }

        context.setProvided(true);
        context.setRawText(toRawText(checklist));
        context.setItems(toItems(checklist));

        return context;
    }

    private String toRawText(JsonNode checklist) {
        if (checklist.isString()) {
            return checklist.asString();
        }

        if (checklist.isArray()) {
            List<String> lines = new ArrayList<>();
            for (int index = 0; index < checklist.size(); index++) {
                JsonNode item = checklist.get(index);
                String text = itemText(item);
                if (text != null && !text.isBlank()) {
                    lines.add(text);
                }
            }
            return String.join("\n", lines);
        }

        return checklist.toPrettyString();
    }

    private List<ChecklistItem> toItems(JsonNode checklist) {
        if (checklist.isArray()) {
            return arrayItems(checklist);
        }

        if (checklist.isString()) {
            return textItems(checklist.asString());
        }

        return List.of();
    }

    private List<ChecklistItem> arrayItems(JsonNode checklist) {
        List<ChecklistItem> items = new ArrayList<>();

        for (int index = 0; index < checklist.size(); index++) {
            JsonNode item = checklist.get(index);
            String text = itemText(item);
            if (text == null || text.isBlank()) {
                continue;
            }

            ChecklistItem checklistItem = new ChecklistItem();
            checklistItem.setId(itemId(item, index));
            checklistItem.setText(text);
            items.add(checklistItem);
        }

        return items;
    }

    private List<ChecklistItem> textItems(String rawText) {
        List<ChecklistItem> items = new ArrayList<>();

        String[] lines = rawText.split("\\R");
        for (int index = 0; index < lines.length; index++) {
            String text = cleanTextLine(lines[index]);
            if (text.isBlank()) {
                continue;
            }

            ChecklistItem item = new ChecklistItem();
            item.setId(String.valueOf(items.size() + 1));
            item.setText(text);
            items.add(item);
        }

        return items;
    }

    private String itemText(JsonNode item) {
        if (item == null || item.isNull()) {
            return null;
        }

        if (item.isString()) {
            return item.asString().trim();
        }

        if (item.isObject()) {
            for (String key : List.of("text", "title", "name", "description")) {
                JsonNode value = item.get(key);
                if (value != null && value.isString() && !value.asString().isBlank()) {
                    return value.asString().trim();
                }
            }
        }

        return item.toString();
    }

    private String itemId(JsonNode item, int index) {
        if (item != null && item.isObject()) {
            JsonNode id = item.get("id");
            if (id != null && !id.isNull() && !id.asString().isBlank()) {
                return id.asString();
            }
        }

        return String.valueOf(index + 1);
    }

    private boolean isEmptyValue(JsonNode checklist) {
        return (checklist.isString() && checklist.asString().isBlank())
                || (checklist.isArray() && checklist.isEmpty())
                || (checklist.isObject() && checklist.isEmpty());
    }

    private String cleanTextLine(String line) {
        return line.strip()
                .replaceFirst("^[-*]\\s+", "")
                .replaceFirst("^\\d+[.)]\\s+", "");
    }
}

