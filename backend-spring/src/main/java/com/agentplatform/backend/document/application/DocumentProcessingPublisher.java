package com.agentplatform.backend.document.application;

/**
 * 文档处理任务发布端口。
 */
public interface DocumentProcessingPublisher {

    void publish(DocumentProcessingMessage message);
}
