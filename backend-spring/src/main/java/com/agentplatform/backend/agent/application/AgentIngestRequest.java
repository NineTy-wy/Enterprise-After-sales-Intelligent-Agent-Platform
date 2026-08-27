package com.agentplatform.backend.agent.application;

/**
 * 文档入库请求。
 */
public record AgentIngestRequest(
        String documentId,
        String tenantId,
        String knowledgeBaseId,
        String fileName,
        String fileType,
        String storagePath
) {
}
