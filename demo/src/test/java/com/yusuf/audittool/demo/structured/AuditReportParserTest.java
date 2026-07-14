package com.yusuf.audittool.demo.structured;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class AuditReportParserTest {

    private final AuditReportParser parser = new AuditReportParser();

    @Test
    void parsesStrictJson() {
        var report = parser.parse(validReport());

        assertTrue(report.isPresent());
        assertEquals("Audit completed.", report.get().getSummary());
    }

    @Test
    void extractsJsonObjectFromMarkdownWrappedOutput() {
        String output = "Here is the report:\n```json\n" + validReport() + "\n```";

        var report = parser.parse(output);

        assertTrue(report.isPresent());
        assertEquals("Keep evidence current.", report.get().getRecommendation());
    }

    @Test
    void acceptsOnlyTrailingCommaAsLenientSyntax() {
        String output = """
                {
                  "summary": "Audit completed.",
                  "findings": [],
                  "observations": [],
                  "recommendation": "Keep evidence current.",
                }
                """;

        var report = parser.parse(output);

        assertTrue(report.isPresent());
        assertEquals(0, report.get().getFindings().size());
    }

    @Test
    void doesNotInventMissingClosingStructure() {
        String output = "{\"summary\": \"Incomplete report\", \"findings\": [";

        assertFalse(parser.parse(output).isPresent());
    }

    private String validReport() {
        return """
                {
                  "summary": "Audit completed.",
                  "findings": [],
                  "observations": [],
                  "recommendation": "Keep evidence current."
                }
                """;
    }
}
