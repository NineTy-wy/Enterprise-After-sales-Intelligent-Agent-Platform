package com.agentplatform.backend.document.infrastructure.messaging;

import com.agentplatform.backend.document.application.DocumentProcessingMessage;
import com.agentplatform.backend.document.application.DocumentProcessingPublisher;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

/**
 * RabbitMQ 文档任务发布器。
 */
@Component
@ConditionalOnProperty(
        name = "app.messaging.enabled",
        havingValue = "true"
)
@Profile("rabbit")
public class RabbitDocumentProcessingPublisher
        implements DocumentProcessingPublisher {

    private final RabbitTemplate rabbitTemplate;
    private final String exchange;

    public RabbitDocumentProcessingPublisher(
            RabbitTemplate rabbitTemplate,
            @Value("${app.messaging.exchange}") String exchange
    ) {
        this.rabbitTemplate = rabbitTemplate;
        this.exchange = exchange;
    }

    @Override
    public void publish(DocumentProcessingMessage message) {
        rabbitTemplate.convertAndSend(exchange, "document.processing", message);
    }
}
