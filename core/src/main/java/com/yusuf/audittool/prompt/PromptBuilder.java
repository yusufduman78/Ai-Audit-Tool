package com.yusuf.audittool.prompt;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.yusuf.audittool.model.AgentContext;

@Component
public class PromptBuilder {

    private static final String CONTEXT_PLACEHOLDER = "{{CONTEXT}}";
    private static final String OUTPUT_REQUIREMENTS_PLACEHOLDER = "{{OUTPUT_REQUIREMENTS}}";
    private static final String DEFAULT_OUTPUT_REQUIREMENTS_PATH = "prompts/output_json.md";

    private final PromptTemplateLoader templateLoader;
    private final AgentContextRenderer contextRenderer;
    private final PromptTemplateLoader outputRequirementsLoader;

    @Autowired
    public PromptBuilder(PromptTemplateLoader templateLoader, AgentContextRenderer contextRenderer) {
        this(
                templateLoader,
                contextRenderer,
                new PromptTemplateLoader(DEFAULT_OUTPUT_REQUIREMENTS_PATH)
        );
    }

    public PromptBuilder(
            PromptTemplateLoader templateLoader,
            AgentContextRenderer contextRenderer,
            PromptTemplateLoader outputRequirementsLoader
    ) {
        this.templateLoader = templateLoader;
        this.contextRenderer = contextRenderer;
        this.outputRequirementsLoader = outputRequirementsLoader;
    }

    public String build(AgentContext context) {
        String template = templateLoader.load();
        String renderedContext = contextRenderer.render(context);

        if (template.contains(OUTPUT_REQUIREMENTS_PLACEHOLDER)) {
            template = template.replace(
                    OUTPUT_REQUIREMENTS_PLACEHOLDER,
                    outputRequirementsLoader.load().strip()
            );
        }

        if (template.contains(CONTEXT_PLACEHOLDER)) {
            return template.replace(CONTEXT_PLACEHOLDER, renderedContext);
        }

        return template.stripTrailing() + "\n\nCONTEXT\n" + renderedContext;
    }
}
