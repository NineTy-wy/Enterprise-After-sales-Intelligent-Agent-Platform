package com.agentplatform.backend.document.infrastructure.messaging;

import com.agentplatform.backend.document.application.DocumentProcessingJobService;
import com.agentplatform.backend.document.application.DocumentProcessingMessage;
import com.agentplatform.backend.document.application.DocumentProcessingPublisher;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

/**
 * 本地异步任务发布器。
 *
 * <p>没有 RabbitMQ 时仍然通过线程池执行任务，适合开发和演示环境。</p>
 */
@Component
@Profile({"local & !rabbit", "postgres & !rabbit"})
public class LocalDocumentProcessingPublisher
        implements DocumentProcessingPublisher {

    private final DocumentProcessingJobService jobService;

    public LocalDocumentProcessingPublisher(
            DocumentProcessingJobService jobService
    ) {
        this.jobService = jobService;
    }

    @Override
    public void publish(DocumentProcessingMessage message) {
        jobService.processAsync(message);
    }
}
