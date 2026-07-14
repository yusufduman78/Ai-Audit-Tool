package com.yusuf.audittool.demo.ollama;

import com.yusuf.audittool.demo.model.AgentOptions;

public interface AgentClient {

    String analyze(String prompt);

    default String analyze(String prompt, AgentOptions options) {
        return analyze(prompt);
    }
}
