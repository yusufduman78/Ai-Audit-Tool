package com.yusuf.audittool.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.yusuf.audittool.agent.AgentClient;
import com.yusuf.audittool.agent.AgentRuntimeException;

@SpringBootTest
@AutoConfigureMockMvc
class AnalyzeControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AgentClient agentClient;

    @Test
    void healthReturnsOk() throws Exception {
        mockMvc.perform(get("/api/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("OK"));
    }

    @Test
    void normalizeReturnsAgentContext() throws Exception {
        mockMvc.perform(post("/api/normalize")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "payload": {
                                    "key": "REQ-101",
                                    "fields": {
                                      "summary": "Login requirement",
                                      "status": {
                                        "name": "Done"
                                      },
                                      "acceptanceCriteria": "",
                                      "testEvidence": []
                                    }
                                  }
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.agentContext.activeFields[0].path").value("key"))
                .andExpect(jsonPath("$.agentContext.activeFields[1].path").value("fields.summary"))
                .andExpect(jsonPath("$.agentContext.activeFields[2].path").value("fields.status"))
                .andExpect(jsonPath("$.agentContext.emptyFields[0].path").value("fields.acceptanceCriteria"))
                .andExpect(jsonPath("$.agentContext.emptyFields[1].path").value("fields.testEvidence"))
                .andExpect(jsonPath("$.agentContext.statistics.activeFieldCount").value(3))
                .andExpect(jsonPath("$.agentContext.statistics.emptyFieldCount").value(2));
    }

    @Test
    void normalizeReturnsBadRequestWhenPayloadIsMissing() throws Exception {
        mockMvc.perform(post("/api/normalize")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Payload is required."))
                .andExpect(jsonPath("$.details").value("Request body must include a payload object."));
    }

    @Test
    void normalizeReturnsCommentContextWithoutDuplicatingCommentFields() throws Exception {
        mockMvc.perform(post("/api/normalize")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "payload": {
                                    "key": "REQ-101",
                                    "fields": {
                                      "summary": "Login requirement",
                                      "comment": {
                                        "startAt": 0,
                                        "total": 1,
                                        "comments": [
                                          {
                                            "body": "Test execution is pending approval.",
                                            "author": { "displayName": "Reviewer A" },
                                            "created": "2026-07-10T10:30:00.000+0000"
                                          }
                                        ]
                                      }
                                    }
                                  }
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.agentContext.activeFields[0].path").value("key"))
                .andExpect(jsonPath("$.agentContext.activeFields[1].path").value("fields.summary"))
                .andExpect(jsonPath("$.agentContext.commentContext.provided").value(true))
                .andExpect(jsonPath("$.agentContext.commentContext.coverage").value("FULL"))
                .andExpect(jsonPath("$.agentContext.commentContext.comments[0].body")
                        .value("Test execution is pending approval."));
    }

    @Test
    void analyzeBuildsPromptAndReturnsAgentOutput() throws Exception {
        when(agentClient.analyze(anyString())).thenReturn("Audit report");

        mockMvc.perform(post("/api/analyze")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "payload": {
                                    "key": "REQ-101",
                                    "fields": {
                                      "summary": "Login requirement"
                                    }
                                  }
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.agentOutput").value("Audit report"));

        verify(agentClient).analyze(contains("Login requirement"));
    }

    @Test
    void analyzeIncludesCommentsInTheAgentPrompt() throws Exception {
        when(agentClient.analyze(anyString())).thenReturn("Audit report");

        mockMvc.perform(post("/api/analyze")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "payload": {
                                    "key": "REQ-101",
                                    "fields": {
                                      "summary": "Login requirement",
                                      "comment": {
                                        "startAt": 0,
                                        "total": 1,
                                        "comments": [
                                          {
                                            "body": "Test execution is pending approval.",
                                            "author": { "displayName": "Reviewer A" },
                                            "created": "2026-07-10T10:30:00.000+0000"
                                          }
                                        ]
                                      }
                                    }
                                  }
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.agentOutput").value("Audit report"));

        verify(agentClient).analyze(contains("COMMENTS"));
        verify(agentClient).analyze(contains("Test execution is pending approval."));
        verify(agentClient).analyze(contains("Reviewer A"));
    }

    @Test
    void analyzeReturnsServiceUnavailableWhenAgentIsOffline() throws Exception {
        when(agentClient.analyze(anyString()))
                .thenThrow(new AgentRuntimeException("Agent runtime is not available."));

        mockMvc.perform(post("/api/analyze")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "payload": {
                                    "key": "REQ-101"
                                  }
                                }
                                """))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.error").value("Agent runtime is not available."))
                .andExpect(jsonPath("$.details").value("Could not connect to Ollama."));
    }

    @Test
    void analyzeExplainsWhenAgentReturnsAnEmptyResponse() throws Exception {
        when(agentClient.analyze(anyString()))
                .thenThrow(new AgentRuntimeException("Agent returned an empty response."));

        mockMvc.perform(post("/api/analyze")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "payload": {
                                    "key": "REQ-101"
                                  }
                                }
                                """))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.error").value("Agent returned an empty response."))
                .andExpect(jsonPath("$.details").value("The local model did not produce a usable output."));
    }
}
