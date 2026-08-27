package com.agentplatform.backend.document.infrastructure.messaging;

import com.agentplatform.backend.document.application.DocumentProcessingJobService;
import com.agentplatform.backend.document.application.DocumentProcessingMessage;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

/**
 * RabbitMQ 文档任务消费者。
 */
@Component
@ConditionalOnProperty(
        name = "app.messaging.enabled",
        havingValue = "true"
)
@Profile("rabbit")
public class RabbitDocumentProcessingListener {

    private final DocumentProcessingJobService jobService;

    public RabbitDocumentProcessingListener(
            DocumentProcessingJobService jobService
    ) {
        this.jobService = jobService;
    }

    @RabbitListener(queues = "${app.messaging.document-processing-queue}")
    public void consume(DocumentProcessingMessage message) {
        jobService.process(message);
    }
}
