package com.yusuf.audittool.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
class AnalyzeControllerTest {

    @Autowired
    private MockMvc mockMvc;

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
}
