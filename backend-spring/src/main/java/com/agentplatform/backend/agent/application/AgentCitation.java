package com.agentplatform.backend.agent.application;

/**
 * RAG 引用片段。
 */
public record AgentCitation(
        String documentId,
        String fileName,
        String chunkId,
        double score,
        String content
) {
}
