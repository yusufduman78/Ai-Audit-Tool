package com.yusuf.audittool.prompt;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

@Component
public class PromptTemplateLoader {

    private final String templatePath;

    public PromptTemplateLoader(@Value("${prompt.template-path:prompts/core_auditor.md}") String templatePath) {
        this.templatePath = templatePath;
    }

    public String load() {
        ClassPathResource resource = new ClassPathResource(templatePath);
        try {
            return resource.getContentAsString(StandardCharsets.UTF_8);
        } catch (IOException exception) {
            throw new UncheckedIOException("Prompt template could not be loaded: " + templatePath, exception);
        }
    }
}
