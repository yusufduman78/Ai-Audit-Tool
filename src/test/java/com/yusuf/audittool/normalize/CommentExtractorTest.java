package com.yusuf.audittool.normalize;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import com.yusuf.audittool.model.CommentContext;
import com.yusuf.audittool.model.CommentCoverage;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

class CommentExtractorTest {

    private final CommentExtractor extractor = new CommentExtractor();
    private final JsonMapper jsonMapper = new JsonMapper();

    @Test
    void extractsJiraCommentsAndKeepsTheirRelationshipData() throws Exception {
        JsonNode payload = jsonMapper.readTree("""
                {
                  "fields": {
                    "comment": {
                      "startAt": 0,
                      "maxResults": 50,
                      "total": 1,
                      "comments": [
                        {
                          "id": "10001",
                          "body": "Verification was completed.",
                          "author": { "displayName": "Reviewer A" },
                          "created": "2026-07-10T10:30:00.000+0000",
                          "updated": "2026-07-10T11:00:00.000+0000",
                          "visibility": { "type": "role", "value": "Reviewers" }
                        }
                      ]
                    }
                  }
                }
                """);

        CommentExtraction extraction = extractor.extract(payload);
        CommentContext context = extraction.commentContext();

        assertTrue(context.isProvided());
        assertEquals(CommentCoverage.FULL, context.getCoverage());
        assertEquals(1, context.getIncludedCount());
        assertEquals("Verification was completed.", context.getComments().getFirst().getBody());
        assertEquals("Reviewer A", context.getComments().getFirst().getAuthorName());
        assertEquals(true, context.getComments().getFirst().getVisibilityRestricted());
        assertTrue(extraction.excludedPathPrefixes().contains("fields.comment"));
    }

    @Test
    void extractsCloudRichTextAndUsesGenericCommentFallback() throws Exception {
        JsonNode payload = jsonMapper.readTree("""
                {
                  "comments": [
                    {
                      "body": {
                        "type": "doc",
                        "content": [
                          {
                            "type": "paragraph",
                            "content": [
                              { "type": "text", "text": "Test" },
                              { "type": "text", "text": " evidence is ready." }
                            ]
                          }
                        ]
                      },
                      "author": { "name": "Reviewer B" }
                    }
                  ]
                }
                """);

        CommentContext context = extractor.extract(payload).commentContext();

        assertEquals(CommentCoverage.UNKNOWN, context.getCoverage());
        assertEquals("Test evidence is ready.", context.getComments().getFirst().getBody());
        assertEquals("Reviewer B", context.getComments().getFirst().getAuthorName());
    }

    @Test
    void marksPagedCommentsAsPartialAndLeavesMissingCommentsUnprovided() throws Exception {
        JsonNode partialPayload = jsonMapper.readTree("""
                {
                  "fields": {
                    "comment": {
                      "startAt": 50,
                      "total": 51,
                      "comments": [ { "body": "Latest comment" } ]
                    }
                  }
                }
                """);
        JsonNode noCommentPayload = jsonMapper.readTree("""
                {
                  "fields": { "summary": "No comments" }
                }
                """);

        assertEquals(CommentCoverage.PARTIAL, extractor.extract(partialPayload).commentContext().getCoverage());
        assertFalse(extractor.extract(noCommentPayload).commentContext().isProvided());
    }
}
