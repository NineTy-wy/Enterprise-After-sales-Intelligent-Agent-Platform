package com.agentplatform.backend.agent.application;

/**
 * Spring 业务后端到 Python Agent 服务的调用端口。
 */
public interface AgentServiceClient {

    AgentIngestResult ingest(AgentIngestRequest request);

    AgentChatResponse chat(AgentChatRequest request);
}
