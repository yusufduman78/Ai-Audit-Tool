package com.yusuf.audittool.prompt;

import org.springframework.stereotype.Component;

import com.yusuf.audittool.model.AgentContext;

@Component
public class PromptBuilder {

    private static final String CONTEXT_PLACEHOLDER = "{{CONTEXT}}";

    private final PromptTemplateLoader templateLoader;
    private final AgentContextRenderer contextRenderer;

    public PromptBuilder(PromptTemplateLoader templateLoader, AgentContextRenderer contextRenderer) {
        this.templateLoader = templateLoader;
        this.contextRenderer = contextRenderer;
    }

    public String build(AgentContext context) {
        String template = templateLoader.load();
        String renderedContext = contextRenderer.render(context);

        if (template.contains(CONTEXT_PLACEHOLDER)) {
            return template.replace(CONTEXT_PLACEHOLDER, renderedContext);
        }

        return template.stripTrailing() + "\n\nCONTEXT\n" + renderedContext;
    }
}
