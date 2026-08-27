package com.agentplatform.backend.agent.application;

/**
 * Agent 文档入库结果。
 */
public record AgentIngestResult(
        String status,
        int chunkCount,
        String message
) {
}
