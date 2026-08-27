package com.agentplatform.backend.document.application;

/**
 * 文档异步处理消息。
 */
public record DocumentProcessingMessage(
        String documentId,
        String tenantId,
        String knowledgeBaseId,
        String fileName,
        String fileType,
        String storagePath
) {
}
