package com.yusuf.audittool.agent;

import com.yusuf.audittool.model.AgentOptions;

public interface AgentClient {

    String analyze(String prompt);

    default String analyze(String prompt, AgentOptions options) {
        return analyze(prompt);
    }
}
