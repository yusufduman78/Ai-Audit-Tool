package com.yusuf.audittool.agent;

import com.yusuf.audittool.api.AgentEndpoint;

@FunctionalInterface
public interface AgentTransport {

    String send(String prompt, AgentEndpoint endpoint);
}
